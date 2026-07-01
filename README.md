# PlayerAnimationLibraryMoreRotation

Language: English | [中文](README_zh.md)

PlayerAnimationLibraryMoreRotation is a Fabric / NeoForge runtime compatibility
library for Player Animation Library and bendable-cuboids. It uses targeted
Mixin patches to extend player animation behavior and also provides a small PAL
playback/sync API for downstream mods.

## Supported Versions

- Library: `1.0.2`
- Minecraft: `26.1.2`
- Fabric Loader: `0.19.3`
- Fabric API: `0.153.0+26.1.2`
- NeoForge: `26.1.2.76`
- Player Animation Library Fabric / Neo: `1.2.4+mc.26.1`
- bendable-cuboids: `2.0.2`
- Java: `25`

## Features

- Adds Y/Z sidecar data and rendering support for PAL player bone `bend`.
- Adds bend-local `position` and `scale` support through `bend.position` and
  `bend.scale` on real player bones.
- Keeps legacy X-only bend behavior compatible. `bend: 35` and
  `bend: [35, 0, 0]` still work.
- Supports vector bend JSON such as `bend: [x, y, z]`, `post: [x, y, z]`, and
  `pre: [x, y, z]`.
- Extends `left_item` / `right_item` held-item Y/Z rotation handling.
- Sorts JSON keyframes by timestamp for more predictable exported animations.
- Provides reusable play, stop, server sync, and client receive APIs for PAL
  animations.
- Adds controller-aware playback and controller-local handlers for PAL
  `timeline`, `sound_effects`, and `particle_effects` keyframes.

## Gradle Dependency

Published artifact ids include the loader name and Minecraft version:

```gradle
repositories {
    mavenLocal()
}

dependencies {
    modImplementation "com.kltyton:playeranimationlibrarymorerotation-fabric-26.1.2:1.0.2"
    // or:
    implementation "com.kltyton:playeranimationlibrarymorerotation-neoforge-26.1.2:1.0.2"
}
```

## Animation Resource Path

PAL still loads animations from:

```text
assets/<namespace>/player_animations/*.json
```

The runtime animation id is:

```text
<namespace>:<key inside the animations object>
```

Example:

```text
assets/mob_battle/player_animations/poison_knife_animation.json
```

with:

```json
{
  "animations": {
    "poison_knife_animation": {}
  }
}
```

uses this animation id:

```text
mob_battle:poison_knife_animation
```

## Bend JSON Format

This library only supports `bend` on real player bones. Do not write bend
transforms as extra virtual bones.

Supported form 1: legacy scalar bend, equivalent to `[x, 0, 0]`:

```json
{
  "bones": {
    "right_arm": {
      "bend": 35
    }
  }
}
```

Supported form 2: three-axis array or legacy timeline object. If a `bend`
object has no `rotation`, `position`, or `scale` member, it is treated as the
rotation track:

```json
{
  "bones": {
    "torso": {
      "bend": [35, 10, -15]
    },
    "right_arm": {
      "bend": {
        "0.0": { "post": [0, 0, 0] },
        "0.5": { "post": [35, 20, -15] },
        "1.0": { "post": [0, 0, 0] }
      }
    }
  }
}
```

Notes:

- X bend still flows through PAL / bendable-cuboids' native path.
- Y/Z bend is stored in this library's sidecar data and applied in the
  bendable-cuboids rendering path.
- `{"value": 35}` is treated as `[35, 0, 0]`.
- bendable-cuboids itself still exposes a single-axis bend API. This library
  layers Y/Z bend as a compatibility mesh transform.

## Bend Bone Position And Scale

Supported form 3: bend channels under the same real player bone:

```json
{
  "bones": {
    "left_arm": {
      "bend": {
        "rotation": {
          "0.0": { "post": [0, 0, 0] },
          "0.5": { "post": [95, 60, -50] },
          "1.0": { "post": [0, 0, 0] }
        },
        "position": {
          "0.0": { "post": [0, 0, 0] },
          "0.5": { "post": [2, 1, 0] },
          "1.0": { "post": [0, 0, 0] }
        },
        "scale": {
          "0.0": { "post": [1, 1, 1] },
          "0.5": { "post": [1.25, 0.8, 1] },
          "1.0": { "post": [1, 1, 1] }
        }
      }
    }
  }
}
```

`bend.rotation` is the same vector bend channel as the old `bend` timeline.
`bend.position` / `bend.scale` apply to the same rendered body part. `post`,
`pre`, `vector`, and `{"value": ...}` keyframes are supported.

Notes:

- `position` uses the same model units as PAL / Minecraft model parts.
- `scale` is a multiplier. `1` means original size, not zero.
- `bend.position` and `bend.scale` modify only the lower bend-segment mesh
  vertices; they do not move or resize the whole arm, leg, body part, or joint.
- Existing animations without `bend.position` / `bend.scale` are unchanged.

## Server Playback API

```java
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimations;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

Identifier animation = Identifier.fromNamespaceAndPath("mob_battle", "poison_knife_animation");

PalMoreAnimations.play((ServerPlayer) player, animation);
PalMoreAnimations.stop((ServerPlayer) player);
```

The server broadcasts to same-dimension clients that support this library's
payload.

Optional controller selection:

```java
Identifier attackController = Identifier.fromNamespaceAndPath("mob_battle", "attack_controller");

PalMoreAnimations.play((ServerPlayer) player, animation, attackController);
```

The server sends only the controller id. Handler lambdas are local JVM state and
must be registered on the physical client before playback.

## Client Playback API

```java
import com.kltyton.playeranimationlibrarymorerotation.client.PalMoreClientAnimations;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;

PalMoreClientAnimations.playLocal(
        avatar,
        Identifier.fromNamespaceAndPath("mob_battle", "poison_knife_animation")
);

PalMoreClientAnimations.stopLocal(avatar);
```

Optional first-person configuration:

```java
PalMoreClientAnimations.playLocal(
        avatar,
        animation,
        PalMoreFirstPersonOptions.SHOW_ARMS_AND_ITEMS
);
```

## Keyframe Controller API

PAL already parses Blockbench / Bedrock effect tracks:

```json
{
  "timeline": {
    "0.3333": "runAttack;"
  }
}
```

It also parses `sound_effects` and `particle_effects`. This library lets a
downstream mod attach controller-local handlers to those native PAL keyframes:

```java
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController;
import com.kltyton.playeranimationlibrarymorerotation.client.PalMoreClientAnimations;
import net.minecraft.resources.Identifier;

public static final Identifier ATTACK_CONTROLLER =
        Identifier.fromNamespaceAndPath("mob_battle", "attack_controller");

public static final PalMoreAnimationController ATTACK_PLAYBACK =
        PalMoreAnimationController.create(ATTACK_CONTROLLER)
                .setCustomInstructionKeyframeHandler(s -> {
                    String instruction = s.instructions().replaceAll("\\s+", "");
                    if ("runAttack;".equals(instruction)) {
                        // Client-side visual action here.
                    }
                })
                .setParticleKeyframeHandler(s -> {
                    String effect = s.effect();
                    String locator = s.locator();
                    String script = s.script();
                })
                .setSoundKeyframeHandler(s -> {
                    String sound = s.sound();
                });

public static void onClientInit() {
    PalMoreClientAnimations.registerController(ATTACK_PLAYBACK);
}
```

Then trigger it from server code:

```java
PalMoreAnimations.play((ServerPlayer) player, animation, ATTACK_CONTROLLER);
```

For purely local playback, a controller object can be passed directly:

```java
PalMoreClientAnimations.playLocal(avatar, animation, ATTACK_PLAYBACK);
```

Controller handlers run on the client-side PAL playback path. Server-authority
gameplay effects should still be executed by server-side game logic or by a
separate validated network flow.

## Network Payload

Clientbound payload id:

```text
playeranimationlibrarymorerotation:player_animation
```

Fields:

```text
avatarEntityId: VarInt
animationId: Identifier
stop: Boolean
controllerId: Identifier
```

Downstream mods do not need to register this payload or receiver again.

## Entrypoints

- Fabric main entrypoint:
  `com.kltyton.playeranimationlibrarymorerotation.fabric.PlayeranimationlibrarymorerotationFabric`
- Fabric client entrypoint:
  `com.kltyton.playeranimationlibrarymorerotation.fabric.client.PlayeranimationlibrarymorerotationFabricClient`
- NeoForge mod entrypoint:
  `com.kltyton.playeranimationlibrarymorerotation.neoforge.PlayeranimationlibrarymorerotationNeoForge`

## Test Command

After a downstream mod places animations under its own
`assets/<namespace>/player_animations/` directory, use PAL's command in a
client world:

```text
/testPlayerAnimation <namespace>:<animation>
```

## Known Limitations

- Y/Z bend is a compatibility implementation, not an upstream bendable-cuboids
  three-axis ABI.
- Legacy PlayerAnimator binary/old-format bend still uses PAL's native X-only
  behavior.
- begin/end tick fade keeps PAL's native X bend lerp; Y/Z do not yet have
  separate transition lengths.
- `bend.position/scale` is a runtime compatibility feature and does not
  register extra normal PAL player bones.

## Build

```powershell
.\gradlew.bat build
```

Fabric client launch validation:

```powershell
.\gradlew.bat :fabric:runClient
```

NeoForge client launch validation:

```powershell
.\gradlew.bat :neoforge:runClient
```
