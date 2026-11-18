# 时行 (Shixing)

个人任务看板。把散在会话、表格、文档里的待办集中到一屏，并用桌面组件常驻显示三个数字：等你拍板的、卡住的、正在跑的。

名字取自《周易·艮卦·彖传》：「时止则止，时行则行，动静不失其时，其道光明。」

## 结构

    AndroidManifest.xml      清单，应用名「时行」，含桌面组件 receiver
    src/com/ray/kanban/
      MainActivity.java      主界面，WebView 加载 assets/index.html，提供设置/关于
      BoardWidget.java       桌面组件 provider，拉数据源并渲染三个数字
    assets/index.html        看板页面（深色科幻风，光效/背景/透明度可调）
    res/layout/widget_board.xml    组件布局（只用 RemoteViews 支持的基础控件）
    res/xml/widget_board_info.xml  组件元数据（4×4）
    res/mipmap-*/            应用图标（晷盘 + 指针）
    build.sh                 无 Gradle 构建：aapt2 → javac → d8 → zipalign → apksigner

## 构建

需要 JDK 17 与 Android SDK（build-tools 34）。构建脚本不依赖 Gradle，适合小内存机器。

    ./build.sh

产物在 build/kanban.apk。

## 版本约定

每次发版 versionCode 与 versionName 同步 +1，并在「关于 → 更新记录」里写清新增、删除、调整。

## 数据源

组件与页面通过一个返回 JSON 数组的地址取数据，字段名与飞书多维表格一致（如「状态」「紧急度」）。留空则只用内置快照。

## 署名

创作者：张瑞 及 Amoo·Rui
