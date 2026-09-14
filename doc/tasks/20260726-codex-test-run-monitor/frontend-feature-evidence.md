# Frontend Feature Evidence

## Feature Goal

- Add a `运行监控` tab under `系统管理 > 测试管理`.
- Display current running task count, method-item progress, target-item progress, and failure reasons.

## Non-Goals

- Do not mock Runner progress.
- Do not infer successful progress from frontend timers.
- Do not redesign unrelated test management tables.

## Acceptance

- Acceptance: user can open `运行监控` from the test management page.
- Acceptance: method items render pending, running, and success states.
- Acceptance: target items render running, success, and failed states.
- Acceptance: clicking a failed target shows the failure reason.

## UI Entry Points

- `IntRuoyiFronted/src/views/system/codex-test-management/index.vue`
- `IntRuoyiFronted/src/api/system/codexTestManagement/index.ts`

## API Contracts

- `GET /system/codex-test-execution/monitor` returns execution list with cases and checkpoint results.
- Case result exposes `progressPhase`, `currentMethodSort`, `currentCheckpointSort`, and `progressMessage`.

## BDD Scenarios

- BDD: Monitor tab -> Given running Codex test executions exist / When the user opens `运行监控` / Then the page displays running count and per-task status.
- BDD: Failed target reason -> Given a target item failed / When the user clicks the red target item / Then the failure reason dialog displays expected, actual, and mismatch text.

## RED / GREEN

- RED: `node IntRuoyiFronted/tests/e2e/system-codex-test-run-monitor-static.spec.js` failed before monitor tab/API/progress contract existed.
- GREEN: `node IntRuoyiFronted/tests/e2e/system-codex-test-run-monitor-static.spec.js` passed.
- GREEN: `node IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js` passed.
- GREEN: `pnpm ts:check` passed after restoring missing local dependency with `pnpm install --frozen-lockfile`.

## Verification

- Verification: focused static contract passed.
- Verification: original test management static contract passed.
- Verification: Vue relaxed type check passed.

## States Covered

- Loading: `v-loading="monitorLoading"`.
- Empty: `暂无运行中的测试任务`.
- Error: visible `el-alert` and message error.
- Success/Running/Failed: green/yellow/red CSS classes.
- Accessibility: failed target is a real button and opens a dialog.

## Blockers

- Blockers: real browser E2E was not run because no confirmed local runtime was validated after code changes.