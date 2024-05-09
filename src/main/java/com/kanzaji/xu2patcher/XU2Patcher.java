package com.kanzaji.xu2patcher;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import org.apache.logging.log4j.Logger;

@Mod(modid = "xu2patcher", name = "XU2 Patcher", dependencies = "required-after:extrautils2")
public class XU2Patcher {
    public static Logger logger;

    public XU2Patcher() {
        XU2PatcherConfig.set(new XU2PatcherConfig("XU2-Patcher"));
        XU2PatcherConfig.get().load();
    }

    @Mod.EventHandler
    public void start(FMLConstructionEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public static void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }
}
