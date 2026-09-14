# DCC-STATIC-020 Approval Route Effective Time

## Task Goal

Fix DCC-STATIC-020 so DCC approval route save, preview, and runtime route selection honor the route `effectiveTime`: a future route must not replace the currently effective route until its effective time has arrived.

## Scope

- Only handle DCC-STATIC-020 from `docs/bugs/20260912-dcc-90-step-static-audit.md`.
- Use the current clean Codex worktree; do not inherit uncommitted changes from `E:\IntRuoyi`.
- Do not execute E2E, start/restart services, write databases, or operate remote systems.
- 2026-09-13 user follow-up authorized local integration into `int_main`; Git push remains outside the current authorization.

## Milestones

- [x] Read required project rules and bug regression skill contract.
- [x] Record BDD scenarios and RED target before production changes.
- [x] Add a failing regression test for future route effective-time selection.
- [x] Implement the minimal route selection/save fix.
- [x] Run targeted unit/static verification.
- [x] Record final evidence and closeout status.

## Expected Verification

- Targeted Maven test for `DccApprovalRouteAdminServiceImplTest`.
- Targeted static or unit contract for `DccCategoryApprovalRouteMapper` effective-time selection.
- No E2E and no service/database runtime operations.

## Current Status

ready_for_closeout

Implementation, targeted verification, cleanup apply, and local `int_main` integration are complete. Git push is not performed because the current authorization covers local fusion into `int_main`, not remote publication.

## Design Constraints Check

- No fallback or silent downgrade.
- Future routes may be stored, but route selection must ignore active routes whose `effectiveTime` is later than the selection time.
- Saving a future route must not deactivate the currently effective route.
- Saving a currently effective route may replace older already-effective active routes in the same category.
- Route preview and runtime submission must use the same effective-time-aware selection rule.
- Shared bug file exists only as an untracked file in `E:\IntRuoyi`; this clean worktree does not include it, so it is not edited unless it appears in this worktree.
