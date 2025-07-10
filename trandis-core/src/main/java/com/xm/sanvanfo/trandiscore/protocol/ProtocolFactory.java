package com.xm.sanvanfo.trandiscore.protocol;

import com.xm.sanvanfo.trandiscore.protocol.request.*;
import com.xm.sanvanfo.trandiscore.protocol.response.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.*;

public class ProtocolFactory {
    private static Map<Integer, Class> registers = new ConcurrentHashMap<>();

    static {
        ProtocolFactory.register(TYPE_BRANCH_BEGIN_MSG_RESPONSE.getCode(), BranchBeginResponse.class);
        ProtocolFactory.register(TYPE_BRANCH_COMMIT_MSG_RESPONSE.getCode(), BranchCommitResponse.class);
        ProtocolFactory.register(TYPE_BRANCH_ROLLBACK_MSG_RESPONSE.getCode(), BranchRollbackResponse.class);
        ProtocolFactory.register(TYPE_GET_TRANSACTION_INFO_RESPONSE.getCode(), GetTransactionInfoResponse.class);
        ProtocolFactory.register(TYPE_HEARTBEAT_MSG_RESPONSE.getCode(), HeartbeatMessageResponse.class);
        ProtocolFactory.register(TYPE_MERGE_MSG_RESPONSE.getCode(), MergeMessageResponse.class);
        ProtocolFactory.register(TYPE_NOT_FOND_MSG_RESPONSE.getCode(), NotFountProcessorResponse.class);
        ProtocolFactory.register(TYPE_SERVER_TO_BRANCH_COMMIT_MSG_RESPONSE.getCode(), ServerToBranchCommitResponse.class);
        ProtocolFactory.register(TYPE_SERVER_TO_BRANCH_ROLLBACK_MSG_RESPONSE.getCode(), ServerToBranchRollbackResponse.class);
        ProtocolFactory.register(TYPE_SERVER_TO_BRANCH_SECTION_NUMBER_MSG_RESPONSE.getCode(), ServerToBranchSectionNumberResponse.class);
        ProtocolFactory.register(TYPE_CREATE_TRANSACTION_MSG_RESPONSE.getCode(), TransactionCreateResponse.class);
        ProtocolFactory.register(TYPE_BRANCH_BEGIN_MSG_REQUEST.getCode(), BranchBeginRequest.class);
        ProtocolFactory.register(TYPE_BRANCH_COMMIT_MSG_REQUEST.getCode(), BranchCommitRequest.class);
        ProtocolFactory.register(TYPE_BRANCH_ROLLBACK_MSG_REQUEST.getCode(), BranchRollbackRequest.class);
        ProtocolFactory.register(TYPE_GET_TRANSACTION_INFO.getCode(), GetTransactionInfoRequest.class);
        ProtocolFactory.register(TYPE_HEARTBEAT_MSG_REQUEST.getCode(), HeartbeatMessageRequest.class);
        ProtocolFactory.register(TYPE_MERGE_MSG_REQUEST.getCode(), MergeMessageRequest.class);
        ProtocolFactory.register(TYPE_SERVER_TO_BRANCH_COMMIT_MSG_REQUEST.getCode(), ServerToBranchCommitRequest.class);
        ProtocolFactory.register(TYPE_SERVER_TO_BRANCH_ROLLBACK_MSG_REQUEST.getCode(), ServerToBranchRollbackRequest.class);
        ProtocolFactory.register(TYPE_SERVER_TO_BRANCH_SECTION_NUMBER_MSG_REQUEST.getCode(), ServerToBranchSectionNumberRequest.class);
        ProtocolFactory.register(TYPE_CREATE_TRANSACTION_MSG_REQUEST.getCode(), TransactionCreateRequest.class);
        ProtocolFactory.register(TYPE_LOCK_RESOURCE_REQUEST.getCode(), BranchLockResourceRequest.class);
        ProtocolFactory.register(TYPE_LOCK_RESOURCE_RESPONSE.getCode(), BranchLockResourceResponse.class);
        ProtocolFactory.register(TYPE_GET_TRANSACTION_LOCKS_REQUEST.getCode(), GetTransactionLocksRequest.class);
        ProtocolFactory.register(TYPE_GET_TRANSACTION_LOCKS_RESPONSE.getCode(), GetTransactionLocksResponse.class);
        ProtocolFactory.register(TYPE_PLUGIN_REQUEST.getCode(), PluginRequest.class);
        ProtocolFactory.register(TYPE_PLUGIN_RESPONSE.getCode(), PluginResponse.class);
    }

    public static void register(Integer code, Class clazz) {
        registers.put(code, clazz);
    }

    public static Class get(Integer code) {
        Class clazz =  registers.get(code);
        if(null == clazz) {
            return LinkedHashMap.class;
        }
        return clazz;
    }
}
