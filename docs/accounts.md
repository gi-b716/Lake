# 账号管理

Lake 通过 Shadow 内置 [In-Game Account Switcher](https://modrinth.com/mod/in-game-account-switcher)
的 Forge 1.8.9 版本 8.0.1。依赖固定为
`maven.modrinth:in-game-account-switcher:8.0.1-forge1.8`，开发环境使用 Loom 重映射后的副本。

登录自己的微软账号：

1. 在主菜单点击右侧的账号图标，打开 In-Game Account Switcher。
2. 点击“添加账号”，再点击“微软”或“Microsoft”。
3. 在打开的浏览器中登录微软账号并完成授权。
4. 返回账号列表，选中添加的账号并点击“登录”。

账号列表支持添加、编辑、删除和切换，配置及账号数据保存在当前游戏目录的
`config/ias.json`。多人游戏页面的入口可以在 IAS 配置中开启。

发布包合并 IAS 的模组元数据和资源，并将其三个访问转换规则合并到 Lake 的
`accesstransformer.cfg`，用于修改客户端 Session 和访问账号界面控件。
如果 Forge 已识别 IAS 模组，由它执行初始化；仅作为开发库加载时，Lake 的
`compat/AccountManager.java` 执行初始化，避免重复注册界面事件。

IAS 使用 LGPL-3.0-or-later，随包保留许可证。源码位于官方仓库的
[legacy 分支](https://github.com/The-Fireplace-Minecraft-Mods/In-Game-Account-Switcher/tree/archive/legacy)。
