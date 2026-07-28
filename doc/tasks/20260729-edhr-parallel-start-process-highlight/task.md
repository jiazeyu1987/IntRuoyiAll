# 20260729-edhr-parallel-start-process-highlight

## Task Goal

让批次执行详情页的当前可执行工序状态与工艺路线关系图一致：当“工序开始”后第一组存在多个并行直接后继工序时，这一组工序都应显示当前运行态黄色背景，而不是只显示排序第一的粗洗工序。

## Milestones

- [x] 识别当前单工序 `currentProcess*` 投影与关系图第一组并行后继口径的差异。
- [x] 用 BDD + RED 复现开始节点第一组 3 个待打开工序只标黄 1 个的问题。
- [x] 实现正式数据链路修复，不放宽填写权限、不引入 fallback。
- [x] 运行目标后端/前端合同和相关回归验证。
- [x] 完成经验沉淀、收尾清理、提交和推送。

## Expected Verification

- 后端目标 JUnit 确认多起点路线创建链路未被本任务破坏。
- `node tests/e2e/edhr-batch-admin-current-process-highlight-static.spec.js`
- `node tests/e2e/edhr-batch-process-state-background-static.spec.js`
- 必要的相邻批次执行详情静态合同与类型检查。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，批次详情已按后端任务门禁 `available=true` 展示所有当前可执行工序组，而不是只取排序第一的单个 `currentProcess*`。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs/frontend-development.md#eDHR 当前工序运行态展示门禁`：当前工序黄底展示不得依赖 `OPEN_FORM`、填写人或 `activeWorkTaskId`；本任务进一步要求并行第一组按后端 `available=true` 任务门禁整体显示运行态。
- `docs/frontend-development.md#eDHR 产品信息虚拟 80 工序门禁`：产品信息虚拟工序必须排除当前正式工序匹配，避免复用来源 `routeProcessId` 误高亮。
- `docs/e2e-rules.md#静态合同与真实 E2E 同步门禁`：本任务新增聚焦静态合同覆盖窄范围黄底行为，并复跑相邻静态合同；真实浏览器 E2E 未运行需在验证报告中说明。
