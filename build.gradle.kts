import fr.brouillard.oss.jgitver.GitVersionCalculator
import fr.brouillard.oss.jgitver.Strategies
import net.minecraftforge.gradle.common.util.RunConfig
import wtf.gofancy.fancygradle.script.extensions.deobf
import net.minecraftforge.gradle.patcher.tasks.GenerateBinPatches

buildscript {
    dependencies { 
        classpath(group = "fr.brouillard.oss", name = "jgitver", version = "0.14.0")
    }
}

plugins {
    `java-library`
    `maven-publish`
    idea
    id("net.minecraftforge.gradle") version "5.1.+"
    id("de.undercouch.download") version "4.1.1"
    id("wtf.gofancy.fancygradle") version "1.1.+"
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

val versionMc = "1.12.2"
val mappingsChannel: String by project
val mappingsVersion: String by project

val versionForge: String by project
val versionJEI: String by project
val extraUtils2Source: String by project

val taskGroup = "xu2 patcher"

version = getGitVersion()
group = "com.kanzaji"
setProperty("archivesBaseName", "XU2-Patcher")

minecraft {
    mappings(mappingsChannel, mappingsVersion)

    runs {
        val config = Action<RunConfig> {
            properties(
                mapOf(
                    "forge.logging.markers" to "SCAN,REGISTRIES,REGISTRYDUMP,COREMODLOG",
                    "forge.logging.console.level" to "debug"
                )
            )
            workingDirectory = project.file("run").canonicalPath
            source(sourceSets["main"])
        }

        create("client", config)
        create("server", config)
    }
}

fancyGradle {
    patches {
        asm
        codeChickenLib
        coremods
        resources
        mergetool
    }
}

repositories {
    maven {
        // JEI Repository
        name = "Progwml6 maven"
        url = uri("https://dvs1.progwml6.com/files/maven/")
    }
    maven {
        // Mirror Maven for JEI
        name = "ModMaven"
        url = uri("https://modmaven.dev")
    }
}

dependencies {
    minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-${versionForge}")

    //TODO: Fix me! (Iplementation should of the source, just need to figure out how to make this point to file compiled by gradle 3.0)
//    implementation(project(":XU2-Patched"))
    implementation(fg.deobf(group = "mezz.jei", name = "jei_1.12.2", version = versionJEI))
//    compileOnly(fg.deobf(group = "mezz.jei", name = "jei_1.12.2", version = versionJEI))
}

tasks {
    named<ProcessResources>("processResources") { 
        // this will ensure that this task is redone when the versions change.
        inputs.property("version", project.version)
        inputs.property("mcversion", versionMc)
    
        filesMatching("mcmod.info") {
            // replace version and mcversion
            expand(
                    "version" to project.version,
                    "mcversion" to versionMc
            )
        }
    }
    
    named("jar") {
        enabled = false
    }

    //TODO: Add Generate Binary Patches ~ Dev 1.12 task and configure this one to use patches from that.
    register<Jar>("Dev Jar ~ 1.12") {
        group = taskGroup;
        archiveFileName.set("XU2-Patcher-dev.jar")

        from(sourceSets.main.get().output) {
            exclude("patches");
        }

        manifest {
            attributes(
                "FMLCorePlugin" to "com.kanzaji.xu2patcher.asm.PatcherFMLPlugin",
                "FMLCorePluginContainsFMLMod" to true
            )
        }
    }

    register<Jar>("Api Jar ~ 1.12") {
        group = taskGroup;
        archiveFileName.set("XU2-Patcher-api.jar")

        from(sourceSets.main.get().output) {
            exclude("com/kanzaji/xu2patcher/asm/**");
            exclude("patches");
        }
    }

    // Has to be so low, because XU2-Patched runClient requires the output of Api Jar task.
    evaluationDependsOnChildren()
    register<Jar>("Release Jar ~ 1.12") {
        group = taskGroup;
        val binPatches = project(":XU2-Patched").tasks.getByName<GenerateBinPatches>("Generate Binary Patches ~ 1.12");
        dependsOn(binPatches)
        mustRunAfter(binPatches)

        archiveClassifier.set("")
        from(sourceSets.main.get().output)
        manifest {
            attributes(
                "FMLCorePlugin" to "com.kanzaji.xu2patcher.asm.PatcherFMLPlugin",
                "FMLCorePluginContainsFMLMod" to true
            )
        }
    }

    register("Setup XU2 Source") {
        group = taskGroup
        dependsOn(project(":XU2-Patched").tasks.getByName<Copy>("Setup Patched Source"))
    }
//
//    whenTaskAdded {
//        if (name.startsWith("prepareRun")) {
//            dependsOn(project(":UX2-Patched").tasks.getByName("patchRunJar"))
//            dependsOn("devJar")
//            dependsOn("patchModifyClassPath")
//            dependsOn("patchGenerateObfToSrg")
//            dependsOn("patchExtractMappingsZip")
//        }
//    }
}

reobf {
    create("jar") {
        dependsOn("Release Jar ~ 1.12")
    }
}

artifacts {
    archives(tasks.getByName("Release Jar ~ 1.12"))
}

sourceSets {
    main {
        java {
            // Shared classes for XU2, need to be able to compile in MC 1.10.2
            srcDirs("src/main/api")
        }
        resources {
            srcDir("src/main/generated")
        }
    }
}

/**
 * @author Su5eD (Taken from IC2-Patcher Project)
 */
fun getGitVersion(): String {
    val jgitver = GitVersionCalculator.location(rootDir)
            .setNonQualifierBranches("master")
            .setVersionPattern("\${M}\${<m}\${<meta.COMMIT_DISTANCE}\${-~meta.QUALIFIED_BRANCH_NAME}")
            .setStrategy(Strategies.PATTERN)
    return jgitver.version
}