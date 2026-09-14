# EDHR-STATIC-020 Loss Release

## Task Goal

Fix EDHR-STATIC-020 only: normal production reports with loss or scrap must not be blocked by comparing total feedback quantity directly to qualified allocation quantity. Qualified allocation plus formally recorded loss must explain the source feedback quantity; unconfirmed, undisposed, or unsupported loss must still block release evidence generation.

## Milestones

- [x] Read required repository and domain rules.
- [x] Record BDD scenarios before implementation.
- [x] Add targeted regression coverage that fails against the current quantity contract.
- [x] Implement the smallest code change in the loss release writer.
- [x] Run targeted non-E2E verification and static contract checks.
- [x] Rebase the task branch onto current `int_main` and re-run targeted verification.
- [x] Push the task branch to `origin`.
- [x] Fast-forward the implementation into `int_main`.
- [x] Clean task-owned temporary evidence and release the task worktree slot.
- [x] Push final task closeout records to `origin/int_main`.

## Expected Verification

- Targeted Maven/JUnit or static contract RED proves current logic rejects `feedbackQuantity=qualifiedQuantity+lossQuantity` when allocation equals qualified quantity.
- Targeted GREEN proves `allocatedQuantity + formalLossQuantity == feedbackQuantity` passes, while missing loss details, unconfirmed loss evidence, or mismatched totals still fail.
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260914-edhr-static-020-loss-release\bug-regression-evidence.md` passes before cleanup deletes the temporary evidence file.
- `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` passes before commit, merge, and push.
- No Playwright/E2E, database writes, service start/stop/restart, server operation, or release operation.

## Design Constraints Check

- Scope remains limited to EDHR-STATIC-020.
- Do not edit shared defect summary documents.
- Preserve no-fallback and fail-fast behavior.
- Do not treat loss as qualified allocation.
- Do not relax formal source, review, signature, or loss-detail validation.
- Use static code logic checks and targeted non-E2E tests only.
- Git commit, branch push, and `int_main` fusion were later authorized by the user.

## Current Status

completed - EDHR-STATIC-020 implementation is merged into local and remote `int_main` ancestry as `53f969d59`; retained task closeout records were pushed in commit `92cf1d533`; task-owned temporary evidence was removed, the residual empty worktree directory was deleted, and runtime slot 22 was marked inactive.
