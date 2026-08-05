# AC-M18 更新生产订单进度修复执行日志

## User Intent

- 用户要求对 AC-M18“更新生产订单进度”的系统代码不符合项进行修复。
- 当前已知缺口：班组长确认分配只更新工序完成池表，未同步正式排产工单工序进度和工单汇总；正式进度同步存在超目标被截断为 100% 而非 fail-fast 的行为。

## Preconditions

- 已读取 `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/backend-development.md`。
- 已读取 `bug-regression-fix-loop`、`backend-api-delivery` 技能及其 evidence contract。
- `docs/experience-index.md` 存在；本任务命中 Maven `-D` 参数引号、Maven Reactor 与 Windows Maven 增量卡住门禁，验证命令将按对应规则执行并记录真实结果。

## Milestone Log

- BDD: Confirmed allocation updates formal schedule progress -> Given a team leader confirmed allocation for a schedule order process with target quantity, When the allocation is applied, Then the process-level reported quantity and order-level summary are updated from the process target while ERP product quantity remains unchanged.
- BDD: Over-target schedule progress is blocked -> Given a schedule order process target quantity, When feedback or confirmed allocation would make reported quantity exceed the target, Then the update fails fast instead of capping progress to 100%.
- BDD: Concurrent remaining target consumption is blocked -> Given another allocation has consumed the remaining target before the current confirmation applies, When the current allocation is applied, Then the service rejects the update and does not over-report formal schedule progress.


## Implementation Notes

- Updated `MesTeamLeaderOrderProcessCompletionService` to synchronize formal schedule-order process progress and order summary after confirmed allocations.
- Updated `MesProScheduleOrderServiceImpl` to aggregate progress by enabled process planned target quantities.
- Changed over-target formal progress sync to throw `PRO_FEEDBACK_QUANTITY_EXCEED` before progress writes.
- Changed missing production quantity factor from implicit `1.000000` default to `PRO_ROUTE_FLOW_CONFIG_PRODUCTION_QUANTITY_FACTOR_INVALID`.

## Verification Evidence

- RED: target Maven command timed out after 124s; current-task PID 35688 was stopped.
- BLOCKER: unrelated Maven PIDs continued using `E:\IntRuoyi\IntRuoyiBackend` target, so GREEN/REGRESSION was not run to avoid corrupting shared Maven target output.
- STATIC: `git diff --check` on edited files passed with only line-ending warning.
- WORKTREE: created detached sparse verification worktree `D:\IntRuoyiWorktree\20260805-ac-m18-verify-sparse` from `ca181206a6ba9b247693cd4db8270d927ab71f82`; no frontend/backend service was started and no port slot was registered.
- WORKTREE: applied AC-M18 diff plus compile prerequisites already present in the main workspace (`MesQaInspectionRegulationServiceImpl`, `MesProBatchRecordCellLinkRuleDO`, `MesPqcProcessInspectionAggregationServiceTest`, `MesPqcInspectionTaskMapper`) so Maven could reach the AC-M18 test set without touching the shared main target.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesProScheduleOrderProgressServiceTest,MesProScheduleOrderServiceImplTest,MesProScheduleOrderFourRiskContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS in isolated worktree; 90 tests run, 0 failures, 0 errors, 0 skipped.
- STATIC: `git diff --check -- <AC-M18 edited files>` -> PASS with only expected LF/CRLF checkout warnings.
- VALIDATOR: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260805-ac-m18-progress-repair/bug-regression-evidence.md` -> PASS.
- VALIDATOR: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260805-ac-m18-progress-repair/backend-api-evidence.md` -> PASS.
- CLEANUP: `git worktree remove --force D:\IntRuoyiWorktree\20260805-ac-m18-verify-sparse` -> PASS; `Test-Path=False`.
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260805-ac-m18-progress-repair --mode preview` -> PASS; delete set only `backend-api-evidence.md` and `bug-regression-evidence.md`.
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260805-ac-m18-progress-repair --mode apply` -> PASS; temporary evidence files deleted after validator PASS was copied into retained reports.
- EXPERIENCE: updated existing `docs/worktree-memory.md` with sparse isolated Maven worktree and compile-baseline verification guidance; no new long-term experience document was created.
- BASELINE: `git status --short --branch` before closeout showed 31 non-AC-M18 dirty/untracked files; secret-pattern scan returned no matches and largest dirty file was 108,759 bytes.
- BASELINE: `git commit -m "chore: preserve concurrent workspace before AC-M18 closeout"` -> `057fba5b9`; staged file list contained only the previously dirty non-AC-M18 workspace files, preserving them before the AC-M18 closeout record.
- CONCURRENCY: during final closeout, concurrent tasks continued writing non-AC-M18 files and remote/local HEAD moved through AC-M10 closeout commits; AC-M18 task files were kept unstaged while each new non-task dirty set was preserved separately.
- LOCK: recovered stale `.git/index.lock` only after confirming it was 0 bytes, older than 60 seconds, and no `git` process was active; exact lock path was removed and `git status --short --branch` recovered.
- BASELINE: additional non-AC-M18 baseline commits were created before AC-M18 closeout docs: `f19a29f0e`, `5e0acef75`, `546915887`, `44e4d5fc4`, `e8e6ab26`, `2151a27b9`, `35133db9f`, `994930ed1`, `dd336f987`.
- BASELINE: `e8e6ab26` preserved concurrent role-matrix evidence after `git diff --cached --check` reported a non-task trailing blank-line issue; the unrelated file was not rewritten in the AC-M18 task.
- CLOSEOUT: after `dd336f987`, `git diff --name-status -- . :(exclude)doc/tasks/20260805-ac-m18-progress-repair/...` returned no non-AC-M18 dirty files; AC-M18 implementation and verification files were already committed in prior shared commits (`ba81bdfe3`, `fdf1b49d8`), and this closeout updates task status to `completed`.
