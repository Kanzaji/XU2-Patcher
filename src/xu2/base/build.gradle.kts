import codechicken.diffpatch.util.PatchMode
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.DownloadAction
import net.minecraftforge.gradle.common.tasks.JarExec
import net.minecraftforge.gradle.patcher.tasks.ApplyPatches
import net.minecraftforge.gradle.patcher.tasks.GeneratePatches
import org.apache.commons.io.FileUtils
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.launcher.daemon.configuration.DaemonBuildOptions.JavaHomeOption

plugins {
    java
    id("de.undercouch.download")
    id("net.minecraftforge.gradle")
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

val mappingsChannel:    String by project
val mappingsVersion:    String by project
val taskGroup:          String = "xu2 patcher | base"
val versionJEI:         String by project
val versionForge:       String by project
val versionForgeFlower: String by project

val XU2SourceURL:       String by project
val XU2SourceZIP:       File = File(buildDir, "downloadSource/source.zip")
val XU2SourceDirty:     File = File(buildDir, "unpackSource")
val src:                File = File(projectDir, "src")
//val decompiledJar: File = File(buildDir, "decompileIC2/output.jar")
//val patchedJar: File = File(buildDir, "applyPatches/output.jar")
//val processedJar: File = File(buildDir, "applyStyle/output.jar")
//val processedJarPatched: File = File(buildDir, "applyStylePatched/output.jar")

val shade: Configuration by configurations.creating

minecraft {
    mappings(mappingsChannel, mappingsVersion)
}

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
    register<Copy>("Setup Source") {
        group = taskGroup;
        dependsOn("Unpack Source")
//        dependsOn("Download Gradle 2.14.1")
        // Searching for the directory in XU2SourceDirty doesn't work unless Unpack Source was launched before.
        // from(XU2SourceDirty.listFiles()?.single { it.isDirectory }?: throw IllegalStateException("Expected one subdirectory in $XU2SourceDirty"))
        // So the only option I can see at the time of writing this is to get the folder name from the URL.
        from(File(XU2SourceDirty,
            XU2SourceURL
                .removePrefix("https://github.com/rwtema/")
                .replace("/archive/","-")
                .removeSuffix(".zip")
        ))
        into(src)

        //TODO: Find a way to make this stop blocking this task from being UP-TO-DATE when nothing else changed.
        // Fixes some deprecated stuff to make XU2 Build files work
        doLast {
            gradleScripts.forEach {
                val file = File(src, it)
                file.writeText(file.readText()
                    .replace("jcenter()", "mavenCentral()")
                    .replace("http://", "https://")
                    .replace("parseConfig(file('../1.10.2/private.properties'))", "parseConfig(file('../1.10.2/version.properties'))")
                )
            }

            // XU2 Source is cursed. I would prefer honestly decompiling the XU2 Jar lol
            // Each module has its own gradle wrapper (different at that!), but the main project "linking" those together doesn't have one,
            // yet it requires gradle 2.14 to work?
            // Using own Gradle 3.0 wrapper to make everything work, use execSourceTask fun for command execution on the source code.

            execSourceTask("--refresh-dependencies dependencies")
        }
    }
}
    
//    register<ApplyAstyle>("applyStyle") {
//        group = taskGroup
//        dependsOn("decompileIC2")
//        input.set(decompiledJar)
//        output.set(processedJar)
//    }
//
//    register<ApplyPatches>("applyPatches") {
//        group = taskGroup
//        dependsOn("applyStyle")
//        base.set(processedJar)
//
////        patches.set(getPatchesDirectory())
//        rejects.set(File(buildDir, "$name/rejects.zip"))
//        output.set(patchedJar)
//        patchMode.set(PatchMode.OFFSET)
//
//        isPrintSummary = true
//    }
//
//    register<ApplyAstyle>("applyStylePatched") {
//        group = taskGroup
//        dependsOn("applyPatches")
//        input.set(patchedJar)
//        output.set(processedJarPatched)
//    }
//
//    register<Copy>("extractSources") {
//        group = taskGroup
//        dependsOn("applyStylePatched")
//
//        from(zipTree(processedJarPatched)) {
//            exclude("ic2/api/energy/usage.txt")
//        }
//        into("src/main/java")
//    }
//
//    register<Copy>("extractResources") {
//        group = taskGroup
//        dependsOn("extractSources")
//
//        from(zipTree(decompiledJar)) {
//            exclude("ic2/**", "org/**")
//        }
//        into("src/main/resources")
//    }
//
//    register<Jar>("sourceJar") {
//        dependsOn("classes")
//        archiveClassifier.set("sources")
//        from(sourceSets.main.get().allJava) {
//            exclude("*.txt")
//        }
//    }
//
//    register<Jar>("sourceJarW-ODep") {
//        // This dependency didn't allow for generatePatches to work correctly.
////        dependsOn("classes")
//        archiveClassifier.set("sources")
//        from(sourceSets.main.get().allJava) {
//            exclude("*.txt")
//        }
//    }

//    register<Jar>("sourceJarWithResources") {
//        dependsOn("classes","extractResources")
//        archiveClassifier.set("sources-with-resources")
//        from(sourceSets.main.get().allSource)
//    }
//
//    register<GeneratePatches>("generatePatches") {
//        group = taskGroup
//        val sourceJar = getByName<Jar>("sourceJarW-ODep")
////        var outputDir = getPatchesDirectory()
//        dependsOn(sourceJar, "applyStyle")
//        base.set(processedJar)
//        modified.set(sourceJar.archiveFile.get().asFile)
////        output.set(outputDir)
//        isPrintSummary = true
//
//        doLast {
////            val outputPath = outputDir.toPath()
////            Files.walk(outputPath)
////                    .filter { path ->
////                        val relative = outputPath.relativize(path).toString()
////                        relative.isNotEmpty() && (!relative.startsWith("ic2") || relative.startsWith("ic2\\profiles") || relative.startsWith("ic2\\sounds")) && path.toFile().isDirectory
////                    }
////                    .map(Path::toFile)
////                    .forEach(FileUtils::deleteDirectory)
//        }
//    }
    
//    register("setup") {
//        group = taskGroup
//        dependsOn("extractResources")
//    }
//
//    named<ShadowJar>("shadowJar") {
//        dependsOn("classes", "extractSources")
//
//        configurations = listOf(project.configurations["shade"])
//
//        archiveClassifier.set("")
//        from("src/main/java/ic2") {
//            include("profiles/**")
//            include("sounds/**")
//            into("ic2")
//        }
//    }
//
//    named("compileJava") {
//        dependsOn("setup")
//    }
//
//    named("processResources") {
//        dependsOn("extractResources")
//    }
//}

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

dependencies {
    minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-${versionForge}")
    compileOnly(group = "mezz.jei", name = "jei_1.12.2", version = versionJEI)
    val ejml = create(group = "com.googlecode.efficient-java-matrix-library", name = "core", version = "0.26")
    implementation(ejml)
    shade(ejml)

//    compileOnly(group = "com.mod-buildcraft", name = "buildcraft-lib", version = versionBuildCraft)
//    compileOnly(group = "com.mod-buildcraft", name = "buildcraft-main", version = versionBuildCraft)
//    ic2(group = "net.industrial-craft", name = "industrialcraft-2", version = "${versionIC2}-ex112", classifier = "dev")
}

/**
 * Used to execute tasks with use of the Gradle Wrapper from the XU2 Project.
 * Note: Yes I know this is cursed, Yes I tried a lot of stuff to make this work.
 */
fun execSourceTask(task: String) {
    val os = System.getProperty("os.name").toLowerCase()
    val javaHome = System.getProperty("java.home")

    val process = if (os.contains("windows")) {
        Runtime.getRuntime().exec("cmd /c set \"JAVA_HOME=$javaHome\" && \"${File(projectDir, "../gradle-3.0-wrapper").absolutePath}\\gradlew.bat\" $task", null, src)
    } else {
        //TODO: Test if this works on SH / Linux
        Runtime.getRuntime().exec("export JAVA_HOME=\"$javaHome\" && \"${File(projectDir, "../gradle-3.0-wrapper").absolutePath}\\gradlew\" $task", null, src)
    }

    do {
        Thread.sleep(50);
        process.inputStream.bufferedReader().lines().forEach(System.out::println)
        process.errorStream.bufferedReader().lines().forEach(System.out::println)
    } while (process.isAlive)

    val exitCode = process.exitValue();

    println("Process ended with exit code: $exitCode")

    if (exitCode != 0) throw IllegalStateException("Exit code different than 0! Something went wrong.")
}

//open class ApplyAstyle : DefaultTask() {
//    @get:InputFile
//    val input: RegularFileProperty = this.project
//            .objects
//            .fileProperty()
//
//    @get:OutputFile
//    val output: RegularFileProperty = this.project
//            .objects
//            .fileProperty()
//
//    @TaskAction
//    fun execute() {
//        val zipFile = ZipFile(input.get().asFile)
//        val out = ZipOutputStream(FileOutputStream(output.get().asFile))
//
//        for (entry in zipFile.entries()) {
//            if (entry.name.startsWith("ic2")) {
//                if (entry.name.endsWith(".java")) {
//                    val txt = BufferedReader(InputStreamReader(zipFile.getInputStream(entry)))
//                            .run {
//                                val builder = StringBuilder()
//                                forEachLine(builder::appendLine)
//                                builder.toString()
//                            }
//                    val reader = StringReader(txt)
//
//                    val newEntry = ZipEntry(entry.name)
//                    out.putNextEntry(newEntry)
//
//                    val outString = reader.readText().trimEnd() + System.lineSeparator()
//                    out.write(outString.toByteArray())
//                    out.closeEntry()
//                } else if (entry.name.startsWith("ic2/profiles") || entry.name.startsWith("ic2/sounds")) {
//                    val newEntry = ZipEntry(entry.name)
//                    out.putNextEntry(newEntry)
//                    out.write(zipFile.getInputStream(entry).readBytes())
//                    out.closeEntry()
//                }
//            }
//        }
//
//        out.close()
//    }
//}

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
            minecraft {
                mappings("snapshot", "20170624-1.12")
            }
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
        } else {
            sourceSets {
                main {
                    java {
                        srcDir("src/main/java")
                        srcDir("../1.10.2/src/compat/java")
                        srcDir("../1.10.2/src/compat111/java")
                        srcDir ("../1.10.2/src/api/java")
                    }
                    resources {
                        srcDir ("../1.10.2/src/main/resources")
                        srcDir ("../1.10.2/src/compat/resources")
                    }
                }
            }

        }

        if (project.name == "1.11") {
            dependencies {
                minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-14.23.5.2860")
                //minecraft(group = "net.minecraftforge", name = "forge", version = "1.11.2-13.20.1.2588")
                compileOnly(group = "mezz.jei", name = "jei_1.11", version = "4.1.1.208")
            }
            minecraft {
                mappings("snapshot", "20170624-1.12")
            }
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
            }
            minecraft {
                mappings("snapshot", "20170624-1.12")
            }
        }
    }
}