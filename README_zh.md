# PlayerAnimationLibraryMoreRotation

语言：[English](README.md) | 中文

PlayerAnimationLibraryMoreRotation 是一个 Fabric / NeoForge 多加载器运行时兼容库。
它通过局部 Mixin 补丁扩展 Player Animation Library 和 bendable-cuboids 的玩家动画行为，
并为下游模组提供一个小型 PAL 播放/停止/同步 API。

## 支持版本

- Minecraft: `26.1.2`
- Fabric Loader: `0.19.3`
- Fabric API: `0.153.0+26.1.2`
- NeoForge: `26.1.2.76`
- Player Animation Library Fabric / Neo: `1.2.4+mc.26.1`
- bendable-cuboids: `2.0.2`
- Java: `25`

## 功能

- 为 PAL 的玩家骨骼 `bend` 增加 Y/Z 轴 sidecar 数据承载与渲染应用。
- 为 bend 骨骼增加 bend-local 位移和尺寸支持，写在真实玩家骨骼的
  `bend.position` / `bend.scale` 下。
- 保留旧动画的 X 轴 bend 行为，`bend: 35` 和 `bend: [35, 0, 0]` 继续可用。
- 支持 JSON 中的 vector bend，例如 `bend: [x, y, z]`、`post: [x, y, z]`、
  `pre: [x, y, z]`。
- 补充 `left_item` / `right_item` 手持物品骨骼的 Y/Z 旋转应用。
- 对 JSON keyframe 按时间排序，让导出顺序不稳定的动画更可预测。
- 提供通用播放 API、停止 API、服务端同维度广播同步和客户端收包播放。

## 动画资源路径

PAL 仍按原规则加载动画：

```text
assets/<namespace>/player_animations/*.json
```

动画 ID 是：

```text
<namespace>:<animations 对象里的 key>
```

例如：

```text
assets/mob_battle/player_animations/poison_knife_animation.json
```

其中 JSON 包含：

```json
{
  "animations": {
    "poison_knife_animation": {}
  }
}
```

则动画 ID 为：

```text
mob_battle:poison_knife_animation
```

## bend JSON 格式

本库只支持真实玩家骨骼上的 `bend`。不要把 bend 变换写成额外的虚拟骨骼。

支持的第一种写法：旧 scalar bend，等价于 `[x, 0, 0]`：

```json
{
  "bones": {
    "right_arm": {
      "bend": 35
    }
  }
}
```

支持的第二种写法：三轴数组或旧时间轴对象。如果 `bend` 对象里没有
`rotation`、`position`、`scale`，就默认当作 `rotation` 解析：

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

说明：

- X 轴 bend 仍交给 PAL / bendable-cuboids 原有路径。
- Y/Z bend 由本库 sidecar 数据保存，并在 bendable-cuboids 渲染路径中应用。
- `{"value": 35}` 会按 `[35, 0, 0]` 处理。
- bendable-cuboids 本身仍是单轴 bend API，本库的 Y/Z 是叠加在其 mesh 顶点上的兼容实现。

## bend 骨骼位移和尺寸

支持的第三种写法：把 bend 的三类通道放在同一个真实玩家骨骼下：

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

`bend.rotation` 和旧 `bend` 时间轴是同一个 vector bend 通道。
`bend.position` / `bend.scale` 应用到同一个渲染部件。支持 `post`、`pre`、
`vector` 和 `{"value": ...}` keyframe。

说明：

- `position` 单位沿用 PAL / Minecraft 模型单位。
- `scale` 是倍率，`1` 是原尺寸，不是 `0`。
- `bend.position` 和 `bend.scale` 只修改 bend 下半段网格顶点，不会平移或缩放整条手臂、腿或身体关节。
- 没有写 `bend.position` / `bend.scale` 的旧动画不受影响。

## 服务端播放 API

```java
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimations;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

Identifier animation = Identifier.fromNamespaceAndPath("mob_battle", "poison_knife_animation");

PalMoreAnimations.play((ServerPlayer) player, animation);
PalMoreAnimations.stop((ServerPlayer) player);
```

服务端会向同维度内支持本库 payload 的客户端广播。

## 客户端播放 API

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

可选第一人称配置：

```java
PalMoreClientAnimations.playLocal(
        avatar,
        animation,
        PalMoreFirstPersonOptions.SHOW_ARMS_AND_ITEMS
);
```

## 网络 payload

本库注册的 clientbound payload：

```text
playeranimationlibrarymorerotation:player_animation
```

字段：

```text
avatarEntityId: VarInt
animationId: Identifier
stop: Boolean
```

下游模组不需要重复注册这个 payload 或客户端 receiver。

## 初始化入口

- Fabric main entrypoint:
  `com.kltyton.playeranimationlibrarymorerotation.fabric.PlayeranimationlibrarymorerotationFabric`
- Fabric client entrypoint:
  `com.kltyton.playeranimationlibrarymorerotation.fabric.client.PlayeranimationlibrarymorerotationFabricClient`
- NeoForge mod entrypoint:
  `com.kltyton.playeranimationlibrarymorerotation.neoforge.PlayeranimationlibrarymorerotationNeoForge`

## 测试命令

下游项目把动画放到自己的 `assets/<namespace>/player_animations/` 后，可以在客户端中使用 PAL 命令测试：

```text
/testPlayerAnimation <namespace>:<animation>
```

## 已知限制

- Y/Z bend 是兼容层实现，不是 bendable-cuboids 官方三轴 ABI。
- 旧的 PlayerAnimator 二进制/旧格式 bend 仍走 PAL 原生 X 轴行为。
- begin/end tick 淡入淡出保留 PAL 的 X bend lerp，Y/Z 还没有独立 transition length。
- `bend.position/scale` 是运行时兼容层能力，不会新增 PAL 普通玩家骨骼。

## 构建

```powershell
.\gradlew.bat build
```

Fabric 客户端启动验证：

```powershell
.\gradlew.bat :fabric:runClient
```

NeoForge 客户端启动验证：

```powershell
.\gradlew.bat :neoforge:runClient
```
