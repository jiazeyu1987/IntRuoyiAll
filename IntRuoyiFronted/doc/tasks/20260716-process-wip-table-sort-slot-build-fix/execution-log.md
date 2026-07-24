# Execution Log - 20260716 ProcessWipTable sort slot build fix

## BDD Scenarios

BDD: ProcessWipTable preserves sort slot props -> Given `UnifiedListTemplate` exposes `sortColumnAttrs` and `handleSortChange` through the `table` slot, When `ProcessWipTable` renders its own `table` slot, Then it must pass those props onward so table implementations can bind sortable columns without build-time unused slot variables.

## TDD / Verification Evidence

- RED: release `build-release` -> FAIL, operation `op-2026-07-15T170422234564100Z-32d8180b-c6c1-448f-8541-5bf8a9adf666` failed during `pnpm build:test`; Vite ESLint reported `src/views/mes/pro/scheduler-workbench/components/ProcessWipTable.vue:27:25` `sortColumnAttrs` defined but never used and `27:60` `handleTemplateSortChange` defined but never used.
- RED: `pnpm build:test` -> FAIL, same `ProcessWipTable.vue` `vue/no-unused-vars` errors reproduced in the frontend main workspace before code changes.
- GREEN: `ProcessWipTable.vue` slot props passthrough -> PASS, `sortColumnAttrs` and `handleSortChange` are passed to the wrapper `table` slot as `sort-column-attrs` and `handle-sort-change`; no ESLint disable or fallback introduced.
- GREEN: `pnpm build:test` -> PASS, Vite produced `dist-test`.
