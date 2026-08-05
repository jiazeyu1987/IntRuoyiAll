# AC-M04 活跃订单验收产物同步

## Task Goal

继续推进 `AC-M04 / 加入活跃订单池` 的验收闭环：核对当前系统已经完成的动作证据，修正旧 E2E 结果产物中 `activeOrderCleanupDeferred` 与最新报告 `activeOrderCleanupCompleted=PASS` 的不一致，并明确 AC-M04 是否可从 `PASS_ACTION_NOT_ACCEPTED` 提升到 `ACCEPTED`。

## Milestones

- [x] 核对当前任务文档、E2E 结果产物和测试脚本中的 AC-M04 状态。
- [x] 判断差异来源：旧结果产物、脚本未同步，或真实运行仍缺口。
- [x] 在不引入 fallback 的前提下，同步或补齐最小正式证据。
- [x] 运行静态/结果一致性校验，必要时给出阻塞原因。
- [x] 更新验证报告和当前状态。
- [x] 按用户要求补齐本机 `RRM_*` 真实 E2E 前置：确认本机运行态、角色账号、正式业务 ID、签名池和调拨/QA 数据；密码只在进程环境中使用，不写入文档或提交。
- [x] 重新运行 `real:check`，若前置为 0 blocker，再运行 full real E2E。
- [ ] 补齐 RRM PQC 正式提交前置：先走真实生产填写页生成本轮 `processPoolEventId`，再把它作为 `productionSubmitEventId/processPoolEventId` 带入 PQC 页面提交。
- [x] 核对 PQC 前置提交失败的根因：确认当前本机库缺 `mes_pro_process_pool_event.event_idempotency_key` / `recordbook_entry_id`，且完整 P0 runtime 迁移需要正式历史 backfill 前置。
- [x] 等待正式授权、备份、rollback 和逐行 repair manifest 后，再处理 88 行历史 P0 backfill blocker；未满足前不得写入 synthetic/backfill 数据。
- [x] 已获用户明确授权“授权修复本机库 P0 backfill”；当前仅限本机 Docker MySQL `ruoyi-vue-pro`，禁止触碰测试服/正式服/备用服。
- [x] 生成备份、rollback 和逐行 repair manifest，并执行最小本机库 backfill + 官方迁移复验。
- [ ] P0 runtime schema 修复后重跑 `real:check` 和 full real E2E，确认 PQC 正式提交是否解除 schema blocker。

## Expected Verification

- 只读核对 `test-report.md`、`verification-report.md`、`task-state.json` 与 `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json`。
- 若修改脚本或结果产物，必须运行相应静态检查或 JSON 结构校验。
- 明确记录 AC-M04 当前状态、已满足项、仍缺项和下一步命令。

## Current Status

in_progress

当前代码脚本已包含 `activeOrderCleanupCompleted` 清理闭环逻辑，canonical 任务报告也证明 AC-M04 已有加入、冲突路线拒绝、跨角色只读、错误角色写入拒绝、最终清理和并发门禁 PASS/GREEN 证据。本轮已按用户要求在本机补齐 `RRM_*` 前置并刷新 full real E2E：`real:check` 已 PASS，full real E2E 已生成 `mode=real` 产物，AC-M04 的 `joinActiveOrder`、冲突路线拒绝、跨角色只读和最终清理均为 PASS；但整体结果仍为 `BLOCKED`，原因是后续 PQC 正式提交、eDHR 放行准备、并发/性能门禁和 62 个矩阵 coverage 准出仍未闭环，因此 AC-M04 暂仍不能提升为 `ACCEPTED`。

2026-08-05 修复复核：旧历史 worktree 的 `activeOrderTransferTraceReadOnly / E2E_TRANSFER_TRACE_DATA` blocker 在当前源码层面未复现为代码链路缺口；当前系统已具备 `transferIds` 页面录入、前端 API 透传、后端加入/重复/并发路径记录正式调拨追溯、只读接口和回归测试。未改生产代码，原因是没有可复现的当前代码缺陷；按 no-fallback 规则，剩余验收必须在完整 `RRM_*` 真实环境下重跑 full real E2E。

2026-08-05 13:05 复验更新：等待主工作区并发 Maven 进程释放后，AC-M04/调拨边界目标 JUnit 已取得 `BUILD SUCCESS`，共 21 个测试通过；角色矩阵大静态前置在同步 AC-M19 聚合幂等键断言后恢复 PASS；调拨只读静态合同 PASS。当前剩余阻塞收敛为真实 E2E 环境缺少完整 `RRM_*` 变量，仍不能刷新 full real E2E 产物，也不能把 AC-M04 标为 `ACCEPTED`。

2026-08-05 继续补齐：用户明确要求“我不懂代码，你来添加”，本轮任务转为由 Agent 在本机测试租户中补齐 `RRM_*` 前置并执行验证。当前主运行态 `8081/48081` 可用；旧 `8098/48098` RRM slot 已不监听。六个历史 RRM 角色账号存在但默认密码登录失败，需在本机授权测试租户中修复账号密码或重新生成任务自有账号后再注入环境变量。

2026-08-05 15:xx 更新：已在本机测试租户修复七个 RRM 角色账号登录前置，使用进程级环境变量注入完整 `RRM_*`，未把密码写入文档或文件。`pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` 已 PASS；full real E2E 先后暴露并修正三个测试脚本/页面稳定性缺口：加入活跃订单后重读最终列表、PQC 规程元信息按真实页面结构断言、PQC 逐件按钮使用稳定标识点击。最新 full real E2E 产物为 `status=BLOCKED`、`mode=real`、`phaseEvidence=6`、`actionEvidence=22`、`gateEvidence=2`、`blockers=74`；其中 AC-M04 关键动作证据均为 PASS，剩余阻塞已转移到 PQC 正式提交、eDHR 放行准备、并发/性能和全矩阵 coverage 准出。

2026-08-05 继续修复更新：PQC 正式提交已经不再卡在前端禁用态，真实生产填写提交已打到后端；后端失败根因为运行库 `mes_pro_process_pool_event` 缺 `event_idempotency_key` / `recordbook_entry_id`，导致 Mapper 查询报 `Unknown column 'event_idempotency_key' in 'field list'`。只读 P0 迁移预检显示完整迁移还受 88 行历史数据 blocker 阻塞：79 行 PQC record 缺 `production_submit_event_id`、2 行历史 `PRODUCTION_SUBMIT` 缺 `event_idempotency_key`、2 行历史 `PRODUCTION_SUBMIT` 缺 `recordbook_entry_id`、5 行 quantity fragment 缺正式 submit root。正式 source audit 进一步确认这些历史行无法从当前结构化记录本来源唯一推导，repair plan gate 要求业务/DBA 授权、备份、rollback 和逐行 manifest 后才能执行历史修复；按 no-fallback 规则，当前任务在授权前阻塞，禁止用旧事件 ID、空默认值、合成幂等键或直接删除历史行绕过。

2026-08-05 授权更新：用户明确回复“授权修复本机库 P0 backfill”。授权范围仅限当前本机 Docker MySQL `127.0.0.2:23306/ruoyi-vue-pro`，目标是补齐 P0 runtime backfill 前置并让现有正式迁移脚本通过；不得访问或修改测试服、正式服、备用服，不得记录数据库密码。

2026-08-05 本机 P0 backfill 修复完成：已生成本机备份 `db-backup/acm04-p0-backfill-extended-20260805-203724.sql`（SHA256 `317BD20FD77F473327B5DAAAEAC5C4A51D474958A9B32A7D652732310C17C8B8`）、review-signature 备份 `db-backup/acm04-review-signature-20260805-204459.sql`（SHA256 `AEF0616C59C4DD85E9CD851B1855D7B72C68FE84469D984632D0E84DF9E5BBC6`）、rollback SQL 和 88 行逐行 repair manifest；本机 apply SQL 已执行，P0 preflight/source/runtime migration verifier 均 PASS。当前 DB schema blocker 已解除，下一步继续重跑 RRM `real:check` 与 full real E2E。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本轮脚本改动均为真实 E2E 稳定定位和页面真实结构同步，不改变业务成功/失败语义。
- `是否从根因和长期维护角度解决`：是，本轮未用旧报告伪造 `result.json`，而是补齐正式 `RRM_*` 前置后重新运行真实 E2E，并用静态合同锁定脚本与页面结构。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs\task-closeout-rules.md`、`docs\e2e-rules.md`、`docs\frontend-development.md`、`docs\login-access.md`、`docs\local-runtime.md`、`docs\worktree-restrictions.md`、`docs\powershell-encoding.md`。
- 命中经验索引：规划型 E2E、真实 E2E 主链路与 result.json 产物隔离、静态合同与真实 E2E 同步、worktree/int_main 运行态 URL 门禁。
