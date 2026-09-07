# DCC 发布通知与关联影响评估独立测试报告

## Environment Used

- Evaluation mode: phase-gated independent verification
- Validation surface: source review plus local Maven/SQL contract execution
- Phase: P1 发布后续账本与冻结快照 + P2 影响评估任务与升版跟踪
- Database writes: not run; P1/P2 首次、重复迁移按计划留到 P4
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

### P2-AC1: 影响评估任务与升版跟踪目标

- Result: passed
- Covers: AC-06, AC-07, AC-08, AC-09, AC-10, AC-11, AC-12, AC-13
- Command run: 25-test corrective suite, 41-test P2 suite, 286-test P1+P2 adjacent suite, SQL/migration/compile/diff gates
- Environment proof: local DCC Maven/H2 runtime and source-level publication transaction trace; no runtime database migration, E2E, service restart or business write
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccRelatedFileImpactAssessmentServiceImpl.java:243`, `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupServiceImpl.java:138`, `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java:301`
- Notes: unique materialization, assignment, decisions, revision linking and publication-safe linked-revision resolution pass. The prior CAS lost-race blocker is closed without weakening fail-fast handling for an unexplained miss.

### P2-AC2: 任务状态与负责人边界

- Result: passed
- Covers: AC-06, AC-07, AC-08, AC-09
- Command run: 41-test P2 suite and 286-test adjacent suite
- Environment proof: service/controller tests plus real H2 task/audit transaction tests
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccRelatedFileImpactAssessmentServiceTest.java`, `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccPublicationImpactAssessmentControllerTest.java`
- Notes: every related Master receives one task; frozen active requester produces PENDING, missing/disabled requester produces UNASSIGNED. Start/decision/link remain assignee-only; reassign/reopen require both `doc_control` and `dcc:controlled-file:approve` in controller and service.

### P2-AC3: 决定、原因、CAS 与文件零副作用

- Result: passed
- Covers: AC-10, AC-11, AC-12
- Command run: 41-test P2 suite and 286-test adjacent suite
- Environment proof: Mockito behavior tests plus real H2 audit-failure rollback tests
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccPublicationImpactTaskMapper.java`, `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccImpactAssessmentTransactionIntegrationTest.java`
- Notes: both decisions require a reason and update through row-version CAS before immutable audit insertion. NO_REVISION_REQUIRED has no controlled-file interaction; REVISION_REQUIRED only enters NOT_STARTED and creates no version.

### P2-AC4: 大版本关联与发布后解决

- Result: passed
- Covers: AC-12, AC-13
- Command run: 25-test corrective suite, 41-test P2 suite and 286-test adjacent suite
- Environment proof: production service/Mapper/finalization call chain, happy-path/non-ACTIVE/lost-race unit tests and real H2 publication transaction test
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccRelatedFileImpactAssessmentServiceImpl.java:256`, `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccPublicationImpactTaskMapper.java:53`, `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupTransactionIntegrationTest.java:194`
- Notes: same-Master/requester/unique-open-revision rules and create+link rollback pass. Only ACTIVE invokes resolution. On CAS miss the service now performs a tenant-scoped locking current read; a newer RESOLVED row or legitimate reopen/relink is accepted without duplicate audit, while a row still representing the same REVISION_LINKED work fails fast.

### P2-AC5: P2 验证证据完整

- Result: passed
- Covers: AC-13
- Command run: corrective, full P2 and adjacent suites plus SQL/migration/compile/diff gates
- Environment proof: local Maven/H2 and static MyBatis SQL annotation inspection
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccRelatedFileImpactAssessmentServiceTest.java:343`, `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/DccPublicationImpactAssessmentSchemaTest.java:31`, `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupTransactionIntegrationTest.java:194`
- Notes: correction tests cover concurrent resolver winner, concurrent reopen, unexplained CAS miss, tenant/id-scoped FOR UPDATE, ACTIVE/Master publication commit and zero duplicate resolution audit.

## Verification Commands

- Transaction integration: `mvn -o -pl yudao-module-dcc "-Dtest=DccPublicationFollowupTransactionIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> passed, `1 test, 0 failures, 0 errors`.
- P1 suite: `mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileAssignmentScopeServiceTest,DccPublicationFollowupServiceTest,DccPublicationFollowupSchemaTest,DccPublicationFollowupTransactionIntegrationTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileRelatedFileServiceTest,DccControlledFileWorkflowServiceImplTest,DccControlledFilePublicationFlowTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> passed, `253 tests, 0 failures, 0 errors`.
- SQL contract: `python -X utf8 -m pytest script/tests/test_dcc_publication_followup_sql.py -q` -> passed, `3 passed`.
- Migration policy: complete 12-file dependency closure through `20260907_dcc_publication_followup.sql` -> passed, `migrationCount=12`; includes both `20260903_dcc_controlled_file_related_file` and `20260906_dcc_new_file_lifecycle_p4`.
- Compile: `mvn -o -pl yudao-module-dcc -DskipTests compile` -> passed.
- Scoped diff check: passed; only line-ending warnings.
- Schema review: seven structured tables and required business unique keys exist; no historical controlled-file INSERT/UPDATE/DELETE appears in the migration.
- Corrective suite: `mvn -o -pl yudao-module-dcc "-Dtest=DccRelatedFileImpactAssessmentServiceTest,DccPublicationFollowupTransactionIntegrationTest,DccPublicationImpactAssessmentSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> passed, `25 tests, 0 failures, 0 errors`.
- P2 focused suite: `mvn -o -pl yudao-module-dcc "-Dtest=DccPublicationImpactAssessmentSchemaTest,DccRelatedFileImpactAssessmentServiceTest,DccImpactRevisionCommandServiceTest,DccImpactAssessmentArchitectureTest,DccImpactAssessmentTransactionIntegrationTest,DccPublicationFollowupServiceTest,DccPublicationFollowupTransactionIntegrationTest,DccPublicationImpactAssessmentControllerTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> passed, `41 tests, 0 failures, 0 errors`.
- P1+P2 adjacent suite: the P1 253-test class set plus all P2 schema/controller/service/architecture/H2 classes -> passed, `286 tests, 0 failures, 0 errors`.
- Combined SQL contracts: `python -X utf8 -m pytest script/tests/test_dcc_publication_followup_sql.py script/tests/test_dcc_publication_impact_assessment_sql.py -q` -> passed, `6 passed`.
- P2 migration policy: complete dependency closure through `20260907_dcc_publication_impact_assessment.sql` -> passed, `migrationCount=13`.
- P2 compile: `mvn -o -pl yudao-module-dcc -DskipTests compile` -> passed.
- P2 scoped diff: tracked diff check passed with line-ending warnings only; 22 untracked P2 files had zero trailing-whitespace findings.

## Business Acceptance Verdict

- AC-01: passed.
- AC-02: passed; the transaction-backed integration test directly proves ledger failure preserves the prior active version and Master pointer while suppressing the completion event.
- AC-03: passed.
- AC-06: passed.
- AC-07: passed.
- AC-14: passed.
- AC-16: passed.
- AC-08: passed.
- AC-09: passed.
- AC-10: passed.
- AC-11: passed.
- AC-12: passed.
- AC-13: passed; legitimate resolver/reopen lost-races preserve ACTIVE publication and do not duplicate audit, while an unexplained miss remains fail-fast.

## Final Verdict

- Outcome: passed
- Phase: P2
- Passed phase ids: P1-AC1, P1-AC2, P1-AC3, P1-AC4, P1-AC5, P2-AC1, P2-AC2, P2-AC3, P2-AC4, P2-AC5
- Failed phase ids:
- Verified business ids: AC-01, AC-02, AC-03, AC-06, AC-07, AC-08, AC-09, AC-10, AC-11, AC-12, AC-13, AC-14, AC-16
- Failed business ids:
- Corrective gap: none
- Summary: P2 passes. The prior AC-13 concurrency blocker is closed, and the 25-test corrective suite, 41-test P2 suite, 286-test adjacent suite, SQL, migration, compile and diff gates are green.

## Open Issues

- P1 remains passed with no regression detected.
- The prior AC-13 blocker is closed: a lost-race now uses `tenant_id + id + deleted = 0 ... FOR UPDATE` as a MySQL REPEATABLE READ current read. Legitimate newer task state remains authoritative, while inconsistent same-work CAS misses still surface `PUBLICATION_IMPACT_VERSION_CONFLICT`.
- P1/P2 runtime MySQL first/repeat migration remains intentionally deferred to P4. This report does not claim that the runtime database migration was executed.
