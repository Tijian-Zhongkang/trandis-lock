package com.xm.sanvanfo.trandiscore.netty.plugins;

import lombok.Data;

import java.util.List;

@Data
public class PluginConfig {
    private List<String> plugins;
    private List<String> timeoutPlugins;
}
