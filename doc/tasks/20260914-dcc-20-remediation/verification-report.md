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

## Uncommitted remediation verification — 2026-09-15

Current branch rechecked as int_qms. No commit/push, E2E, runtime restart or production database write performed in this continuation. Merge evidence above applies to the integration baseline, not the following uncommitted fixes.

- DCC-MAIN-14: workflow now applies project EDIT/OWNER and per-target name visibility before relation binding in all three upload paths. Persistence rejects missing source or source/target project mismatch and same-Master targets. Workflow/relation group passed 152 tests before the additional product/approval tests. Related-upload static contract passed.
- DCC-MAIN-15: new `/dcc/controlled-files/project-product` endpoint reuses the save resolver and validates current project access. Upload displays the resolved product number and refuses submission while lookup is unresolved/failed; old project responses cannot overwrite current state. Four frontend contracts/behaviors passed, and bound-product/missing-product backend tests passed. No-product-binding policy remains unresolved; the existing PROJECT_CODE source is explicitly labeled and this item is not closed.
- DCC-MAIN-19: distinct APPROVAL_PDF purpose; upload validates current task assignee, frozen roster membership, final stage, file/category, exact length-framed task/file session scope and content detail authorization. Final binding verifies scope again and resolves only APPROVAL_PDF. Approval actors gain temporary status/cleanup access with existing uploader/tenant/unbound restrictions, while ordinary upload still needs submit permission. Purpose-based size limits retain a formal configuration option.
- Latest targeted backend group: DccControlledFileWorkflowServiceImplTest 146 PASS; DccControlledFileUploadApiTest 33 PASS; DccUploadEndpointHttpContractTest 3 PASS; DccFrozenApprovalSignaturesTest 4 PASS. Actual DccControlledFileFinalizationServiceImplTest separately rerun: 41 PASS. An earlier command misspelled its class and therefore did not run that class; only the explicit later report proves its pass.
- Frontend regression: `node --test` for dcc-approval-pdf-task, dcc-project-product-resolution, dcc-related-file-pagination, dcc-checkin-cleanup, dcc-checkin-upload-state, dcc-template-invalid-items, dcc-directory-name-content and dcc-final-approval-defaults: 17 PASS. Changed upload/detail/API/policy files pass ESLint. These are local behavior/contracts, not real-page E2E.
- DCC-MAIN-20: session cleanup lost-claim RED reproduced false successful cleanup. Added current-state verification before accepting a lost claim; cleanup 27, finalization 41 and frozen-signature policy 4 PASS.
- DCC-MAIN-16 partial: version history previously exposed a different version's source hash despite absent content access and omitted actionProjection. Added per-version content redaction and action projection; full query regression 134 PASS. Top-level name-only version history, direct directory content grants, per-version visibility filtering and pending/retired-action policy audit remain required.
- `git diff --check` PASS; Git reports only CRLF conversion warnings. No new frontend full build performed for this continuation yet.

The full 20-item goal remains in_progress. Remaining scope includes 03/04/05/11 checkout/version entry design, 15 unbound product policy, 16 per-version content/action projection, 18 remaining callbacks, 20 rejected reupload and cleanup races, and complete cross-entry static closure for the implemented items. No partial test group is evidence that all 20 issues are closed.

## Follow-up current reads and version permissions — 2026-09-15

- 16: name-only browser rows now include the permitted version chain, each version is independently checked for visibility/content access, and explicit directory content access permits detail/preview within assigned scope. Download remains independently authorized. Pending approval action projection retains an authorized PREVIEW capability while mutation locking remains active. Query 138 + project 37 PASS at this stage.
- 05: checkin/cancel lock Master then file before the checkout record; working submission and version-chain calculations use locking current reads. Submit idempotency uses FOR UPDATE and its write has explicit tenant, identity, requester, WORKING, not-checked-out and not-deleted predicates. Existing query/workflow group passed 286; subsequent workflow group 147 PASS. New DccWorkingSubmissionConditionTest runs the real MyBatis SQL in isolated H2, checks all six eligibility cases and races checkout against submission in two sessions; exactly one wins. Condition test plus DccCheckoutCheckinLifecycleTest: 5 PASS. This is not live MySQL or real-page E2E evidence.
- 04: metadata-only CAD retry compares actual source-service content hashes of distinct PDF identities; equal-content copy replays the existing version and altered content is rejected with no insert/bind/copy. Query regression increased to 141 PASS.
- No frontend source change or runtime restart in this follow-up. No commit/push or production database write. New code test fixture is task-owned and persists as source under the module test tree.
- Open implementation focus remains 03/11 (major/minor checkin and closing upload/standalone revision), 15 unbound-product rule, 18 remaining async contexts, 20 rejected reupload, plus per-item cross-entry closure review. Earlier pending descriptions in the preceding section are superseded only for the exact follow-up behaviors listed here; overall status is still in_progress.

## Latest continuation — 2026-09-15

- DCC-MAIN-03/11: query/workflow implementation now accepts explicit MINOR/MAJOR checkin type, locks the current chain, requires fresh CAD/PDF for major or rejected/rework paths, applies owner/baseline rules, and uses the requested valid iteration-one initial version for new uploads. Standalone major/revision upload and impact-create entry points were removed; the impact workbench links an existing open major revision. Workflow 134, query 146 and frontend version/checkin/cleanup contracts passed.
- DCC-MAIN-15: an unbound project resolves as `UNBOUND` with null product identity. Product-bound DHF/DMR submission fails before insert; non-product categories retain null product fields. Workflow product tests and frontend metadata-resolution contracts passed.
- DCC-MAIN-19: finalization now cryptographically re-verifies every frozen valid approval signature before activation and preserves the specific invalid-evidence error. Finalization 42, electronic-signature 39 and frozen-roster 4 tests passed.
- Frontend verification: relaxed `vue-tsc` completed successfully with an enlarged heap; upload/checkin/cleanup behavior 8, version-governance static contract, major-action static contract and related-upload static contract passed. No real-page E2E or runtime restart was performed.
- Static audit corrected an outdated major-action contract regex and the upload form type that rejected a resolved product master ID. `git diff --check` remains clean. Overall status remains `in_progress`; cross-entry closure for all 20 items is still required.
- Additional static contracts for quick review, template leaf repair, training tail-second settlement, major-action projection, drawing preview PDF selection and related-file authorization passed. One legacy `dcc-static-011-invalid-saved-training-recipient-static.spec.cjs` verifier still asserts the superseded training/distribution side-effect path; current ordinary approval behavior intentionally bypasses that path and its Java regression now asserts the new policy. It is kept as an audit finding, so no full-static-suite pass is claimed.

## Static closure and local regression — 2026-09-15

This continuation closes the code/static-analysis portion for DCC-MAIN-01 through DCC-MAIN-20 on current `int_qms` source. It does not claim real-page E2E, production database behavior, commit, or push because those actions are not authorized in the current turn.

- Static contracts: 21 backend DCC `.cjs` contracts PASS and 35 frontend DCC `.mjs` behavior/contracts PASS. Coverage includes approval mutation removal, required candidates, project access, project template repair, checkout/checkin replay and permissions, drawing PDF selection, major/minor version governance, related-file authorization, upload session cleanup, product resolution, final approval defaults and stale-response ownership.
- Backend regression: Maven with local `.runtime` JDK/Maven, `-pl yudao-module-dcc -am`, selected 16 DCC test classes, PASS: 487 tests, 0 failures, 0 errors, 0 skipped.
- Frontend verification: `pnpm ts:check` PASS using `tsconfig.relaxed.json`; `pnpm build:local` PASS.
- Additional defect found and fixed during closure: withdrawn resubmission of historical stored `V1.0` records failed after Windchill version migration. Added `parseStoredInitial` with a narrow `V1.0 -> A/1` mapping only for stored initial records; public new upload still rejects `V1.0`. Also corrected ordinary upload submit error feedback to show `A/1` format instead of external-review numeric version wording.
- Residual static scan: ordinary DCC `/return-task`, `/transfer-task`, `/sign-task`, standalone major revision and upload revision-candidate entry points are absent from ordinary controlled-file controller/API/page paths. Remaining mutation endpoints are under external review. Remaining `V1.0` defaults are external-review/NAS legacy paths or schema examples, not ordinary new-upload flow.
- Whitespace/static hygiene: `git diff --check` PASS; Git reports CRLF conversion warnings only.
- Closeout boundary: no remaining code/static defect is identified for DCC-MAIN-01 through DCC-MAIN-20. Formal task completion still requires current-turn authorization for Git commit/push, and real-page E2E if the user wants browser-path evidence beyond the static/local regression scope.
