# 20260523 IntRuoyi Runtime Control Panel Frontend

## Goal

Add an infra runtime control panel page to the IntRuoyi admin UI for viewing and restarting IntRuoyi and Website runtimes across local, test, and production environments.

## Milestones

- [x] Identify previous task state and current repository status.
- [x] Add frontend RED contract test for API wiring, component rows, and production guard fields.
- [x] Implement API client for runtime-control overview, restart, and operation history.
- [x] Implement `基础设施 / 运行控制台` page using the IntPP dense operations-console style.
- [x] Add production restart reason and exact `PROD` confirmation flow.
- [x] Add reconnect-aware polling and visible error state.
- [x] Run frontend static contract check and type check.
- [x] Complete real-path Playwright verification after local backend runtime ownership blocker is removed.
- [x] Mark task completed and record final verification evidence.
- [x] Commit only current-task changes.

## Expected Verification

- `node tests/e2e/runtime-control-static.spec.js`
- `pnpm ts:check`
- Playwright verification through `http://localhost:8081` with the test tenant when local runtime prerequisites are available.

## Current Status

Completed. The current-source local runtime now recovers through the backend/full restart path, and the live runtime-control page verifies successfully against the freshly started local backend on `48081`.

## Verification Evidence

- Frontend static contract passed: `node tests/e2e/runtime-control-static.spec.js`.
- Frontend type check passed with `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`.
- Playwright login path passed through `http://localhost:8081` with the visible test tenant defaults.
- Runtime-control route is available at `/infra/monitors/runtime-control` after menu creation through the system menu API.
- Real Playwright verification passed through `http://127.0.0.1:8081/infra/monitors/runtime-control`; the page loaded the runtime matrix from `http://127.0.0.1:48081/admin-api/infra/runtime-control/overview`, and the production Website restart dialog blocked empty reason / missing `PROD` without sending a restart request.
- Verification screenshot saved to `D:\ProjectPackage\Int\IntRuoyi\output\playwright\runtime-control-panel-live.png`.

## Remaining Blockers

- `pnpm ts:check` with default Node heap exhausted memory.
- No functional blocker remains for the runtime-control task itself.
- Scoped commit still depends on staging only runtime-control files and excluding unrelated dirty work in both repositories.

## Notes

- Previous completed frontend task observed before this implementation: `20260523-clean-outdated-unfinished-task-dirs-frontend`.
- Frontend repository already had unrelated untracked task directories before current task edits; they are not part of this task.
- Real-page verification used a task-scoped helper script during closeout, and that helper was deleted by `task-closeout-cleanup` after verification passed.
- Task-only frontend commit: `8e414b8c` (`任务: 新增运行控制台页面`).
