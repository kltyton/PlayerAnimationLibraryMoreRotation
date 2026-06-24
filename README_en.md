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

## bend XYZ JSON Format

Legacy format remains compatible:

```json
{
  "bones": {
    "right_arm": {
      "bend": 35
    }
  }
}
```

New vector format:

```json
{
  "bones": {
    "right_arm": {
      "bend": [35, 10, -15]
    },
    "left_arm": {
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

## Test Commands

This repository includes test animation resources. In a client world, use PAL's
command:

```text
/testPlayerAnimation mob_battle:poison_knife_animation
/testPlayerAnimation playeranimationlibrarymorerotation:item_rotation_yz_check
```

## Known Limitations

- Y/Z bend is a compatibility implementation, not an upstream bendable-cuboids
  three-axis ABI.
- Legacy PlayerAnimator binary/old-format bend still uses PAL's native X-only
  behavior.
- begin/end tick fade keeps PAL's native X bend lerp; Y/Z do not yet have
  separate transition lengths.

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
