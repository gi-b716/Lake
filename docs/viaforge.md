# ViaForge 集成

Lake 内置官方 [ViaForge 4.3.2 的 Minecraft 1.8.9 发行包](https://github.com/ViaVersion/ViaForge/releases/tag/4.3.2)，
通过 Modrinth Maven 的 `maven.modrinth:viaforge:4.3.2+1.8.9` 固定版本获取。
开发运行使用 Loom 重映射后的依赖；发布 JAR 通过 Shadow 合并发行包及其内置协议库。

发布包同时保留 Lake 与 ViaForge 的模组元数据、Mixin 配置及 ViaForge 的核心加载器。
Lake 的核心加载器在 Forge 配置好类加载器后注册两套 Mixin，并在 Gradle 开发运行时优先使用 Lake 的 Mixin 版本。
Forge 若识别 ViaForge 模组，会先初始化它；开发环境仅作为库加载时，由 Lake 初始化。
初始化入口会检查已有实例，无需另装 ViaForge。
协议可通过 ViaForge 在主菜单和多人游戏页面提供的界面选择，也支持服务器单独保存版本。
ViaForge 的设置保存在游戏目录的 `ViaForge` 文件夹。

`compat/Protocols.java` 优先读取当前连接跟踪的服务器版本；没有连接时读取 ViaForge 的选择，
尚未初始化时回退到 1.8。单人世界始终按 1.8 处理。

Sprint 的版本处理参照 [Unfair 的 Sprint](https://github.com/UnfairGaming/Unfair/blob/master/src/main/java/cn/unfair/module/modules/movement/Sprint.java)：

- 在客户端 tick 开始时保持疾跑键，让原版移动逻辑决定是否疾跑。
- 1.9 及以上按住潜行时取消疾跑。
- 1.13 及以上在水中取消疾跑。
- 暂停或关闭自动疾跑时，恢复疾跑键的实际键盘或鼠标状态。

ViaForge 及随附协议库的原有许可证和资源随依赖保留。ViaForge 使用 GPL-3.0-or-later，
源码见其官方仓库的 [4.3.2 标签](https://github.com/ViaVersion/ViaForge/tree/4.3.2)。
