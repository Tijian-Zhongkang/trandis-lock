package com.xm.sanvanfo.trandiscore.netty;

@SuppressWarnings({"unused"})
public class NettyPoolKey {
    private TransactionRole transactionRole;
    private String applicationId;
    private String address;
    private String clientId;

    /**
     * Instantiates a new Netty pool key.
     *
     * @param transactionRole the client role
     * @param applicationId         the address
     */
    public NettyPoolKey(TransactionRole transactionRole, String applicationId, String address, String clientId) {
        this.transactionRole = transactionRole;
        this.applicationId = applicationId;
        this.address = address;
        this.clientId = clientId;
    }


    /**
     * Gets get client role.
     *
     * @return the get client role
     */
    @SuppressWarnings("WeakerAccess")
    public TransactionRole getTransactionRole() {
        return transactionRole;
    }

    /**
     * Sets set client role.
     *
     * @param transactionRole the client role
     * @return the client role
     */
    public NettyPoolKey setTransactionRole(TransactionRole transactionRole) {
        this.transactionRole = transactionRole;
        return this;
    }

    /**
     * Gets get address.
     *
     * @return the get address
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Sets set address.
     *
     * @param applicationId the serverGroup
     * @return the key
     */
    public NettyPoolKey setApplicationId(String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAddress() {return  address;}

    public NettyPoolKey setAddress(String address) {
        this.address = address;
        return this;
    }
    @Override
    public String toString() {
        return "transactionRole:" +
                transactionRole.name() +
                "," +
                "applicationId:" +
                applicationId +
                "," +
                "address:" +
                address +
                "clientId:" +
                clientId;
    }

    @Override
    public boolean equals(Object key) {
        if(!(key instanceof NettyPoolKey)) {
            return false;
        }
        NettyPoolKey nettyPoolKey = (NettyPoolKey)key;
        return nettyPoolKey.getApplicationId().equals(this.getApplicationId()) &&
                nettyPoolKey.getAddress().equals(this.getAddress()) &&
                nettyPoolKey.getTransactionRole().equals(this.getTransactionRole());
    }

    @Override
    public  int hashCode() {
       int result = 1;
       result += (result *59 + (null == transactionRole ? 43 : transactionRole.hashCode()));
       result += (result *59 + (null == applicationId ? 43 : applicationId.hashCode()));
       result += (result *59 + (null == address ? 43 : address.hashCode()));
       result += (result *59 + (null == clientId ? 43 : clientId.hashCode()));
       return result;

    }

    /**
     * The enum Client role.
     */
    public enum TransactionRole {

        /**
         * client
         */
        CLIROLE(1),
        /**
         * server
         */
        SERVERROLE(2),

        NOTRANSACTIONCLI(3),

        NOTRANSACTONSERVER(4);

        TransactionRole(int value) {
            this.value = value;
        }

        /**
         * Gets value.
         *
         * @return value value
         */
        public int getValue() {
            return value;
        }

        /**
         * value
         */
        private int value;
    }
}
