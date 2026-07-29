# 20260729 提交失败原因显示

## Task Goal

修复批记录提交失败弹窗只显示“提交失败”而不显示具体失败原因的问题。提交失败时必须把后端或提交链路返回的真实原因展示给用户，不能吞掉异常或用默认失败文案掩盖原因。

## Milestones

- [x] 定位提交失败弹窗与错误传播链。
- [x] 先补充 BDD 场景和 RED 静态回归测试，证明失败原因缺失。
- [x] 实现最小修复，展示真实失败原因。
- [x] 运行目标测试和相邻回归验证，记录证据。
- [x] 完成收尾、经验沉淀、提交与推送。

## Expected Verification

- `node tests/e2e/edhr-fill-workspace-action-result-dialog-static.spec.js`
- `node tests/e2e/edhr-fill-workspace-static.spec.js`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- 真实提交失败路径 E2E：当前现有真实脚本覆盖提交成功/审批策略；本次未新增写入型失败 E2E，原因是需要单独构造可提交执行记录、失败签名场景和清理链路，当前变更已由聚焦静态合同锁定失败原因传递与页面展示。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是保留并展示提交失败真实原因。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端保存/提交错误必须通过 UI 明确暴露真实错误，不得吞异常或只显示默认成功/失败状态。
- 前端静态合同隔离门禁：本任务使用聚焦静态合同覆盖提交失败弹窗，不修改无关宽合同。
- E2E 写入门禁：真实写入 E2E 需要测试租户、可追踪任务数据和清理链路；未用 mock、拦截或 API-only 代替真实页面路径。
