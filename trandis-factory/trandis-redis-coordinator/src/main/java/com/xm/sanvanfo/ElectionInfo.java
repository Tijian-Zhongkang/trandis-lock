package com.xm.sanvanfo;

import lombok.Data;

@Data
public class ElectionInfo {
    private Long createTime;
    private Integer roundNumber;
    private Integer votesNumber;
}
