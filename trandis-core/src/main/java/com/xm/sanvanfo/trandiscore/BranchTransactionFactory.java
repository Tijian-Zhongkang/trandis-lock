package com.xm.sanvanfo.trandiscore;

import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.trandiscore.IBranchTransactionCreator;
import com.xm.sanvanfo.trandiscore.TransactionInfo;
import com.xm.sanvanfo.trandiscore.constant.TransactionLevelEx;
import com.xm.sanvanfo.trandiscore.session.GlobalTransactionInfo;
import com.xm.sanvanfo.trandiscore.session.TransactionType;
import com.xm.sanvanfo.trandiscore.transaction.BranchTransaction;
import com.xm.sanvanfo.trandiscore.transaction.CollectionBranchTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CustomPlugin(registerClass = IBranchTransactionCreator.class, name = "trandisFactory")
public class BranchTransactionFactory implements IBranchTransactionFactoryCreator {

    private final String applicationId;
    private final TransactionLevelEx level;
    private final String clientId;
    private final Map<Class<? extends BranchTransaction>, IBranchTransactionCreator> creatorMap = new ConcurrentHashMap<>();

    public BranchTransactionFactory(String applicationId, TransactionLevelEx level, String clientId) {
        this.applicationId = applicationId;
        this.level = level;
        this.clientId = clientId;
    }

    @Override
    public void register(Class<? extends BranchTransaction> transactionType, IBranchTransactionCreator creator) {
        creatorMap.put(transactionType, creator);
    }

    @Override
    public TransactionInfo create(GlobalTransactionInfo transactionInfo, Boolean trunk) {

        TransactionType transactionType = transactionInfo.getBranchTransactionType();
        if(transactionType.getMainType().equals(CollectionBranchTransaction.class)) {
            TransactionInfo info = transform(transactionInfo);
            int sub = 0;
            List<BranchTransaction> children = new ArrayList<>();
            for (TransactionType clazz:transactionType.getChildrenType()
            ) {
                sub++;
                transactionInfo.setBranchTransactionType(clazz);
                transactionInfo.setTransactionName(transactionInfo.getTransactionName() + "-sub" + sub);
                children.add(create(transactionInfo, trunk).getBranchTransaction());
            }
             info.setBranchTransaction(new CollectionBranchTransaction(info, children, level));
            return info;
        }
        else {
            return creatorMap.get(transactionType.getMainType()).create(transactionInfo, trunk);
        }
    }

    @Override
    public String applicationId() {
        return applicationId;
    }

    @Override
    public String clientId() {
        return clientId;
    }


}
