# PlayerAnimationLibraryMoreRotation

一个 Fabric / NeoForge 多加载器运行时兼容库，用 Mixin 扩展
Player Animation Library 和 bendable-cuboids 的玩家动画能力，并提供通用的
PAL 播放/停止/服务端同步 API。

## 支持版本

- Minecraft: `26.1.2`
- Fabric Loader: `0.19.3`
- Fabric API: `0.153.0+26.1.2`
- NeoForge: `26.1.2.76`
- Player Animation Library Fabric / Neo: `1.2.4+mc.26.1`
- bendable-cuboids: `2.0.2`
- Java: `25`

## 功能

- 为 PAL 的玩家骨骼 `bend` 增加 Y/Z 轴数据承载与渲染应用。
- 保留旧动画的 X 轴 bend 行为，`bend: 35` 和 `bend: [35, 0, 0]` 继续可用。
- 支持 JSON 中的 vector bend，例如 `bend: [x, y, z]`、`post: [x, y, z]`、`pre: [x, y, z]`。
- 补充 `left_item` / `right_item` 手持物品骨骼的 Y/Z 旋转应用。
- 对 JSON keyframe 按时间排序，让导出顺序不稳定的动画更可预测。
- 提供通用播放 API、停止 API、服务端同维度广播同步、客户端收包播放。

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
assets/mod_id/player_animations/animation.json
```

其中 JSON 包含：

```json
{
  "animations": {
    "animation": {}
  }
}
```

则动画 ID 为：

```text
mod_id:animation
```

## bend XYZ JSON 格式

旧格式仍兼容：

```json
{
  "bones": {
    "right_arm": {
      "bend": 35
    }
  }
}
```

新的三轴格式：

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

说明：

- X 轴 bend 仍交给 PAL / bendable-cuboids 原有路径。
- Y/Z bend 由本库 sidecar 数据保存，并在 bendable-cuboids 渲染路径中应用。
- `{"value": 35}` 会按 `[35, 0, 0]` 处理。
- bendable-cuboids 本身仍是单轴 bend API，本库的 Y/Z 是叠加在其 mesh 顶点上的兼容实现。

## 服务端播放 API

```java
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimations;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

Identifier animation = Identifier.fromNamespaceAndPath("mod_id", "animation");

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
        Identifier.fromNamespaceAndPath("mod_id", "animation")
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

仓库内包含测试动画资源，可在客户端中使用 PAL 命令测试：

```text
/testPlayerAnimation mod_id:animation
/testPlayerAnimation playeranimationlibrarymorerotation:item_rotation_yz_check
```

## 已知限制

- Y/Z bend 是兼容层实现，不是 bendable-cuboids 官方三轴 ABI。
- 旧的 PlayerAnimator 二进制/旧格式 bend 仍走 PAL 原生 X 轴行为。
- begin/end tick 淡入淡出保留 PAL 的 X bend lerp，Y/Z 还没有独立 transition length。

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
