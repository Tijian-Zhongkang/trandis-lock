package com.xm.sanvanfo.trandiscore.constant;

public enum TransactionStatusEx {

    /**
     * Un known global status.
     */
    // Unknown
    UnKnown(0),

    /**
     * The Begin.
     */
    // PHASE 1: can accept new branch registering.
    Begin(1),

    BeginFail(2),

    /**
     * PHASE 2: Running Status: may be changed any time.
     */
    // Committing.
    Committing(3),

    /**
     * The Commit retrying.
     */
    // Retrying commit after a recoverable failure.
    CommitRetrying(4),

    /**
     * Rollbacking global status.
     */
    // Rollbacking
    Rollbacking(5),

    /**
     * The Rollback retrying.
     */
    // Retrying rollback after a recoverable failure.
    RollbackRetrying(6),

    /**
     * The Timeout rollbacking.
     */
    // Rollbacking since timeout
    TimeoutRollbacking(7),

    /**
     * The Timeout rollback retrying.
     */
    // Retrying rollback (since timeout) after a recoverable failure.
    TimeoutRollbackRetrying(8),

    /**
     * All branches can be async committed. The committing is NOT done yet, but it can be seen as committed for TM/RM
     * client.
     */
    AsyncCommitting(9),

    /**
     * PHASE 2: Final Status: will NOT change any more.
     */
    // Finally: global transaction is successfully committed.
    Committed(10),

    /**
     * The Commit failed.
     */
    // Finally: failed to commit
    CommitFailed(11),

    /**
     * commit fail throwing
     */
    CommitFailedThrowing(12),

    /**
     * The Rollbacked.
     */
    // Finally: global transaction is successfully rollbacked.
    Rollbacked(13),

    /**
     * The Rollback failed.
     */
    // Finally: failed to rollback
    RollbackFailed(14),

    /**
     * The Timeout rollbacked.
     */
    // Finally: global transaction is successfully rollbacked since timeout.
    TimeoutRollbacked(15),

    /**
     * The Timeout rollback failed.
     */
    // Finally: failed to rollback since timeout
    TimeoutRollbackFailed(16),

    /**
     * The Finished.
     */
    // Not managed in session MAP any more
    Finished(17);

    private int code;

    TransactionStatusEx(int c) {
        this.code = c;
    }

    public int getCode() {
        return code;
    }

    public static TransactionStatusEx get(int code) {

        return TransactionStatusEx.values()[code];
    }
}
