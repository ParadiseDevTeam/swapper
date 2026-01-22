import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    idea
    alias(libs.plugins.kotlin)
    alias(libs.plugins.runPaper)
    alias(libs.plugins.pluginYml)
}

group = "me.prdis"
version = "0.0.1"
val codeName = "swapper"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    library(kotlin("stdlib"))

    compileOnly(libs.paper)
    compileOnly(libs.cloud)

    bukkitLibrary(libs.cloud)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks {
    jar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")
        archiveVersion.set("")
    }
    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs = listOf("-Xms4096M", "-Xmx4096M", "--add-modules=jdk.incubator.vector", "-XX:+UseG1GC", "-XX:+ParallelRefProcEnabled", "-XX:MaxGCPauseMillis=200", "-XX:+UnlockExperimentalVMOptions", "-XX:+DisableExplicitGC", "-XX:+AlwaysPreTouch", "-XX:G1HeapWastePercent=5", "-XX:G1MixedGCCountTarget=4", "-XX:InitiatingHeapOccupancyPercent=15", "-XX:G1MixedGCLiveThresholdPercent=90", "-XX:G1RSetUpdatingPauseTimePercent=5", "-XX:SurvivorRatio=32", "-XX:+PerfDisableSharedMem", "-XX:MaxTenuringThreshold=1", "-Dusing.aikars.flags=https://mcflags.emc.gs", "-Daikars.new.flags=true", "-XX:G1NewSizePercent=30", "-XX:G1MaxNewSizePercent=40", "-XX:G1HeapRegionSize=8M", "-XX:G1ReservePercent=20", "-Dcom.mojang.eula.agree=true")
    }
}

idea {
    module {
        excludeDirs.addAll(listOf(file("run"), file("out"), file(".idea"), file(".kotlin")))
    }
}

bukkit {
    val mc = libs.versions.minecraft.get()
    val split = mc.split(".")

    name = rootProject.name
    version = rootProject.version.toString()
    main = "${project.group}.${codeName}.plugin.${codeName.replaceFirstChar { it.uppercase() }}Plugin"

    apiVersion = split.apply { if (split.size == 3) this.dropLast(1) }.joinToString(".")
}
