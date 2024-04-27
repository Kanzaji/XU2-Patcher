package com.kanzaji.xu2patcher.asm;

import net.minecraft.launchwrapper.IClassTransformer;

public class PatcherClassTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        return com.kanzaji.xu2patcher.asm.BinPatchManager.INSTANCE.applyPatch(name, transformedName, bytes);
    }
}
