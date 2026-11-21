# 命令注册

命令入口是 `LakeCommands.createDispatcher()`。每个命令在独立的处理器中注册，解析和用法提示由
`command/tree/CommandDispatcher` 负责。当前注册树：

```text
help (别名 commands)
└── <command>
test
├── on
└── off
```

`.help` 列出注册项，`.help test` 显示指定命令及其子节点。帮助文本从节点的名称、别名、描述和执行器生成。
`.test` 使用父节点执行器，`.test on` 和 `.test off` 使用对应子节点执行器。多余参数或未知子节点不会执行父节点。

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
