# Bug Regression Evidence: EDHR-STATIC-011 reopened freeze lifecycle

## Bug

EDHR-STATIC-011 was reopened because eDHR nonconformance review disposal could restore a stale `previousWorkOrderTemporaryFrozen` boolean from one review row instead of recalculating the work order's effective temporary freeze state from the current freeze lifecycle.

## Expected

Disposing a non-void review must unfreeze the work order only when no other pending/void review blocker remains and the current overlapping review lifecycle did not begin with an external/manual freeze that is still represented by that lifecycle. Historical external freeze snapshots from closed, non-overlapping lifecycles must not affect later reviews.

## Reproduction

- Same-round A/B reviews: closing either review first keeps the work order frozen until the remaining review closes; after the final non-void close, the review-owned freeze clears.
- Second-round manual freeze: a manual freeze added after an earlier lifecycle and before a later review remains after that later review closes.
- Historical external freeze cleared: an external freeze captured by an earlier lifecycle does not refreeze a later review lifecycle after it has been manually released.

## Root Cause

The old dispose path restored `review.getPreviousWorkOrderTemporaryFrozen()` directly for non-void dispositions. That single persisted snapshot cannot distinguish a true external/manual freeze at the start of the current lifecycle from a freeze introduced by another overlapping review, and it can also leak a closed historical lifecycle into a later reopened review.

## Fix

- Capture external freeze state at review creation by checking whether the current work-order freeze is already caused by an effective review blocker.
- Recompute work-order temporary freeze at disposal from pending/void blockers plus the external/manual freeze state attached to the same overlapping freeze lifecycle.
- Add lifecycle-ordered mapper access and targeted unit/static tests for the reopened EDHR-STATIC-011 scenarios.

## RED / GREEN Evidence

- RED: `node -e "<static baseline check against git show HEAD:MesProEdhrNonconformanceReviewServiceImpl.java>"` -> FAIL as expected with `RED: old dispose restores stale per-review boolean and lacks freeze lifecycle recomputation`.
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-011-freeze-lifecycle-static.spec.cjs` -> PASS.
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrNonconformanceReviewApplicationScopeTest test "-Dsurefire.failIfNoSpecifiedTests=false"` -> PASS; `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`.
- GREEN: `git diff --check` -> PASS with no whitespace errors.

## Verification

Targeted regression coverage now includes same-round multi-review close order, second-round manual freeze preservation, historical external freeze isolation, and a static contract that prevents returning to the work-order historical first-review snapshot approach.

## Blockers

Final completed closeout is blocked by a rule conflict: the current task restriction prohibits Git commit/push, while the project closeout rules require commit/push before marking the task completed.

## Risk And Regression Scope

The change is limited to `MesProEdhrNonconformanceReviewServiceImpl`, the direct review mapper, and targeted regression tests under `yudao-module-mes`.
