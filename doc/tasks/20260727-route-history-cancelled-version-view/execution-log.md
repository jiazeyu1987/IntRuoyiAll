# Execution Log

## User Intent

- 用户报告：查看老的工艺流程版本时提示“工艺路线候选版本未满足发布条件，routeVersionId=262，status=CANCELLED”。
- 预期：已取消历史版本可只读查看自身冻结快照，不能编辑、提交或发布。

## Preflight

- 已读取 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/experience-index.md`。
- 已读取 `docs/worktree-restrictions.md`、`docs/powershell-memory.md`、`docs/powershell-encoding.md`。
- 已读取 `bug-regression-fix-loop` 技能及 `references/bug-contract.md`。
- 当前 `E:\IntRuoyi` 的 `int_main` 存在其他任务未提交改动；本任务不提交、不覆盖这些改动，改用隔离 worktree。
- Worktree 预检通过：目标路径位于 `D:\IntRuoyiWorktree\`，分支为 `codex/20260727-route-history-cancelled-version-view`。

## BDD

BDD: 查看已取消工艺路线历史版本 -> Given 路线版本状态为 CANCELLED 且存在完整冻结快照 / When 用户在版本工作区点击查看 / Then 页面从该版本快照显示关系图和配置且所有写操作保持禁用

BDD: 已取消版本仍禁止写入 -> Given 路线版本状态为 CANCELLED / When 客户端尝试保存关系图或流程配置 / Then 后端继续返回版本状态不允许写入且不修改当前生效数据

## Evidence

- 前端 `index.vue` 的 `canViewRouteVersion` 对所有非 `DRAFT` 版本显示“查看”。
- 后端 `MesProRouteProcessFlowServiceImpl` 与 `MesProRouteFlowConfigServiceImpl` 的读取状态集合未包含 `CANCELLED`。
- 精确异常文本来自 `ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE`。

## RED

RED: mvn -pl yudao-module-mes -am "-Dtest=MesProRouteProcessFlowServiceImplTest,MesProRouteFlowConfigServiceImplTest,MesProRouteScheduleConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected reason: new regression tests reproduce `CANCELLED` / `REJECTED` / `SUPERSEDED` read paths as blocked by `PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE`, and candidate schedule snapshots return empty rows for readonly statuses.

## GREEN

GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProRouteProcessFlowServiceImplTest,MesProRouteFlowConfigServiceImplTest,MesProRouteScheduleConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 96 tests, 0 failures, 0 errors.

GREEN: node --check tests/e2e/mes-route-cancelled-version-view-static.spec.js -> PASS.

GREEN: node tests/e2e/mes-route-cancelled-version-view-static.spec.js -> PASS, `PASS: mes route cancelled version uses readonly historical viewer`.

## Implementation

- `MesProRouteProcessFlowServiceImpl` now treats `REJECTED`, `CANCELLED`, and `SUPERSEDED` as readonly snapshot route versions for graph reads.
- `MesProRouteFlowConfigServiceImpl` now allows `REJECTED` and `CANCELLED` closed candidates through the readonly config snapshot path.
- `MesProRouteScheduleConfigServiceImpl` now reads candidate schedule snapshots for readonly candidate statuses instead of querying nonexistent published config rows.
- Regression tests prove `CANCELLED` still fails writes for graph, flow config, and schedule config.

## Experience Consolidation

GREEN: experience-preflight -> PASS, reused existing `docs/backend-development.md` instead of creating a new long-term document.

- Added `docs/backend-development.md#历史关闭候选版本只读快照边界`.
- Added `docs/experience-index.md` keywords for `CANCELLED` / `REJECTED` / `SUPERSEDED` historical route-version readonly snapshot failures.
- Preserved `bug-regression-evidence.md` through `task.md` `Cleanup Keep`.

## Commit And Closeout

GREEN: branch-runtime-port-guard -> PASS, registered current worktree as `int_main slot=8`, frontend `8089`, backend `48089`.

GREEN: implementation-commit -> PASS, `3d809a8e fix: allow readonly cancelled route versions`.

BLOCKER: task-closeout-cleanup preview -> Main worktree is dirty and cannot receive ff-only merge: `E:\IntRuoyi`.

- Preview keep: `task.md`, `execution-log.md`, `verification-report.md`, `bug-regression-evidence.md`.
- Preview delete: none.
- Impact: implementation is committed on the task branch, but cleanup apply / ff-only merge / worktree removal cannot run safely until unrelated main-worktree changes are resolved.

## Current Status

- Milestones 1-4 complete.
- Current status: ready_for_closeout; implementation branch is ready to push, closeout apply is blocked by dirty main worktree.
