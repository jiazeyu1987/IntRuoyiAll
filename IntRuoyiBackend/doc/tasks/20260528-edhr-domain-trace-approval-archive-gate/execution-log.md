# Execution Log

BDD: Approval revalidates locked DomainTrace before side effects -> Given a submitted eDHR execution has an approval snapshot with a locked `domainTraceHash`, When an approver approves the BPM task, Then the backend calls DomainTrace approval verification with the execution id and locked hash before recording an approval signature or approving the BPM task.

BDD: Rejection revalidates locked DomainTrace before side effects -> Given a submitted eDHR execution has an approval snapshot with a locked `domainTraceHash`, When an approver rejects the BPM task, Then the backend calls DomainTrace approval verification with the execution id and locked hash before recording a reject signature or rejecting the BPM task.

BDD: Approval action fails fast when locked DomainTrace hash is absent -> Given a submitted eDHR execution has an approval snapshot whose `snapshotJson` omits or blanks `domainTraceHash`, When approve or reject is requested, Then the backend rejects the action before recording any signature or calling BPM.

BDD: Archive generation revalidates locked DomainTrace before archive side effects -> Given an approved and closed eDHR execution has an approved approval snapshot with a locked `domainTraceHash`, When archive generation is requested, Then the backend calls DomainTrace archive verification with the execution id and locked hash before creating archive rows, renderer output, files, or archive seal signatures.

BDD: Archive generation fails fast when locked DomainTrace hash is absent or stale -> Given an approved and closed eDHR execution has an approval snapshot with missing or stale `domainTraceHash`, When archive generation is requested, Then the backend rejects the action before renderer, file, archive row, event, or seal-signature side effects.

RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionArchiveServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, production code did not call `domainTraceService.verifyForApproval(...)` or `domainTraceService.verifyForArchive(...)`; missing approval snapshot `domainTraceHash` was not rejected before side effects; changed DomainTrace did not block approval/reject/archive side effects.

RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest#approveBatchRecordExecution_missingDomainTraceHashInSnapshot_failsFastBeforeSignatureAndBpm+rejectBatchRecordExecution_missingDomainTraceHashInSnapshot_failsFastBeforeSignatureAndBpm" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, missing locked `domainTraceHash` still called BPM `validateTask`; expected fail-fast before touching BPM for invalid snapshot evidence.

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionArchiveServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 53 tests passed; approval/reject/archive now call locked DomainTrace verification before signature, BPM, renderer, file, archive, and seal side effects, and missing/stale locked hash paths fail fast.

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordDomainTraceServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 60 tests passed; adjacent DomainTrace service regression remains green with the new approval/archive callers.

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest#approveBatchRecordExecution_missingDomainTraceHashInSnapshot_failsFastBeforeSignatureAndBpm+rejectBatchRecordExecution_missingDomainTraceHashInSnapshot_failsFastBeforeSignatureAndBpm" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests passed; missing locked `domainTraceHash` now fails before BPM `validateTask`, DomainTrace verification, signature, approve, or reject calls.

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordDomainTraceServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 60 tests passed after moving locked hash validation before BPM task validation.

BLOCKED: `mvn -pl yudao-server -am -DskipTests package` -> FAIL at Spring Boot repackage, environmental blocker: existing process locked `yudao-server\target\yudao-server.jar`; upstream compile/package modules completed before the jar rename failure. Current task used `spring-boot:run` on port 48098 instead of killing the unrelated 48080 process.

GREEN: backend runtime 48098 -> PASS, `/actuator/health` returned `{"status":"UP"}` using MySQL `int-ruoyi-mysql:23306` and Redis `26379`.

GREEN: `pnpm e2e:edhr:approval-tracking:check` -> PASS, frontend E2E script syntax is valid.

RED: `pnpm e2e:edhr:approval-tracking` with suffix `GATE05280510` -> FAIL, expected environment reason: restarted frontend was not yet using this worktree's `VITE_BASE_URL=http://127.0.0.1:48098`; trace showed API traffic to old backend `127.0.0.1:48080`, and the run failed after reject while reopening execution detail. No release decision was made from this run.

GREEN: Frontend 8081 restarted from current worktree with `.env.local`; trace inspection after restart confirmed API traffic only to `127.0.0.1:48098`.

GREEN: `node doc\tasks\20260528-edhr-archive-approval-evidence\scripts\seed-edhr-real-e2e-contexts.cjs --suffix GATE05280525 --apply` -> PASS, fresh test-tenant contexts created for tenant 122: DRAFT task 922035, APPROVE task 922036, REJECT task 922037, SUBMITTED task 922038.

GREEN: `pnpm e2e:edhr:approval-tracking` with suffix `GATE05280525` -> PASS. Real test-tenant UI path completed on frontend `http://localhost:8081` against backend `127.0.0.1:48098`; approved executionId=40, rejected executionId=41, archiveId=9, approvalSnapshotId=25, archive sha256 `27a36dfd8b8fc30f78e02c1505ea90e26263ffdcbf596a2127d6b189c79f959f`.

GREEN: `node doc/tasks/20260528-edhr-domain-trace-approval-archive-gate/scripts/verify-edhr-domain-trace-approval-archive-db-hash.cjs` with `EDHR_DB_HASH_EXECUTION_ID=40`, `EDHR_DB_HASH_ARCHIVE_ID=9`, `EDHR_DB_HASH_TENANT_ID=122` -> PASS. The script recomputed approval snapshot hash `fea8061edf67142bb558ef54c15fa341f7cc8986ce764079678f490b6ce6ac61`, confirmed locked `domainTraceSnapshotId=11`, locked `domainTraceHash=2c7c5aa13178e7c452697672e86ca1efa2c22ca00ada1e2ff22da0e19dd72a79`, `domainTraceStatus=VERIFIED`, archive status `SEALED`, and 8/8 DomainTrace item hashes verified.

REVIEW: Franklin independent reviewer -> PASS. Required changes: none. Decision: missing or non-VERIFIED locked hash now fails before BPM `validateTask`, signature, approve, or reject; normal path keeps signature and BPM side effects after `verifyForApproval`.

REVIEW: Zeno E2E failure triage -> PASS for root-cause classification. The failed `GATE05280510` E2E was caused by frontend runtime hitting old backend 48080; DB showed reject succeeded and DomainTrace data was VERIFIED. Recommendation was to restart frontend against 48098 and rerun with fresh contexts, which was completed by `GATE05280525`.

## Notes

- Scope is backend production code under `ruoyi-vue-pro`, with root-owned DB/hash verifier and frontend real E2E used only for final evidence.
- No fallback, mock success, production data mutation, or live tenant mutation was used. E2E mutated only test tenant 122.
