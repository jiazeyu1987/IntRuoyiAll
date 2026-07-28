# 20260728 批记录表单列表产品名称下拉筛选执行日志

## User Intent

用户要求将截图红框中的“产品名称”输入下拉框改为：点击显示候选产品名称，点击候选后直接过滤无需查询按钮；也支持手动输入或复制产品名称后点击查询按钮过滤。

## Command / Evidence Log

- BDD: 点击产品名称输入框展示候选 -> Given 批记录表单目录存在多个产品名称 / When 用户点击产品名称筛选输入框 / Then 下拉展示当前批记录表单目录实际存在的产品名称候选。
- BDD: 点击候选立即过滤 -> Given 候选下拉中存在目标产品名称 / When 用户点击该候选 / Then 快速筛选写入 `productName` 并立即请求列表过滤，无需点击查询按钮。
- BDD: 手动输入查询过滤 -> Given 用户手动输入或复制产品名称 / When 用户点击查询按钮 / Then 列表按输入文本作为 `productName` 过滤。
- 2026-07-28: 任务启动。已加载 frontend-feature-delivery、backend-api-delivery、frontend/backend 规则、task-closeout、PowerShell 编码规则。
- 2026-07-28: `git status --short --branch --untracked-files=all` 显示 `int_main` 分支已有既有脏改动且本地 ahead 4；本任务实现前按规则做脏工作区基线提交。

## Baseline Dirty Worktree

- Baseline commit: `dfc71011 chore: baseline dirty worktree before product name dropdown`。
- Baseline scope: 保存实施前已存在的 22 个脏改动文件，包括批记录报表后端测试、产品名称 autocomplete 前端静态合同、其它并行任务文档/脚本与 `docs/e2e-rules.md`。
- Baseline verification: 基线提交后 `git status --short --branch --untracked-files=all` 显示工作区无未提交文件，分支为 `int_main...origin/int_main [ahead 5]`。

## RED

待记录。

## GREEN

待记录。

## REGRESSION

待记录。

## Blockers

暂无。
