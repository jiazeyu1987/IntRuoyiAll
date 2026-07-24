# Task: Keep a single schedule calendar entry

## Goal

Enforce a single user-facing entry for the production schedule calendar:
`排班管理 -> 生产排程日历`.

## Scope

- Remove the production scheduling page shortcut button for `排程日历`
- Remove the old hidden route `/mes/pro/task/calendar`
- Keep the unified visible menu route under scheduling management

## Previous Task Check

- `doc/tasks/20260514-pro-schedule-calendar-menu-entry/task.md` is completed.

## Milestones

- [x] S1: Previous frontend task checked complete before new work.
- [x] S2: Frontend task document created before production code changes.
- [x] S3: Old alternate entry points removed.
- [x] S4: Focused verification passes.
- [x] S5: Evidence updated and change committed.

## Expected Verification

- Users only enter the page from `排班管理 -> 生产排程日历`
- The production scheduling page no longer exposes a second shortcut button
- The hidden old route is no longer present in `remaining.ts`

## Current Status

Completed on `feature/auto-schedule-first-loop`. The only user-facing entry is
now `排班管理 -> 生产排程日历`.
