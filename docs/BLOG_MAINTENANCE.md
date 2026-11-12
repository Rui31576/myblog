<title>博客技术档案_2026-11-12</title>

项目：个人博客 RuiCore | 边地修行录
仓库：Rui31576/myblog（公开）
站点：https://rui31576.github.io/myblog/
最后更新：2026-11-12 23:15 CST
状态：可用（构建已恢复）

这份文档记三件事：这个博客是怎么搭的、这次修了什么、以后怎么发文章。目的只有一个——换窗口或隔几个月再回来时，不用重新摸一遍。

## 一、这个博客的构成

- 仓库 `Rui31576/myblog`，公开，只有一个分支 `main`
- 直接 fork 了 `nanxiaobei/hugo-paper` 主题，**布局文件在仓库根目录**，没有 `theme=` 配置，也没用 submodule
- 发布方式：`.github/workflows/hugo.yml` 里配了 GitHub Actions，往 `main` 推代码就自动 `hugo --minify` 构建，再用 `actions/deploy-pages` 发到 Pages
- 站点挂在 `/myblog/` 子路径下，不是根域名，所以 `rui31576.github.io` 本身是 404，这是正常的
- `hugo.toml` 里 `baseURL` 写死为 `https://Rui31576.github.io/myblog/`
- 文章在 `content/posts/`，日记在 `content/diary/`

一个历史信息：仓库里带着完整的 `.obsidian/` 目录，装了 obsidian-git 等插件，最后一次提交信息是 `vault backup: 2025-12-22 09:21:28`。也就是说当初是**把这个仓库当 Obsidian 库写，靠 obsidian-git 插件自动提交推送**，博客因此停了近一年。

## 二、这次修了什么

### 1. 两篇文章的文件名多了 `.md`

`first-post.md.md`、`hello.md.md`。Hugo 把整个文件名当作永久链接，于是地址变成 `/posts/first-post.md/`，很难看，也不符合常规。

**已改**：重命名为 `first-post.md`、`hello.md`，地址分别为 `/posts/first-post/`、`/posts/hello/`，旧的带 `.md` 地址已 404。

### 2. hello 那篇日期显示成 `Jan 1, 1`

原因不是缺日期，而是**文件第一行有一句 `<!-- end list -->`**，把 YAML front matter 顶到了第二行。Hugo 要求 front matter 必须在文件最开头，否则整块不生效，日期取零值，显示成公元 1 年。

**已改**：删掉那行前导注释。现在页面显示 2025-12-21。

### 3. `.trash/` 被提交了

Obsidian 删掉的草稿进了回收站目录，被一起提交，共 6 个废弃文件。

**已改**：从版本库移除。

### 4. `exampleSite/` 整个目录还在

那是主题自带的示例站，17 个文件，跟这个站无关，白占体积还容易混淆。

**已改**：从版本库移除。

### 5. 构建失败：Hugo 版本撞上新版

前四项推上去之后，Actions 构建直接失败，站点没有更新。

原因：工作流里写的是 `hugo-version: 'latest'`。主题是 2021 年的老主题，新版 Hugo 有破坏性改动，`hugo --minify` 退出码 1。

**已改**：把版本固定为 `0.145.0`，构建立刻通过。

**这条最要紧：以后不要把 `hugo-version` 改回 `latest`**，否则站会突然挂，而且报错只有一句「Process completed with exit code 1」，很难查。

## 三、踩过的坑（可复用）

**坑一：构建失败的日志拿不到。** GitHub Actions 的 job 日志下载接口要仓库管理员权限，普通账号调 API 只会返回 403。能看到的最细信息就是 check-run 的 annotations，里面只有「exit code 1」。所以遇到构建失败，要么让人去网页看日志，要么用「改动一个已知变量再试一次」的方式二分定位——这次就是靠固定 Hugo 版本试出来的。

**坑二：从国内网络 clone 这个 8.7MB 的仓库会卡死。** 实测 HTTPS 报 `Recv failure: Connection was reset`，SSH 传到 0.03MB 就停住。

绕法是只取元数据不取内容：

```
git clone --filter=blob:none --no-checkout --depth 1 git@github.com:用户/仓库.git 目标目录
cd 目标目录
git sparse-checkout init --no-cone
git sparse-checkout set /content /hugo.toml /.github
git read-tree -mu HEAD
```

这样只下载 commit 和 tree，外加指定的少量文件，几秒完成。改完直接 push，push 只需要新对象，不受影响。

**坑三：文件名带多余后缀、front matter 前有杂行。** 这两类错误 Hugo 都不报错，只是默默生成一个错误的 URL 或错误的日期，不主动查很难发现。

**坑四：`ceshi.md` 里的图片实际是坏的。** 正文写的是 `![](IMG20251221213915.jpg)`，相对路径，而那张图在仓库根目录。Hugo 不发布根目录的散文件，所以线上解析成 `/posts/ceshi/IMG20251221213915.jpg`，必然 404。要修就得把图移到 `static/`，正文写成 `/myblog/文件名.jpg`。

## 四、以后怎么发文章

最省事的方式：内容口述或发文字，由助手写成文件推到 `content/posts/`，GitHub Actions 自动构建，一到两分钟上线，不需要在网页上点任何东西。

文章格式：

```markdown
---
title: "标题"
date: 2026-11-13T08:00:00+08:00
draft: false
tags: ["梦记", "清明梦"]
categories: ["修行笔记"]
---

正文从这里开始。
```

四条约定：

- **文件名就是网址**。`content/posts/qingming-dream-01.md` 对应 `/posts/qingming-dream-01/`。文件名里不要再带 `.md`。
- **front matter 必须在第一行**，前面不能有任何注释、空行或 HTML 标记。
- **`draft: true` 是草稿**，不会发布；改 `false` 才上线。
- **图片放 `static/`**，正文里写 `/myblog/图片名.jpg`（因为站点在子路径下，不能用 `/图片名.jpg`）。

建议的分类：梦境类用标签 `梦记`、`清明梦`，修行类用 `内丹`，分类统一放「修行笔记」。梦记适合按日期命名，比如 `2026-11-13-dream.md`，自然形成时间线。

## 五、证据

- 清理提交：`6015f6c`（改名、修 front matter、移除 .trash 与 exampleSite）
- 版本锁定提交：`9a2bb77`（Hugo 固定 0.145.0）
- 构建：`9a2bb77` 的 Actions 运行成功，`6015f6c` 那次失败
- 验证：`/posts/first-post/` 200、`/posts/hello/` 200、`/posts/my-story/` 200、`/posts/first-post.md/` 404
- hello 页面日期已显示 2025-12-21

## 六、待办

1. **`ceshi.md` 的图片是坏的**，图在根目录 `IMG20251221213915.jpg`（3.7MB）。要么移进 `static/` 并改正文路径，要么把这篇测试文删掉。
2. **`images/` 目录约 2.6MB**，是主题的示例截图（screenshot.png、tn.png 等），这个站用不到，可以删。
3. `.obsidian/` 约 2.8MB，是 Obsidian 插件本体。如果确定不再用 Obsidian 写，可以删；如果还想用，保留。
4. 可选：给博客挂自定义域名。`amoorui.com` 现在给隧道和看板用了，博客可以走 `blog.amoorui.com`，加一条 CNAME 即可。
