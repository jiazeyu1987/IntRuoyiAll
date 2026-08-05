# AC-M23 放行负责人审核放行闭环修复

## Task Goal

修复 AC-M23「放行负责人审核并放行生产订单」代码级缺口：放行负责人必须能在正式入口签名放行或退回，终态操作必须有审计；越权、重复放行、签名缺失、预检未通过必须被拒绝并保留可验证证据。

## Milestones

1. 建立 AC-M23 BDD/TDD 场景和当前缺口复现。
2. 修复后端放行终态审计、放行审批签名证据校验、退回路径责任人校验。
3. 修复前端批次详情放行负责人退回入口和静态契约。
4. 运行定向后端、前端静态验证并记录结果。
5. 完成验证报告和收尾状态更新。

## Expected Verification

- 后端定向测试覆盖 submit 审计、approve 签名证据校验、reject 退回与越权拒绝。
- 前端静态契约覆盖批次详情页调用 `rejectEdhrRelease`、使用 `mes:pro-edhr-release:reject` 权限、保留质量拒收入口不混淆。
- 现有 AC-M23 release service regression 和审批中心 release adapter 相邻测试通过或记录明确 blocker。
- 若真实 E2E 前置不可用，不宣称真实页面验收通过，只记录未运行原因和剩余风险。

## Applicable Gates

- eDHR 放行负责人来源门禁：展示与授权必须共用 `RELEASE_APPROVE`，不得用关闭负责人、当前登录人或静态阶段角色替代。
- 前端静态契约隔离门禁：如全量静态/ts 检查存在无关历史失败，需用任务专用最小静态契约证明当前行为 RED/GREEN。
- E2E 脚本入口存在性门禁：真实 E2E 只有在 Playwright 操作正式页面并完成目标断言后才能记 PASS。
- 后端 no-fallback 门禁：缺少签名证据、负责人规则、预检通过状态或审计前置时必须 fail fast。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，修复放行终态服务校验、审计和前端正式退回入口。
- 是否存在临时补丁或绕过：否。

## Current Status

blocked

Implemented code changes, frontend static verification, and evidence validators. Backend Maven verification is blocked by concurrent `yudao-module-mes` Maven processes in the shared main workspace; an AC-M23-only detached verification worktree also could not reach Surefire because clean HEAD lacks the non-task QA regulation `publish(...)` implementation currently present as dirty shared-workspace code. This task is not ready for closeout, commit, or push.

## Milestone Status

- M1 BDD/TDD scenarios: completed.
- M2 backend repair: implemented, pending Maven GREEN.
- M3 frontend release-return entry: completed and static contracts PASS.
- M4 verification: partially complete; frontend/static/evidence validators PASS, backend Maven blocked before Surefire.
- M5 closeout: pending backend verification, cleanup, commit, and push.
