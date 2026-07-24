# Task: Move production schedule calendar under scheduling management

## Goal

Move the production schedule calendar to the `排班管理` menu group so the
system exposes one unambiguous calendar entry, while keeping the existing page
implementation intact.

## Scope

- Add a new backend menu item under `排班管理`
- Grant the local admin role access to the new menu item
- Update canonical MySQL seed data accordingly
- Do not remove or refactor the underlying schedule calendar backend APIs

## Previous Task Check

- `doc/tasks/20260513-pro-schedule-calendar-followups/task.md` is blocked by
  missing non-MySQL validation environments and is left explicitly uncommitted.

## Milestones

- [x] M1: Previous backend task checked and blocked status acknowledged before new work.
- [x] M2: Backend task document created before production code changes.
- [x] M3: New menu data and local DB patch implemented.
- [x] M4: Verification passes and evidence updated.
- [x] M5: Task-scoped backend changes committed without unrelated blocked files.

## Expected Verification

- `排班管理` contains `生产排程日历`
- The local admin can see the new menu entry
- Canonical MySQL seed includes the same menu and role-menu association

## Current Status

Completed on `feature/auto-schedule-first-loop`. The unified `排班管理 ->
生产排程日历` menu entry is present in local MySQL and in canonical MySQL seed
data.
