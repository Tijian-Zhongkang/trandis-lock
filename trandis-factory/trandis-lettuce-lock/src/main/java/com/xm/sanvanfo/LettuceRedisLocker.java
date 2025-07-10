package com.xm.sanvanfo;


import com.xm.sanvanfo.common.utils.EncryptUtils;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.serializer.Serializer;
import io.lettuce.core.AbstractRedisAsyncCommands;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScriptOutputType;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.lang3.ClassUtils;


import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked"})
public class LettuceRedisLocker<K,V> extends AbstractRedisLocker<K,V> {


    private final AbstractRedisAsyncCommands<K, V> connection;
    private final ExecutorService consumeService;

    public LettuceRedisLocker(CoordinatorConfig config, AbstractRedisAsyncCommands<K, V> connection,
                              com.xm.sanvanfo.interfaces.Serializer<K,V> redisSerializer, Serializer objSerializer) {
        super(config, redisSerializer, objSerializer);
        this.connection = connection;
        consumeService = Executors.newSingleThreadExecutor(new DefaultThreadFactory("LettuceRedisLocker"));
    }

    @Override
    public <T> T execScript(String script, Class<T> clazz, Function<Object, T> func, List<Object> keys, List<Object> argv) throws Exception {
        String sha = EncryptUtils.sha1Encode(script);
        RedisFuture<Object> future;
        ScriptOutputType outputType = fromJavaType(clazz);
        K[] k = (K[]) keys.stream().map(o -> {
            String str = quote(o.toString(), config.getSpace());
            return redisSerializer.toKey(str);
        }).toArray();
        V[] v = (V[]) argv.stream().map(o->{
            String str = o.toString();
            return redisSerializer.toValue(str);
        }).toArray();
        Object result;
        try {
            future = v.length > 0 ? connection.evalsha(sha, outputType, k, v) : connection.evalsha(script, outputType, k);
            result = future.get();
        }
        catch (Exception ex) {
            if(exceptionNotContainsNoScriptError(ex)) {
                throw ex;
            }
            future = v.length > 0 ? connection.eval(script, outputType, k, v) : connection.eval(script, outputType, k);
            result = future.get();
        }
        if(null != func) {
            return func.apply(result);
        }
        return convertToT(result, outputType, clazz);

    }

    @Override
    public <T> LockerConvertFuture<T> execScriptAsync(String script, Class<T> clazz, Function<Object, T> func, List<Object> keys, List<Object> argv) throws Exception {
        String sha = EncryptUtils.sha1Encode(script);
        RedisFuture<Object> future;
        ScriptOutputType outputType = fromJavaType(clazz);
        K[] k = (K[]) keys.stream().map(o -> {
            String str = quote(o.toString(), config.getSpace());
            return redisSerializer.toKey(str);
        }).toArray();
        V[] v = (V[]) argv.stream().map(o->{
            String str = o.toString();
            return redisSerializer.toValue(str);
        }).toArray();
        future = v.length > 0 ? connection.evalsha(sha, outputType, k, v) : connection.evalsha(script, outputType, k);
        LockerConvertFuture.Scripture scripture = new LockerConvertFuture.Scripture(()->{
            RedisFuture<Object> scriptFuture = v.length > 0 ? connection.eval(script, outputType, k, v) : connection.eval(script, outputType, k);
            return new LockerConvertFuture<>(consumeService, null, scriptFuture, t->{
                try {
                    Object o = scriptFuture.get();
                    if(null != func) {
                        return func.apply(o);
                    }
                    return convertToT(o, outputType, clazz);
                }
                catch (Exception ex) {
                    throw new BusinessException(ex, "sha script error");
                }

            });
        });
        return new LockerConvertFuture<>(consumeService, scripture, future, t->{
            try {
                Object o = future.get();
                if(null != func) {
                    return func.apply(o);
                }
                return convertToT(o, outputType, clazz);
            }
            catch (Exception ex) {
                throw new BusinessException(ex, "sha script error");
            }

        });
    }

    @Override
    public void addNode(String nodeKey, String nodeId) {
        K k = redisSerializer.toKey(quote(nodeKey, config.getSpace()));
        V v = redisSerializer.toValue(nodeId);
        RedisFuture<Long> future = connection.sadd(k, v);
        try {
            future.get();
        }
        catch (InterruptedException| ExecutionException ex) {
            throw new BusinessException(ex, "lettuce addNode get error");
        }
    }

    @Override
    public void deleteNode(String nodeKey, String nodeId) {
        K k = redisSerializer.toKey(quote(nodeKey, config.getSpace()));
        V v = redisSerializer.toValue(nodeId);
        RedisFuture<Long> future = connection.srem(k, v);
        try {
            future.get();
        }
        catch (InterruptedException| ExecutionException ex) {
            throw new BusinessException(ex, "lettuce deleteNode get error");
        }
    }

    @Override
    public List<String> getAllNode(String nodeKey) {
        K k = redisSerializer.toKey(quote(nodeKey, config.getSpace()));
        RedisFuture<Set<V>> future = connection.smembers(k);
        try {
            Set<V> set = future.get();
            return set.stream().map(redisSerializer::toValueString).collect(Collectors.toList());
        }
        catch (InterruptedException| ExecutionException ex) {
            throw new BusinessException(ex, "lettuce getAllNode get error");
        }

    }

    @Override
    public void logLockInfo() {

    }

    @SuppressWarnings("unused")
    private <T> T convertToT(Object result, ScriptOutputType outputType, Class<T> clazz) {
        return convertToT(result, clazz);
    }

    private ScriptOutputType fromJavaType(Class<?> javaType) {

        if (javaType == null) {
            return ScriptOutputType.STATUS;
        }

        if (ClassUtils.isAssignable(List.class, javaType)
           || ClassUtils.isAssignable(Map.class, javaType)
           || ClassUtils.isAssignable(Set.class, javaType)) {
            return ScriptOutputType.MULTI;
        }

        if (ClassUtils.isAssignable(Boolean.class, javaType)) {
            return ScriptOutputType.BOOLEAN;
        }

        if (ClassUtils.isAssignable(Long.class, javaType)
            || ClassUtils.isAssignable(Integer.class, javaType)) {
            return ScriptOutputType.INTEGER;
        }
        return ScriptOutputType.VALUE;
    }
}
