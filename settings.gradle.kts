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
// Only here so IDE will get that those are source files.
// Tasks from those are in the wrapper project - XU2-Base and XU2-Patched respectively.

include(":XU2-Base:Source")
project(":XU2-Base:Source").projectDir = file("src/xu2/base/src")
