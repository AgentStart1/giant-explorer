# 插件

[yue-html]giant-explorer 的插件。包含imgTouchCanvas，支持图片缩放。

[yue]giant-explorer 的插件。同时支持作为独立app 运行。

[li]giant-exploror 的插件。功能是zip 解压缩。

GEP 元数据位于 `META-INF/giant-explorer-plugin.ini`：前三行依次为入口类、
Fragment 类列表（shell 插件可为空）和版本号。可选第四行 `type=shell` 表示
实现 `GiantExplorerShellPlugin` 的插件；省略或填写 `type=fragment` 则使用 Fragment 入口。
Shell 插件由宿主通过 GEP ClassLoader 实例化，注入插件管理器，并将 `group()` 返回的
操作添加到文件菜单；点击后调用 `start()`。Li 使用此方式，无需编译进宿主。
