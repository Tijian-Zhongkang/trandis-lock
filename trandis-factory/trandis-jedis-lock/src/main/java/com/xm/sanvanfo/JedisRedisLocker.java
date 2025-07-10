package com.xm.sanvanfo;

import com.xm.sanvanfo.common.utils.EncryptUtils;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.serializer.Serializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class JedisRedisLocker extends AbstractRedisLocker<byte[],byte[]> {

    private final JedisCluster jedisCluster;
    private final Jedis jedis;
    private static final String allNoneEx = "jedis and jedisCluster cannt be all null";
    public JedisRedisLocker(CoordinatorConfig config, Jedis jedis, com.xm.sanvanfo.interfaces.Serializer<byte[], byte[]> redisSerializer, Serializer objSerializer) {
        super(config, redisSerializer, objSerializer);
        this.jedis = jedis;
        this.jedisCluster = null;
    }

    public JedisRedisLocker(CoordinatorConfig config, JedisCluster jedisCluster, com.xm.sanvanfo.interfaces.Serializer<byte[], byte[]> redisSerializer, Serializer objSerializer) {
        super(config, redisSerializer, objSerializer);
        this.jedis = null;
        this.jedisCluster = jedisCluster;
    }

    @Override
    public <T> T execScript(String script, Class<T> clazz, Function<Object, T> func, List<Object> keys, List<Object> argv) throws Exception {
        String sha = EncryptUtils.sha1Encode(script);
        List<byte[]> k = keys.stream().map(o -> {
            String str = quote(o.toString(), config.getSpace());
            return redisSerializer.toKey(str);
        }).collect(Collectors.toList());
        List<byte[]> v = argv.stream().map(o->{
            String str = o.toString();
            return redisSerializer.toValue(str);
        }).collect(Collectors.toList());
        Object result;
        try {
            if(null != jedis) {
                result = jedis.evalsha(sha.getBytes(), k, v);
            }
            else if(null != jedisCluster) {
                result = jedisCluster.evalsha(sha.getBytes(), k, v);
            }
            else {
                throw new BusinessException(allNoneEx);
            }
        }
        catch (Exception ex) {
            if(exceptionNotContainsNoScriptError(ex)) {
                throw ex;
            }
            if(null != jedis) {
                result = jedis.eval(sha.getBytes(), k, v);
            }
            else if(null != jedisCluster) {
                result = jedisCluster.evalsha(sha.getBytes(), k, v);
            }
            else {
                throw new BusinessException(allNoneEx);
            }
        }
        if(null != func) {
            return func.apply(result);
        }
        return convertToT(result, clazz);
    }

    @Override
    public <T> LockerConvertFuture<T> execScriptAsync(String script, Class<T> clazz, Function<Object, T> func, List<Object> keys, List<Object> argv) throws Exception {
        return null;
    }

    @Override
    public void addNode(String nodeKey, String nodeId) {
        byte[] k = redisSerializer.toKey(quote(nodeKey, config.getSpace()));
        byte[] v = redisSerializer.toValue(nodeId);
        if(null != jedis) {
            jedis.sadd(k, v);
        }
        else if(null != jedisCluster) {
            jedisCluster.sadd(k, v);
        }
        else {
            throw new BusinessException(allNoneEx);
        }
    }

    @Override
    public void deleteNode(String nodeKey, String nodeId) {
        byte[] k = redisSerializer.toKey(quote(nodeKey, config.getSpace()));
        byte[] v = redisSerializer.toValue(nodeId);
        if(null != jedis) {
            jedis.srem(k, v);
        }
        else if(null != jedisCluster) {
            jedisCluster.srem(k, v);
        }
        else {
            throw new BusinessException(allNoneEx);
        }
    }

    @Override
    public List<String> getAllNode(String nodeKey) {
        byte[] k = redisSerializer.toKey(quote(nodeKey, config.getSpace()));
        Set<byte[]> sets;
        if(null != jedis) {
            sets = jedis.smembers(k);
        }
        else if(null != jedisCluster) {
            sets = jedisCluster.smembers(k);
        }
        else {
            throw new BusinessException(allNoneEx);
        }
        return sets.stream().map(redisSerializer::toValueString).collect(Collectors.toList());
    }

    @Override
    public void logLockInfo() {

    }
}
