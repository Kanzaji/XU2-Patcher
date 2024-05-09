package com.kanzaji.xu2patcher;

import java.io.File;



/**
 * Special credits to Darkhax for the original solution of fixing those, with <a href="https://github.com/Darkhax-Minecraft/NoTemaStahp">NoTemaStahp</a> mod!
 */
public class XU2PatcherConfig extends net.minecraftforge.common.config.Configuration {

    private static XU2PatcherConfig config;
    public final boolean slimeFix;
    public final boolean opEffectsFix;
    public final boolean cheatyTema;

    protected XU2PatcherConfig(String file) {

        super(new File("config/" + file + ".cfg"));

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