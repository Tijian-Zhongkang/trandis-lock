package com.xm.sanvanfo.trandiscore.netty.balancer;

import com.xm.sanvanfo.common.plugin.IPlugin;

import java.util.List;

public interface Balancer extends IPlugin {

    enum BalanceType {
        RandomBalancer(0),
        RoundRobinBalancer(1),
        ConsistentHashBalancer(2),
        LeastActiveBalancer(3);

        private int code;

        BalanceType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static BalanceType getType(int code) {
            return BalanceType.values()[code];
        }

    }

   <T> List<T> getAvailableInvokers(String clusterName) throws Exception;

   <T> T select(String clusterName, String xid, boolean cached, boolean checkExists) throws Exception;

   void clearCache(String xid, boolean removeStore);


   void setVirtualNodeNumber(Integer num);
}
