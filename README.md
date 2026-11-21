# WorldWeaver End Fix

An unofficial compatibility build of WorldWeaver: New Dawn 21.0.31 for Minecraft 1.21.1 NeoForge. The End biome hook composes additional biome results with the generated stream. World configuration saving is synchronized so parallel generation can share the configuration file.

Install Java 21 and the matching NeoForge 1.21.1 version. Replace the existing WorldWeaver 21.0.31 JAR with the JAR from Releases. Keep the upstream dependencies of WorldWeaver. The replacement retains the `wover` mod ID.

## Rebuild

Download [WorldWeaver: New Dawn 21.0.31](https://modrinth.com/mod/worldweaver-neoforge/version/KxUUeX9u), ASM 9.9.1 and ASM Tree 9.9.1. Use Python 3.11+ and Java 21.

```sh
python build.py worldweaver-21.0.31.jar --asm asm-9.9.1.jar --asm-tree asm-tree-9.9.1.jar --output worldweaver-21.0.31-end-fix-r2.jar
```

The patch programs and original icon changes use MIT. The archive retains the MIT notices of [WorldWeaver: New Dawn](https://github.com/Reijin2312/WorldWeaver_Neoforge), the original WorldWeaver and NetherDescent. Report compatibility issues for this build here.
