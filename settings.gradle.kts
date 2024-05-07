pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "net.minecraftforge.gradle") {
                useModule("${requested.id}:ForgeGradle:${requested.version}")
            }
        }
    }

    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://files.minecraftforge.net/maven")
        }
        maven {
            name = "FancyGradle"
            url = uri("https://gitlab.com/api/v4/projects/26758973/packages/maven")
        }
        maven {
            name = "Garden of Fancy"
            url = uri("https://maven.gofancy.wtf/releases")
        }
    }
}

rootProject.name = "XU2-Patcher"

include(":XU2-Base")
include(":XU2-Patched")

project(":XU2-Base").projectDir = file("src/xu2/base")
project(":XU2-Patched").projectDir = file("src/xu2/patched")


// XU2-Projects
// Only here, so the IDE will get that those are source files.
// Tasks from those are in the wrapper project - XU2-Base and XU2-Patched respectively.

include(":XU2-Base:Source")
include(":XU2-Base:Source:1.10.2")
include(":XU2-Base:Source:1.11")
include(":XU2-Base:Source:1.12")
val baseSource = File("src/xu2/base/src");
project(":XU2-Base:Source").projectDir = baseSource
project(":XU2-Base:Source:1.10.2").projectDir = File(baseSource, "1.10.2")
project(":XU2-Base:Source:1.11").projectDir = File(baseSource, "1.11")
project(":XU2-Base:Source:1.12").projectDir = File(baseSource, "1.12")
project(":XU2-Base:Source:1.10.2").buildFileName = "null"
project(":XU2-Base:Source:1.11").buildFileName = "null"
project(":XU2-Base:Source:1.12").buildFileName = "null"

include(":XU2-Patched:Source")
include(":XU2-Patched:Source:1.10.2")
include(":XU2-Patched:Source:1.11")
include(":XU2-Patched:Source:1.12")
val patchedSource = File("src/xu2/patched/src");
project(":XU2-Patched:Source").projectDir = patchedSource
project(":XU2-Patched:Source:1.10.2").projectDir = File(patchedSource, "1.10.2")
project(":XU2-Patched:Source:1.11").projectDir = File(patchedSource, "1.11")
project(":XU2-Patched:Source:1.12").projectDir = File(patchedSource, "1.12")
project(":XU2-Patched:Source:1.10.2").buildFileName = "null"
project(":XU2-Patched:Source:1.11").buildFileName = "null"
project(":XU2-Patched:Source:1.12").buildFileName = "null"

//TODO:
// - Add Dev Jar generation
// - Client Runs on the patcher? Do I need to? Client runs should work on the Patch project itself.
// - Fix 1.10.2 and 1.11 project sources using 1.12 Forge (Causes some issues in the IDE, but mod compiles using original build.gradle)