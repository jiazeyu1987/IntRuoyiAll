# Task: 三张工序页修改后再次对图复核

## Goal

对 `精洗工序生产记录`、`清洗工序生产记录`、`清洁工序生产记录` 重新做一轮
live 截图复核，确认在最近一轮通用版式规则修改之后，和对应源图相比是否继续
有优化。

## Scope

- 不新增产品功能，只做真实重生、真实截图、真实对图
- 对比对象固定为：
  - 源图：`page-05.png`、`page-06.png`、`page-08.png`
  - 当前 Route B 真实生成报表
- 输出逐张结论，说明是否继续优化，以及剩余的通用差异

## Previous Task Check

- Previous task:
  `doc/tasks/20260517-batch-record-generic-layout-rules-followup/task.md`
- Status before this follow-up: completed
- Impact: 上一轮已经完成通用规则增强并做过一轮 live 复核；本轮只验证最新结果是否继续优化

## Milestones

- [x] M1: 确认前后端运行态与当前 Route B 报表可访问
- [x] M2: 真实重生 Route B
- [x] M3: 抓取三张最新 live 截图
- [x] M4: 与三张源图逐张比较并形成结论
- [x] M5: 更新任务证据并收口

## Expected Verification

- `GET http://127.0.0.1:48081/v3/api-docs`
- `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B`
- 任务目录 `artifacts/` 下生成三张 live 截图用于复核

## Current Status

Completed. The latest Route B screenshots show further improvement after the recent generic-rule updates: `精洗` stays closest to the source, `清洗` improves the most in repeated-structure handling, and `清洁` becomes more stable in width and hierarchy while still leaving some residual spacing differences.
