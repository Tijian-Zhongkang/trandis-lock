package com.xm.sanvanfo.trandiscore.netty.processor.server;

import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.NetUtils;
import com.xm.sanvanfo.common.utils.ObjectHolder;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.*;
import com.xm.sanvanfo.trandiscore.protocol.AbstractMessage;
import com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware;
import com.xm.sanvanfo.trandiscore.protocol.ProtocolConstants;
import com.xm.sanvanfo.trandiscore.protocol.request.MergeMessageRequest;
import com.xm.sanvanfo.trandiscore.protocol.response.MergeMessageResponse;
import com.xm.sanvanfo.trandiscore.protocol.response.NotFountProcessorResponse;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
public class MergeMessageProcessor extends ServerSendResponseProcessor {

    public MergeMessageProcessor(AbstractNettyRemotingServer server, String serverId, ExecutorService executorService) {
        super(server, serverId, executorService);
    }

    @Override
    protected MessageTypeAware processToResponse(ChannelHandlerContext ctx, RpcMessage rpcMessage, ObjectHolder<NettyChannel> channelHolder,
                                             boolean updateChannel)  {

        Object obj = rpcMessage.getBody();
        MergeMessageResponse response = new MergeMessageResponse();
        response.setIds(new ArrayList<>());
        response.setResults(new ArrayList<>());
        try {
            Asserts.isTrue(obj instanceof MergeMessageRequest);
            MergeMessageRequest request = (MergeMessageRequest) obj;
            List<AbstractMessage> list = request.getMsgs();
            List<Integer> ids = request.getIds();
            int length = list.size();
            if(updateChannel) {
                NettyPoolKey key = new NettyPoolKey(NettyPoolKey.TransactionRole.CLIROLE, request.getApplicationId(),
                        NetUtils.toStringAddress(ctx.channel().remoteAddress()), request.getClientId());
                NettyChannel channel = new NettyChannel(key, ctx.channel());
                channel = server.updateChannel(channel);
                channelHolder.setObj(channel);
            }
            for (int i = 0; i < length; i++
            ) {
                AbstractMessage message = list.get(i);
                Integer id = ids.get(i);
                RemotingProcessor processor = server.getProcessor(message.getMessageType().getCode());
                if (null == processor) {
                    NotFountProcessorResponse elResponse = new NotFountProcessorResponse();
                    elResponse.setType(message.getMessageType().getCode());
                    elResponse.setCode(404);
                    elResponse.setMsg(message.getMessageType().toString() + "dos not has processor");
                    response.getIds().add(id);
                    response.getResults().add(elResponse);

                } else {
                    RpcMessage rpcMess = convertRpcMessage(id, message, rpcMessage);
                    if (ServerSendResponseProcessor.class.isAssignableFrom(processor.getClass())) {
                        MessageTypeAware elResponse = ((ServerSendResponseProcessor)processor)
                                .processToResponse(ctx, rpcMess, channelHolder, false);
                        response.getIds().add(id);
                        response.getResults().add(elResponse);

                    } else {
                        processor.process(ctx, rpcMess);
                    }
                }
            }
        }
        catch (Exception ex) {
              log.error("MergeMessageProcessor process error:" + BusinessException.exceptionFullMessage(ex));
        }
        return response;
    }

    private RpcMessage convertRpcMessage(Integer id, AbstractMessage message, RpcMessage rpcMessage) {
        RpcMessage rpcMess = new RpcMessage();
        rpcMess.setCodec(rpcMessage.getCodec());
        rpcMess.setCompressor(rpcMessage.getCompressor());
        rpcMess.setBody(message);
        rpcMess.setId(id);
        rpcMess.setMessageType(ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
        return rpcMess;
    }
}
