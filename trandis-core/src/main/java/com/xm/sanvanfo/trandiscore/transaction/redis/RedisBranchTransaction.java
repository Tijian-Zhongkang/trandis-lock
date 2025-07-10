package com.xm.sanvanfo.trandiscore.transaction.redis;

import com.xm.sanvanfo.common.utils.EnvUtils;
import com.xm.sanvanfo.trandiscore.TransactionInfo;
import com.xm.sanvanfo.trandiscore.constant.TransactionLevelEx;
import com.xm.sanvanfo.trandiscore.constant.TransactionStatusEx;
import com.xm.sanvanfo.trandiscore.transaction.AbstractSingleBranchTransaction;
import com.xm.sanvanfo.trandiscore.transaction.TransactionException;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class RedisBranchTransaction extends AbstractSingleBranchTransaction {

    private RedisBranchTransactionConfiguration configuration;
    private RedisConnectionProvider provider;

    public RedisBranchTransaction(TransactionInfo transactionInfo, Boolean trunk,
                                  RedisConnectionProvider provider, RedisBranchTransactionConfiguration configuration, TransactionLevelEx levelEx) {
        super(transactionInfo, trunk, levelEx);
        this.provider = provider;
        this.configuration = configuration;
        this.clientId = EnvUtils.getClientId(this.configuration.getNetworkCardName(), this.configuration.getIpType(),
                this.configuration.getPort(), this.configuration.getApplicationId());
    }

    @Override
    public void begin() throws TransactionException {
         begin(configuration.getDefaultTimeout());
    }

    @Override
    public void begin(long timeout) throws TransactionException {
        begin(timeout, configuration.getDefaultGroupName());
    }

    @Override
    public void returnResource() {
        provider.closeConnection();
    }

    @Override
    protected void beginInner(long timeout, String name) throws TransactionException {

    }


    @Override
    protected void commitInner() throws TransactionException {

        provider.updateImage(getLocks(), transactionInfo.getXid());
    }

    @Override
    protected void rollbackInner() {
        executeUndoLogs();
        removeUndoLogs(transactionInfo.getCurrentSection());
    }

    @Override
    protected Integer getDefaultTimeout() {
        return configuration.getDefaultTimeout();
    }

    @Override
    protected void removeUndoLogs() {
        provider.flushUndoLog(transactionInfo.getXid(), transactionInfo.getTransactionName());
    }

    @Override
    protected void removeUndoLogs(int section) {
        provider.flushUndoLog(transactionInfo.getXid(), transactionInfo.getTransactionName(), section);
    }

    @Override
    protected void executeUndoLogs() {
        provider.executeUndoLog(transactionInfo.getXid(), transactionInfo.getTransactionName(), transactionInfo.getCurrentSection());
    }

    @Override
    protected void executeUndoLogs(int section) {
        provider.executeUndoLog(transactionInfo.getXid(), transactionInfo.getTransactionName(), section);
    }

    @Override
    protected void removeVersion(TransactionStatusEx statusEx) {
        provider.removeVersion(transactionInfo.getXid(), transactionInfo.getTransactionName(), statusEx);
    }

    @Override
    protected void removeVersion(int section, TransactionStatusEx statusEx) {
        provider.removeVersion(transactionInfo.getXid(), transactionInfo.getTransactionName(), statusEx, section);
    }


    @Override
    protected String getApplicationId() {
        return configuration.getApplicationId();
    }

    @Override
    public String getSpaceName() {
        return provider.getSpaceName();
    }


}
