# Execution Log: EDHR-STATIC-011 reopened freeze lifecycle

## Bug

EDHR-STATIC-011 was reopened because work-order temporary freeze recovery can still rely on stale review history. A partial approach that counts `previous_work_order_temporary_frozen=true` on the first historical review can wrongly clear a manual freeze added before a later review, or wrongly restore a freeze that was already manually released before a later review.

## Expected

Work-order temporary freeze after disposing an eDHR nonconformance review must be recomputed from effective current freeze reasons: remaining pending/void review blockers, the current review's own void disposition, and the external/manual freeze state for the current overlapping review lifecycle. Historical freeze snapshots from closed, non-overlapping review lifecycles must not affect later lifecycles.

## Reproduction

Reproduction is a deterministic backend unit/static case, not E2E:

- Same-round A/B reviews: initial work order is not externally frozen; A opens and freezes the order; B opens while A is pending; closing A then B must finally unfreeze, and closing B then A must also finally unfreeze.
- Second-round manual freeze: A opens/closes with no external freeze; a user/process later manually freezes the work order; B opens and closes; B must preserve the manual freeze.
- Historical external freeze cleared: A opens with external freeze true and closes; the work order is later manually released; B opens with no external freeze and closes; B must not resurrect A's historical freeze.

## BDD

BDD: Same-round reviews only release review-owned freeze after all blockers close -> Given a work order starts unfrozen and two overlapping eDHR reviews freeze it / When the reviews are disposed in either order / Then the order remains frozen while any review blocker remains and becomes unfrozen after the final non-void review closes.

BDD: Later manual freeze survives later review disposal -> Given an earlier review lifecycle has completed and the work order is manually frozen before a later review starts / When the later review is disposed with a recoverable disposition / Then the manual freeze remains active.

BDD: Historical external freeze does not leak into a later lifecycle -> Given an earlier review captured an external freeze but that freeze was manually released before a later review starts / When the later review is disposed / Then the earlier lifecycle snapshot does not refreeze the work order.

## Root Cause

The dispose path restored `review.getPreviousWorkOrderTemporaryFrozen()` directly for every non-void disposition. In reopened or overlapping lifecycles that boolean could be stale historical state, because a later review could capture a freeze introduced by another still-pending review, and a later lifecycle could inherit or resurrect an earlier external freeze snapshot that was no longer effective.

The create path now captures whether the work order was externally frozen at the start of the current effective lifecycle, and the dispose path recomputes temporary freeze from current blockers: void disposition, remaining pending/void reviews, and the external/manual freeze state attached to the same overlapping freeze lifecycle.

## RED / GREEN Evidence

- RED: `node -e "<static baseline check against git show HEAD:MesProEdhrNonconformanceReviewServiceImpl.java>"` -> FAIL as expected with `RED: old dispose restores stale per-review boolean and lacks freeze lifecycle recomputation`.
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-011-freeze-lifecycle-static.spec.cjs` -> PASS, `mes-edhr-static-011-freeze-lifecycle-static: PASS`.
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrNonconformanceReviewApplicationScopeTest test "-Dsurefire.failIfNoSpecifiedTests=false"` -> PASS, `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`.
- REGRESSION: `git diff --check` -> PASS; only Git line-ending warnings were reported, with no whitespace errors.
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260913-edhr-static-011-reopened-freeze-lifecycle\bug-regression-evidence.md` -> PASS, `Bug regression evidence is valid.`
- Command syntax note: an earlier unquoted PowerShell Maven attempt parsed `-Dsurefire.failIfNoSpecifiedTests=false` incorrectly as `.failIfNoSpecifiedTests=false`; the corrected quoted command above is the recorded verification command.

## Verification

- Added regression coverage for overlapping same-round reviews, arbitrary close order, second-round manual freeze preservation, and historical external freeze release before a later lifecycle.
- Added static contract coverage proving dispose no longer directly restores a single review's historical freeze boolean and the mapper no longer selects the work-order historical first review as the external freeze source.
- Ran `task-closeout-cleanup --mode preview --json`; preview kept `task.md`, `execution-log.md`, `verification-report.md`, and `bug-regression-evidence.md`, had no delete candidates, and blocked with `Current worktree branch could not be resolved.` because this linked worktree is detached HEAD.
- Consolidated reusable closeout experience into `docs/worktree-memory.md` and indexed it in `docs/experience-index.md`.
- No E2E, service startup/restart, database write, Git commit, Git push, or remote operation was performed.

## int_main Integration

- User authorization: on 2026-09-13 the user authorized handling `E:\IntRuoyi` dirty state and integrating this task into `int_main`.
- Main baseline: `E:\IntRuoyi` had a pre-existing dirty state that was captured separately as `f773eab07 chore: baseline current int_main state`.
- Conflict resolution: `MesProEdhrNonconformanceReviewServiceImpl` was merged to preserve current QA electronic signature snapshot logic and add freeze lifecycle recomputation through `selectFreezeLifecycleByWorkOrderId`.
- Conflict resolution: `MesProEdhrNonconformanceReviewApplicationScopeTest` was merged to retain QA signature/password tests and add reopened freeze lifecycle scenarios.
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-011-freeze-lifecycle-static.spec.cjs` -> PASS, `mes-edhr-static-011-freeze-lifecycle-static: PASS`.
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrNonconformanceReviewApplicationScopeTest test "-Dsurefire.failIfNoSpecifiedTests=false"` from `E:\IntRuoyi\IntRuoyiBackend` -> PASS, `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`.
- REGRESSION: `git diff --check` from `E:\IntRuoyi` -> PASS; only Git line-ending warnings were reported, with no whitespace errors.
- No E2E, service startup/restart, database write, or server operation was performed.

## Blockers

Detached worktree closeout blocker was resolved by integrating from the main `E:\IntRuoyi` `int_main` worktree after explicit user authorization. No remaining blocker is known before final cleanup preview/apply and push verification.

## Closeout

- GREEN: `git fetch origin` -> PASS.
- GREEN: `git rebase origin/int_main` -> PASS, local `int_main` replayed cleanly onto the latest remote baseline.
- GREEN: post-rebase `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-011-freeze-lifecycle-static.spec.cjs` -> PASS.
- GREEN: post-rebase `git diff --check origin/int_main..HEAD` -> PASS.
- GREEN: post-rebase `mvn -pl yudao-module-mes -Dtest=MesProEdhrNonconformanceReviewApplicationScopeTest test "-Dsurefire.failIfNoSpecifiedTests=false"` -> PASS, `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`.
- GREEN: `task-closeout-cleanup --mode preview --json` from `E:\IntRuoyi` -> PASS, keep set contained `task.md`, `execution-log.md`, `verification-report.md`, and `bug-regression-evidence.md`; delete/blocked/warnings were empty.
- GREEN: `task-closeout-cleanup --mode apply --json` from `E:\IntRuoyi` -> PASS, deleted paths were empty and this is the main worktree (`linked=false`).
- Implementation commit on `int_main`: `b0bf6686e chore: submit current int_main state`.
- Task evidence commit on `int_main`: `aef23c05a docs: record int_main task evidence`.
- Closeout note: unrelated parallel DCC task docs under `doc/tasks/20260913-dcc-static-026-finalization-retry-event-key/` were dirty during EDHR closeout and intentionally excluded from EDHR commits.
