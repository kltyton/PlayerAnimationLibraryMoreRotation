# Runtime version matrix

The root targets Minecraft 26.2. Older versions are independent Gradle projects under `versions/<Minecraft>/`. All projects produce MoreRotation 1.1.0 for Fabric and NeoForge and embed the matching Bendable Cuboids artifact.

## Dependencies

| Minecraft | Java | PAL | Bendable Cuboids | Fabric Loader | Fabric API | NeoForge |
| --- | --- | --- | --- | --- | --- | --- |
| 1.21.1 | 21 | 1.1.6+mc.1.21.1 | 2.0.0+alpha.1 | 0.19.5 | 0.116.17+1.21.1 | 21.1.250 |
| 1.21.7 | 21 | 1.0.12+mc.1.21.8 | 1.0.5 | 0.19.5 | 0.129.0+1.21.7 | 21.7.25-beta |
| 1.21.8 | 21 | 1.1.2+mc.1.21.8 | 1.0.5 | 0.19.5 | 0.136.1+1.21.8 | 21.8.54 |
| 1.21.9 | 21 | 1.1.3+mc.1.21.9 | 1.0.7 | 0.19.5 | 0.134.1+1.21.9 | 21.9.16-beta |
| 1.21.10 | 21 | 1.1.3+mc.1.21.9 | 1.0.7 | 0.19.5 | 0.138.4+1.21.10 | 21.10.64 |
| 1.21.11 | 21 | 1.1.10+mc.1.21.11 | 2.0.1 | 0.19.5 | 0.141.6+1.21.11 | 21.11.45 |
| 26.1 | 25 | 1.2.6+mc.26.1 | 2.0.2 | 0.19.5 | 0.145.1+26.1 | 26.1.0.19-beta |
| 26.1.1 | 25 | 1.2.6+mc.26.1 | 2.0.2 | 0.19.5 | 0.145.4+26.1.1 | 26.1.1.15-beta |
| 26.1.2 | 25 | 1.2.6+mc.26.1 | 2.0.2 | 0.19.5 | 0.155.3+26.1.2 | 26.1.2.109 |
| 26.2 | 25 | 1.2.6+mc.26.2 | 2.0.4 | 0.19.5 | 0.160.0+26.2 | 26.2.0.87 |

The 1.21.1 Bendable Cuboids port is submitted to the corresponding upstream branch as [PR #40](https://github.com/PlayerAnimationLibrary/BendableCuboids/pull/40). The 26.1 family uses 2.0.2 because the loader metadata inside 2.0.4 requires 26.2. Historical split-loader artifacts are pinned by Modrinth version ID in their Gradle properties.

## Acceptance and final builds

All 20 loader/version combinations passed the user's in-game animation acceptance. The four additional constant-pose regressions (1.21.7/1.21.8, Fabric/NeoForge) also passed. All ten final builds succeeded after removing temporary acceptance commands, registrations, and animation resources.

| Minecraft | Fabric animation | NeoForge animation | Constant pose, both loaders | Final clean build |
| --- | --- | --- | --- | --- |
| 1.21.1 | passed | passed | not required | passed |
| 1.21.7 | passed | passed | passed | passed |
| 1.21.8 | passed | passed | passed | passed |
| 1.21.9 | passed | passed | not required | passed |
| 1.21.10 | passed | passed | not required | passed |
| 1.21.11 | passed | passed | not required | passed |
| 26.1 | passed | passed | not required | passed |
| 26.1.1 | passed | passed | not required | passed |
| 26.1.2 | passed | passed | not required | passed |
| 26.2 | passed | passed | not required | passed |

## Published release files

MoreRotation 1.1.0 files for each Minecraft version and loader. Modrinth versions are publicly listed. CurseForge accepted all uploads; file links become available after platform processing and review.

| Minecraft | Fabric, Modrinth | NeoForge, Modrinth | Fabric, CurseForge | NeoForge, CurseForge |
| --- | --- | --- | --- | --- |
| 1.21.1 | [99grUAKV](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/99grUAKV) | [qhKVkTkf](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/qhKVkTkf) | [8871882](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871882) | [8871888](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871888) |
| 1.21.7 | [mWHKP7OI](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/mWHKP7OI) | [wsaHhC41](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/wsaHhC41) | [8871889](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871889) | [8871890](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871890) |
| 1.21.8 | [Lbmw3UUU](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/Lbmw3UUU) | [GHDzTR6S](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/GHDzTR6S) | [8871891](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871891) | [8871892](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871892) |
| 1.21.9 | [DVhqJg8a](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/DVhqJg8a) | [dyLnhgHO](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/dyLnhgHO) | [8871893](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871893) | [8871894](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871894) |
| 1.21.10 | [dzMA1bav](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/dzMA1bav) | [I4XQTnGs](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/I4XQTnGs) | [8871895](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871895) | [8871896](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871896) |
| 1.21.11 | [FAVS7iVp](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/FAVS7iVp) | [EQjYuGdI](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/EQjYuGdI) | [8871897](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871897) | [8871898](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871898) |
| 26.1 | [c3Jjlys6](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/c3Jjlys6) | [2EkhSh8r](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/2EkhSh8r) | [8871899](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871899) | [8871900](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871900) |
| 26.1.1 | [BFVR6Rtk](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/BFVR6Rtk) | [ekgwyVen](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/ekgwyVen) | [8871901](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871901) | [8871902](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871902) |
| 26.1.2 | [agFhU5Ov](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/agFhU5Ov) | [23xRtQNb](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/23xRtQNb) | [8871903](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871903) | [8871904](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871904) |
| 26.2 | [hI8oLSCG](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/hI8oLSCG) | [2XJWysuR](https://modrinth.com/mod/playeranimationlibrarymorerotation/version/2XJWysuR) | [8871905](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871905) | [8871906](https://www.curseforge.com/minecraft/mc-mods/player-animation-library-more-rotation/files/8871906) |

## Artifacts and evidence

Distribution files are in each project's `fabric/build/libs/` and `neoforge/build/libs/`. The 20 release jars and matching source jars passed metadata, embedded-dependency, license, and temporary-content checks.

| Minecraft | Fabric JAR | NeoForge JAR |
| --- | --- | --- |
| 1.21.1 | [fabric](../versions/1.21.1/fabric/build/libs/playeranimationlibrarymorerotation-fabric-1.21.1-1.1.0.jar) | [neoforge](../versions/1.21.1/neoforge/build/libs/playeranimationlibrarymorerotation-neoforge-1.21.1-1.1.0.jar) |
| 1.21.7 | [fabric](../versions/1.21.7/fabric/build/libs/playeranimationlibrarymorerotation-fabric-1.21.7-1.1.0.jar) | [neoforge](../versions/1.21.7/neoforge/build/libs/playeranimationlibrarymorerotation-neoforge-1.21.7-1.1.0.jar) |
| 1.21.8 | [fabric](../versions/1.21.8/fabric/build/libs/playeranimationlibrarymorerotation-fabric-1.21.8-1.1.0.jar) | [neoforge](../versions/1.21.8/neoforge/build/libs/playeranimationlibrarymorerotation-neoforge-1.21.8-1.1.0.jar) |
| 1.21.9 | [fabric](../versions/1.21.9/fabric/build/libs/playeranimationlibrarymorerotation-fabric-1.21.9-1.1.0.jar) | [neoforge](../versions/1.21.9/neoforge/build/libs/playeranimationlibrarymorerotation-neoforge-1.21.9-1.1.0.jar) |
| 1.21.10 | [fabric](../versions/1.21.10/fabric/build/libs/playeranimationlibrarymorerotation-fabric-1.21.10-1.1.0.jar) | [neoforge](../versions/1.21.10/neoforge/build/libs/playeranimationlibrarymorerotation-neoforge-1.21.10-1.1.0.jar) |
| 1.21.11 | [fabric](../versions/1.21.11/fabric/build/libs/playeranimationlibrarymorerotation-fabric-1.21.11-1.1.0.jar) | [neoforge](../versions/1.21.11/neoforge/build/libs/playeranimationlibrarymorerotation-neoforge-1.21.11-1.1.0.jar) |
| 26.1 | [fabric](../versions/26.1/fabric/build/libs/playeranimationlibrarymorerotation-fabric-26.1-1.1.0.jar) | [neoforge](../versions/26.1/neoforge/build/libs/playeranimationlibrarymorerotation-neoforge-26.1-1.1.0.jar) |
| 26.1.1 | [fabric](../versions/26.1.1/fabric/build/libs/playeranimationlibrarymorerotation-fabric-26.1.1-1.1.0.jar) | [neoforge](../versions/26.1.1/neoforge/build/libs/playeranimationlibrarymorerotation-neoforge-26.1.1-1.1.0.jar) |
| 26.1.2 | [fabric](../versions/26.1.2/fabric/build/libs/playeranimationlibrarymorerotation-fabric-26.1.2-1.1.0.jar) | [neoforge](../versions/26.1.2/neoforge/build/libs/playeranimationlibrarymorerotation-neoforge-26.1.2-1.1.0.jar) |
| 26.2 | [fabric](../fabric/build/libs/playeranimationlibrarymorerotation-fabric-26.2-1.1.0.jar) | [neoforge](../neoforge/build/libs/playeranimationlibrarymorerotation-neoforge-26.2-1.1.0.jar) |

Local evidence:

- `build/upgrade-audit/manual-port-acceptance.json`: per-loader human acceptance and exact runtime log names.
- `build/upgrade-audit/final-build-results.json`: final clean build result for all ten projects.
- `build/upgrade-audit/final-artifact-verification.json`: metadata/license/content checks and embedded dependency hashes.
- `build/upgrade-audit/SHA256SUMS.txt`: final distribution jar hashes.

See [validation.md](validation.md) for commands, Blender/Blockbench evidence, and verification limits.
