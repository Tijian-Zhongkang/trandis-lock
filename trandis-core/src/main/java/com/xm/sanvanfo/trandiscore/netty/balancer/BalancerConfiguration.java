package com.xm.sanvanfo.trandiscore.netty.balancer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
class BalancerConfiguration {
    private Balancer.BalanceType balanceType;
    private Integer virtualNodeNumber;
}
