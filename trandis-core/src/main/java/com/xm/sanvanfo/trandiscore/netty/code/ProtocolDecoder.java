package com.xm.sanvanfo.trandiscore.netty.code;

import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.compress.Compressor;
import com.xm.sanvanfo.trandiscore.compress.CompressorFactory;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.ProtocolFactory;
import com.xm.sanvanfo.trandiscore.protocol.ProtocolConstants;
import com.xm.sanvanfo.trandiscore.serializer.SerializerTypeUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import com.xm.sanvanfo.trandiscore.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@SuppressWarnings({"WeakerAccess"})
@Slf4j
public class ProtocolDecoder extends LengthFieldBasedFrameDecoder {

    public ProtocolDecoder() {
        // default is 8M
        this(ProtocolConstants.MAX_FRAME_LENGTH);
    }

    public ProtocolDecoder(int maxFrameLength) {
        super(maxFrameLength, 3, 4, -7, 0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) {
        Object decoded;
        try {
            decoded = super.decode(ctx, in);
            if (decoded instanceof ByteBuf) {
                ByteBuf frame = (ByteBuf)decoded;
                try {
                    return decodeFrame(frame);
                } finally {
                    frame.release();
                }
            }
        } catch (Exception exx) {
            log.error("Decode frame error, cause: {}", exx.getMessage());
            throw new BusinessException(exx, "decode error");
        }
        return decoded;
    }

    @SuppressWarnings("unchecked")
    public Object decodeFrame(ByteBuf frame) throws Exception {
        byte b0 = frame.readByte();
        byte b1 = frame.readByte();
        if (ProtocolConstants.MAGIC_CODE_BYTES[0] != b0
                || ProtocolConstants.MAGIC_CODE_BYTES[1] != b1) {
            throw new IllegalArgumentException("Unknown magic code: " + b0 + ", " + b1);
        }

        frame.readByte();
        // TODO version compatible here

        int fullLength = frame.readInt();
        short headLength = frame.readShort();
        byte messageType = frame.readByte();
        byte codecType = frame.readByte();
        byte compressorType = frame.readByte();
        int bodyType = frame.readInt();
        int requestId = frame.readInt();

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setCodec(codecType);
        rpcMessage.setId(requestId);
        rpcMessage.setCompressor(compressorType);
        rpcMessage.setMessageType(messageType);

        // direct read head with zero-copy
        int headMapLength = headLength - ProtocolConstants.V1_HEAD_LENGTH;
        if (headMapLength > 0) {
            Map<String, String> map = HeadMapSerializer.INSTANCE().decode(frame, headMapLength);
            rpcMessage.getHeadMap().putAll(map);
        }

        // read body
        int bodyLength = fullLength - headLength;
        if (bodyLength > 0) {
            byte[] bs = new byte[bodyLength];
            frame.readBytes(bs);
            Compressor compressor = CompressorFactory.getCompressor(compressorType);
            bs = compressor.decompress(bs);
            Serializer serializer = PluginLoader.INSTANCE().load(Serializer.class, SerializerTypeUtils.getByCode(rpcMessage.getCodec()));
            rpcMessage.setBody(serializer.deserialize(bs, ProtocolFactory.get(bodyType)));
        }

        return rpcMessage;
    }
}
