# 模块注册

模块统一在 `src/main/java/com/github/gavin/lake/module/Modules.java` 的 `register` 方法中注册。
`Lake.java` 只调用这个入口；注册发生在配置加载和模块初始化之前。

新增模块时，在 `module/modules` 下创建继承 `Module` 的类，并在 `Modules.register` 中添加：

```java
registry.register(new YourModule());
```

模块构造函数通过 `super(id, name, description, category, enabled, key)` 定义信息。
ID 使用小写字母、数字和连字符，以字母开头；ID、名称和具体类不能与已注册模块重复。
`enabled` 和 `key` 是默认值，已有配置会覆盖它们。按键 `0` 表示未绑定。

生命周期方法：

- `onEnable()`：模块开启时执行；启动时恢复为开启的模块也会调用它。
- `onPreTick()`：客户端 tick 的 START 阶段执行，适合在原版移动逻辑之前修改输入；尚未进入世界时也会调用。
- `onTick()`：进入世界后，开启的模块在每个客户端 tick 的 END 阶段执行。
- `onDisable()`：模块关闭时执行，用于恢复状态和清理资源。

构造函数只定义模块和设置；玩家、世界相关操作放进生命周期方法，并检查是否为空。
这些生命周期方法由 Lake 统一调用，无需单独注册 Forge 事件。

设置通过模块内部的 `register(new BooleanSetting(...))` 或
`register(new NumberSetting(...))` 添加，必须在模块注册之前完成，通常写成字段初始化。
菜单会自动显示模块、分类、bind 和这两类设置；状态与设置自动保存到游戏目录的
`config/lake/modules.json`。

`SprintModule` 是自动疾跑的例子：默认开启且未绑定，在原版移动逻辑之前保持疾跑键，
由原版判断移动方向、潜行、使用物品、失明、碰墙和饥饿条件。打开界面或离开世界时恢复实际按键状态。
参照 Unfair 的 Sprint，连接 1.9 及以上服务器时按住潜行取消疾跑；连接 1.13 及以上时在水中取消疾跑。
单人世界按 1.8 协议处理。关闭模块会恢复实际疾跑键状态，并清除当前疾跑状态。

在游戏内可使用 `.module sprint on`、`.module sprint off`、`.bind sprint G` 和
`.bind sprint NONE` 控制模块；也可在菜单中切换，中键点击模块行设置 bind。
