package com.kanzaji.xu2patcher;

import java.io.File;



public class XU2PatcherConfig extends net.minecraftforge.common.config.Configuration {

    private static XU2PatcherConfig config;
    /**
     * Determines the max size of the Imgur images in the screen for the client.
     */
    public final int maxImageSize;
    /**
     * Determines if the fix for Slimes not spawning in flat-surface worlds should be applied.
     */
    public final boolean slimeFix;
    /**
     * Determines if OP/"Cheaty" effects of some XU2 items should be nerfed.
     */
    public final boolean opEffectsFix;
    /**
     * Determines if Tema Cheaty Prevention is active :D
     */
    public final boolean cheatyTema;

    protected XU2PatcherConfig(String file) {

        super(new File("config/" + file + ".cfg"));

        this.maxImageSize = this.getInt("maxImageSize", "patches", 1024, -1, Integer.MAX_VALUE,
            "Determines the maximum size of the image from Imgur for the Screens. Extra Utilities 2 by default has limit of 256Kb. Set to -1 to disable the limit.");

        // Special credits to Darkhax for the original solution of fixing those, with <a href="https://github.com/Darkhax-Minecraft/NoTemaStahp">NoTemaStahp</a> mod!
        slimeFix = this.getBoolean("fixSlimeSpawning", "patches", true,
            "Fixes Slimes not spawning in flat-surface worlds."
        );
        opEffectsFix = this.getBoolean("opEffectsFix", "patches", true,
            "Prevents certain XU2 Items such as the law sword, fire axe, and compound bow from having op effects in the anvil."
        );
        cheatyTema = this.getBoolean("preventCheatyTema", "patches", true,
            "Prevents XU2 from giving RWTema special treatment in certain parts of the code and getting op items whenever joining a server."
        );

        if (this.hasChanged()) this.save();
    }

    public static XU2PatcherConfig get() {
        return config;
    }

    protected static void set(XU2PatcherConfig config) {
        XU2PatcherConfig.config = config;
    }
}