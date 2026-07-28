# 20260728 Switch Filler Wangxin E2E

## Task Goal

修复 eDHR 辅助填写模式中 wangxin 账号打开“切换填写人”后，其他填写人候选可见但被禁用的问题；候选必须来自执行详情 `assistSwitchTasks` 快照，点击其他填写人后继续走正式 `openTask`，并让表单上下文随所选填写人/任务变化。

## Milestones

- [x] M1: 建立任务证据、读取门禁并记录 BDD/TDD 验证要求。
- [x] M2: 复现当前静态回归，锁定前端禁用和快照消费缺失。
- [x] M3: 最小化修复前端切换填写人快照读取、可选态和选中态。
- [x] M4: 运行静态合同、ESLint、后端编译、定向 JUnit 和真实 Playwright E2E；记录全量 `pnpm ts:check` 的无关阻塞。
- [ ] M5: 完成验证报告、经验沉淀、cleanup、提交/推送边界和最终状态。

## Expected Verification

- RED/GREEN: `node IntRuoyiFronted/tests/e2e/edhr-switch-filler-selectability-static.spec.js`。
- RED/GREEN: `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-assist-filler-switch-snapshot-static.spec.cjs`。
- REGRESSION: `pnpm exec eslint src/views/mes/pro/edhr/ExecutionPage.vue src/api/mes/pro/feedback/index.ts --format stylish`。
- REGRESSION: `pnpm ts:check`；当前失败在无关文件 `BatchRecordCellRulesConfirmDialog.vue` 与 `BatchExecutionDetailPage.vue` 的既有类型缺口，非本任务 `ExecutionPage.vue`。
- REGRESSION: `mvn -pl yudao-module-mes -am "-DskipTests" compile`。
- E2E: 使用 `int_main` 本地 `http://localhost:8081` / `http://127.0.0.1:48081`，以 wangxin 真实前端路径进入辅助填写页，验证其他填写人可点击、点击后上下文切换、无全量批次详情重载、无 API 错误。

## Current Status

blocked_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，修复执行详情快照消费和候选可选态，而不是前端伪造候选或绕过后端授权。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- `docs/backend-development.md#切换填写人快照读取边界`：候选必须来自执行详情 `assistSwitchTasks`，不得在弹窗打开时调用全量 `getEdhrBatchExecution`；传统批记录执行记录必须按 `batchExecutionId + taskId` 隔离，不得置空 taskId 复用旧 execution。
- `docs/e2e-rules.md#静态合同与真实 E2E 同步门禁`：先用聚焦静态合同 RED/GREEN 锁定本缺陷，再执行真实页面 E2E。
- `docs/frontend-development.md#前端静态契约隔离门禁`：若宽合同存在无关失败，只记录阻塞，不顺手修改无关逻辑。

## Cleanup Keep

- doc/tasks/20260728-switch-filler-wangxin-e2e/bug-regression-evidence.md
- doc/tasks/20260728-switch-filler-wangxin-e2e/frontend-feature-evidence.md
- doc/tasks/20260728-switch-filler-wangxin-e2e/backend-api-evidence.md
- doc/tasks/20260728-switch-filler-wangxin-e2e/real-e2e-evidence.md
- doc/tasks/20260728-switch-filler-wangxin-e2e/verification-report.md
- doc/tasks/20260728-switch-filler-wangxin-e2e/e2e-artifacts/switch-filler-wangxin-real.e2e.cjs
- doc/tasks/20260728-switch-filler-wangxin-e2e/e2e-artifacts/switch-filler-wangxin-real-result.json
