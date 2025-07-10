package com.xm.sanvanfo.trandiscore.protocol;

public interface MessageTypeAware {

     enum MessageType {

         TYPE_CREATE_TRANSACTION_MSG_REQUEST(20),

         TYPE_CREATE_TRANSACTION_MSG_RESPONSE(30),

         TYPE_HEARTBEAT_MSG_REQUEST(120),

         TYPE_HEARTBEAT_MSG_RESPONSE(130),

         TYPE_SERVER_TO_BRANCH_COMMIT_MSG_REQUEST(420),

         TYPE_SERVER_TO_BRANCH_COMMIT_MSG_RESPONSE(430),

         TYPE_SERVER_TO_BRANCH_ROLLBACK_MSG_REQUEST(520),

         TYPE_SERVER_TO_BRANCH_ROLLBACK_MSG_RESPONSE(530),

         TYPE_SERVER_TO_BRANCH_SECTION_NUMBER_MSG_REQUEST(620),

         TYPE_SERVER_TO_BRANCH_SECTION_NUMBER_MSG_RESPONSE(630),

         TYPE_BRANCH_BEGIN_MSG_REQUEST(720),

         TYPE_BRANCH_BEGIN_MSG_RESPONSE(730),

         TYPE_BRANCH_COMMIT_MSG_REQUEST(820),

         TYPE_BRANCH_COMMIT_MSG_RESPONSE(830),

         TYPE_BRANCH_ROLLBACK_MSG_REQUEST(920),

         TYPE_BRANCH_ROLLBACK_MSG_RESPONSE(930),

         TYPE_GET_TRANSACTION_INFO(1020),

         TYPE_GET_TRANSACTION_INFO_RESPONSE(1030),

         TYPE_LOCK_RESOURCE_REQUEST(1120),

         TYPE_LOCK_RESOURCE_RESPONSE(1130),

         TYPE_GET_TRANSACTION_LOCKS_REQUEST(1220),

         TYPE_GET_TRANSACTION_LOCKS_RESPONSE(1320),

         TYPE_PLUGIN_REQUEST(9920),

         TYPE_PLUGIN_RESPONSE(9930),

         TYPE_MERGE_MSG_REQUEST(121),

         TYPE_NOT_FOND_MSG_RESPONSE(139),

         TYPE_MERGE_MSG_RESPONSE(131);



         private int code;
          MessageType(int code) {
             this.code = code;
         }

         public int getCode() {
              return code;
         }

         public static MessageType get(int code) {

             return MessageType.values()[code];
         }
    }

    MessageType getMessageType();
}
