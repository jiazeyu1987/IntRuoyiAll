# Frontend Feature Evidence

## Feature Goal

Feature: 个人工作台对普通批记录与 FormCenter 动态表单使用统一导航；FormCenter 工作任务不再强制要求传统 `executionId`，而是通过正式 `task/open` 响应进入批次详情并自动打开路线表单抽屉。

## Entry Points

Entry points: `/mes/pro/feedback/edhr-work-task` 个人工作台“处理”按钮；`/mes/pro/feedback/edhr-batch-execution/detail?openRouteForm=1&batchExecutionId=...&batchTaskId=...&workTaskId=...`。

## API Contracts And Data States

Acceptance: 工作台行级操作必须绑定当前行 `workTaskId`；动态表单任务允许 `executionId` 为空，但必须有 `batchExecutionId/batchTaskId/workTaskId/formCenterInstanceId/formTemplateId`。
API state: `task/open` 返回 `workTaskId/taskId/formCenterInstanceId/formTemplateId/executionPageQuery`，前端不得用 API-only 或旧直连 execution URL 替代真实页面入口。

## BDD

BDD: 动态表单工作台入口统一 -> Given 工作任务为 FormCenter 动态表单且没有传统 executionId, When 用户点击个人工作台“处理”, Then 前端调用正式 openTask 并打开批次详情路线表单抽屉。
BDD: 行级定位 -> Given 工作台列表包含多个任务, When E2E 点击“处理”, Then 必须按目标批次和任务编码所在可见行点击，不得点击页面第一个按钮。

## RED

RED: 静态合同初始失败，旧导航对 FormCenter 工作任务仍依赖 `executionId` 或未统一使用 `navigateToEdhrWorkTask`。

## GREEN

GREEN: `node tests\e2e\edhr-work-task-formcenter-navigation-static.spec.js` -> PASS。
GREEN: `node tests\e2e\edhr-work-task-notify-workbench-fill-navigation-static.spec.js` -> PASS。
GREEN: `node tests\e2e\edhr-work-task-board-unified-navigation-static.spec.js` -> PASS。

## E2E Path

Verification: `node tests\e2e\edhr-work-task-process-advance-real.e2e.js` -> PASS；Playwright 登录测试租户 `测试租户` 的 `aoteman/admin`，从个人工作台查询批次、点击可见目标行“处理”、打开 FormCenter 抽屉并点击真实“提交”按钮。

## Blockers

Blockers: none。
