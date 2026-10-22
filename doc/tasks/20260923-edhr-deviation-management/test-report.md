# Test Report

## Environment Used

- Evaluation mode: phase-gated
- Validation surface: task-defined

## Results

## P1

- Executor outcome: implementation and executor-side test set are ready for independent review; P1 is not marked complete in task-state.json by this executor.
- BDD/RED/GREEN and exact commands: recorded in execution-log.md under “P1 Executor Work”.
- Baseline: the clean baseline first failed reactor tests in pre-existing yudao-module-infra Surefire (599 tests, 13 failures, 1 error), and a compile-only reactor run then reached MES and failed on missing OperationFact.reviewMaterialsJson and MesTeamLeaderActiveOrderDetail.SourcePickListDocument. Both detail DTO projections were restored and the full 25-module compile-only reactor subsequently passed.
- Executor GREEN: final combined targeted Maven command passed 50 tests: 2 number-generator, 7 deviation-service, 4 signature integration, 33 release-service, 2 mapper façade, and 2 existing release façade tests; zero failures/errors/skips. Signature coverage includes dedicated EDHR_DEVIATION subject binding, canonical content hash, GxP append rollback, missing credential, and idempotent replay.
- Current independent rerun before candidate mapper addition: Node contracts -> PASS, 6/6; focused Maven reactor -> BUILD SUCCESS, 44/44.
- Candidate SQL regression: `MesProEdhrBatchExecutionMapperTest.selectDeviationOptionsPage_scopesTenantAndKeepsBeforePqcBatchButExcludesVoidedAndReleased` runs the real MyBatis mapper against H2. Initial RED was a fixture-tenant mismatch (`tenant_id=0` rows vs query tenant 1); after seeding exact tenant identities, it passed and asserts only the pre-PQC candidate remains while voided, released, and other-tenant batches are excluded.
- Final focused Maven reactor including the H2 candidate mapper test: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationNumberGeneratorTest,MesProEdhrDeviationServiceImplTest,MesProEdhrReleaseServiceImplTest,MesProEdhrBatchExecutionMapperTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> BUILD SUCCESS, 46 tests: mapper 2, sequence 2, deviation service 7, release service 33, existing release façade 2; 0 failures/errors/skips.
- Final Node contracts: `node --test src/test/js/mes-edhr-deviation-p1-contract.spec.cjs src/test/js/mes-team-leader-active-order-detail-missing-dto-contract.spec.cjs` -> PASS, 6/6. Document, backend API and database evidence validators -> PASS; `git diff --check` -> PASS.
- MySQL isolation/runtime inspection: `docker inspect codex-edhr-p1-mysql-20260924` confirms image `mysql:8.0.39`, `NetworkMode=none`, no published ports, and a task-container volume. Read-only inspection of `edhr_test` confirms the four deviation tables, parent menu 5700, query/create permission rows, and zero remaining helper procedures. Snapshot rows show tenant 880001/month 209912 sequence `last_value=24`; one concurrent reservation row (`concurrent-idem`, deviation_id NULL); eight batches left PENDING_APPROVAL with their eight deviations OPEN; four RELEASED transactions without an OPEN deviation; OPEN+RELEASED join count=0. These results corroborate the described scenarios and no unsafe terminal state.
- Database evidence boundary: `execution-log.md` now records exact migration first/re-apply, mapper SQL, contention harness, rollback, and create-first/release-first schedules. Independent inspection confirmed isolated MySQL 8.0.39 and terminal rows before the task container/volume were removed. These verify MySQL DDL, reserved identifiers, mapper expressions and batch-first InnoDB serialization; they do not execute the complete Spring write path against MySQL.
- Independent final rerun: targeted Maven reactor -> BUILD SUCCESS, 50/50 (2 mapper, 2 number, 7 deviation service, 4 signature integration, 33 release service, 2 release façade); Node contracts -> PASS 7/7; database/backend evidence validators, document validator and `git diff --check` -> PASS. `python -X utf8 script/gxp_audit_coverage_gate.py --root . --policy config/gxp-audit-policy.yaml` -> FAIL as expected: `edhr.deviation.create: annotated GxP write is missing from policy (...)`.
- Snapshot note: one earlier independent Maven attempt while the worktree was being edited failed compilation because the internal payload-hash helper briefly had an interface visibility mismatch. The helper is now internal to the implementation; the latest focused rerun after that correction passed 50/50.
- Signature path review: controller create and candidate endpoints both enforce `mes:pro-edhr-deviation:create`, and create takes actor ID from `SecurityFrameworkUtils`. The signature service path checks `isElectronicSignatureEnabled(actorId)`, reauthenticates the same actor with the submitted password, and creates action `DEVIATION_INITIATION`; the subject source is `EDHR_DEVIATION:<deviationId>` and the canonical initiation hash is passed as the subject content hash. The create service stores the returned ID/hash and the audit command links that ID/hash. `signaturePassword` is excluded from the normalized idempotency hash and canonical deviation content.
- Signature test boundary: `MesProEdhrDeviationSignatureIntegrationTest` uses H2/MyBatis for deviation, reservation, and sequence rows, but Mockito replaces both `MesProBatchRecordExecutionSignatureService` and `GxpAuditService`. The four tests verify service orchestration, active-transaction append, signature-failure and append-failure rollback, hash/audit linkage, and a same-key replay with a different credential not signing or appending twice. They do not execute real signature-password reauthentication, persist a record through `ElectronicSignatureServiceImpl`, or commit a real GxP ledger event. This integration test therefore proves the domain transaction boundary and binding contract with provider stubs, not end-to-end signature/GxP service persistence.
- Signature storage review: the inspected production call path delegates to `ElectronicSignatureServiceImpl.sign`, which persists `system_electronic_signature`; `recordSignatureForSystemUser` returns that unified signature ID and does not insert a `mes_pro_batch_record_execution_signature` row for this system-user path. `execution-log.md` now records this correctly. No schema change is implied.
- Credential DTO hardening: `MesProEdhrDeviationCreateReqVO.signaturePassword` is excluded from Lombok `toString()`. The dedicated regression passes, and the password remains excluded from explicit hashes/audit payloads.
- GxP boundary: the service is annotated and calls `gxpAuditService.append` with an ABSENT-to-PRESENT envelope, signature ID/hash, and reason formatted as `DEVIATION_INITIATION[<category codes>]: <description>`. The active approved policy has no `edhr.deviation.create` operation; `GxpAuditServiceImpl` therefore fails closed and the transaction rolls back. The test injects this policy failure via the GxP mock, while the coverage gate independently confirms the missing policy registration. The user-confirmed business `signaturePolicy=REQUIRED` does not supply the GxP operation's owner, approved reference/version, reason policy, or retention class. Do not add inferred policy values.
- Whole-repository migration policy gate: failed before P1 evaluation because existing 20260921_mes_edhr_nonconformance_review_materials_json.sql lacks release-migration metadata; the exact P1 dependency-closure gate passes.
- Independent lock-order review: found and reproduced a release->batch vs deviation-create batch->release lock inversion in the public approve preflight. Fixed by using a non-locking transaction read before replay/status inspection and deferring mutating approval locks to the finalizer after the batch lock. The static regression is GREEN; the earlier focused Maven run passed 44 tests and the current run including the candidate mapper test passes 46.
- Independent tester verdict before policy registration: **BLOCKED** for P1 completion. This was superseded for the policy gap by the new role-owned registration and independent PASS recorded below; full Spring-service/MySQL concurrency remains outside the executed evidence.
- Lock-order review: the first independent rerun exposed `finalizeRelease()` locking the release transaction before `finalizeApproval()` locked the batch. The service now reads transaction status without a lock, then finalization locks batch before release transaction; the added static regression passes. The H2-backed release integration test verifies an open deviation rejects approval and leaves release/batch states pending, but it does not create a concurrent create-versus-release race.
- Candidate and idempotency review: candidate SQL explicitly scopes tenant, excludes voided batches and batches with a RELEASED transaction; the H2 `BaseDbUnitTest` runs the real MyBatis mapper against seeded SQL rows for an eligible pre-PQC batch, a voided batch, a released batch, and another-tenant batch, and asserts only the eligible batch is returned. Create separately rechecks tenant/voided/released state while holding the formal batch lock. The tenant/key reservation SQL was exercised with 24 concurrent MySQL sessions and unique-key/rollback checks; Java service payload comparison, locked replay, deviation insert, and request-link transaction were not executed against MySQL.
- Acceptance scope: P1-AC4 now explicitly covers pre-PQC formal-batch candidate/create by batchExecutionId; its candidate SQL behavior is covered by the H2 MyBatis mapper test above. QA's critical-deviation NCR is assigned to P4. The NCR association and direct pre-PQC QA NCR path remain future P4 work, not a P1 pass claim.
- Mapping audit: T04 -> P1-AC4 correctly tests pre-PQC formal-batch candidate/create; T20 -> P4-AC3 correctly tests direct QA NCR transfer before PQC push. Corrected `development-plan.md` so P1 owns candidate/create identity and P4 owns NCR transfer. Updated `test-report.md` P1-AC4 BDD to include both candidate lookup and deviation create, and to point NCR transfer to P4.
- Compliance boundary before policy update: deviation creation called the initiation-signature service and GxP audit in the same Spring transaction, while the prior policy inventory lacked the operation. That gap is addressed by the role-based policy registration recorded below; the signature provider and audit service remain mocked in the H2 orchestration test.
- Policy registration update: the user authorized role-based approver/owner configuration and asked for configurable values. The policy now registers `edhr.deviation.create` with version `20260924-edhr-deviation-01`, approval reference `EDHR-DEVIATION-GXP-20260924`, `ROLE_QA_QUALITY_OWNER`, `signaturePolicy=REQUIRED`, and `GXP_BATCH_RECORD`; the migration seeds this operation idempotently for tenant 1. Coverage gate and isolated MySQL first/repeat migration verification pass. Independent review of this newly unblocked P1 gate is still required.
- Latest regression after policy registration: Maven reactor -> BUILD SUCCESS, 69/69 tests (including 18 signature-service tests and the four deviation signature integration tests); Node contracts -> PASS 7/7. The first post-registration Maven attempt exposed a missing test-context import for the number generator; adding the existing generator bean import fixed the test setup, with no production fallback.
- Independent review status: the policy review agent independently re-ran the current snapshot and PASSed the role-owned policy registration, coverage gate, Node 7/7, Maven 69/69, static policy tests 6/6, and isolated MySQL core-plus-deviation migration twice. It confirmed no shared database/service writes. It also identified that migration dependency evidence must include `20260908_gxp_audit_trail_core.sql`; migration metadata and evidence now include that seven-file closure. P1 still requires main-agent state advancement after this review.
- Independent review boundary: the operation row is the runtime lookup used by `GxpAuditServiceImpl`; the separate `gxp_audit_policy_version` table has no service lookup in this path and remains reserved for a quality-role policy-version signature workflow. No fabricated approver user id, timestamp, or signature was inserted.
- Evidence correction: `execution-log.md` and the final `execution-log.md` section record isolated MySQL migration, corrected SQL and concurrency results. `test-report.md` records the SQL-backed H2 candidate mapper test and MySQL-vs-service boundary.
- Documentation validator rerun: after correcting expected Markdown inventory, duplicate execution-log heading, 30-versus-28 phase ownership and the old roadmap-node state expectation, `python -X utf8 doc/tasks/20260923-edhr-deviation-management/validate-docs.py --skills-root C:\Users\BJB110\.codex\skills` -> PASS: 17 Markdown files, 28 AC-to-BDD mappings, current phase-state and cleanup inventory, consistency checks, and product/system/acceptance validators.

## P2

- Executor GREEN: P2 handling service test -> BUILD SUCCESS, 6/6; focused P1+P2 regression -> BUILD SUCCESS, 75/75 (P1 69 + P2 6).
- Covered behaviors: one handling row and optimistic content version, normal closure with QA/quality owner in either order, critical management representative requirement, old-version signature invalidation, and same-account multi-node signatures.
- Independent tester GREEN: standard reactor P2 command -> BUILD SUCCESS, 7/7, including current-version readback and unified signature-node coverage. A deliberately MES-only command failed due to stale parallel dependency cache; the project-standard `-am` command passed and is the authoritative result.
- Scope boundary: P2 backend service/Mapper/VO and unified signature action integration are verified; frontend entry/detail and E2E remain P3 scope.

## P3

- P3 backend compile: `mvn -pl yudao-module-mes -am '-DskipTests' compile` -> BUILD SUCCESS after adding tenant-scoped deviation page/get endpoints.
- P3 frontend static contract: `node tests/e2e/mes-edhr-deviation-p3-static.spec.cjs` -> PASS; verifies API paths, three status tabs, detail/empty/error states, route and batch trace deviation tab.
- P1+P2 regression after P3 backend additions: Maven reactor -> BUILD SUCCESS, 77/77 tests.
- Frontend typecheck: PASS via a temporary junction to same-version dependencies; real browser E2E was not authorized and was not run.

## P4

- P4 focused integration: `MesProEdhrDeviationNcrIntegrationTest` -> PASS 7/7; covers QA transfer of multiple same-batch critical deviations before PQC push, whole-set rejection, pending-review duplicate rejection, idempotent replay/conflict, and signature failure zero progression.
- Existing NCR regression: `MesProEdhrNonconformanceReviewApplicationScopeTest` -> PASS 27/27; existing PQC submission/release review paths remain green.
- Migration policy gate: P4 nine-migration dependency closure -> PASS.
- P4 frontend static contract: `node tests/e2e/mes-edhr-deviation-p4-static.spec.cjs` -> PASS; DEVIATION source and multi-selection API shape are covered.
- Boundary: four action freeze gates and three dispositions continue through existing NCR service; no claim of full Java/MySQL concurrent race or browser E2E. Independent P4 review passed.
- P4 frontend contract: `node tests/e2e/mes-edhr-deviation-p4-static.spec.cjs` -> PASS; the client exposes the DEVIATION source and multi-selection transfer contract without presenting transfer closure as verification success.
- P4/P1/P2 combined regression after NCR changes: Maven reactor -> BUILD SUCCESS, 84/84 tests, including P4 integration 7/7 and existing NCR application scope 27/27.
- P4 migration runtime evidence: disposable MySQL first/repeat application -> PASS; review columns, idempotency index, and deviation-create permission remained idempotent after repeat. No shared DB write.
- Independent gate: P4 reviewer PASS; the review reran P1/P2/P4/P5-related selection at 111/111, GxP coverage, migration dependency closure, static contracts, document/evidence validators and whitespace checks. P4 is eligible for phase completion.

## P5

- Partial read-model slice: deviation detail now returns description, close reason, close time, and associated NCR id; P3 static contract asserts these fields are rendered without mapping transfer closure to verification success. Historical signature and repair evidence are presented read-only by the P5 detail slice.
- P5 signature read slice: handling detail now exposes effective signature IDs and evidence fields (real signer display name, meaning, server time evidence, verification status, content/evidence hashes, policy version and subject version) from the unified signature query path; standard reactor handling test passes 9/9 after this read contract.
- P5 trace hosts: the read-only deviation pane is now used by both the batch detail trace drawer and the form trace batch drawer; static P3/P4 contracts remain green. Frontend typecheck passed through a temporary same-version dependency junction; real browser E2E remains unrun because it was not authorized.

## P6

- Pending.

## Final Verdict

- Outcome: P1, P2 and P4 are independently verified. P5 read evidence and error-state tests pass; P3/P5 browser E2E remains unrun because it was not authorized. P6 final gate remains pending.

## Independent P4 Gate and P5 Completion Update

- Independent P4 reviewer PASS: Maven P1/P2/P4/P5-related selection 112/112, GxP coverage `operations=9 annotations=8`, nine-migration dependency closure PASS, static P3/P4 contracts PASS, document/evidence validators PASS, and `git diff --check` has no whitespace errors. P4 behavior and existing NCR paths are accepted; full Spring/MySQL concurrency and browser E2E remain explicit limitations.
- P5 backend regression after signature evidence completion: `ElectronicSignatureServiceImplTest` 13/13 and `MesProEdhrDeviationHandlingServiceTest` 9/9 PASS in the standard reactor. The tests cover directory-resolved signer display name, current-version signature evidence mapping, content/version binding and historical signature invalidation.
- P5 frontend contract: `node tests/e2e/mes-edhr-deviation-p3-static.spec.cjs` -> PASS; the detail page renders signatureEvidence and actorDisplayName, and handling read failures expose a retryable error instead of an empty-state fallback.
- P5 boundary: real browser E2E remains unrun because no real E2E authorization was provided. Frontend typecheck passed via a temporary same-version dependency junction. P6 final gate remains pending.

- P5 evidence completion: unified signature query resolves `actorDisplayName` from the system user directory, and the response/front end render node, signer, meaning, server time, `timeEvidenceId`, verification status, content/evidence hashes and policy version. A handling-read failure has its own error and retry state; it is not treated as an empty or processed record.
- P5 targeted regression: `ElectronicSignatureServiceImplTest`, `MesProEdhrDeviationHandlingServiceTest`, and `MesPqcReleaseOrderDetailServiceTest` -> BUILD SUCCESS, 27/27 tests. The broader selected run reports 112/112 PASS under constrained Maven memory; an additional unrelated active-order test selection hit its existing uninitialized ERP pick-list fixture and is excluded from the deviation evidence.

- Frontend typecheck recheck: using the same-version dependency tree through a temporary task-local junction (removed after the run), `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS. This validates the current worktree sources without installing or changing dependency files.

- P5 history slice: deviation handling detail now returns revisionHistory and signatureHistory, with `CURRENT_VALID` and `SUPERSEDED_INVALID` states. The frontend detail renders both sections. Backend targeted handling tests pass 10/10; the selected P1/P2/P4 regression after this change reports 113/113.

- P5 trace association: deviation response and both read-only trace presentations now include linked NCR code/status/disposition when present. Backend deviation/handling/NCR selection passes 25/25 after the read-model addition.
- P3 management interaction: standard list template, server-side filter/sort fields, request serial protection, permission-gated create action, formal batch selector, and electronic-signature create form are present. Frontend typecheck PASS and P3/P4 static contracts PASS.
- P3 source resolution: active-order-only detail entry resolves formal batch execution candidates through the backend origin relation and rejects missing/ambiguous sources; backend 10/10 selected tests, frontend typecheck, and static contracts pass.
- P3/P5 read interaction hardening: trace reads now protect against stale responses, retrieve all pages up to reported total, expose retry, and distinguish unauthorized/session errors; history tables show complete signature evidence and before/after revision content. Frontend typecheck/static contracts pass.
- P3 form completeness: handling editor now covers investigation timing/members, corrective owner/due date, CAPA fields and linked review code; formal batch detail includes a dedicated 偏差 main tab. Typecheck/static contracts pass.
- P3 handling interaction: detail page now exposes save, electronic-signature and normal-close actions against the existing versioned handling service; all write calls use the formal permission boundary and typecheck passes.
- P3 host navigation: batch-record tab navigation preserves current query parameters, while formal-batch and active-order detail hosts expose a dedicated read-only 偏差 tab. Static contracts remain green.
- Final allowed-scope verification: documentation validator PASS, P3/P4 static contracts PASS, frontend relaxed typecheck PASS via same-version temporary junction, and deviation backend selected tests 27/27 PASS. Real browser E2E remains unrun because it requires explicit current-turn authorization.
- Authorized SQL migration verification: isolated MySQL first/repeat chain PASS; 4 deviation tables, 4 NCR extension columns, 10 permission rows including 7 role-action permissions, 1 GxP operation row, and 0 temporary procedures. Container and temporary fixtures were removed; no shared database was touched.
- P3 list filtering/sorting: keyword, level, initiated-time range and custom server sort parameters are wired through `UnifiedListTemplate`; request serial protection remains active and typecheck/static contracts pass.
- P5 timestamp evidence: unified signature readback now includes `timeZone=Asia/Shanghai`; the detail table renders it beside server signedAt and timeEvidenceId. Signature/handling selected tests and frontend typecheck pass.



## P6 Main-Agent Runtime and Browser Recheck

- Backend targeted reactor rerun -> BUILD SUCCESS, 92 tests, 0 failures/errors/skips. Frontend `vue-tsc` relaxed and deviation-related ESLint -> PASS. P3/P4/template-static contracts -> PASS.
- Authorized local runtime migration first/repeat check -> PASS: deviation tables, NCR columns, permission rows and GxP policy row are present and idempotent in `int-ruoyi-mysql`.
- Real Playwright read-only flow -> PASS for login, deviation list, three status tabs, empty state, create dialog, and formal batch-detail 偏差 tab. Trace and screenshots are stored under `output/playwright/deviation-p6/`.
- Write E2E -> BLOCKED by a real data precondition, not a product assertion: the authorized `打开/创建` path returned business code `1040750424` for the first 20 candidate work orders because none had an enabled product route. Existing batch data was not reused because it is historical data from another task.
- Non-target avatar HTTP 502 was recorded separately as environment noise; it is outside the deviation request path and is not used as a feature PASS claim.

## P6 Real Write E2E Completion

- Task-owned fixture created by Playwright: `P6-DEV-20260926-CAP4-577008`, batch execution ID `900000001150`, formal code `EDHRB-1790401592564`.
- Deviation initiation PASS: `PC-202609-0001`, linked batch `900000001150`, initiation signature `12576`, HTTP 200/business code 0.
- Handling save PASS after JSON normalization: handling ID `1`, content version `1`, `investigationMembersJson=[]`, verification content persisted.
- Five ordinary deviation nodes signed PASS: `PREPARER=12577`, `VERIFIER=12578`, `DEPARTMENT_OWNER=12579`, `QA=12580`, `QUALITY_OWNER=12581`.
- Normal close PASS: `CLOSED/NORMAL_COMPLETED`, handling `CLOSED_LOOP`, verification `PASS`; read-only database check confirms the final state.
- Remaining browser noise is limited to a non-target avatar 502; no deviation or handling target request failed and no pageerror occurred.

## Superseding Current Evidence

Historical notes above that describe browser E2E as unauthorized are superseded by the current authorized Playwright evidence: task-owned batch creation, deviation initiation, handling save, five ordinary signature nodes, normal close, and final detail/trace verification all passed on September 26, 2026.

## P6 Critical Deviation Full-Chain Browser Evidence

- `p6-critical-20260926-r9`: real page created critical deviation `PC-202609-0006`, signed and created NCR `EDHR-NCR-20260926235142-900000001150`; both write responses returned business code 0.
- `p6-critical-20260927-r12-resume`: resumed the pending review, confirmed batch status `冻结中`, uploaded task-owned review material and completed `让步放行`; dispose response returned business code 0.
- `p6-critical-20260926-r13-trace`: real batch detail `偏差` tab shows the closed critical deviation and concession disposition; no pageerror occurred.
- Runtime source: registered worktree `piancha_0923` on frontend `8094` / backend `48094`; backend was rebuilt from the updated MES module and health returned `UP` after restart.
- Known limitation: the task-owned manual batch has no active-order origin, so production report, PQC submit and PQC release freeze actions were not claimed as PASS; the verified chain is batch-level deviation → electronic-signature NCR → frozen batch state → QA concession → batch trace.
