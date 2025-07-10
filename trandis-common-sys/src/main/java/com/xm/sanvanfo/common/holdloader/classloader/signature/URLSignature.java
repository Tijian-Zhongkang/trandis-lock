package com.xm.sanvanfo.common.holdloader.classloader.signature;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class URLSignature {

    private static List<ISignature> signatures = Collections.synchronizedList(new ArrayList<>());

    static {
         signatures.add(new FileURLSignature());
         signatures.add(new PathURLSignature());
    }

    public static void register(ISignature signature) {
        register(signature, -1);
    }

    public static void register(ISignature signature, int pos) {
        if(-1 == pos || pos >= signatures.size()) {
            signatures.add(signature);
        }
        else {
            signatures.add(pos, signature);
        }
    }

    public static String sign(URL url) throws Exception {
        ISignature find = find(url);
        if(null == find) {
            throw new Exception("获取数字签名错误，没有找到签名处理类");
        }
        return find.sign(url);
    }

    private static ISignature find(URL url) {
        ISignature find = null;
        for (ISignature signature:signatures
        ) {
            if(signature.checkSignature(url)) {
                find = signature;
                break;
            }
        }
        return find;
    }
}
