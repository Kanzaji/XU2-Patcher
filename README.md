# XU2-Patcher
XU2-Patcher is a mod that allows for easy modification of the Extra Utilities 2 code at runtime.

Special thanks to [@Ridanisaurus](https://github.com/Ridanisaurus) for the great logo, [@Su5eD](https://github.com/Su5eD/) for the [IC2-Patcher](https://github.com/Su5eD/IC2-Patcher/) project, and [@Darkhax](https://github.com/Darkhax) for [NoTemaStahp](https://github.com/Darkhax-Minecraft/NoTemaStahp) mod, and to people that created PRs to the Extra Utilities 2 repository!

## Patches
- Fixes XU2 Screens not being able to download imgur images anymore.
- Allows for re-enabling Slime Spawning in the flat worlds (Part of NoTemaStahp integration)
- Allows for disabling op anvil effects for few XU2 tools (Part of NoTemaStahp integration)
- Allows for disabling Tema "little small advantage" in code 😋 (Part of NoTemaStahp integration)
- Fixes a crash when a villager profession was disabled, and you tried to open the book entry of that profession.
  <br>(Fixes [#42](https://github.com/rwtema/Extra-Utilities-2-Source/issues/42) / Impl [#390](https://github.com/rwtema/Extra-Utilities-2-Source/pull/390))
- Fixes a crash when radar wasn't able to check inventory of some blocks (it will log it at debug level now if something happens).
  <br>(Fixes [#293](https://github.com/rwtema/Extra-Utilities-2-Source/issues/293) / Impl [#381](https://github.com/rwtema/Extra-Utilities-2-Source/pull/381))
- Fixes Villager Profession names being corrupted.
  <br>(Fixes [#325](https://github.com/rwtema/Extra-Utilities-2-Source/issues/325) [#363](https://github.com/rwtema/Extra-Utilities-2-Source/issues/363) / Impl [#366](https://github.com/rwtema/Extra-Utilities-2-Source/pull/366))
- Fixes a crash with a builder incorrectly placing a machine, causing a crash.
  <br>(Impl [#302](https://github.com/rwtema/Extra-Utilities-2-Source/pull/302))

## Contribution
W.I.P. However tasks in Gradle setup should be self explanatory 😄

## Licensing
Entire project, except cases specified below, are licensed under MITT License, which can be found [here](https://github.com/Kanzaji/XU2-Patcher/blob/master/LICENSE).

Parts of the code which are marked as `@author @Su5eD` are part of a public domain, as of the IC2-Patcher license `The Unlicense` which can be found [here](https://github.com/Su5eD/IC2-Patcher/blob/master/LICENSE).

Package `com.kanzaji.xu2patcher.asm.BinPatchManager` is a modified version of MinecraftForge's [ClassPatchManager](https://github.com/MinecraftForge/MinecraftForge/blob/1.12.x/src/main/java/net/minecraftforge/fml/common/patcher/ClassPatchManager.java) class, and is licensed under the GNU Lesser General Public License version 2.1, which can be found [here](https://github.com/MinecraftForge/MinecraftForge/blob/1.12.x/LICENSE.txt).

Extra Utilities 2 Source code is `ALl Rights Reserved`, so you can't include it in this repository or share compiled binaries of it. This is also the reason this mod exists, and not a recompiled version of XU2.
