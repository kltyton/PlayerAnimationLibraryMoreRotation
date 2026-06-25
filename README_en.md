# PlayerAnimationLibraryMoreRotation

A Fabric / NeoForge multiloaded runtime compatibility library that extends
Player Animation Library and bendable-cuboids player animation behavior with Mixin patches, plus a
small reusable PAL playback/sync API for downstream mods.

## Supported Versions

- Minecraft: `26.1.2`
- Fabric Loader: `0.19.3`
- Fabric API: `0.153.0+26.1.2`
- NeoForge: `26.1.2.76`
- Player Animation Library Fabric / Neo: `1.2.4+mc.26.1`
- bendable-cuboids: `2.0.2`
- Java: `25`

## Features

- Adds Y/Z data storage and rendering support for PAL player bone `bend`.
- Adds position and scale support for bend bones, preferably through
  `bend.position` / `bend.scale` on the real player bone.
- Keeps old X-only bend behavior compatible. `bend: 35` and `bend: [35, 0, 0]`
  still work.
- Supports vector bend JSON such as `bend: [x, y, z]`, `post: [x, y, z]`, and
  `pre: [x, y, z]`.
- Extends `left_item` / `right_item` held-item Y/Z rotation handling.
- Sorts JSON keyframes by timestamp for more predictable exported animations.
- Provides reusable play, stop, server sync, and client receive APIs for PAL
  animations.

## Animation Resource Path

PAL still loads animations from:

```text
assets/<namespace>/player_animations/*.json
```

The runtime animation id is:

```text
<namespace>:<key inside the animations object>
```

For example:

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

## bend JSON Format

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
          "0.5": { "post": [-10, 6, 0] },
          "1.0": { "post": [0, 0, 0] }
        },
        "scale": {
          "0.0": { "post": [1, 1, 1] },
          "0.5": { "post": [0.35, 2.7, 0.55] },
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
