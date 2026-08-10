# 批记录测试新增订单分配 Tab

## 任务目标

在“批记录测试”独立页面新增“订单分配”内部 Tab，将已确认的工序报工池、FIFO、手动调整、放行锁定、分配展示、历史流转、并发校验和调整审计规则整理为可直接点击“测试”的结构化测试任务，供订单分配功能开发完成后持续验证。

## 里程碑

- [x] P1：形成 PRD、测试计划和稳定验收编号。
- [x] P2：先补充失败的静态合同，锁定 Tab、测试段落和执行入口。
- [x] P3：实现“订单分配”Tab 及完整列表能力。
- [x] P4：完成静态回归、类型检查和真实浏览器验证。
- [x] P5：完成经验沉淀和任务清理收尾。

## 预期验证

- 静态合同验证新增 Tab、稳定 DOM 锚点、列表配置、测试按钮以及所有结构化业务段落。
- `pnpm ts:check` 验证 Vue/TypeScript 类型契约。
- Playwright 从真实“批记录测试”菜单进入页面，切换“订单分配”Tab，验证段落可见、操作按钮存在且页面无 console/page error。
- 不在本任务内伪造或宣称订单分配生产功能已完成；本任务交付的是可供后续功能验收的测试定义和页面入口。

## 经验门禁摘要

- 沿用现有 `BatchRecordTestPage.vue` 的标准 `UnifiedListTemplate`、稳定 `table-key`、租户选择、CODE_READONLY 原子执行和持久化描述恢复契约。
- 内部 Tab 不新增动态菜单；真实浏览器仍须通过现有“批记录测试”菜单进入，并记录页面锚点、console error 和 page error。
- 已存在持久化测试项但缺少结构化 `checkpoint.remark` 时必须失败，不回退到源码默认描述。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；新增测试分类使用现有正式测试定义、持久化和执行链路。
- 是否存在临时补丁或绕过：否。

## Current Status

completed：实现、聚焦回归、类型检查、真实菜单 Playwright、独立盲测、经验复核和任务清理全部完成。

## Cleanup Candidates

- output/playwright/batch-record-test-order-allocation-tab/.playwright-cli
- output/playwright/batch-record-test-order-allocation-tab/auth-state.json
- output/playwright/batch-record-test-order-allocation-tab/order-allocation-tab.png
- output/playwright/batch-record-test-order-allocation-tab/order-allocation-tab-1440x900.png
- output/playwright/batch-record-test-order-allocation-tab/order-allocation-tab-fresh-1440x900.png

## Cleanup Keep

- doc/tasks/20260809-batch-record-test-order-allocation-tab/prd.md
- doc/tasks/20260809-batch-record-test-order-allocation-tab/test-plan.md
- doc/tasks/20260809-batch-record-test-order-allocation-tab/task-state.json
- doc/tasks/20260809-batch-record-test-order-allocation-tab/test-report.md
- output/playwright/batch-record-test-order-allocation-tab/independent/browser-verification.json
- output/playwright/batch-record-test-order-allocation-tab/independent/order-allocation-1440x900.png
- output/playwright/batch-record-test-order-allocation-tab/independent/order-allocation-list-full.png
