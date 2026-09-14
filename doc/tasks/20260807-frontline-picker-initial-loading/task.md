# 一线生产工序与员工选择初始化修复

## Task Goal

修复一线生产填写页初始化期间点击“工序”或“员工”时选择列表空白的问题，使工序加载不再被模板目录请求串行阻塞，并让选择弹框明确展示加载、前置条件、空数据和错误状态。

## Milestones

- [x] M1：定位截图时刻的请求顺序与前端状态链路。
- [x] M2：补充 BDD 场景和可稳定复现旧行为的 RED 静态合同。
- [x] M3：实现生产模式工序上下文并行初始化和选择弹框状态展示。
- [x] M4：运行目标合同、相邻回归、类型检查和技能证据校验。
- [x] M5：执行任务清理预览/应用并完成收尾记录。

## Expected Verification

- `node tests/e2e/frontline-production-picker-initial-loading-static.spec.cjs`
- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `node tests/e2e/role-matrix-ac-m10-sop-production-static.spec.cjs`（历史大合同基线观察；若先失败于旧锚点，按静态契约隔离门禁记录）
- `node tests/e2e/edhr-frontline-production-employee-options-match-leader-personnel-static.spec.cjs`
- `node tests/e2e/frontline-team-config-static.spec.cjs`
- `node tests/e2e/edhr-frontline-production-prototype-parity-static.spec.cjs`
- `node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check -- src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue tests/e2e/frontline-production-picker-initial-loading-static.spec.cjs ../doc/tasks/20260807-frontline-picker-initial-loading`
- Bug regression evidence validator
- Frontend feature evidence validator
- Vite 目标 SFC 转换请求返回 HTTP 200。

## Experience Gate Summary

- `docs/experience-index.md` 存在。
- 适用 `docs/frontend-development.md#前端静态契约隔离门禁`：使用任务专用最小合同稳定证明 RED/GREEN，避免扩大现有大合同范围。
- 适用 `docs/frontend-development.md#前端选择弹框即时反馈门禁`：选择弹框必须即时反馈；异步失败继续通过正式错误状态暴露，不得吞异常或伪造数据。
- 一线生产工序与员工继续使用正式 `device-account/processes` 和 `runtime-config` 数据链路，不改变权限或后端契约。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；仅调整正式请求时序和可见状态，异常仍抛出并展示。
- `是否从根因和长期维护角度解决`：是；解除无关模板目录请求对工序/员工上下文的串行阻塞，并补齐选择器状态模型。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260807-frontline-picker-initial-loading/task.md
- doc/tasks/20260807-frontline-picker-initial-loading/execution-log.md
- doc/tasks/20260807-frontline-picker-initial-loading/verification-report.md

## Current Status

completed：实现、验证、经验沉淀和 cleanup preview/apply 均已完成；真实登录 E2E 阻塞边界已记录。
