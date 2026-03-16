import codechicken.diffpatch.util.PatchMode
import de.undercouch.gradle.tasks.download.Download
import net.minecraftforge.gradle.patcher.tasks.ApplyPatches
import net.minecraftforge.gradle.patcher.tasks.GeneratePatches
import org.gradle.kotlin.dsl.register

plugins {
    java
    idea
    id("de.undercouch.download")
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

val taskGroup:          String = "xu2 patcher ~ base"
val XU2SourceURL:       String by project
val src:                File = projectDir.resolve("src")
val patchesDir:         File = projectDir.resolve("patches")

tasks {
    // Download and setup the source.
    register<Download>("Download Source") {
        group = taskGroup
        inputs.property("sourceUrl", XU2SourceURL)
        src(XU2SourceURL)
        dest(buildDir.resolve("sources/XU2-Sources.zip"))
        overwrite(false)
    }

    register<Zip>("Flatten Source") {
        group = taskGroup
        val sourceJar = getByName<Download>("Download Source")
        dependsOn(sourceJar)

        archiveFileName.set("XU2-Processed.zip")
        destinationDirectory.set(buildDir.resolve("sources"))

        val xu2RepoName = XU2SourceURL
            .removePrefix("https://github.com/rwtema/")
            .replace("/archive/", "-")
            .removeSuffix(".zip")

        from(zipTree(sourceJar.dest).matching {
            include("$xu2RepoName/**")
        }) {
            eachFile {
                path = path.removePrefix("$xu2RepoName/")
            }
            includeEmptyDirs = false
        }
    }

    // Setup Patches Generation
    register<GeneratePatches>("Generate Patches") {
        group = taskGroup

        val baseSourceJar = getByName<Zip>("Flatten Source")
        val sourceJar = getByName<Zip>("Source Jar ~ Full (Base)")

        dependsOn(baseSourceJar)
        dependsOn(sourceJar)

        base.set(baseSourceJar.archiveFile.get().asFile)
        modified.set(sourceJar.archiveFile.get().asFile)
        output.set(patchesDir)
        isPrintSummary = true
    }

    register<ApplyPatches>("Patch Source") {
        group = taskGroup
        val baseSourceJar = getByName<Zip>("Flatten Source")
        dependsOn(baseSourceJar)

        base.set(baseSourceJar.archiveFile.get().asFile)
        patches.set(patchesDir)
        rejects.set(buildDir.resolve("sources/Rejected-Patches.zip"))
        output.set(buildDir.resolve("sources/XU2-Patched.zip"))
        patchMode.set(PatchMode.OFFSET)
        isPrintSummary = true
    }

    register<Copy>("Setup Base Source") {
        group = taskGroup;
        val patchedSource = getByName<ApplyPatches>("Patch Source")
        dependsOn(patchedSource)

        from(zipTree(patchedSource.output))
        into(src)

        doLast {
            // Using own Gradle 3.0 wrapper to make everything work, as XU2 project is cursed in structure,
            // use execSourceTask func for command execution on the source code.
            execSourceTask("--refresh-dependencies")
            execSourceTask(":1.10.2:setupDecompWorkspace")
            execSourceTask(":1.11:setupDecompWorkspace")
            execSourceTask(":1.12:setupDecompWorkspace")

            // Strips new lines at the end of the source files, as those cause random patches to generate in the patched project.
            // This will generate a lot of patches in the base, but we don't really care!
            fileTree(src) {
                include("**/*.java")
                forEach { file ->
                    val content = file.readText(Charsets.UTF_8)
                    if (content.endsWith("\n")) file.writeText(content.removeSuffix("\n"))
                }
            }

            // Removes random test file that for some reason is included in the source jar
            src.resolve("1.10.2/src/main/resources/assets/test").delete()
            // Remove the META-INF from the source, how the heck did it get here I have no idea.
            src.resolve("META-INF").delete()

            // Get the Minecraft/Forge Jars to the libs.
            val libs = projectDir.resolve("../libs")
            src.resolve("1.10.2/.gradle/minecraft/forgeSrc-1.10.2-12.18.3.2511-PROJECT(1.10.2).jar")
                .copyTo(libs.resolve("Forge-1.10.2.jar"))
            src.resolve("1.10.2/.gradle/minecraft/forgeSrc-1.10.2-12.18.3.2511-PROJECT(1.10.2)-sources.jar")
                .copyTo(libs.resolve("Forge-1.10.2-Sources.jar"))
            src.resolve("1.11/.gradle/minecraft/forgeSrc-1.11.2-13.20.1.2588-PROJECT(1.11).jar")
                .copyTo(libs.resolve("Forge-1.11.2.jar"))
            src.resolve("1.11/.gradle/minecraft/forgeSrc-1.11.2-13.20.1.2588-PROJECT(1.11)-sources.jar")
                .copyTo(libs.resolve("Forge-1.11.2-Sources.jar"))
            src.resolve("1.12/.gradle/minecraft/forgeSrc-1.12.2-14.23.5.2779-PROJECT(1.12).jar")
                .copyTo(libs.resolve("Forge-1.12.2.jar"))
            src.resolve("1.12/.gradle/minecraft/forgeSrc-1.12.2-14.23.5.2779-PROJECT(1.12)-sources.jar")
                .copyTo(libs.resolve("Forge-1.12.2-Sources.jar"))
        }
    }

    register<Copy>("Add Gradle to Source") {
        group = taskGroup
        if (!srcExists()) dependsOn("Setup Base Source")
        from (projectDir.resolve("../gradle-3.0-wrapper"))
        into(src)
    }

    // Jar Generation
    register("Build ~ 1.12 (Base)") {
        group = taskGroup
        if (!srcExists()) dependsOn("Setup Base Source")
        doFirst { buildSource("1.12") }
    }

    register("Build ~ 1.11 (Base)") {
        group = taskGroup
        if (!srcExists()) dependsOn("Setup Base Source")
        doFirst { buildSource("1.11") }
    }

    register("Build ~ 1.10 (Base)") {
        group = taskGroup
        if (!srcExists()) dependsOn("Setup Base Source")
        doFirst { buildSource("1.10.2") }
    }

    // Source Jar Generation
    register<Zip>("Source Jar ~ Full (Base)") {
        group = taskGroup
        description = "Used to package all versions of the mod into a single jar, for Patching the Patched Project. Should not be used to generate binary patches"
        if (!srcExists()) dependsOn("Setup Base Source")
        archiveClassifier.set("sources")
        from(src) {
            include("**")
            exclude("gradle")
            exclude("**/.idea")
            exclude("**/.gradle")
            exclude("**/build")
            exclude("**/run")
        }
    }

    // Those are here mostly for Patch generation tasks, as those are a bit faster than build task.
    // If full jar or deobf one is required, use build (as it also generates source jar)
    register("Source Jar ~ 1.12 (Base)") {
        group = taskGroup
        if (!srcExists()) dependsOn("Setup Source")
        doFirst { packageSource("1.12") }
    }

    register("Source Jar ~ 1.11 (Base)") {
        group = taskGroup
        if (!srcExists()) dependsOn("Setup Source")
        doFirst { packageSource("1.11") }
    }

    register("Source Jar ~ 1.10 (Base)") {
        group = taskGroup
        if (!srcExists()) dependsOn("Setup Source")
        doFirst { packageSource("1.10.2") }
    }
}

fun buildSource(ver: String) {
    val libs = src.resolve("$ver/build/libs")
    val final = buildDir.resolve("libs/$ver")
    execSourceTask(":$ver:build")

    // If version changed, it is possible that additional jars will be created.
    // If that happens, Delete and build again to not break other tasks.
    if (libs.list().size > 3) {
        libs.deleteRecursively()
        buildSource(ver)
        return;
    }

    final.mkdirs()
    libs.listFiles()?.forEach {
        val name: String = if (it.name.endsWith("-deobf.jar")) {
            "ExtraUtils2-Deobf.jar"
        } else if (it.name.endsWith("-sources.jar")) {
            "ExtraUtils2-Sources.jar"
        } else {
            "ExtraUtils2.jar"
        }
        it.copyTo(File(final, name), overwrite = true)
    }
}

fun packageSource(ver: String) {
    val libs = src.resolve("$ver/build/libs")
    val final = buildDir.resolve("libs/$ver")
    execSourceTask(":$ver:sourceJar")

    final.mkdirs()
    var sourcesCopied = false
    var duplicate = false
    libs.listFiles()?.forEach {
        if (it.name.endsWith("sources.jar")) {
            if (sourcesCopied) duplicate = true;
            it.copyTo(final.resolve("ExtraUtils2-Sources.jar"), overwrite = true)
            sourcesCopied = true;
        }
    }
    if (duplicate) {
        libs.deleteRecursively()
        packageSource(ver)
    }
}

repositories {
    mavenCentral()
    maven { 
        name = "Progwml6 maven"
        url = uri("https://dvs1.progwml6.com/files/maven/")
    }
}

//minecraft {
//    mappings(mappingsChannel, mappingsVersion)
//}
//
//dependencies {
//    minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-${versionForge}")
//}

/**
 * Used to execute tasks with use of the Gradle Wrapper from the XU2 Project.
 * Note: Yes I know this is cursed, Yes I tried a lot of stuff to make this work.
 */
fun execSourceTask(task: String) {
    val os = System.getProperty("os.name").toLowerCase()
    val javaHome = System.getProperty("java.home")

    println("Using Java at: $javaHome to execute task \"$task\" on the XU2 Source...")


    val pb = if (os.contains("windows")) {
        ProcessBuilder(File(projectDir,"../gradle-3.0-wrapper/gradlew.bat").absolutePath, task)
    } else {
        //TODO: Test if this works on SH / Linux
        ProcessBuilder(File(projectDir,"../gradle-3.0-wrapper/gradlew").absolutePath, task)
    }

    // Doesn't work in IDE :V Still requires manual reading of the output streams.
//    pb.inheritIO()
    pb.directory(src)
    pb.environment()["JAVA_HOME"] = javaHome;

    val process = pb.start();
    val reader = process.inputStream.bufferedReader()
    val errorReader = process.errorStream.bufferedReader()

    Thread { reader.lines().forEach { println(it) } }.start()
    Thread { errorReader.lines().forEach { System.err.println(it) } }.start()

    process.waitFor()

    val exitCode = process.exitValue();

    println("Process ended with exit code: $exitCode")

    if (exitCode != 0) throw IllegalStateException("Exit code different than 0! Something went wrong.")
}

fun srcExists(): Boolean {
    return File(src, "build.gradle").exists();
}

// Only here so source code is possible to edit from the main project view. For better compatibility, open the Source project separately.
subprojects {
    if (project.name in listOf("1.10.2", "1.11", "1.12")) {
        apply(plugin = "java-library")
        apply(plugin = "idea")

        // Inheritance gone!
//        configurations.all {
//            exclude(group = "net.minecraftforge", module = "forge")
//        }

        var forgeVer = "unknown"

        repositories {
            maven { url = uri("https://maven.thiakil.com") }
            maven { url = uri("https://dvs1.progwml6.com/files/maven") }
            maven { url = uri("https://maven.blamejared.com") }
        }

        // Why SWITCH is called WHEN in Kotlin ughr.
        when (project.name) {
            "1.10.2" -> {
                forgeVer = "1.10.2"
                dependencies {
                    compileOnly(group = "mezz.jei", name = "jei_1.10.2", version = "3.13.3.380")
                    compileOnly(group = "slimeknights.mantle", name = "Mantle", version = "1.10.2-1.1.3.199")
                    compileOnly(group = "slimeknights", name = "TConstruct", version = "1.10.2-2.6.1.464")
                }

                sourceSets {
                    main {
                        java {
                            srcDir("src/main/java")
                            srcDir("src/compat/java")
                            srcDir("src/compat111/java")
                            srcDir("src/api/java")
                        }
                        resources {
                            srcDir("src/main/resources")
                            srcDir("src/compat/resources")
                        }
                    }
                }
            }

            "1.11" -> {
                forgeVer = "1.11.2"
                dependencies {
                    compileOnly(group = "mezz.jei", name = "jei_1.11", version = "4.1.1.208")
                    parent?.let { compileOnly(it.project("1.10.2")) }
                }
                sourceSets { main { java { srcDir("src/main/java") } } }
            }

            "1.12" -> {
                forgeVer = "1.12.2"
//                apply(plugin="net.minecraftforge.gradle")
                dependencies {
                    //minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-14.23.5.2769")
//                    minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-14.23.5.2860")
                    compileOnly(group = "CraftTweaker2", name = "CraftTweaker2-API", version = "4.1.9.6")
                    compileOnly(group = "mezz.jei", name = "jei_1.12.2", version = "4.12.1.217")
                    compileOnly(group = "slimeknights.mantle", name = "Mantle", version = "1.12-1.3.1.22")
                    compileOnly(group = "slimeknights", name = "TConstruct", version = "1.12-2.7.2.508")
                    compileOnly(group = "com.azanor.baubles", name = "Baubles", version = "1.12-1.5.2")
                    parent?.let { compileOnly(it.project("1.10.2")) }
                }
//                minecraft { mappings("snapshot", "20170624-1.12") }
                sourceSets { main { java { srcDir("src/main/java") } } }
            }
        }


        val forge = projectDir.resolve("../../../libs").resolve("Forge-${forgeVer}.jar")
        dependencies {
            if (forge.exists()) {
                implementation(files(forge))
            } else {
                logger.warn("!! Forge JAR missing for XU2:${project.name} at: ${forge.absolutePath} | Did you execute Setup Base Source?")
            }
        }
    }
}