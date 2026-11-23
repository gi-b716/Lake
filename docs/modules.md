# 模块注册

模块统一在 `src/main/java/com/github/gavin/lake/module/Modules.java` 的 `register` 方法中注册。
`Lake.java` 只调用这个入口；注册发生在配置加载和模块初始化之前。

新增模块时，在 `module/modules` 下创建继承 `Module` 的类，并在 `Modules.register` 中添加：

```java
registry.register(new YourModule());
```

`module/modules/combat` 等分类包只放模块类。模块专用的辅助实现放在分类包下的模块子包，
例如 `module/modules/combat/autoclicker/ClickTiming`；通用辅助实现放到对应的公共包，
例如原版点击入口接口位于 `input/MinecraftClickAccess`。

模块构造函数通过 `super(id, name, description, category, enabled, key)` 定义信息。
ID 使用小写字母、数字和连字符，以字母开头；ID、名称和具体类不能与已注册模块重复。
`enabled` 和 `key` 是默认值，已有配置会覆盖它们。按键 `0` 表示未绑定。

生命周期方法：

- `onEnable()`：模块开启时执行；启动时恢复为开启的模块也会调用它。
- `onPreTick()`：客户端 tick 的 START 阶段执行，适合在原版移动逻辑之前修改输入；尚未进入世界时也会调用。
- `onInputTick()`：原版 `runTick` 的输入阶段执行，在本 tick 的瞄准更新之后、连续挖掘与玩家移动更新之前；点击等动作在这里执行。
- `onTick()`：进入世界后，开启的模块在每个客户端 tick 的 END 阶段执行。
- `onRenderTick(float partialTicks)`：每个渲染帧的 START 阶段执行，适合视觉更新；尚未进入世界时也会调用。不要在这里直接执行点击或发送动作包。
- `onDisable()`：模块关闭时执行，用于恢复状态和清理资源。

构造函数只定义模块和设置；玩家、世界相关操作放进生命周期方法，并检查是否为空。
这些生命周期方法由 Lake 统一调用，无需单独注册 Forge 事件。

设置通过模块内部的 `register(new BooleanSetting(...))`、`register(new NumberSetting(...))`
或 `register(new RangeSetting(...))` 添加，必须在模块注册之前完成，通常写成字段初始化。
`RangeSetting` 在同一滑条上用两个滑块调整下限和上限；滑块不能互相越过，允许区间重合。
配置使用 `{"lower": 10, "upper": 14}` 保存区间，加载时统一限制取值范围并排序两个端点。
菜单会自动显示模块、分类、bind 和这些设置；状态与设置自动保存到游戏目录的
`config/lake/modules.json`。

`SprintModule` 是自动疾跑的例子：默认开启且未绑定，在原版移动逻辑之前保持疾跑键，
由原版判断移动方向、潜行、使用物品、失明、碰墙和饥饿条件。打开界面或离开世界时恢复实际按键状态。
参照 Unfair 的 Sprint，连接 1.9 及以上服务器时按住潜行取消疾跑；连接 1.13 及以上时在水中取消疾跑。
单人世界按 1.8 协议处理。关闭模块会恢复实际疾跑键状态，并清除当前疾跑状态。

在游戏内可使用 `.module sprint on`、`.module sprint off`、`.bind sprint G` 和
`.bind sprint NONE` 控制模块；也可在菜单中切换，中键点击模块行设置 bind。

`AutoClickerModule` 位于 Combat 分类，默认关闭且未绑定，仅有三个设置：

- `CPS`：默认 10–14，可在 1–20 之间调整两个端点，步长为 1。
- `Break Blocks`：默认开启；瞄准非空气、非液体方块时保持原版连续挖掘，离开方块后恢复点击。
- `Hold to Click`：默认开启；要求物理左键保持按下，关闭后在游戏窗口内自动点击。

算法参考 OpenB4 的短暂慢速阶段与偶发停顿，以及 Vape Extra+ 的目标漂移和逐次扰动。
CPS 区间定义基础点击速度；慢速阶段与停顿可能暂时降低实际 CPS。调度使用单调时钟，
在原版输入阶段检查截止时间，每个客户端 tick 最多点击一次，卡顿后丢弃积压点击。
点击会先执行原版挥手、攻击流程，再由原版玩家更新发送移动包，避免渲染阶段点击造成动作包顺序错乱。
实际点击间隔受客户端 20 TPS 的输入采样影响，但算法保留截止时间，避免每次向后取整累积误差。
打开界面、失去焦点、死亡、使用物品或松开要求按住的左键时暂停并重置调度。
点击沿用原版瞄准、挥手和攻击流程，并屏蔽重复的物理点击；无需修改 LWJGL 的物理鼠标状态。
可通过 `.module autoclicker on`、`.module autoclicker off` 或菜单控制模块。
