# libs/

The repository includes three CraftEngine development jars, all used as `compileOnly` dependencies.
They are verified by CI and are never bundled into the built bridge plugin or uploaded as a standalone
release asset.

- `craft-engine-paper-plugin-26.7.4.jar` — community CraftEngine build used by the `26.x` (26.2) and
  `1.21.11` targets.
- `craft-engine-paper-plugin-26.9.2.jar` — official CraftEngine 26.9.2 release from Modrinth, used by
  the `26.3` target.
- `craft-engine-adventure-26.7.4.jar` — CraftEngine's own relocated Adventure API artifact from
  `https://repo.momirealms.net/releases/`. CraftEngine downloads and relocates Adventure at runtime,
  so the plugin jar does not contain `net.momirealms.craftengine.libraries.adventure.*`; from 26.9.2
  on, `Context extends Pointered`, so that parent type has to be on the compile classpath or `javac`
  fails to resolve `ItemBuildContext` with "cannot find class file for Pointered".

To compile against another CraftEngine version, provide its path explicitly:

```
./gradlew shadowJar -PcraftEngineJar=/path/to/craft-engine-paper-plugin.jar
```

CraftEngine remains separate third-party software and is not covered by this project's MIT license.
Running a server still requires installing a compatible CraftEngine plugin separately.
