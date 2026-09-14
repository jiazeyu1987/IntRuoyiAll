# Merge 1385 Dirty Worktree Verification Report

## Scope

User-authorized full dirty-state submission from `C:\Users\BJB110\.codex\worktrees\1385\IntRuoyi` into `int_main`.

## Expected Result

The authorized dirty batch is committed, integrated into `int_main`, verified with focused/static checks, and pushed if repository state permits.

## Results

- IN PROGRESS: backend blockers resolved; frontend and final integration verification pending.
- Latest backend verification: DCC 347, MES 32, server multipart 4 tests PASS. Python tooling regression 156 tests PASS; added executable discovery regression also confirms conflicting declarations fail.
- Manual rollback discovery now excludes explicitly declared rollback files while retaining strict forward metadata validation. No SQL file contents or database state changed.
- Preserved fusion commit: `8aebc6eed`; rebased onto main baseline `90adf7d6e` after baseline commits `36548b47c` and `90adf7d6e`.
- PASS: frontend API fail-closed contract (10 tests), upload-purpose static contract, backend direct-download runtime contract, and PowerShell direct-download runtime configuration tests.
- FAIL: Python tooling suite with task-local temporary directory: 154 passed, 1 failed. Release SQL discovery includes `20260822_mes_process_pool_active_order_completion_receipt_rollback.sql`, which declares `rollback-migration` and manual destructive rollback, not forward `release-migration` metadata. Do not relabel it as a forward migration merely to pass this test.
- BLOCKED: frontend check-in test cannot resolve `typescript`. `pnpm install --frozen-lockfile` was started, then interrupted after blockers were confirmed; dependency installation and type checking are not PASS.
- Previous Maven run: 347 DCC tests, 4 failures in workflow tests. The four obsolete route fixtures/assertions were corrected; the new Maven run was interrupted without a completed result. MES/server tests remain unverified in this continuation.
- Uncommitted corrections: Collection import, four-stage workflow test fixtures/assertions, local-profile static assertion, Docker extra-argument assertion.
- E2E, deployment, database writes, and service restarts were not performed.

## Risks And Blockers

- Source worktree starts on detached HEAD.
- Dirty batch spans multiple domains and historical task scopes.
- Latest main observation: `7393f6731`; unrelated edits in EDHR static-021 task records, DCC documentation, and closeout rules, plus a new DCC handoff document. These were left untouched. Re-read current rules and re-evaluate integration against current main before resuming.
- Integration worktree and slot 24 remain allocated; source detached worktree is preserved. Cleanup preview/apply is not eligible while required verification is incomplete.
