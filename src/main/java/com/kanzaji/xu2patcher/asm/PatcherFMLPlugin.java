package com.kanzaji.xu2patcher.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions("com.kanzaji.xu2patcher.asm")
@IFMLLoadingPlugin.SortingIndex(-1)
public class PatcherFMLPlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[] { com.kanzaji.xu2patcher.asm.PatcherClassTransformer.class.getName() };
    }

    @Override
    public String getModContainerClass() {
        return com.kanzaji.xu2patcher.asm.PatcherDummyModContainer.class.getName();
    }

    @Override
    public void injectData(Map<String, Object> data) {
        if (data.get("coremodLocation") == null || data.get("mcLocation") == null) return;
        com.kanzaji.xu2patcher.asm.BinPatchManager.INSTANCE.setup(data);
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
