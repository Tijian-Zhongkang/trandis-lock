package com.xm.sanvanfo.trandiscore;

import com.xm.sanvanfo.trandiscore.transaction.BranchTransaction;
import lombok.Data;


@Data
public class TransactionInfo {
    private String xid;
    private String transactionName;
    private String applicationId;
    private Long timeout;
    private String groupName;
    private BranchTransaction branchTransaction;
    private Integer retryTimes;
    private Integer currentSection;
    private Boolean asyncCommit  = true;
    private Integer asyncThreadNum = 5;

}
