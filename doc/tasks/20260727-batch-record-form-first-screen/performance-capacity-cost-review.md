# Performance Capacity Cost Evidence

## Scope

- Critical path: entering `/mes/pro/feedback/edhr-execution/form` for eDHR batch record form filling.
- Code path: `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`.
- Non-goal: backend API latency, database query tuning, archive generation throughput, or tenant data changes.

## Targets

- No numeric SLA was provided by the user or project docs for first screen time.
- Evidence-backed target for this task: reduce first screen blocking requests by removing archive, tracking timeline, and signature summary from the `loading` critical path.

## Capacity

- No new capacity assumption introduced.
- First screen now waits only for execution detail and draft-only cell-link prefill when applicable.
- Secondary requests run after `requestAnimationFrame`, preventing archive, tracking, and signature latency from blocking initial form render.

## Cost

- No new vendor, infrastructure, browser dependency, or recurring cost introduced.
- Network request count is unchanged for complete page use; request timing is shifted out of the critical path.

## Quotas

- No API quota or rate-limit assumption changed.
- Stale request guards prevent old route loads from writing into current page state.

## Verification

- `node tests/e2e/edhr-execution-first-screen-defer-static.spec.js -> PASS`
- `pnpm ts:check -> PASS`
- `git diff --check -- <task-owned files> -> PASS` with CRLF warnings only.

## Findings

- PASS: `loadExecution` no longer awaits `loadLatestArchive`, `loadTrackingAndSignatures`, `getLatestEdhrExecutionArchive`, `getEdhrTrackingTimeline`, or `getEdhrExecutionSignaturePage`.
- PASS: deferred secondary loader schedules via `requestAnimationFrame`.
- PASS: secondary tracking and signature calls are parallelized with `Promise.allSettled`.
- PASS: secondary failures continue to surface through visible error state.

## Blockers

- Real timing measurement is BLOCKED until a confirmed local runtime, test account, tenant fixture, and representative execution record are available.
