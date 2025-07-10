package com.xm.sanvanfo.trandiscore.protocol;

public class ProtocolConstants {

    /**
     * Magic code
     */
    public static  final  byte[] MAGIC_CODE_BYTES = {(byte) 0xda, (byte) 0xda};

    /**
     * Protocol version
     */
    public static  final  byte VERSION = 1;

    /**
     * Max frame length
     */
    public static  final  int MAX_FRAME_LENGTH = 8 * 1024 * 1024;

    /**
     * HEAD_LENGTH of protocol v1
     */
    public static  final  int V1_HEAD_LENGTH = 20;

    /**
     * Message type: Request
     */
    public static  final  byte MSGTYPE_RESQUEST_SYNC = 0;
    /**
     * Message type: Response
     */
    public static  final  byte MSGTYPE_RESPONSE = 1;
    /**
     * Message type: Request which no need response
     */
    public static  final  byte MSGTYPE_RESQUEST_ONEWAY = 2;

}
