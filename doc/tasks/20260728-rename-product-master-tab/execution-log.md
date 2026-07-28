# 执行日志：产品主数据页签重命名

## User Intent

- 用户要求：将产品主数据页签的名字改成展厅主数据。

## Preconditions

- 当前工作区：`E:\IntRuoyi`。
- 当前分支：`int_main`，预检显示本地已领先 `origin/int_main` 4 个提交，工作区无脏文件。
- 已读取规则：`docs/frontend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 已读取技能：`clear-frontend-copy`。

## BDD / TDD Evidence

- BDD: 产品主数据页签改名 -> Given 用户进入产品主数据页面 / When 顶部页签或页面标题展示该入口名称 / Then 用户看到 `展厅主数据` 而不是 `产品主数据`。

## Milestone Log

- M1 in_progress: 正在定位标题来源。
