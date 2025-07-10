package com.xm.sanvanfo.trandiscore.session;

import lombok.Data;

import java.util.List;

@SuppressWarnings({"WeakerAccess"})
@Data
public class TransactionType {
    private Class mainType;
    private List<TransactionType> childrenType;
}
