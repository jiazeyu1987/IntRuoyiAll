# DCC 发布通知与关联影响评估独立测试报告

## Environment Used

- Evaluation mode: phase-gated independent verification
- Validation surface: source review plus local Maven/SQL contract execution
- Phase: P1 发布后续账本与冻结快照
- Database writes: not run; P1 首次/重复迁移按计划留到 P4
- E2E and service restart: not run
- Tester-owned change: only this `test-report.md`

## Results

### P1-AC1: 发布后续账本与冻结快照目标

- Result: passed
- Covers: AC-01, AC-02, AC-03, AC-06, AC-07, AC-14, AC-16
- Command run: transaction integration test plus the 253-test P1 suite and SQL/migration/compile gates listed below
- Environment proof: local DCC Maven/H2 runtime with the real `PlatformTransactionManager`, real file/Master/follow-up Mappers and production `applyApprovedPublishControlledFile`; no runtime database migration or E2E
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupTransactionIntegrationTest.java`, `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java`
- Notes: a targeted visibility-user insert failure rolls back the publication transaction: A/1 remains ACTIVE, B/1 is written as FINALIZATION_FAILED only by the separate failure transaction, Master still points to A/1, all seven follow-up tables remain empty and the completion event is not emitted.

### P1-AC2: 每个发布版本唯一批次

- Result: passed
- Covers: AC-01, AC-14
- Command run: 252-test P1 suite
- Environment proof: `DccPublicationFollowupServiceTest` and `DccPublicationFollowupSchemaTest` passed
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccPublicationFollowupBatchMapper.java`, `IntRuoyiBackend/sql/mysql/20260907_dcc_publication_followup.sql`
- Notes: tenant + published controlled-file unique key, immutable creation token, duplicate callback return, conflicting identity fail-fast, and shared publishedTime/publishedAt are covered. B/2 remains eligible for its own publication batch.

### P1-AC3: 结构化可见范围、候选和关系快照

- Result: passed
- Covers: AC-03, AC-06, AC-07
- Command run: 252-test P1 suite
- Environment proof: publication-followup, assignment-scope, related-file and full 97-test query regressions passed
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupServiceImpl.java`, `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileAssignmentScopeService.java`
- Notes: requester, CURRENT_VIEW_MATRIX and PUBLIC_FOLDER sources are filtered through the same assignment hard-scope service used by QueryServiceImpl. Project/category/directory remain context, governance access is not materialized as an associated party. Forward/reverse relations use other Masters' current formal versions and deduplicate by Master while retaining direction rows.

### P1-AC4: 幂等重放与小版本零副作用

- Result: passed
- Covers: AC-14, AC-16
- Command run: 252-test P1 suite
- Environment proof: duplicate callback, conflicting identity, B/2 publication and check-in dependency tests passed
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupServiceTest.java`
- Notes: duplicate publication does not rewrite snapshots; identity conflict fails fast. Check-in QueryService has no publication-followup dependency, while formally published B/2/B/3 iterations remain eligible for batches.

### P1-AC5: P1 验证证据完整

- Result: passed
- Covers: AC-01, AC-02
- Command run: `mvn -o -pl yudao-module-dcc "-Dtest=DccPublicationFollowupTransactionIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=true" test`, followed by the full 253-test P1 suite
- Environment proof: real H2 transaction manager and real persistence Mappers; single integration test and full suite both passed
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/target/surefire-reports/cn.iocoder.yudao.module.dcc.service.file.DccPublicationFollowupTransactionIntegrationTest.txt`
- Notes: the test does not copy production transaction logic and does not mock the transaction manager. `CapturingTransactionTemplate` delegates to `super.executeWithoutResult`; the faulting mapper delegates all behavior to the real Mapper except the one injected child insert failure.

## Verification Commands

- Transaction integration: `mvn -o -pl yudao-module-dcc "-Dtest=DccPublicationFollowupTransactionIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> passed, `1 test, 0 failures, 0 errors`.
- P1 suite: `mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileAssignmentScopeServiceTest,DccPublicationFollowupServiceTest,DccPublicationFollowupSchemaTest,DccPublicationFollowupTransactionIntegrationTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileRelatedFileServiceTest,DccControlledFileWorkflowServiceImplTest,DccControlledFilePublicationFlowTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> passed, `253 tests, 0 failures, 0 errors`.
- SQL contract: `python -X utf8 -m pytest script/tests/test_dcc_publication_followup_sql.py -q` -> passed, `3 passed`.
- Migration policy: complete 12-file dependency closure through `20260907_dcc_publication_followup.sql` -> passed, `migrationCount=12`; includes both `20260903_dcc_controlled_file_related_file` and `20260906_dcc_new_file_lifecycle_p4`.
- Compile: `mvn -o -pl yudao-module-dcc -DskipTests compile` -> passed.
- Scoped diff check: passed; only line-ending warnings.
- Schema review: seven structured tables and required business unique keys exist; no historical controlled-file INSERT/UPDATE/DELETE appears in the migration.

## Business Acceptance Verdict

- AC-01: passed.
- AC-02: passed; the transaction-backed integration test directly proves ledger failure preserves the prior active version and Master pointer while suppressing the completion event.
- AC-03: passed.
- AC-06: passed.
- AC-07: passed.
- AC-14: passed.
- AC-16: passed.

## Final Verdict

- Outcome: passed
- Phase: P1
- Passed phase ids: P1-AC1, P1-AC2, P1-AC3, P1-AC4, P1-AC5
- Failed phase ids:
- Verified business ids: AC-01, AC-02, AC-03, AC-06, AC-07, AC-14, AC-16
- Failed business ids:
- Corrective gap: none
- Summary: P1 passes. The real transaction integration test closes the prior rollback-evidence gap, and the 253-test suite, SQL contract, 12-file migration closure, compile and scoped diff checks are all green.

## Open Issues

- No P1 code or verification blocker remains.
- P1 runtime migration first/repeat execution remains intentionally deferred to P4. This report does not claim that the runtime database migration was executed.
