# Execution Log

## User Intent

- 用户要求继续，并授权将 `codex/replan-current-route-after-feedback` 融合进 `int_main`。
- 业务口径：老报工/老任务只影响已完成数量；剩余未完成部分必须按当前最新工艺路线重新找工序、工作站、产线和产能；老报工缺工作站不应触发“受保护任务未绑定工作站”。

## Scope

- Include: `origin/int_main` 融合、`origin/codex/replan-current-route-after-feedback` 融合、冲突解决验证、目标重排回归、必要任务记录与经验沉淀。
- Preserve: 并行残余改动通过独立基线提交保留；不使用 reset、checkout、rebase 或 force push。

## Preflight Evidence

- `git branch --show-current`: `int_main`。
- `git remote -v`: `origin https://github.com/jiazeyu1987/IntRuoyiAll.git`。
- Read gates: `docs/powershell-memory.md`, `docs/task-closeout-rules.md`, `docs/backend-development.md`, `docs/frontend-development.md`, `docs/database-rules.md`, `docs/e2e-rules.md`, `docs/powershell-encoding.md`, `docs/worktree-restrictions.md`, `docs/branch-runtime-ports.md`。
- Skill gates read: `project-experience-consolidation`, `task-closeout-cleanup`, and `task-closeout-cleanup/references/closeout-rules.md`。
- `scripts\preflight\branch-runtime-port-guard.ps1`: PASS, `int_main` frontend `8081`, backend `48081`。

## Baseline Commits

- `93ed7a841bc2e0f02965b88181045b79b7f4a1be` - `chore: baseline pre-replan merge workspace state`。
- `0e33c7f4bd0a2d450e45ba66813b656694623469` - `chore: baseline residual pre-replan merge workspace state`。
- `bdd31e608` - `chore: baseline residual post-replan merge workspace state`，用于保存融合后出现的并行残余改动；暂存前 `git diff --cached --name-status` 已确认未包含当前融合任务记录，`git diff --cached --check` PASS。
- `0f6ef01c3` - `chore: baseline residual closeout workspace state`，用于保存 closeout 前继续出现的并行后端残余改动。
- `bdea0ba9c` - `chore: baseline active-order e2e task evidence`，用于保存活跃订单池任务后续 E2E 证据文档。
- `da25efec0` - `chore: baseline frontline employee e2e evidence`，用于保存一线生产员工弹窗任务后续 E2E 证据。
- `b943b2b85` - `chore: baseline frontline employee experience log`，用于保存一线生产员工弹窗任务经验归档日志。
- `159a5ba95` - `chore: baseline submit round2 task records`，用于保存并行 round2 提交任务记录。
- `a87234f9a` - `chore: baseline concurrent ERP and PQC workspace state`，用于保存推送收尾前出现的并行 ERP/PQC 工作区状态。
- `3c8e900aa` - `chore: baseline concurrent task record updates`，用于保存并行任务记录的后续增量。
- `9f0a20319` - `chore: baseline concurrent process and PQC task state`，用于保存流程配置、PQC 测试数据和 EDHR 静态合同等并行状态。

## Merge Evidence

- `git fetch origin int_main codex/replan-current-route-after-feedback`: PASS。
- `origin/int_main` merge commit: `4c865c4b1`。
- `origin/codex/replan-current-route-after-feedback` actual delta checked with merge-base `b0b38693e6a7b04a3480e8efddcc10405fc48359`:
  - `MesProAutoScheduleServiceImpl.java`
  - `MesProAutoScheduleServiceImplTest.java`
  - `doc/tasks/20260806-replan-current-route-after-feedback/*`
  - `docs/backend-development.md`
  - `docs/experience-index.md`
- Actual replan delta did not overlap the then-unstaged residual file list, so branch merge proceeded without mixing parallel work.
- Replan branch merge commit: `d310b19e3`。

## BDD / TDD / Verification Evidence

- `BDD: replan remaining quantity from current route -> Given a work order has finished/feedback protected history with missing workstation, When replan preview runs after the route changes, Then completed quantity only reduces remaining quantity and the remaining work is scheduled from the latest current route workstation/line/capacity.`
- `GREEN: node IntRuoyiFronted/tests/e2e/mes-process-pool-team-leader-static.spec.js -> PASS`
- `GREEN: node tests/e2e/production-leader-active-order-pool-tab-static.spec.js -> PASS` in `IntRuoyiFronted`
- `GREEN: node IntRuoyiFronted/tests/e2e/team-leader-workbench-static.spec.cjs -> PASS`
- `GREEN: git diff --check -- IntRuoyiBackend IntRuoyiFronted -> PASS`
- `GREEN: scripts\preflight\branch-runtime-port-guard.ps1 -> PASS`
- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityFromCurrentRouteWhenFeedbackTaskHasNoWorkstation+replanPreview_shouldNotReserveFeedbackProtectedRouteProcessCapacityWithoutLineKey" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS`

## Experience Consolidation

- Updated existing `docs/powershell-memory.md` with `脏工作区功能分支融合增量门禁`。
- Updated `docs/experience-index.md` with keywords for merge-base actual branch delta and dirty worktree fusion.
- No new long-term experience document was created.

## Current Status

- Implementation and required verification passed.
- `task-closeout-cleanup` preview/apply: PASS, keep core task records, delete `<none>`, blocked `<none>`, warnings `<none>`。
- `git push origin int_main`: PASS，first push updated `origin/int_main` from `3fd9a221e` to `b943b2b85`。
- Additional push sync observed: `origin/int_main` reached `3c8e900aa` before this final record update.
- Remaining: push `9f0a20319` plus this final completion-record commit and verify branch is no longer ahead of `origin/int_main`; active parallel tasks may still leave task-outside dirty files, which are not part of this fusion scope.
