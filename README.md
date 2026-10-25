# 📦 ResourceLibrary
**ResourceLibrary** is a lightweight Spigot/Paper plugin library that allows other plugins to dynamically register resource pack files at startup. It generates and serves a custom ZIP pack over HTTP and automatically sends it to players using Minecraft’s native ``setResourcePack()`` API — no manual file hosting required.
***
# ✨ Features
- 📁 Merge JSON models, textures, and more from multiple plugins
- 🚀 Hosts the resource pack with an embedded HTTP server (no config needed!)
- 🧠 JSON merging with conflict resolution (as configured!)
***
# 🧩 Usage
## 1. Install
### Gradle (Kotlin DSL):
```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.Bogieloo:ResourceLibrary:1.1")
}
```
### Maven:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Bogieloo</groupId>
        <artifactId>ResourceLibrary</artifactId>
        <version>1.1</version>
    </dependency>
</dependencies>
```
***
## 2. Register Your Resources
In your plugin’s ``onEnable()``:
```java
@Override
public void onEnable() {
    ResourcePackManager.getInstance().registerResource(
        "assets/minecraft/models/item/custom_sword.json",
        new File(getDataFolder(), "custom_sword.json"),
        false
    );

    ResourcePackManager.getInstance().registerResourceEntry(this, "tanks", true);
}
```
***
## 3. Install ResourceLibrary on the Server
- Drop the ResourceLibrary JAR into the ``plugins/`` folder.
- Make sure port ``61234`` is open to all players!
- Enjoy!
***
Made with ❤️ by devs for devs.

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/Q5Q3LZWWC)
