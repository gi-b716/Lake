# 命令注册

命令入口是 `LakeCommands.createDispatcher()`。每个命令在独立的处理器中注册，解析和用法提示由
`command/tree/CommandDispatcher` 负责。当前注册树：

```text
help (别名 commands)
└── <command>
modules (别名 list)
└── <category>
toggle (别名 t)
└── <module>
module
└── <module>
    ├── on
    └── off
bind
└── <module>
    └── <key>
clickgui (别名 gui)
```

`.help` 列出注册项，`.help module` 显示指定命令及其子节点。帮助文本从节点的名称、别名、描述和执行器生成。
`.modules` 列出模块，`.module <module> on/off` 设置状态，`.bind <module> <key>` 设置按键，`.gui` 打开模块菜单。

新增命令示例（在注册入口调用处理器的 `register` 方法）：

```java
dispatcher.register(literal("config")
    .description("Manage configurations")
    .then(literal("load")
        .then(argument("name", ArgumentParser.word())
            .executes(context -> {
                String name = context.argument("name", String.class);
                // 在此调用配置加载逻辑，并通过 ClientChat 回显结果。
            }))));
```

字面量节点优先匹配，参数节点支持自定义 `ArgumentParser<T>`，内置单词和有范围的整数解析器。
每个节点最多有一个参数子节点，参数可继续连接字面量或参数节点。包含空格的参数可以加引号；
引号内支持转义引号和反斜杠。

注册时检查名称和别名冲突、重复参数名、循环引用、共享节点和缺少执行器的叶子节点。
注册成功后节点不可修改。预期的执行失败使用 `CommandException`，统一在聊天框回显。
