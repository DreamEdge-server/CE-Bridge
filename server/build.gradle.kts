plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = "com.ceclientbridge"
version = "1.2.0"

data class ServerProfile(
    val paperDevBundle: String,
    val javaVersion: Int,
    val targetFamily: String,
    val craftEngineJar: String
)

val target = providers.gradleProperty("target")
    .orElse(providers.gradleProperty("bridgeTarget"))
    .orElse("26.2")
    .get()
val profiles = mapOf(
    // 26.2 is the 26-family baseline; 26.3 is the newest 26-family target. Both keep
    // targetFamily "26.x", the wire label BridgeHandshake understands, so 26.2/26.3
    // clients and servers remain mutually compatible.
    "26.2" to ServerProfile("26.2.build.65-beta", 25, "26.x", "libs/craft-engine-paper-plugin-26.7.4.jar"),
    "26.3" to ServerProfile("26.3.build.145-beta", 25, "26.x", "libs/craft-engine-paper-plugin-26.9.2.jar"),
    // 1.21.11 pairs with client-legacy/ so a 1.21.11 server can serve the legacy
    // Fabric client. Java 21 keeps 1.21.11 loadable.
    "1.21.11" to ServerProfile("1.21.11-R0.1-SNAPSHOT", 21, "1.21.11", "libs/craft-engine-paper-plugin-26.7.4.jar")
)
val profile = profiles[target] ?: throw GradleException(
    "Unsupported or unavailable server target '$target'. Available profiles: ${profiles.keys.joinToString()}")
val craftEngineJar = providers.gradleProperty("craftEngineJar")
    .orElse(profile.craftEngineJar)
    .get()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(profile.javaVersion))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // NMS access (ServerPlayer/RecipeManager/etc.) for the recipe-resync workaround - paper-api alone
    // isn't enough for that, unlike the rest of this plugin which only needs the public Bukkit API.
    paperweight.paperDevBundle(profile.paperDevBundle)
    // CraftEngine：物品/方块/配方公开 API 来源，版本随 target 走（26.2/1.21.11 用 26.7.4，26.3 用 26.9.2），用本地 jar
    compileOnly(files(craftEngineJar))
    // CraftEngine 把 Adventure 重定位到 net.momirealms.craftengine.libraries.adventure.*，这些类不在插件 jar 里
    // （运行时由 CraftEngine 自己下载并重定位）。26.9.2 起 Context extends Pointered，javac 解析 ItemBuildContext
    // 时必须有该父类型，否则报「找不到 Pointered 的类文件」。用 CraftEngine 自己发布的同名构件补齐编译类路径。
    compileOnly(files("libs/craft-engine-adventure-26.7.4.jar"))
}

sourceSets {
    main {
        java.srcDir("../protocol/src/main/java")
        java.srcDir(if (profile.targetFamily == "1.21.11") "src/1.21.11/java" else "src/26.x/java")
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(profile.javaVersion)
    }
    compileTestJava {
        options.encoding = "UTF-8"
        options.release.set(profile.javaVersion)
    }
    register<JavaExec>("bridgeChannelsTest") {
        dependsOn(testClasses)
        classpath = sourceSets.test.get().runtimeClasspath
        mainClass.set("com.ceclientbridge.net.BridgeChannelsTest")
    }
    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("CraftEngineClientBridge-${project.version}-${target}.jar")
    }
    build {
        dependsOn(shadowJar)
    }
    processResources {
        filteringCharset = "UTF-8"
        expand("version" to project.version, "bridge_target" to profile.targetFamily)
    }
}
