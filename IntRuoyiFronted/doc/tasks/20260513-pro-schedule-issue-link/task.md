# Task: Make schedule issue work orders clickable

## Goal

Make the work-order code in the schedule issue dialog clickable and navigate to
the corresponding production work-order page.

## Scope

- Update the schedule calendar issue dialog so the `工单` column renders a
  clickable entry when a work order id/code exists.
- Navigate to the production work-order page and carry the target work-order
  context in route query params.
- Update the production work-order page so it can consume the incoming route
  query, filter to the target code, and open the corresponding detail dialog.

## Previous Task Check

- `doc/tasks/20260513-pro-schedule-pagination-fix/task.md` is completed.

## Milestones

- [x] L1: Previous frontend task checked complete before new work.
- [x] L2: Task document created before production code changes.
- [x] L3: RED behavior recorded for the non-clickable issue work-order cell.
- [x] L4: Calendar dialog and work-order page route handoff implemented.
- [x] L5: Focused verification passes.
- [x] L6: Evidence updated and change committed.

## Expected Verification

- The issue dialog `工单` column is clickable when work-order data exists.
- Clicking the code navigates to the production work-order page.
- The target work order is filtered and its detail dialog opens automatically.

## Current Status

Completed on `feature/auto-schedule-first-loop`. The issue dialog work-order
cell now routes to the production work-order page and opens the corresponding
detail dialog.
