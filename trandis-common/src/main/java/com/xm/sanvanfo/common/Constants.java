package com.xm.sanvanfo.common;

import java.nio.charset.Charset;

public class Constants {

    /**
     * The constant IP_PORT_SPLIT_CHAR.
     */
    public static final String IP_PORT_SPLIT_CHAR = ":";
    /**
     * The constant CLIENT_ID_SPLIT_CHAR.
     */
    public static final String CLIENT_ID_SPLIT_CHAR = ":";
    /**
     * The constant ENDPOINT_BEGIN_CHAR.
     */
    public static final String ENDPOINT_BEGIN_CHAR = "/";
    /**
     * The constant DBKEYS_SPLIT_CHAR.
     */
    public static final String DBKEYS_SPLIT_CHAR = ",";

    /**
     * The constant ROW_LOCK_KEY_SPLIT_CHAR.
     */
    public static final String ROW_LOCK_KEY_SPLIT_CHAR = ";";

    /**
     * the start time of transaction
     */
    public static final String START_TIME = "start-time";

    /**
     * app name
     */
    public static final String APP_NAME = "appName";

    /**
     * TCC start time
     */
    public static final String ACTION_START_TIME = "action-start-time";

    /**
     * TCC name
     */
    public static final String ACTION_NAME = "actionName";

    /**
     * Use TCC fence
     */
    public static final String USE_TCC_FENCE = "useTCCFence";

    /**
     * phase one method name
     */
    public static final String PREPARE_METHOD = "sys::prepare";

    /**
     * phase two commit method name
     */
    public static final String COMMIT_METHOD = "sys::commit";

    /**
     * phase two rollback method name
     */
    public static final String ROLLBACK_METHOD = "sys::rollback";

    /**
     * host ip
     */
    public static final String HOST_NAME = "host-name";

    /**
     * branch context
     */
    public static final String TCC_ACTION_CONTEXT = "actionContext";

    /**
     * default charset name
     */
    public static final String DEFAULT_CHARSET_NAME = "UTF-8";

    /**
     * default charset is utf-8
     */
    public static final Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_CHARSET_NAME);
    /**
     * The constant OBJECT_KEY_SPRING_APPLICATION_CONTEXT
     */
    public static final String OBJECT_KEY_SPRING_APPLICATION_CONTEXT = "springApplicationContext";
    /**
     * The constant BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER
     */
    public static final String BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER = "springApplicationContextProvider";
    /**
     * The constant BEAN_NAME_FAILURE_HANDLER
     */
    public static final String BEAN_NAME_FAILURE_HANDLER = "failureHandler";
    /**
     * The constant SAGA_TRANS_NAME_PREFIX
     */
    public static final String SAGA_TRANS_NAME_PREFIX = "$Saga_";

    /**
     * The constant RETRY_ROLLBACKING
     */
    public static final String RETRY_ROLLBACKING = "RetryRollbacking";

    /**
     * The constant RETRY_COMMITTING
     */
    public static final String RETRY_COMMITTING = "RetryCommitting";

    /**
     * The constant ASYNC_COMMITTING
     */
    public static final String ASYNC_COMMITTING = "AsyncCommitting";

    /**
     * The constant TX_TIMEOUT_CHECK
     */
    public static final String TX_TIMEOUT_CHECK = "TxTimeoutCheck";

    /**
     * The constant UNDOLOG_DELETE
     */
    public static final String UNDOLOG_DELETE = "UndologDelete";
}
