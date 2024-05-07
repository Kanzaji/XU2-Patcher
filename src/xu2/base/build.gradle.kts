import de.undercouch.gradle.tasks.download.Download
import org.gradle.internal.impldep.org.bouncycastle.util.encoders.UTF8

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
            gradleScripts.forEach {
                val file = File(src, it)
                file.writeText(file.readText()
                    .replace("jcenter()", "mavenCentral()")
                    .replace("http://", "https://")
                    .replace("parseConfig(file('../1.10.2/private.properties'))", "parseConfig(file('../1.10.2/version.properties'))")
                )
            }

            // Using own Gradle 3.0 wrapper to make everything work, as XU2 project is cursed in structure,
            // use execSourceTask fun for command execution on the source code.
            execSourceTask("--refresh-dependencies dependencies")

            // Strips new lines at the end of the source files, as those cause random patches to generate.
            fileTree(src) {
                include("**/*.java")
                forEach { file ->
                    val content = file.readText(Charsets.UTF_8)
                    if (content.endsWith("\n")) file.writeText(content.removeSuffix("\n"))
                }
            }
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
        from(src) { include("**/*.java") }
    }

    register<Jar>("Source Jar ~ 1.12 (Base)") {
        group = taskGroup
        if (!srcExists()) dependsOn("Setup Source")
        archiveClassifier.set("sources")
        archiveBaseName.set("XU2-Base-1.12")
        from(src) {
            include("1.10.2/src/main/java/**")
            include("1.12/src/main/java/**")
            eachFile {
                // A bit crude, but I don't have any other ideas :P
                // It does result in empty folders, but those *shouldn't* be a problem.
                this.path = this.path
                    .replaceFirst("1.10.2/src/main/java/", "")
                    .replaceFirst("1.12/src/main/java/", "")
            }
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    register<Jar>("Source Jar ~ 1.11 (Base)") {
        group = taskGroup
        if (!srcExists()) dependsOn("Setup Source")
        archiveClassifier.set("sources")
        archiveBaseName.set("XU2-Base-1.11")
        from(src) {
            include("1.10.2/src/main/java/**")
            include("1.10.2/src/compat111/java/**")
            include("1.11/src/main/java/**")
            eachFile {
                // A bit crude, but I don't have any other ideas :P
                // It does result in empty folders, but those *shouldn't* be a problem.
                this.path = this.path
                    .replaceFirst("1.10.2/src/main/java/", "")
                    .replaceFirst("1.10.2/src/compat111/java/", "")
                    .replaceFirst("1.11/src/main/java/", "")
            }
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    register<Jar>("Source Jar ~ 1.10 (Base)") {
        group = taskGroup
        if (!srcExists()) dependsOn("Setup Source")
        archiveClassifier.set("sources")
        archiveBaseName.set("XU2-Base-1.10")
        from(project("Source:1.10.2").sourceSets.main.get().allJava)
    }
}

fun buildSource(ver: String) {
    val libs = File(src, "$ver/build/libs")
    val final = File(buildDir, "libs/$ver")
    execSourceTask(":$ver:jar")

    final.mkdirs()
    val jar = libs.listFiles()?.get(0)
    jar?.copyTo(File(final, jar.name), overwrite = true)
    libs.deleteRecursively()
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