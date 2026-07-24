# Task: BPM 报销流程前端交付

## Goal

在前端仓库中交付可实际使用的报销流程页面，包括报销发起、报销列表、报销详情与流程预测展示，并对接真实后端接口。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260514-dcc-empty-tabs-e2e-screenshots/task.md`
- Status before this task: completed
- Impact: the real browser verification path is already working on `http://localhost:8081`, so this task can extend that runtime for reimbursement workflow verification.

## Milestones

- [x] M1: 检查上一条前端任务状态并创建本任务文档。
- [x] M2: 记录 BDD 场景与前端 RED 证据。
- [x] M3: 补齐当前前端缺失的 BPM 发起、我的流程、待办任务入口路由。
- [x] M4: 接入已创建的真实后端流程定义与流程预测时间线。
- [x] M5: 运行真实路径验证并补齐证据文档。

## Expected Verification

- 前端存在报销流程的发起页、列表页、详情页。
- 发起页能展示流程预测时间线，并提交真实报销请求。
- 页面遵循当前 Int 统一前端风格，不引入 mock 数据或演示型分支。

## Current Status

Completed. The frontend now exposes BPM start and todo routes needed by the current app, and the real browser path has been verified from process start to leader approval.

## Final Verification Result

- Real-browser command:
  - `npx --yes --package @playwright/cli playwright-cli -s=bpm-expense-workflow run-code --filename doc\\tasks\\20260514-bpm-expense-workflow\\scripts\\verify-expense-workflow.mjs`
- Real-browser artifacts:
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\bpm-expense-workflow\expense-submitter.png`
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\bpm-expense-workflow\expense-leader-approve.png`
- Verified instance:
  - `8d5e3e20-4f44-11f1-8912-00155db32d8f`
