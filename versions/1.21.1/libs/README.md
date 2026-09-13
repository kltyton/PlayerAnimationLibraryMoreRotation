# Bendable Cuboids 1.21.1 dependency

The local Maven repository contains the Fabric and NeoForge builds of Bendable Cuboids `2.0.0+alpha.1+mc.1.21.1`, used for compilation and jar-in-jar packaging.

Source: [PlayerAnimationLibrary/BendableCuboids PR #40](https://github.com/PlayerAnimationLibrary/BendableCuboids/pull/40), targeting branch `1.21.1`. Tested source commit: `d18f156110aa03202c0aab40cedb2f5da75d6aa9` on the contributing fork. Build with Java 21 and `gradlew.bat build -Pmod_version=2.0.0+alpha.1`. The two artifacts are `fabric/build/libs/BendableCuboidsFabric-2.0.0+alpha.1+mc.1.21.1.jar` and `forge/build/libs/BendableCuboidsForge-2.0.0+alpha.1+mc.1.21.1.jar`.

Bendable Cuboids is MIT licensed; its license is supplied alongside these jars and in the parent mod's `META-INF/LICENSE-bendable-cuboids.txt`. The POM files provide local coordinates only; the runtime PAL dependency is declared separately by this project.
