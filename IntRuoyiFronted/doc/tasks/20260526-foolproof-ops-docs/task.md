# 任务：傻瓜式运维十项能力前端设计配合

## 任务目标

- 在前端 worktree 中为“公司 IT 傻瓜式运维”补充运行控制台 UI/交互开发文档。
- 配合后端主控任务 `20260526-foolproof-ops-docs`，重点覆盖决策向导、候选选择、巡检面板、站内信入口、备份演练可视化和事故闭环页面。
- 本阶段只写开发文档，不修改生产前端代码。

## Worktree

- 前端 worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-foolproof-ops-docs\yudao-ui-admin-vue3`
- 后端主控 worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-foolproof-ops-docs\ruoyi-vue-pro`
- 分支名：`task/20260526-foolproof-ops-docs`

## BDD 场景

- BDD: IT 通过向导选择正确运维动作 -> Given IT 打开运行控制台, When 选择应用异常、数据异常、发布前检查或发布后观察, Then 页面展示推荐动作、所需证据、责任人和阻断条件。
- BDD: 回滚和恢复候选不可手填未知值 -> Given 后端返回可回滚镜像和可恢复备份点, When 用户执行回滚或恢复, Then 页面只能选择候选并展示验证证据。
- BDD: 巡检和事故状态可视化 -> Given 后端返回巡检、备份演练、站内信和事故数据, When 用户查看运行控制台, Then 页面以可扫描状态展示 PASS、WARN、BLOCKED、NO-GO。

## 里程碑

- [x] M1：建立前端任务文档。
- [x] M2：由子 agent 编写前端交互和 API 接线设计。
- [x] M3：配合主 reviewer 审查文档可实现性、副作用和 BDD/TDD 完整性。
- [x] M4：记录验证结果与收尾预览。

## 预期验证

- 前端设计文档落盘到 `subagent-output/frontend-ops-console-design.md`。
- 文档包含 BDD、RED、GREEN、REGRESSION、接口契约、权限态、危险确认和无副作用说明。
- UTF-8 读取正常。

## 当前状态

- 状态：completed
- 已完成：前端 worktree 与任务文档已建立；前端子 agent 设计文档已落盘；主 reviewer 已完成条件放行审查。
- 当前结论：前端文档任务完成，可作为实现规划输入；后续实现前必须按后端 `review-report.md` 统一接口命名并确认测试 runner。
- 最终验证：PASS。UTF-8、关键标记和 task-closeout-cleanup preview 均通过。
- 阻塞：暂无。

## Cleanup Keep

- `doc/tasks/20260526-foolproof-ops-docs/subagent-output/frontend-ops-console-design.md`
