# Validation

Date: 2026-09-13

## Runtime acceptance

The user confirmed the temporary animation tests on all 20 combinations: Fabric and NeoForge for Minecraft 1.21.1, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, and 26.2. The root targets 26.2; other projects are under `versions/<Minecraft>/`.

The animation campaign exercised ordinary rotation/position/scale, native X bend, extended Y/Z bend, bend position/scale, combined transforms, and stopping playback. Visual acceptance comes from the user's in-game checks. Gradle success and log inspection establish startup/exit and resource/Mixin behavior, not visual correctness by themselves.

Additional constant-pose regressions on both loaders of 1.21.7 and 1.21.8 all passed user acceptance. These specifically verify that Bendable Cuboids 1.0.5 rewriting native vertices each frame does not erase unchanged extended deformation, and that stopping clears the pose.

Per-loader results and exact accepted log names are recorded in `build/upgrade-audit/manual-port-acceptance.json`. Logs live in the same directory. The dependency versions are listed in [version-matrix.md](version-matrix.md).

The 1.21.1 Bendable Cuboids port was also tested by entering the overworld without crashing on both loaders, satisfying the user's acceptance criterion. Its actual animation rendering was accepted with MoreRotation. The patch is submitted to the upstream `1.21.1` branch as [PR #40](https://github.com/PlayerAnimationLibrary/BendableCuboids/pull/40).

## Build and artifact verification

All ten final clean builds passed after removing the temporary command, registration hooks, and generated animation. All 20 distribution jars and matching source jars passed the package verifier. Existing user samples under `test/` are preserved. Results are recorded in `build/upgrade-audit/final-build-results.json`, `final-artifact-verification.json`, and `SHA256SUMS.txt`.

Commands for independent version projects, with Java 21 for 1.21.x and Java 25 for 26.x:

```powershell
.\versions\<Minecraft>\gradlew.bat -p versions/<Minecraft> clean build
.\versions\<Minecraft>\gradlew.bat -p versions/<Minecraft> :fabric:runClient
.\versions\<Minecraft>\gradlew.bat -p versions/<Minecraft> :neoforge:runClient
```

Root build command preserves the audit directory while cleaning all output modules:

```powershell
.\gradlew.bat :common:clean :fabric:clean :neoforge:clean build
.\gradlew.bat :fabric:runClient
.\gradlew.bat :neoforge:runClient
```

Final package verification checks both loader metadata files, embedded Bendable Cuboids and Mixinsquared paths, the Bendable Cuboids license, resource/data pack metadata, absence of temporary acceptance files in binaries and source jars, and the SHA-256 of the embedded 1.21.1 port against the manually tested dependency. The verifier is `build/upgrade-audit/verify_delivery.py`.

The development `runClient` launches and distribution-jar structural inspection are separate evidence. The final distribution jars have not been launched in a separate installed modpack.

## Blender and Blockbench

The tools repository contains the unmodified upstream Blender snapshot at commit `4f7d946ef537cf75ab54f021f140bc75456c89d2`, including the August 2026 changes. Its upstream GPL license and provenance remain in that directory. The main tools and official plugin copies are version 0.4.1 and byte-identical.

- Both plugin copies passed seven Node regression tests covering animation-local rig metadata, loop/PAL metadata preservation, per-axis Bezier handles, sparse disabled axes, mirroring, and curved-track retarget sampling.
- Blender 5.2.1 exported the real `cartwheel` action: length 1.292 seconds, `loopTick` 0.208, nine bones, parent/model metadata, and icon data. The exporter restored the original action and action slot. Evidence: `build/upgrade-audit/blender-export/result.json` and `blender-export.log`.
- Blockbench 5.2.0 beta 2 loaded the plugin and completed a real import/export/save/reopen round trip in an isolated profile. Evidence: `build/upgrade-audit/blockbench-blender-roundtrip-final.png` and `blender-export/blockbench-roundtrip-final.json` / `.bbmodel`.

## Reproduction and limits

To perform further animation checks after the temporary command is removed, place an animation in `assets/<namespace>/player_animations/` and call `PalMoreAnimations.play(serverPlayer, animationId)` / `stop(serverPlayer)`, or the client-local API. The preserved `test/assets/` animations are optional inputs; they are not shipped in the mod. See the [README](../README.md) for playback API examples.

Check each player part and second skin, native X-only bend, Y/Z-only bend, nested `bend.rotation` / `bend.position` / `bend.scale`, transitions to an animation without those tracks, and held-item alignment. Torso/body follows the upper segment of native bend; limbs follow the lower segment. Angle units and hook locations are documented in [implementation.md](implementation.md).

Development account Realms/user-property authentication messages and host OS performance-counter warnings do not indicate a MoreRotation failure. A final 1.21.9 NeoForge launch with corrected shared `pack.mcmeta` entered the overworld, saved, and exited successfully without pack metadata errors; see `build/upgrade-audit/1.21.9-neoforge-metadata-recheck.log`. That development launcher still reports a missing custom Log4j context selector and uses its default selector; it did not prevent world entry or affect the animation acceptance.

Dedicated-server startup, remote multiplayer, broad third-party modpack compatibility, performance benchmarks, and every possible animation/rig combination were not validated in this campaign. Retargeted curved tracks are sampled at 60 Hz, and custom bones require matching geometry in the destination model.
