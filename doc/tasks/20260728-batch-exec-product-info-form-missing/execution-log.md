# 20260728 批次执行批记录表单产品信息表单缺失修复执行日志

## User Intent

用户反馈：“批次执行里面的批记录表单的产品信息表单缺失了”。

## Bootstrap Evidence

- 已读取 `bug-regression-fix-loop` 技能及 `references/bug-contract.md`。
- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/e2e-rules.md`、`docs/database-rules.md`、`docs/local-runtime.md`。
- 已读取 `docs/experience-index.md` 并摘取本任务命中的批记录表单、eDHR 批次执行、静态契约和 Git/PowerShell 门禁到 `task.md`。

## BDD

- BDD: 批次执行展示产品信息批记录表单 -> Given 某工序在工序设置中正式绑定了产品信息批记录表单，When 用户进入批次执行并查看该工序的批记录表单，Then 产品信息表单必须出现在批记录表单列表/卡片中，且来源为逐工序批记录绑定而不是表单槽位或工序开始配置。

## Git Baseline

- 启动检查发现 `int_main` 相对 `origin/int_main` ahead 1，且存在既有脏改动；按项目门禁，后续会将既有脏改动与当前任务文件分离，当前任务文件不得进入既有脏工作区基线提交。

## Milestone Log

- in_progress: 建立任务文档和 BDD，准备隔离既有脏工作区基线并定位缺陷根因。

## Verification Evidence

- 待补充 RED/GREEN/REGRESSION。

## Blockers

- 暂无。
