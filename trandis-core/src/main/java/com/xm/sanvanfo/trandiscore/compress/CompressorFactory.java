package com.xm.sanvanfo.trandiscore.compress;

import com.xm.sanvanfo.common.plugin.CustomPlugin;
import com.xm.sanvanfo.common.plugin.IPlugin;
import com.xm.sanvanfo.common.plugin.PluginLoader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class CompressorFactory {

    /**
     * The constant COMPRESSOR_MAP.
     */
    @SuppressWarnings("WeakerAccess")
    protected static final Map<CompressorType, Compressor> COMPRESSOR_MAP = new ConcurrentHashMap<>();

    static {
        COMPRESSOR_MAP.put(CompressorType.NONE, new NoneCompressor());
    }

    /**
     * Get compressor by code.
     *
     * @param code the code
     * @return the compressor
     */
    public static Compressor getCompressor(byte code) {
        CompressorType type = CompressorType.getByCode(code);
        return COMPRESSOR_MAP.computeIfAbsent(type,
                key -> {
            try {
                return PluginLoader.INSTANCE().load(Compressor.class, type.name());
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }


    /**
     * None compressor
     */
    @CustomPlugin(registerClass = Compressor.class, name = "default")
    public static class NoneCompressor implements Compressor, IPlugin {
        @Override
        public byte[] compress(byte[] bytes) {
            return bytes;
        }

        @Override
        public byte[] decompress(byte[] bytes) {
            return bytes;
        }
    }
}
