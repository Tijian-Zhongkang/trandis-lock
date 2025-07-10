package com.xm.sanvanfo.trandiscore.netty;


public class TransactionBranchClientManager {

    private static TransactionBranchClientManager manager;
    private final RemotingClient client;

    public static void init(RemotingClient client) {

        if(null != manager) {
            return;
        }
        synchronized (TransactionBranchClientManager.class) {
            if(null != manager) {
                return;
            }
            manager = new TransactionBranchClientManager(client);
        }
    }

    private TransactionBranchClientManager(RemotingClient client) {
        this.client = client;
    }

    public RemotingClient getRemotingClient() {
        return client;
    }

    public static TransactionBranchClientManager INSTANCE() {
        return manager;
    }
}
