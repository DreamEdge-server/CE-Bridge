# libs/

Pinned CraftEngine development jars, used as `compileOnly` dependencies only. CI verifies each
SHA-256 before compiling, and none of them is bundled into the built bridge plugin or uploaded as a
release asset.

| Jar | SHA-256 | Used by |
| --- | --- | --- |
| `craft-engine-paper-plugin-26.7.4.jar` | `8771A23713BEABCFEFA46DD0B4C768D7EB1112DBDE803243C972C08EC157D406` | `26.2` and `1.21.11` targets |
| `craft-engine-paper-plugin-26.9.2.jar` | `19535F1987E8A27EBE8C6D811E7DEAE9A1F05DBD3EF3359435AFF5A3D819E0F9` | `26.3` target |
| `craft-engine-adventure-26.7.4.jar` | `726680EA16C6E3241D24D433602B9FD63DA7B527C4B96BE39FDC49D023A88BEC` | compile-time support for every target |

`craft-engine-adventure-26.7.4.jar` is CraftEngine's own relocated Adventure API artifact from
`https://repo.momirealms.net/releases/`. CraftEngine downloads and relocates Adventure at runtime, so
the plugin jar does not contain `net.momirealms.craftengine.libraries.adventure.*`; from 26.9.2 on,
`Context extends Pointered`, so that parent type has to be on the compile classpath or `javac` fails
to resolve `ItemBuildContext` with "cannot find class file for Pointered".

To compile against another CraftEngine version, provide its path explicitly:

```
./gradlew shadowJar -PcraftEngineJar=/path/to/craft-engine-paper-plugin.jar
```

CraftEngine remains separate third-party software and is not covered by this project's MIT license.
Running a server still requires installing a compatible CraftEngine plugin separately.
