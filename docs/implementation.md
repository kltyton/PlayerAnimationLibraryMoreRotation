# Implementation notes

## Source discovery

Scanned sources and bytecode before implementation:

- Current repository source and resources.
- Mob Battle reference files requested in `G:\weituo\code\26.1\Mob Battle`.
- PAL source jars from Gradle cache:
  - `PlayerAnimationLibFabric-1.2.4+mc.26.1-sources.jar`
  - `PlayerAnimationLibCore-1.2.4+mc.26.1-sources.jar`
- Fabric networking source jar:
  - `fabric-networking-api-v1-6.3.1+554860db4c-sources.jar`
- Mixinsquared README/wiki for Fabric dependency and `@TargetHandler` usage:
  - `https://github.com/Bawnorton/MixinSquared`
  - `https://github.com/Bawnorton/MixinSquared/wiki`
- Minecraft 26.1.2 Mojmap decompile under:
  - `G:\weituo\mcp\minecraft-dev-cache\decompiled\26.1.2\mojmap`
- bendable-cuboids `2.0.2` local jar via `javap`, because no sources jar was present.

## PAL call chain

Resource loading:

```text
PlayerAnimResources.onResourceManagerReload
  -> manager.listResources("player_animations", *.json)
  -> UniversalAnimLoader.loadAnimations
  -> registry key Identifier(namespace, animation key)
```

Playback:

```text
PlayerAnimationAccess.getPlayerAnimationLayer(avatar, PlayerAnimLibMod.ANIMATION_LAYER_ID)
  -> PlayerAnimationController.triggerAnimation(Identifier)
  -> PlayerAnimResources.getAnimation(Identifier)
```

Model application:

```text
AnimationLoader
  -> BoneAnimation(rotation, position, scale, bend)
AnimationController.processCurrentAnimation
  -> computes rotation x/y/z, position x/y/z, scale x/y/z, bend
AvatarAnimManager.updatePart
  -> RenderUtil.translatePartToBone
  -> ModelPart x/y/z, xRot/yRot/zRot, xScale/yScale/zScale
```

Minecraft 26.1.2 `ModelPart` supports all three rotation, position, and scale
axes. Base PAL already carries these channels for standard player bones.

## Item rotation support

Added:

```text
com.kltyton.playeranimationlibrarymorerotation.mixin.PlayerItemRotationFixMixin
com.kltyton.playeranimationlibrarymorerotation.mixin.ItemInHandLayerBendVectorMixin
```

Behavior:

- `PlayerItemRotationFixMixin` targets Minecraft's original
  `ItemInHandLayer#submitArmWithItem` at the item submit call. PAL applies
  `left_item` / `right_item` rotations there with Y/Z swapped, so this mixin
  removes PAL's item-local rotation/scale and replays the item-local transform
  with literal X/Y/Z axes.
- `ItemInHandLayerBendVectorMixin` targets bendable-cuboids'
  `ItemInHandLayerMixin_playerAnim#renderMixin` through Mixinsquared. That
  handler is where bendable-cuboids already applies held-item X bend, so this
  addon runs at the same matrix level. At handler HEAD it applies missing parent
  arm scale in PAL's active animation path; at handler RETURN it adds the
  missing bend Y/Z, `bend.position`, and `bend.scale`.
- Minecraft's native `ModelPart.translateAndRotate` would normally apply arm
  position, rotation, and scale to the held-item pose. PAL replaces the active
  animation path for `PlayerModel#translateToHand` and applies arm position and
  rotation manually, but it only keeps a small scale-derived offset instead of
  scaling the item.
- bendable-cuboids adds a separate held-item X-bend correction around
  `(0, 0.25, 0)`. PlayerAnimationLibraryMoreRotation leaves that native path
  untouched for X-only bend. When Y/Z bend or bend position/scale is present,
  it first removes bendable-cuboids' item X matrix and then replays one combined
  lower-bend matrix before vanilla item orientation and before the item-local
  `left_item` / `right_item` transform. Parent arm scale runs immediately
  before the native bend point, so the item sees the parent scale before it
  receives bend and item-local transforms.
- `PoseStack` right-multiplies transforms. When a held item has mixed bend
  X/Y/Z, simply appending Y/Z after bendable-cuboids' X bend gives the wrong
  effective vertex order. For Y/Z or transform channels, this addon first
  removes bendable-cuboids' item X matrix and then replays one combined lower
  bend matrix whose effective order matches the mesh path:

```text
X bend -> Y bend -> Z bend -> bend scale -> bend position
```

## Keyframe ordering

Added:

```text
com.kltyton.playeranimationlibrarymorerotation.mixin.AnimationLoaderKeyframeSortMixin
```

Target:

```text
com.zigythebird.playeranimcore.loading.AnimationLoader#getKeyframes(JsonElement)
```

Behavior:

- Before PAL parses the native `bend` keyframes, this mixin checks only the
  fourth `AnimationLoader#bakeBoneAnimations -> getKeyframes(...)` argument
  (`entryObj.get("bend")`). If the `bend` value is this addon's new container
  object, PAL receives `bend.rotation`; if the container has only `position` or
  `scale`, PAL receives `null` and creates no native X bend frames.
- Sorts PAL's parsed keyframe list by timestamp before PAL builds rotation,
  position, scale, or native X bend keyframes.
- `PalMoreBendResources` applies the same timestamp sort to the vector bend
  sidecar path so X/Y/Z bend timelines stay aligned.
- Correctly ordered animations keep the same order.
- This keeps PAL's own animation registration from rejecting JSON shaped like
  `bend: { "rotation": ..., "position": ..., "scale": ... }`; the additional
  `position` and `scale` channels are still handled by the sidecar resource
  parser.

## Bend limitation

The local source/bytecode shows bend is scalar throughout the active upstream
ABI:

```text
AnimationLoader builds bend x/y/z frames, then stores only bendFrames.xKeyframes()
BoneAnimation stores List<Keyframe> bendKeyFrames
PlayerAnimBone stores float bend
PlayerBendHelper.bend(ModelPart, float)
BendableCube.applyBend(float)
```

PlayerAnimationLibraryMoreRotation keeps that ABI intact and adds a sidecar
vector-bend path instead of replacing PAL records or bendable-cuboids classes.

## bendable-cuboids native X bend definition

The local `bendable-cuboids-2.0.2.jar` has no sources jar in the Gradle cache,
so this was checked with `javap` bytecode inspection.

Native player bend flow:

```text
PlayerModelMixin_playerAnim#bc$updatePart
  -> PlayerBendHelper.bend(ModelPart, bone.bend)
  -> BendableModelPart.bc$getCuboid(0)
  -> BendableCube.applyBend(float)
  -> BendableCuboid.iteratePositions(BendUtil.getBend(cube, bendX))
```

Definition:

- Native bend is a vertex deformation on the first bendable cuboid of a
  `ModelPart`. It is not a separate child bone in the Minecraft model tree.
- The input `float` is an angle in radians by the time bendable-cuboids applies
  it. PAL JSON bend degrees are converted by PAL before they reach
  `PlayerAnimBone.bend`.
- `BendUtil.applyBendToMatrix` performs a local X-axis rotation around the
  cuboid bend pivot (`getBendX/Y/Z`). This is why the upstream ABI is
  effectively X-only.
- `BendableCuboid` stores the original cuboid vertex positions and recalculates
  bent positions from those originals each time bend changes.
- `BendableCube.isBendInverted()` returns true when the cuboid bend direction is
  a positive axis direction. In the vanilla player bake path, body and jacket
  are explicitly baked with `Direction.DOWN`, cape with `Direction.UP`, and
  other player parts fall back to `Direction.UP`.
- Therefore body/jacket native X bend and arm/leg native X bend can have
  different sign behavior even when the JSON bend X value is the same.

This addon leaves that native X bend definition in place. It uses the native X
function first, then layers Y/Z bend and `bend.position` / `bend.scale` mesh
transforms on top of the resulting vertices. The addon's extra
Y/Z/position/scale follows bendable-cuboids' player bend direction: body and
jacket (`Direction.DOWN`) affect the upper segment, while arms, sleeves, legs,
and pants (`Direction.UP`) affect the lower segment. The native X direction
still comes from bendable-cuboids' `BendUtil`; Y/Z are the addon's own
distributed rotations. The Y/Z sign is intentionally kept in the same direction
as the evaluated PAL sidecar values: `state.bendY()` and `state.bendZ()` are
applied directly rather than negated.

## Bend vector support

Added:

```text
client.compat.PalMoreBendResources
client.compat.PalMoreBendableCuboids
compat.PalMoreBendHolder
mixin.AnimationControllerBendVectorMixin
mixin.PlayerAnimBoneBendVectorMixin
mixin.PlayerModelBendVectorMixin
```

Data model:

- `PlayerAnimBone.bend` remains the authoritative X bend value.
- `PalMoreBendHolder` is mixed into every `PlayerAnimBone` and carries
  `bendX`, `bendY`, and `bendZ`.
- `PalMoreBendHolder` also carries an active flag. A bone is active only when
  the current animation stack wrote a vector bend sidecar value this frame.
  This lets render code distinguish a real `[x, 0, 0]` vector track from stale
  Y/Z values left by a previous animation.
- `PalMoreBendResources` registers a client resource reload listener:

```text
player_animation_library:animation
  -> playeranimationlibrarymorerotation:bend_vectors
```

The reload listener scans the same
`assets/<namespace>/player_animations/*.json` resources after PAL has loaded
the real `Animation` objects, parses `bend` plus supported bend transform
channels, and stores `Map<Animation, Map<boneName, KeyframeStack>>` in an
identity map keyed by the already-loaded PAL `Animation` instances.

Reason for this design:

- It does not change PAL's public records (`BoneAnimation`) or binary network
  format.
- It does not need an overwrite of PAL's private `AnimationLoader` methods.
- Existing X-only animation data still flows through PAL's original path.
- Y/Z data is available only where it is needed: client animation evaluation and
  client player model rendering.

Runtime chain:

```text
JSON bend
  -> PAL AnimationLoader keeps native X in BoneAnimation.bendKeyFrames
  -> PalMoreBendResources keeps full bend KeyframeStack sidecar
  -> AnimationControllerBendVectorMixin computes bendY/bendZ each render frame
  -> PlayerAnimBoneBendVectorMixin copies/scales/adds sidecar values through
     snapshots, fades, mirrors, and render bones
  -> AvatarAnimManagerBendVectorResetMixin resets render bones to
     (native bendX, 0, 0) after RenderUtil.copyVanillaPart and before
     AvatarAnimManager.get3DTransform lets the active animation rewrite Y/Z
  -> PlayerModelBendVectorMixin uses Mixinsquared @TargetHandler to run after
     bendable-cuboids PlayerModelMixin_playerAnim#bc$updatePart
  -> PalMoreBendableCuboids applies vector bend to the main part and matching
     jacket/sleeve/pants overlay part
```

`PalMoreBendableCuboids.applyVectorBend` must do real vertex work. During the
`YS.json` investigation on 2026-06-25, the method was found to return after a
debug message and clear `LAST_VECTOR_BEND`, so any `bend.rotation` Y/Z or
`bend.position` / `bend.scale` data loaded by `PalMoreBendResources` reached the
render hook but did not affect the player mesh. The method now:

- treats `BendableModelPart` as a runtime mixin-added interface by casting
  `ModelPart` through `Object`;
- iterates `bc$getCuboid(index)` until it returns `null`;
- starts from bendable-cuboids' native X deformation
  `BendUtil.getBend(cube, bendX)`;
- layers direction-consistent Y/Z bend and bend-only position/scale onto
  `BendableCuboid.iteratePositions(...)`;
- forces a native X-only reset when a previous extended bend was active but the
  current frame has no Y/Z/transform sidecar data.

This keeps legacy X-only bend behavior on the original bendable-cuboids path
and only replaces the cuboid positions when extended sidecar channels are
present or need to be cleared.

## YS.json conversion investigation

The original animation at
`C:\Users\lib73\Downloads\新建文件夹\YS.json` uses DragonCore-style names:

```text
root, Body, Body_Lower, Head,
Right_Arm, Right_Arm_Lower, Right_Hand,
Left_Arm, Left_Arm_Lower,
Right_Leg, Right_Leg_Lower,
Left_Leg, Left_Leg_Lower
```

The converted resource
`assets/playeranimationlibrarymorerotation/player_animations/animation.json`
uses PAL names and stores lower limb rotations as parent-bone
`bend.rotation`:

```text
root.position -> body.position
Body.rotation -> torso.rotation
Right_Arm_Lower.rotation -> right_arm.bend.rotation
Left_Arm_Lower.rotation -> left_arm.bend.rotation
Right_Leg_Lower.rotation -> right_leg.bend.rotation
Left_Leg_Lower.rotation -> left_leg.bend.rotation
Right_Hand.rotation -> right_item.rotation
```

For arm and leg lower bones, the current conversion preserves the source lower
rotation values exactly in `bend.rotation`. Therefore those differences in game
were primarily a runtime-consumption problem: the values were present in JSON
but the mesh-side vector bend method was not applying them.

The current worktree has two generated `animation.json` states: a staged version
and a working-tree version. The working-tree version removes `position` tracks
from `torso`, `right_leg`, and `left_leg` that existed in the staged generated
version. That is a converter/export-path issue to keep separate from the runtime
fix: a parented Blockbench/DragonCore rig must be baked into PAL's flatter
runtime model consistently, otherwise PAL will play the resource but its body
pose can still differ from the original `YS.json`.

Supported JSON:

```json
{
  "animations": {
    "example": {
      "bones": {
        "rightArm": {
          "bend": [35, 10, -15]
        },
        "leftArm": {
          "bend": {
            "0.0": { "post": [0, 0, 0] },
            "0.4": { "pre": [20, 0, 0], "post": [20, 25, 0], "lerp_mode": "linear" }
          }
        }
      }
    }
  }
}
```

Recommended nested bend channel JSON:

```json
{
  "animations": {
    "nested_bend_channels": {
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
  }
}
```

Compatibility:

- Scalar `bend: 35` is treated as `[35, 0, 0]` in the sidecar, matching old
  runtime behavior where PAL only applied the X list.
- Object `{"value": 35}` is also treated as `[35, 0, 0]`, including inside
  timestamped keyframes, without adding a duplicate frame for the same time.
- Explicit `[x, 0, 0]` stays X-only.
- A `bend` object containing `rotation`, `position`, or `scale` is treated as
  the new container format. `bend.rotation` is the bend angle track, while
  `bend.position` and `bend.scale` are part transforms for the same real player
  bone.
- A `bend` object without those member names remains the old timestamped bend
  timeline format, so existing `{"0.0": {"post": [...]}}` files keep working.
- `post`, `pre`, `vector`, and `{"value": ...}` are accepted in the sidecar
  parser for the supported bend channels.
- Pure X bend still uses bendable-cuboids' original `PlayerBendHelper.bend`
  result. The vector renderer returns early when Y/Z are zero unless it needs to
  clear a previously-applied vector bend.

Render implementation:

- bendable-cuboids exposes only `BendableCube.applyBend(float)`, so Y/Z cannot
  be represented as native bendable-cuboids state.
- `AvatarAnimManagerBendVectorResetMixin` targets PAL's real
  `AvatarAnimManager#updatePart` method. It is not targeting a method added by
  another mixin; mixin-added player-model handlers still use Mixinsquared.
- `PlayerModelBendVectorMixin` does not directly inject `pal$updatePart` or
  `pal$resetAll`, because those methods are introduced by PAL's player-model
  mixin. It uses Mixinsquared `@TargetHandler` against bendable-cuboids'
  `PlayerModelMixin_playerAnim` handlers:

```text
bc$updatePart(AvatarAnimManager, ModelPart, PlayerAnimBone, CallbackInfo)
bc$resetAll(AvatarAnimManager, CallbackInfo)
```

  The mixin priority is `2500`, above bendable-cuboids' verified priority
  `2002`, as required by Mixinsquared's handler targeting.
- `ItemInHandLayerBendVectorMixin` follows the same rule for the
  bendable-cuboids held-item handler:

```text
ItemInHandLayerMixin_playerAnim#renderMixin(...)
```

  It runs after bendable-cuboids' X-bend item matrix correction and adds only
  the missing Y/Z/position/scale bend channels.
- When Y or Z is non-zero, `PalMoreBendableCuboids` directly calls
  bendable-cuboids' `BendableModelPart`, `BendableCube`, `BendableCuboid`, and
  `BendUtil.getBend(cube, bendX)` APIs to get the original X deformation, then
  applies additional distributed local Y and Z rotations to the cuboid vertices.
- Y/Z bend rotation uses the same segment factor as `bend.position` and
  `bend.scale`. For body and jacket, vertices on the lower side of the middle
  hinge keep the native X result and the upper end receives the full Y/Z
  rotation, matching PAL's native torso X bend side. For arms, legs, and their
  overlays, vertices on the upper side keep the native X result and the lower
  end receives the full Y/Z rotation. Native X bend remains delegated to
  bendable-cuboids; the added Y/Z rotations are applied by this addon's vertex
  pass. The added Y/Z signs are applied directly from `state.bendY()` and
  `state.bendZ()` so extended bend rotation follows PAL-style positive rotation
  semantics instead of using the previous negated compatibility sign.
- Held-item bend inheritance uses the same direct Y/Z sign as the mesh path, so
  items remain aligned with the arm bend endpoint after the sign change.
- The helper tracks the last vector-applied `ModelPart` in a weak map. When Y/Z
  return to zero it directly reapplies the native X bend function, avoiding
  stale vector-deformed vertices when bendable-cuboids would otherwise early
  return because the X float did not change.

Current limitations:

- Y/Z bend is a compatibility vertex transform layered on top of
  bendable-cuboids' single-axis mesh, not a new upstream three-axis
  bendable-cuboids ABI.
- bendable-cuboids is a hard runtime dependency of this addon. The vector bend
  bridge uses direct imports and does not include an optional runtime fallback.
- Begin/end tick fade paths preserve PAL's native X bend lerp but do not yet
  compute separate transition lengths for Y and Z.
- The sidecar parser currently targets PAL/Bedrock JSON under the top-level
  `"animations"` object. Legacy PlayerAnimator binary/old-format bend remains
  PAL's original scalar X behavior.

## Bend bone position and scale

Added support for `position` and `scale` on real player bone `bend` channels.
The only supported transform format is the same-bone container:

```json
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
```

Source finding:

- PAL's `HumanoidAnimationController#registerBones` registers the real player
  bones such as `torso`, `right_arm`, and `left_leg`.
- bendable-cuboids' `PlayerModelMixin_playerAnim#bc$updatePart` receives the
  real PAL bone for the rendered `ModelPart` and only passes `bone.bend` to
  `PlayerBendHelper.bend(ModelPart, float)`.

Compatibility data model:

- `PalMoreBendResources` now reads `bend.rotation`, `bend.position`, and
  `bend.scale` from real player bones after PAL has loaded animations.
- A `bend` value without `rotation`, `position`, or `scale` is old syntax and
  is treated as `bend.rotation`.
- `AnimationControllerBendVectorMixin` computes these tracks each frame and
  stores the result in `PalMoreBendHolder` on the same real bone.
- `PlayerModelBendVectorMixin` applies the bend transform after
  bendable-cuboids has updated the part's bend mesh:
  - Y/Z `rotation` is distributed over the same side as native bendable-cuboids
    X bend for that part: torso/body and jacket use the upper segment; arms,
    legs, sleeves, and pants use the lower segment.
  - `position` is distributed over that same segment. The opposite side of the
    middle hinge keeps its native-X-bent position; the active segment receives
    the full offset near its far end.
  - `scale` is distributed over that same segment around the middle hinge. It
    does not multiply `ModelPart.xScale/yScale/zScale`.
  - Matching overlay parts, such as jacket, sleeves, and pants, receive the same
    bend mesh deformation. The parent `ModelPart` transform is not changed.

Compatibility:

- Existing animations that do not contain `bend.position` or `bend.scale` are
  unchanged.
- Existing scalar or X-only `bend` remains supported.
- Bend transform JSON must be written on the real player bone's `bend` object;
  extra virtual bend bones are intentionally not supported by this addon.
- `scale` values are multipliers. `1` means original size, not zero.

## Playback migration

Added public APIs:

```text
PalMoreAnimations
PalMoreAnimationController
PalMoreFirstPersonOptions
client.PalMoreClientAnimations
```

Added networking:

```text
network.payload.PlayerAnimationPayload
network.PalMoreNetworking
client.network.PalMoreClientNetworking
```

The common initializer registers only the clientbound payload codec. The client
initializer registers the receiver. Common code does not reference Minecraft
client classes.

## Playback controller and keyframe handlers

Added controller-aware playback without changing the old two-argument API:

```text
PalMoreAnimations.play(ServerPlayer, Identifier)
  -> default controller id

PalMoreAnimations.play(ServerPlayer, Identifier, PalMoreAnimationController)
  -> sends controller.id()

PalMoreClientAnimations.playLocal(Avatar, Identifier, PalMoreAnimationController)
  -> installs controller delegates on PAL's default PlayerAnimationController
  -> triggers the animation id
```

PAL source finding:

- `AnimationController` already has `setCustomInstructionKeyframeHandler`,
  `setParticleKeyframeHandler`, and `setSoundKeyframeHandler`.
- PAL calls the controller-local handler first. If it returns `PASS`, PAL then
  invokes the corresponding global `CustomKeyFrameEvents` event.
- PAL's `PlayerAnimLibMod` registers a global sound keyframe handler, so
  sound keyframes still play normally when the PalMore controller has no sound
  handler or explicitly returns `PASS`.

Network boundary:

- `PlayerAnimationPayload` now carries `controllerId` after `stop`.
- The payload sends only an `Identifier`; handler lambdas are local JVM state
  and are not serialized.
- Client payload handling resolves the id through
  `PalMoreClientAnimations.registerController(...)`, falling back to
  `PalMoreAnimationController.DEFAULT` if no controller is registered.

The installed PAL delegates read the PalMore controller's current handler field
when a keyframe fires. This makes chained local setup such as
`playLocal(..., controller).setCustomInstructionKeyframeHandler(...)` work as
long as the handler is set before that keyframe is reached.


## 26.2 upgrade and independent legacy projects

The root targets Minecraft 26.2 with PAL 1.2.6 and Bendable Cuboids 2.0.4. Independent Gradle projects under `versions/` target 1.21.1, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, and 26.1.2. Each packages the matching Bendable Cuboids artifact through Fabric include or NeoForge jar-in-jar. The 26.1 family uses Bendable Cuboids 2.0.2 because the 2.0.4 embedded loader metadata requires 26.2.

- PAL 1.2.6: `AnimationControllerBendVectorMixin` reads the format flag from the current queued animation; the old shadowed controller method no longer exists.
- PAL 1.1.x/1.0.x: the adapters use the dependency's `IBoneEnabled`, bone getter APIs, `AnimationPoint` interpolation, and nested queued-animation type. Minecraft resource identifiers, player/render-state types, resource reload registration, and held-item rendering signatures follow each version's actual sources.
- Bendable Cuboids 1.0.x: `CubePositionInvokerMixin` accesses the injected `bc$iteratePositions` method because these artifacts keep deformable vertices on `ModelPart.Cube` rather than a `BendableCuboid` subclass.
- Minecraft 1.21.1: the PAL model reset hook receives both the model part and its second layer. The local Bendable Cuboids port corrects the access-widener vector descriptor, NeoForge loading-list API, Java toolchain, and development runtime Javassist dependency. Its player rendering hook disables the original-cube fast path during active animation and restores it afterwards, allowing Y/Z-only bend and bend position/scale to draw their modified vertices when native X bend is zero.

The 1.21.1 Bendable Cuboids artifacts live in the version project's narrowly scoped `libs/maven` repository. When rebuilding those artifacts under the same coordinate, invalidate the corresponding Loom remapped dependency before runtime verification; otherwise the runtime can retain old bytecode despite the source jar changing.


For Minecraft 1.21.8 specifically, `PlayerModelBendVectorMixin` restores PAL's `IMutableModel` animation reference from the render state, and clears it when inactive. PAL 1.1.2 no longer populates that reference, but Bendable Cuboids 1.0.5 requires it to select deformed geometry. Minecraft 1.21.7/1.21.8 also reapply extended mesh transforms every frame because Bendable Cuboids 1.0.5 always rewrites vertices when applying native bend; a same-value cache would otherwise erase constant Y/Z-only or bend position/scale poses. Newer Bendable Cuboids versions skip identical native bend and retain the existing cache.


Each shared `pack.mcmeta` covers both the resource and data format from that Minecraft client jar's `version.json`. The 1.21.9–1.21.11 format transition requires the legacy `pack_format` / `supported_formats` fields and a minimum of 64 for one shared metadata file to satisfy both client and server decoders (their legacy cutoffs differ: 64 versus 81). This does not expand the mod's declared Minecraft compatibility. Java library dependencies used by legacy development clients match their PAL POMs, including Javassist and the version-specific Mocha runtime; these libraries are supplied by PAL in normal mod installations.
