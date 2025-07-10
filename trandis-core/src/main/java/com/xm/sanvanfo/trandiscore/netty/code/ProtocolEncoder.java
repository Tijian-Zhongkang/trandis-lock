package com.xm.sanvanfo.trandiscore.netty.code;

import com.xm.sanvanfo.common.plugin.PluginLoader;
import com.xm.sanvanfo.trandiscore.compress.Compressor;
import com.xm.sanvanfo.trandiscore.compress.CompressorFactory;
import com.xm.sanvanfo.trandiscore.netty.RpcMessage;
import com.xm.sanvanfo.trandiscore.protocol.ProtocolConstants;
import com.xm.sanvanfo.trandiscore.serializer.Serializer;
import com.xm.sanvanfo.trandiscore.serializer.SerializerType;
import com.xm.sanvanfo.trandiscore.serializer.SerializerTypeUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ProtocolEncoder extends MessageToByteEncoder {


    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        try {
            if (msg instanceof RpcMessage) {
                RpcMessage rpcMessage = (RpcMessage) msg;

                int fullLength = ProtocolConstants.V1_HEAD_LENGTH;
                int headLength = ProtocolConstants.V1_HEAD_LENGTH;

                byte messageType = rpcMessage.getMessageType();
                out.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
                out.writeByte(ProtocolConstants.VERSION);
                // full Length(4B) and head length(2B) will fix in the end.
                out.writerIndex(out.writerIndex() + 6);
                out.writeByte(messageType);
                out.writeByte(rpcMessage.getCodec());
                out.writeByte(rpcMessage.getCompressor());
                out.writeInt(rpcMessage.getBodyType());
                out.writeInt(rpcMessage.getId());

                // direct write head with zero-copy
                Map<String, String> headMap = rpcMessage.getHeadMap();
                if (headMap != null && !headMap.isEmpty()) {
                    int headMapBytesLength = HeadMapSerializer.INSTANCE().encode(headMap, out);
                    headLength += headMapBytesLength;
                    fullLength += headMapBytesLength;
                }


                Serializer serializer = PluginLoader.INSTANCE().load(Serializer.class, SerializerTypeUtils.getByCode(rpcMessage.getCodec()));
                byte[] bodyBytes = serializer.serialize(rpcMessage.getBody());
                Compressor compressor = CompressorFactory.getCompressor(rpcMessage.getCompressor());
                bodyBytes = compressor.compress(bodyBytes);
                fullLength += bodyBytes.length;
                out.writeBytes(bodyBytes);

                // fix fullLength and headLength
                int writeIndex = out.writerIndex();
                // skip magic code(2B) + version(1B)
                out.writerIndex(writeIndex - fullLength + 3);
                out.writeInt(fullLength);
                out.writeShort(headLength);
                out.writerIndex(writeIndex);
            } else {
                throw new UnsupportedOperationException("Not support this class:" + msg.getClass());
            }
        } catch (Throwable e) {
            log.error("Encode request error!", e);
            throw new Exception(e);
        }
    }
}
