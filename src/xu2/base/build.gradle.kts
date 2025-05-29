import de.undercouch.gradle.tasks.download.Download

plugins {
    java
    id("de.undercouch.download")
    id("net.minecraftforge.gradle")
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

val mappingsChannel:    String by project
val mappingsVersion:    String by project
val taskGroup:          String = "xu2 patcher ~ base"
val versionJEI:         String by project
val versionForge:       String by project
val versionForgeFlower: String by project

val XU2SourceURL:       String by project
val XU2SourceZIP:       File = File(buildDir, "downloadSource/source.zip")
val XU2SourceDirty:     File = File(buildDir, "unpackSource")
val src:                File = File(projectDir, "src")

tasks {

    register<Download>("Download Source") {
        group = taskGroup
        src(XU2SourceURL)
        dest(XU2SourceZIP)
        //TODO: Add a temp file with current url saved, so if it changes, it re-downloads the source
        //A check of the folder name inside of source.zip might be a good idea*
        overwrite(false)
    }

    register<Copy>("Unpack Source") {
        group = taskGroup
        dependsOn("Download Source")
        from(zipTree(XU2SourceZIP))
        into(XU2SourceDirty)
    }

    val gradleScripts: List<String> = listOf("1.10.2/build.gradle", "1.11/build.gradle", "1.12/build.gradle")
    register<Copy>("Setup Base Source") {
        group = taskGroup;
        dependsOn("Unpack Source")

        from(File(XU2SourceDirty,
            XU2SourceURL
                .removePrefix("https://github.com/rwtema/")
                .replace("/archive/","-")
                .removeSuffix(".zip")
        ))
        into(src)

        doLast {
            // Fixes some deprecated stuff to make XU2 Build files work
            //TODO: Find a better way to apply those fixes - Literally re-writing files with replace method is not a good idea!
            gradleScripts.forEach {
                val file = File(src, it)
                file.writeText(file.readText()
                    .replace("jcenter()", "mavenCentral()")
                    .replace("http://", "https://")
                    .replace("parseConfig(file('../1.10.2/private.properties'))", "parseConfig(file('../1.10.2/version.properties'))")
                    // Allows for accessing the api jar (XU2-Patcher without ASM) by XU2.
                    // Allows for adding CF Dependencies for testing.
                    // Fixes MultiPart Maven
                    .replace(
                        "    maven {\n        url \"https://dvs1.progwml6.com/files/maven\"\n    }",
                        "    maven {\n        url \"https://dvs1.progwml6.com/files/maven\"\n    }\n" +
                                "    flatDir {\n        dirs '../../../../../build/libs'\n    }\n" +
                                "    maven {\n        url \"https://cursemaven.com\"\n    }\n" +
                                "    maven {\n        url \"https://maven.cil.li/\"\n    }"
                    )
                    // Makes runClient work, as TConstruct has its own JEI dependency that conflicts on the game launch.
                    // Adds Railcraft dependency on runtime, see #1.
                    .replace(
                        "    deobfCompile \"slimeknights:TConstruct:1.12-2.7.2.508\"",
                        "    deobfCompile (\"slimeknights:TConstruct:1.12-2.7.2.508\")  {\n" +
                                "        exclude group: 'mezz.jei', module: 'jei_1.12'\n    }\n" +
                                "    runtime \"curse.maven:railcraft-51195:3853491\"\n" +
                                "    runtime name: \"XU2-Patcher-api\""
                    )
                    // Updates forge to the minimum required by Railcraft
                    .replace(
                        "    version = \"1.12.2-14.23.5.2769\"",
                        "    version = \"1.12.2-14.23.5.2779\""
                    )
                    // Allows attaching a debugger to XU2-Client
                    .replace(
                        "minecraft {",
                        "minecraft {\n    tasks {\n        \"runClient\" {\n            jvmArgs " +
                                "\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005\", " +
                                "\"-Dlog4j.configurationFile=\\\"log4j2.xml\\\"\"" +
                                "\n        }\n    }"
                    )
                    // Allows for accessing classes in api source directory in XU2 Source.
                    // Those require to be compatible with Minecraft 1.10.2 / 1.11 and 1.12! (So mostly config files)
                    .replace(
                        "    api{\n        java {\n",
                        "    api{\n        java {\n            // XU2-Patcher\n            srcDir '../../../../main/api'\n"
                    )
                    // Disabled getAssets task, to allow booting the client in a reasonable time (and not waiting 15 minutes for a broken task to timeout)
                    .replace(
                        "archivesBaseName = \"extrautils2-1.12\"\n\n\n\n",
                        "archivesBaseName = \"extrautils2-1.12\"\n" +
                                "\n" +
                                "tasks.getByName(\"getAssets\").configure {\n" +
                                "    enabled = false;\n" +
                                "}\n" +
                                "\n" +
                                "tasks.getByName(\"getAssetIndex\").configure {\n" +
                                "    enabled = false;\n" +
                                "}\n" +
                                "\n" +
                                "tasks.getByName(\"runClient\").configure {\n" +
                                "    outputs.upToDateWhen {false}\n" +
                                "}\n"
                    )
                )
            }

            // Using own Gradle 3.0 wrapper to make everything work, as XU2 project is cursed in structure,
            // use execSourceTask fun for command execution on the source code.

            // Strips new lines at the end of the source files, as those cause random patches to generate.
            fileTree(src) {
                include("**/*.java")
                forEach { file ->
                    val content = file.readText(Charsets.UTF_8)
                    if (content.endsWith("\n")) file.writeText(content.removeSuffix("\n"))
                }
            }

            // Removes random test file that for some reason is included in the source jar
            File(src, "1.10.2/src/main/resources/assets/test").delete()
        }
    }

    register<Copy>("Add Gradle to Source") {
        group = taskGroup
        if (!srcExists()) dependsOn("Setup Base Source")
        from (File(projectDir, "../gradle-3.0-wrapper"))
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
    register<Jar>("Source Jar ~ Full (Base)") {
        group = taskGroup
        description = "Used to package all versions of the mod into a single jar, for Patching the Patched Project. Should not be used to generate binary patches"
        if (!srcExists()) dependsOn("Setup Base Source")
        archiveClassifier.set("sources")
        from(src) {
            include("**/*.java")
            exclude("**/gradle")
            exclude("**/.gradle")
            exclude("**/META-INF")
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
    val libs = File(src, "$ver/build/libs")
    val final = File(buildDir, "libs/$ver")
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
    val libs = File(src, "$ver/build/libs")
    val final = File(buildDir, "libs/$ver")
    execSourceTask(":$ver:sourceJar")

    final.mkdirs()
    var sourcesCopied = false
    var duplicate = false
    libs.listFiles()?.forEach {
        if (it.name.endsWith("sources.jar")) {
            if (sourcesCopied) duplicate = true;
            it.copyTo(File(final,"ExtraUtils2-Sources.jar"), overwrite = true)
            sourcesCopied = true;
        }
    }
    if (duplicate) {
        libs.deleteRecursively()
        packageSource(ver)
    }
}

//reobf {
//    create("jar") {
//        dependsOn("shadowJar")
//    }
//}

//artifacts {
//    archives(tasks.getByName("sourceJar"))
//}

repositories {
    mavenCentral()
    maven { 
        name = "Progwml6 maven"
        url = uri("https://dvs1.progwml6.com/files/maven/")
    }
}

minecraft {
    mappings(mappingsChannel, mappingsVersion)
}

dependencies {
    minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-${versionForge}")
    compileOnly(group = "mezz.jei", name = "jei_1.12.2", version = versionJEI)
}

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
    if (project.name != "Source") {
        apply(plugin="java-library")
        apply(plugin="idea")
        apply(plugin="net.minecraftforge.gradle")

        repositories {
            maven { url = uri("https://files.minecraftforge.net/maven") }
            maven { url = uri("https://maven.thiakil.com") }
            maven { url = uri("https://dvs1.progwml6.com/files/maven") }
            maven { url = uri("https://maven.blamejared.com") }
        }

        // Those dependencies are taken from the respective source subproject.
        // Reference build.gradle files in those projects for more details
        // Note: Including MC 1.12.2 here instead of proper versions due to FG5 not working with any other version for some reason?
        if (project.name == "1.10.2") {
            dependencies {
                minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-14.23.5.2860")
                //minecraft(group = "net.minecraftforge", name = "forge", version = "1.10.2-12.18.3.2511")
                compileOnly(group = "mezz.jei", name = "jei_1.10.2", version = "3.13.3.380")
                compileOnly(group = "slimeknights.mantle", name = "Mantle", version = "1.10.2-1.1.3.199")
                compileOnly(group = "slimeknights", name = "TConstruct", version = "1.10.2-2.6.1.464")
            }
            minecraft { mappings("snapshot", "20170624-1.12") }
            sourceSets {
                main {
                    java {
                        srcDir("src/main/java")
                        srcDir("src/compat/java")
                        srcDir("src/compat111/java")
                        srcDir ("src/api/java")
                    }
                    resources {
                        srcDir ("src/main/resources")
                        srcDir ("src/compat/resources")
                    }
                }
            }
        }

        if (project.name == "1.11") {
            dependencies {
                minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-14.23.5.2860")
                //minecraft(group = "net.minecraftforge", name = "forge", version = "1.11.2-13.20.1.2588")
                compileOnly(group = "mezz.jei", name = "jei_1.11", version = "4.1.1.208")
                parent?.let { compileOnly(it.project("1.10.2")) }
            }
            minecraft { mappings("snapshot", "20170624-1.12") }
            sourceSets { main { java { srcDir("src/main/java") } } }
        }

        if (project.name == "1.12") {
            dependencies {
                //minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-14.23.5.2769")
                minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-14.23.5.2860")
                compileOnly(group = "CraftTweaker2", name = "CraftTweaker2-API", version = "4.1.9.6")
                compileOnly(group = "mezz.jei", name = "jei_1.12.2", version = "4.12.1.217")
                compileOnly(group = "slimeknights.mantle", name = "Mantle", version = "1.12-1.3.1.22")
                compileOnly(group = "slimeknights", name = "TConstruct", version = "1.12-2.7.2.508")
                compileOnly(group = "com.azanor.baubles", name = "Baubles", version = "1.12-1.5.2")
                parent?.let { compileOnly(it.project("1.10.2")) }
            }
            minecraft { mappings("snapshot", "20170624-1.12") }
            sourceSets { main { java { srcDir("src/main/java") } } }
        }
    }
}