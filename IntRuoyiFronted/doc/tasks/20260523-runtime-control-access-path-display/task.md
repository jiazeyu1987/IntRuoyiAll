# 20260523 Runtime Control Access Path Display

## Goal

Show explicit access-path text in the runtime control panel for `IntRuoyi 前端` and `Website 前端` across `Local / Test / Production`.

## Milestones

- [x] Check previous frontend task status and current repository state.
- [x] Record BDD scenarios and add a RED frontend contract check for access-path rendering.
- [x] Implement explicit access-path display in the runtime control page.
- [x] Re-run static verification and real-page verification.
- [x] Mark task completed and record final verification evidence.
- [x] Commit only current-task changes.

## Expected Verification

- `node tests/e2e/runtime-control-static.spec.js`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session runtime-control run-code --filename <task-script>`

## Current Status

Completed. The runtime control page now shows explicit `访问路径` text for `IntRuoyi 前端` and `Website 前端` in all three environments.

## Verification Evidence

- RED static contract reproduced with `node tests/e2e/runtime-control-static.spec.js`; it failed on missing `访问路径` label before the page change.
- GREEN static contract passed: `node tests/e2e/runtime-control-static.spec.js`.
- GREEN type check passed: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`.
- GREEN real-page verification passed with Playwright, confirming rendered access URLs:
  - `IntRuoyi 前端`: `http://127.0.0.1:8081/`, `http://172.30.30.58:8081/`, `http://172.30.30.57:8081/`
  - `Website 前端`: `http://127.0.0.1:4173/`, `http://172.30.30.58:8083/`, `http://172.30.30.57:8083/`

## Remaining Blockers

- No functional blocker remains for this access-path display follow-up.
- Scoped commit still depends on staging only this task's frontend files.

## Closeout

- `task-closeout-cleanup` preview/apply completed.
- The task-scoped Playwright helper script was deleted after verification passed.

## Notes

- Previous completed frontend task: `20260523-infra-runtime-control-panel`.
- This follow-up is frontend-only unless inspection proves the backend no longer returns access URLs.
