# Task: EDHR-STATIC-011 reopened freeze lifecycle

## Task Goal

Fix reopened EDHR-STATIC-011 so eDHR nonconformance review disposal recalculates work-order temporary freeze state from the current effective freeze lifecycle instead of restoring a stale per-review boolean.

## Milestones

- [x] Read required project rules, closeout rules, backend/database/PowerShell/worktree rules, bug-regression-fix-loop skill, and reopened audit evidence.
- [x] Record BDD and RED evidence for reopened freeze lifecycle scenarios.
- [x] Implement the minimal backend fix in the eDHR nonconformance review scope.
- [x] Run targeted non-E2E static/unit verification.
- [x] Integrate the fix into `int_main` after explicit user authorization.
- [x] Complete closeout checks and mark task completed.

## Expected Verification

- Targeted regression/unit tests cover same-round multi-review arbitrary close order, second-round manual freeze before new review, and historical external freeze cleared before a later review.
- Static check confirms freeze recomputation does not select the work-order historical first review as an external freeze source.
- `git diff --check` passes.
- No E2E, service start/restart, or database write is performed for this task.
- Git commit/push is permitted only for the `int_main` integration and closeout after the user's 2026-09-13 authorization.

## Current Status

completed

## Design Constraints Check

- Preserve no-fallback policy: do not add compatibility shims, swallowed exceptions, mock success, or default success paths.
- Keep the change inside EDHR-STATIC-011-required backend review lifecycle code and targeted tests.
- Work-order changes remain inside existing transactional and `selectByIdForUpdate` lock boundaries.
- Distinguish review-introduced freeze from external/manual freeze lifecycle; do not rely on the work-order history first review row.
- Respect the remaining restriction: static code logic/unit verification only; no Playwright/E2E, service startup, or database writes.
- Preserve current `int_main` QA electronic signature snapshot logic while integrating freeze lifecycle recomputation.

## Cleanup Keep

- doc/tasks/20260913-edhr-static-011-reopened-freeze-lifecycle/bug-regression-evidence.md
