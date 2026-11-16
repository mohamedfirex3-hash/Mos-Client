# Mos Client (source project)

A Fabric client-side mod starter for Minecraft Java 1.21.11. Press **Right Shift** to open the dark/orange Mos Client menu.

## Build on Windows
1. Install JDK 21 and Git.
2. Install Gradle (or add a Gradle wrapper using a compatible Gradle 8.x/9.x release supported by Fabric Loom).
3. From this folder run `gradle build`.
4. The output JAR will be under `build/libs/`.

The UI is a visual menu prototype: feature toggles currently change their displayed state only. They are not wired to actual gameplay features yet. Microsoft login belongs to the launcher, not to this mod.

This source project has not been compiled or tested in Minecraft in the current environment. Verify the Fabric Loom and Yarn versions against the official Fabric Maven before building.
