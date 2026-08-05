# AC-M04 活跃订单验收产物同步

## Task Goal

继续推进 `AC-M04 / 加入活跃订单池` 的验收闭环：核对当前系统已经完成的动作证据，修正旧 E2E 结果产物中 `activeOrderCleanupDeferred` 与最新报告 `activeOrderCleanupCompleted=PASS` 的不一致，并明确 AC-M04 是否可从 `PASS_ACTION_NOT_ACCEPTED` 提升到 `ACCEPTED`。

## Milestones

- [x] 核对当前任务文档、E2E 结果产物和测试脚本中的 AC-M04 状态。
- [x] 判断差异来源：旧结果产物、脚本未同步，或真实运行仍缺口。
- [ ] 在不引入 fallback 的前提下，同步或补齐最小正式证据。
- [x] 运行静态/结果一致性校验，必要时给出阻塞原因。
- [x] 更新验证报告和当前状态。

## Expected Verification

- 只读核对 `test-report.md`、`verification-report.md`、`task-state.json` 与 `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json`。
- 若修改脚本或结果产物，必须运行相应静态检查或 JSON 结构校验。
- 明确记录 AC-M04 当前状态、已满足项、仍缺项和下一步命令。

## Current Status

blocked

当前代码脚本已包含 `activeOrderCleanupCompleted` 清理闭环逻辑，canonical 任务报告也证明 AC-M04 已有加入、冲突路线拒绝、跨角色只读、错误角色写入拒绝、最终清理和并发门禁 PASS/GREEN 证据。但当前 shell 缺少全部 `RRM_*` 真实 E2E 环境变量，`real:check` 只能生成 ENV blocker-only 的 `result.json`，不能安全刷新 full real E2E 产物，也不能把 AC-M04 从 `PASS_ACTION_NOT_ACCEPTED` 提升为 `ACCEPTED`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本轮未用旧报告伪造 `result.json`，而是明确要求在正式 `RRM_*` 环境下重新运行真实 E2E 后再刷新产物。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs\task-closeout-rules.md`、`docs\e2e-rules.md`、`docs\frontend-development.md`、`docs\login-access.md`、`docs\local-runtime.md`、`docs\worktree-restrictions.md`、`docs\powershell-encoding.md`。
- 命中经验索引：规划型 E2E、真实 E2E 主链路与 result.json 产物隔离、静态合同与真实 E2E 同步、worktree/int_main 运行态 URL 门禁。
