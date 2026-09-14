# Verification report

## Overall scope
The 20-item remediation remains in progress. This report records the completed validation for integrating remote int_main into int_qms, plus preserved partial fixes. It does not assert all 20 items are complete.

## Main integration
- Source: origin/int_main 70b53cf270fb989b48c3850e315e691011d69fe4, fetched and independently rechecked by ls-remote.
- Target: int_qms. Prior work preserved in b0f8119e5 and f76b1337d.
- Resolved 25 conflicted files; main runtime v7 adopted with QMS ports 8061/48061. Historical QMS records retained.
- DCC checkout/checkin/cancel-checkout contracts and project-file-template implementation are present.
- Incoming print error code 1080000189 preserved; local directory-cycle error moved to 1080000347 to avoid collision.
- Browser conflict resolution preserves main traceability navigation and loaded-state metadata while retaining request ownership checks. Both main detail action groups enforce the preserved ordinary-file action removal.

## Verification evidence
- Branch runtime port guard: PASS (int_qms/int_qms, frontend 8061, backend 48061).
- pnpm install --frozen-lockfile: PASS, no lockfile change.
- pnpm build:local: PASS (exit 0, Build successful).
- Frontend Node checks: 15 PASS (9 browser request-order, 2 directory-cycle, 2 ordinary-action visibility, 2 route-source checks).
- Maven -pl yudao-module-dcc,yudao-module-erp -am with targeted test selection: all 24 reactor modules compiled; 46 tests PASS (BPM bridge 7, BPM mutation guard 7, ERP runtime 6, directory administration 19, cycle 3, ordinary removed actions 4).
- Imported feature regression: DCC query 131 PASS, project templates 6 PASS, form-center callbacks 21 PASS, form-center idempotency 10 PASS.
- Workflow regression: 135 PASS after aligning two incoming obsolete transfer/sign missing-post expectations to unconditional action rejection; no-write assertions retained.
- Total targeted backend tests: 349 PASS. No tests skipped in the selected classes. Ancestor modules without selected tests use surefire.failIfNoSpecifiedTests=false; selected-class reports explicitly checked.
- No unresolved index entries. Resolution changes pass whitespace checks relative to source main; inherited upstream documentation whitespace is not represented as a local repair.
- No database writes, server deployment, service restart, or real browser E2E performed for this merge. Full standalone TypeScript check is not claimed; prior pre-merge attempt exhausted default heap and the enlarged attempt was interrupted. The merged frontend production build passed.

## Cleanup review
Preserve task.md, execution-log.md, verification-report.md and formal source tests. Remove only the task-owned one-use merge-resolution script after validation. Retain build output and the user-approved excluded .runtime directory. No worktree or service cleanup required. Separate task-closeout-cleanup executable is unavailable in this checkout; file ownership was reviewed directly.

## Delivery
Merge commit e2b7eac93 pushed successfully to origin/int_qms. Source main 70b53cf27 verified as an ancestor. New local object scan found no blobs over 100 MB. Overall 20-item Current Status remains in_progress; this integration does not close the full remediation task.
