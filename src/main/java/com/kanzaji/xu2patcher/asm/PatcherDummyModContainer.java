package com.kanzaji.xu2patcher.asm;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;
import org.jetbrains.annotations.NotNull;

public class PatcherDummyModContainer extends DummyModContainer {
    public PatcherDummyModContainer() {
        super(new ModMetadata());
        ModMetadata meta = super.getMetadata();
        meta.modId = "xu2patcher-core";
        meta.name = "XU2 Patcher Core";
        meta.version = "1.0";
        meta.authorList = Lists.newArrayList("Su5eD", "Kanzaji");
        meta.description = "XU2 Patcher Coremod used to patch XU2 on runtime.";
        meta.screenshots = new String[0];
    }

    @Override
    public boolean registerBus(@NotNull EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }
}
