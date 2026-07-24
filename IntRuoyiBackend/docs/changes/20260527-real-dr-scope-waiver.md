# Change Decision: 真实 DR 放行范围豁免

- change_id: `20260527-real-dr-scope-waiver`
- task_id: `20260526-foolproof-ops-implementation`
- decision_time: `2026-05-27 Asia/Shanghai`
- decision: `ACCEPTED`
- requester_scope_change: `允许不执行真实 DR，仅按当前非破坏性证据放行。`

## 背景

当前 paired worktree 的代码、文档、静态契约、backup-ops 非破坏性测试和独立 reviewer 复审均已通过当前范围要求；唯一剩余阻塞为真实备份、恢复数据、回滚版本串联没有执行。

真实 DR 原前置条件包括用户审批、`RUNTIME_CONTROL_ALLOW_REAL_DR=1`、current-code Linux-capable action origin、真实 rehearsal、已演练 `RUNTIME_CONTROL_REAL_DR_RESTORE_BACKUP_ID`，以及真实 DR 后四个带实际 URL 的 `HEALTH_OK` 证据。

## 决策

接受用户明确范围豁免：本次任务可以不执行真实 destructive DR，仅以当前非破坏性证据、只读候选验证、fail-fast 门禁测试、静态契约和独立 reviewer 复核作为放行依据。

## 影响

- 本次最终结论调整为 `PASS_WITH_SCOPE_WAIVER`。
- 真实 DR 链路不得被声明为已验证、已演练或已通过。
- `REAL_DR_APPROVAL_AND_TAG` 从当前阻塞项移入已豁免残余风险。
- 后续若要声明生产级 DR readiness，仍必须按原前置条件执行真实 DR 并记录证据。

## 风险和后续动作

- 残余风险：Linux 测试服上的真实备份、恢复数据、回滚版本串联尚未被本轮实际执行验证。
- 触发条件：上线前、生产级 DR readiness 声明前、或下一次涉及恢复/回滚实操能力验收前，必须补做真实 DR。
- 回滚/移除策略：若后续恢复原严格门禁，将 `task-state.json` 中的 waived prereq 恢复为 blocking prereq，并把 reviewer 结论从 `PASS_WITH_SCOPE_WAIVER` 改回真实 DR 证据门禁。

## 下游文档更新要求

- 更新 `doc/tasks/20260526-foolproof-ops-implementation/task.md`、`review-report.md`、`verification-report.md`、`test-report.md`、`test-plan.md`、`execution-log.md` 和 `task-state.json`。
- 前端镜像目录同步标注本次放行依赖主控任务的 scope waiver。
- 最终复核仍需确认无 fallback、无 mock 成功、无静默降级，且非破坏性验证命令通过。
