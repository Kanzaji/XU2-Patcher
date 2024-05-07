package com.kanzaji.xu2patcher.asm;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;

public class PatcherDummyModContainer extends DummyModContainer {
    public PatcherDummyModContainer() {
        super(new ModMetadata());
        ModMetadata meta = super.getMetadata();
        meta.modId = "xu2patcher-core";
        meta.name = "XU2 Patcher Core";
        meta.version = "1.0";
        meta.authorList = Lists.newArrayList("Kanzaji");
        meta.description = "XU2 Patcher Core mod used to patch XU2 on runtime.";
        meta.screenshots = new String[0];
        meta.credits = "Special thanks to Su5eD for creating IC2-Patcher, which this project is based on!";
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }
}
