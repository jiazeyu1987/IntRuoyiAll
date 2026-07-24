# Task: Fix schedule scope pagination limit

## Goal

Fix the frontend production scheduling pages so they no longer request work-order
pages with `pageSize > 200`, which currently triggers the backend validation
error `请求参数不正确:每页条数最大值为 200`.

## Scope

- Fix `src/views/mes/pro/task/index.vue`
- Fix `src/views/mes/pro/task/calendar/index.vue`
- Keep existing API contract unchanged
- Fetch the filtered work-order scope in multiple pages when the total exceeds
  the backend max page size

## Previous Task Check

- `doc/tasks/20260513-pro-schedule-calendar-v1/task.md` is completed.

## Milestones

- [x] P1: Previous frontend task checked complete before new work.
- [x] P2: Bug-fix task document created before production code changes.
- [x] P3: RED reproduction evidence recorded.
- [x] P4: Pagination-safe scope loading implemented in both pages.
- [x] P5: Focused verification passes.
- [x] P6: Task evidence updated and change committed.

## Expected Verification

- Opening production scheduling or schedule calendar no longer triggers
  `pageSize > 200`.
- Scope work-order ids still include all filtered rows, not just the first page.
- Existing auto-schedule preview/apply flow still works.

## Current Status

Completed on `feature/auto-schedule-first-loop`. Pagination-safe scope loading
is in place on both production scheduling pages and the scoped commit has been
created.
