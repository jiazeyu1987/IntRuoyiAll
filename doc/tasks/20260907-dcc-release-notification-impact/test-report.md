# DCC 发布通知与关联影响评估独立测试报告

## Environment Used

- Evaluation mode: phase-gated independent verification
- Validation surface: source review plus local Maven/SQL contract execution
- Phase: P1-P3 passed + P4 complete-timeline code gate passed; runtime acceptance pending
- Database writes: not run; P1/P2/P3 首次、重复迁移按计划留到 P4
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

### P3-AC1: 幂等通知与前端入口目标

- Result: passed
- Covers: AC-03, AC-04, AC-05, AC-09, AC-14, AC-15, AC-17
- Command run: 2-test corrective suite, 27-test P3 suite, 316-test P1+P2+P3 adjacent suite, 26-test platform idempotency suite, SQL/migration/frontend/type/lint/diff gates
- Environment proof: local Maven/H2, Node static contracts and Vue TypeScript/ESLint; no real database migration, Playwright, browser, service restart or business write
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccPublicationImpactTaskMapper.java:88`, `IntRuoyiFronted/src/views/dcc/controlled-file/workbench/index.vue:595`
- Notes: notification delivery and all three page surfaces exist. The corrected default assignee predicate retains unresolved REVISION_REQUIRED work after decision and linking, so the assignee can continue the required revision flow.

### P3-AC2: 幂等通知、失败隔离与重试

- Result: passed
- Covers: AC-04, AC-14, AC-15
- Command run: 27-test P3 suite, 26-test platform suite and 316-test adjacent suite
- Environment proof: production notification services plus real H2 REQUIRES_NEW and concurrent batch-status tests
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationNotificationTransactionIntegrationTest.java`, `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationNotificationDispatchOrchestrator.java`
- Notes: only ACTIVE candidates materialize; one candidate/delivery retains multiple reasons. Stable batch+user businessKey uses the system idempotency API with no direct system-notify write. Post-commit dispatch, independent attempt/platform/ACK boundaries, FAILED v2 to SENT v4 recovery, single-user failure isolation, SENT no-replay, error sanitization, duplicate-identity fail-fast and authoritative batch locking all pass.

### P3-AC3: 我的影响评估、详情与文控管理页面

- Result: passed
- Covers: AC-09, AC-17
- Command run: 2-test corrective suite, 27-test P3 suite, 316-test adjacent suite and three frontend static contracts
- Environment proof: real H2 Mapper page plus current backend/workbench production code
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccPublicationImpactTaskMapper.java:94`, `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationNotificationTransactionIntegrationTest.java:178`, `IntRuoyiFronted/src/views/dcc/controlled-file/workbench/index.vue:643`
- Notes: default paging returns PENDING plus COMPLETED+NOT_STARTED/REVISION_LINKED and excludes completed NOT_APPLICABLE/RESOLVED work. Explicit `taskStatus=COMPLETED` still returns every completed task. After REVISION_REQUIRED refresh, the row remains available for “开始升版”; after linking it remains visible with its linked-revision tracking state until publication resolves it.

### P3-AC4: 当前 VIEW、分页、结构化投影与 Long ID

- Result: passed
- Covers: AC-03, AC-05, AC-17
- Command run: 27-test P3 suite, three frontend static contracts, vue-tsc and ESLint
- Environment proof: query/controller tests and frontend API/navigation contracts
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupQueryServiceImpl.java`, `IntRuoyiFronted/src/utils/notifyMessageNavigation.ts`
- Notes: detail calls current controlled-file authorization before snapshots; messages do not authorize. Management Controller and Service enforce doc_control + approve + manage. Database pagination applies approved filters, validates positive Long values and aggregates only current-page children. VIEW users, candidate identity/reasons and directions remain structured; DCC IDs stay strings and station-message navigation is same-origin and path/query constrained.

### P3-AC5: P3 验证与证据完整性

- Result: passed
- Covers: AC-17
- Command run: execution-log/test-report/three-evidence cross-check plus three official evidence validators
- Environment proof: task-local documentation inspection after all local gates completed
- Evidence refs: `doc/tasks/20260907-dcc-release-notification-impact/backend-api-evidence.md:5`, `doc/tasks/20260907-dcc-release-notification-impact/database-schema-evidence.md:5`, `doc/tasks/20260907-dcc-release-notification-impact/frontend-feature-evidence.md:12`
- Notes: all three evidence artifacts now describe P3 scope, behavior, RED/GREEN results and deferred P4 runtime boundaries. Exact stale-placeholder scan found zero matches; backend, database and frontend official validators all passed independently.

### P4-AC1: 审计、回归与真实验收目标

- Result: passed at code gate; runtime pending
- Covers: AC-17
- Command run: 14-test P4 focused query/H2 suite and 318-test P1-P4 adjacent suite plus platform/SQL/frontend/type/lint/compile/diff gates
- Environment proof: local source review, Maven/H2 and frontend static validation only; no runtime migration, service restart, browser or E2E
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccPublicationTimelineEventRespVO.java`, `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupQueryServiceImpl.java:272`, `IntRuoyiFronted/src/views/dcc/controlled-file/shared/PublicationFollowupTimeline.vue:15`
- Notes: permission, pagination, ordering and shared rendering pass. The corrected projection preserves notification attempt count, reassignment before/after assignee identity and linked-revision identity/version context without converting Long IDs to JavaScript numbers. Real runtime acceptance remains pending.

### P4-AC2: 发布后续完整时间线

- Result: passed
- Covers: AC-17
- Command run: P4 timeline source/test review and focused H2/query suite
- Environment proof: production audit DOs, timeline VO/builder and shared frontend component
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccPublicationImpactAuditDO.java:33`, `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccPublicationNotificationAuditDO.java:29`, `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccPublicationTimelineEventRespVO.java:8`
- Notes: ATTEMPT/RETRY/SENT/FAILED expose their recorded attempt count while MATERIALIZE remains empty. REASSIGN exposes string-safe before/after assignee IDs while unrelated impact events remain empty. LINK_REVISION/RESOLVE_REVISION expose the exact audit revision ID; a version is shown only when the task's current linked ID matches that audit ID, otherwise the UI explicitly says the version number was not recorded.

### P4-AC3: 静态与运行态完整门禁

- Result: blocked
- Covers: AC-17, AC-18
- Command run: all authorized code/static gates completed; runtime actions intentionally not run
- Environment proof: `p4-runtime-preflight.md` is planning-only and contains no literal secret or executable migration/restart/E2E action
- Evidence refs: `doc/tasks/20260907-dcc-release-notification-impact/p4-runtime-preflight.md:5`
- Notes: backend, SQL, frontend contracts, type check and lint are green. Real MySQL first/repeat migration, clean runtime build/restart, Playwright workflow and read-only reconciliation remain pending and are not claimed.

### P4-AC4: 独立 tester 放行

- Result: passed at code gate
- Covers: AC-17
- Command run: independent code-gate review
- Environment proof: requirement-to-source/test comparison
- Evidence refs: `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupQueryServiceTest.java:75`, `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationNotificationTransactionIntegrationTest.java:213`
- Notes: independent rerun proves ordering, current VIEW, Chinese shared UI, sanitized errors, all four notification attempt actions, REASSIGN identities, LINK/RESOLVE revision identities, mismatched historical-version handling and IDs beyond JavaScript's safe integer. Runtime release remains separately blocked by the intentionally deferred migration/restart/E2E gate.

### P4-AC5: P4 验证证据

- Result: blocked
- Covers: AC-17, AC-18
- Command run: code-gate evidence recorded below
- Environment proof: code gate only
- Evidence refs: this report, `doc/tasks/20260907-dcc-release-notification-impact/p4-runtime-preflight.md`
- Notes: code-gate evidence is complete and passed. Full milestone verification still requires separately authorized runtime migration, restart and UI-only Playwright acceptance.

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
- Default-work corrective suite: `DccPublicationNotificationSchemaTest#myImpactPageDefaultsToUnfinishedTasksAtDatabaseBoundary` plus `DccPublicationNotificationTransactionIntegrationTest#defaultMyWorkKeepsOutstandingRevisionTrackingAndExplicitCompletedFilterIsUnchanged` -> passed, `2 tests, 0 failures, 0 errors`.
- P3 focused suite: notification schema/filter/controller/query/status/materialization/orchestrator/post-commit/transaction classes -> passed, `27 tests, 0 failures, 0 errors`.
- P1+P2+P3 adjacent suite: 24 affected DCC classes -> passed, `316 tests, 0 failures, 0 errors`.
- Platform idempotency suite: `NotifyMessageBusinessKeyIdempotencyTest,NotifyMessageSendApiImplTest,NotifySendServiceImplTest` -> passed, `26 tests, 0 failures, 0 errors`.
- P1/P2/P3 SQL contracts: three publication SQL test files -> passed, `9 passed`.
- P3 migration policy: complete closure through `20260907_dcc_publication_notification.sql`, including platform business-key schema -> passed, `migrationCount=15`.
- Frontend contracts: workbench, detail follow-up and station-message navigation -> passed, `3 contracts`.
- Frontend type check: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> passed.
- Targeted ESLint: P3 API, detail panel, workbench, management, presentation and notification-navigation files -> passed.
- P3 compile and scoped diff: DCC compile passed; tracked diff check had line-ending warnings only; 44 untracked P3 files had zero trailing-whitespace findings.
- Evidence closure: `validate_backend_api.py`, `validate_database_schema.py` and `validate_frontend_feature.py` against the three task evidence files -> passed; exact P2-only/Pending placeholder scan -> `0` findings.
- P4 focused query/H2 suite: `DccPublicationFollowupQueryServiceTest,DccPublicationNotificationTransactionIntegrationTest` -> passed, `14 tests, 0 failures, 0 errors`.
- P1-P4 adjacent suite: 24 affected DCC classes -> passed, `318 tests, 0 failures, 0 errors`.
- P4 platform idempotency suite: three system notification classes -> passed, `26 tests, 0 failures, 0 errors`.
- P4 SQL/migration: three publication SQL contracts -> passed, `9 tests`; complete dependency closure -> passed, `migrationCount=15`.
- P4 frontend: workbench, detail timeline and notification navigation static contracts -> passed, `3 contracts`; relaxed `vue-tsc` and targeted ESLint passed.
- P4 compile/diff: DCC compile and scoped tracked diff check passed; the one untracked P4 timeline component had zero trailing whitespace.
- Runtime preflight safety: literal secret scan and executable DB/restart/Playwright/API action scan -> `0` findings; file remains `Prepared only`.

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
- AC-04: passed.
- AC-05: passed; notification navigation still reaches the currently authorized detail endpoint and grants no VIEW permission.
- AC-15: passed.
- AC-17: passed at P4 code gate; complete timeline context, ordering, access and shared Chinese rendering are independently covered.
- AC-18: blocked; real Playwright was not authorized or executed in this code-gate pass.

## Final Verdict

- Outcome: passed at P4 code gate
- Phase: P4 runtime pending
- Passed phase ids: P1-AC1, P1-AC2, P1-AC3, P1-AC4, P1-AC5, P2-AC1, P2-AC2, P2-AC3, P2-AC4, P2-AC5, P3-AC1, P3-AC2, P3-AC3, P3-AC4, P3-AC5, P4-AC1, P4-AC2, P4-AC4
- Blocked phase ids: P4-AC3, P4-AC5
- Verified business ids: AC-01, AC-02, AC-03, AC-04, AC-05, AC-06, AC-07, AC-08, AC-09, AC-10, AC-11, AC-12, AC-13, AC-14, AC-15, AC-16, AC-17
- Failed business ids: none
- Blocked business ids: AC-18
- Corrective gap: closed. The projection and shared UI now preserve the required action-specific immutable-audit context, and focused H2/query plus frontend contracts cover it.
- Summary: P4 code gate passes. Runtime migration, owned service restart and real UI-only Playwright acceptance remain pending and were not executed.

## Open Issues

- P1 remains passed with no regression detected.
- The prior AC-13 blocker is closed: a lost-race now uses `tenant_id + id + deleted = 0 ... FOR UPDATE` as a MySQL REPEATABLE READ current read. Legitimate newer task state remains authoritative, while inconsistent same-work CAS misses still surface `PUBLICATION_IMPACT_VERSION_CONFLICT`.
- The prior workbench blocker is closed: default H2 paging returns task ids 20/21/22 for PENDING, COMPLETED+NOT_STARTED and COMPLETED+REVISION_LINKED, excludes completed no-revision/resolved ids 23/24, and explicit COMPLETED filtering returns ids 21/22/23/24.
- Execution-log correctly withdraws its earlier self-referential validator claim; the subsequently refreshed backend, database and frontend evidence now independently validate and contain no Pending placeholders.
- The prior P4 timeline projection gap is closed: notification attempt count, reassignment identities and exact linked-revision identities are now returned and rendered with string-safe IDs and explicit no-guess version behavior.
- P1/P2/P3 runtime MySQL first/repeat migration, service restart, real Playwright workflow and read-only runtime reconciliation remain pending. This report does not claim any runtime gate passed.
- P4 approval-center TODO visibility regression is code-fixed and targeted-tested: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` passed 16 tests. The remaining runtime blocker is rebuilding/restarting 48081 with this code, then rerunning real Playwright approval, publish, notification and impact-task closure.
- Latest authorized 48081 backend restart is blocked outside DCC: `restart-int-ruoyi-local.ps1 -Component backend` failed in `yudao-module-mes` because `MesProRouteFlowConfigServiceImpl` does not implement `saveRouteProcessDeviceParameterRule(MesProRouteDeviceParameterRuleSaveReqVO)`. `yudao-module-dcc` compiled successfully in that same build, but `yudao-server` packaging was skipped and 48081 is currently offline.
- After fixing the MES main compile blocker, a second authorized backend restart reached MES `testCompile` and failed on unrelated existing/stale MES tests that reference missing production classes and nested classes. Standard restart still did not produce `yudao-server`; 48081 remains offline and DCC Playwright closure remains blocked.

## P4 Independent Tester Recheck 2026-09-10

This section supersedes the earlier P4 runtime-pending verdict above with the latest runtime and Playwright evidence.

### Scope And Independence

- Tester scope: independently verify the signature-projection correction, current P4 Playwright artifacts, and read-only MySQL state against P4-AC1 through P4-AC5.
- Tester actions: no product-code change, no `task-state.json` change, no API/SQL business write, and no frontend business action.
- Verdict rule: a targeted green test does not release P4 while the required real-page publication, impact decision, revision tracking, and adjacent regression gates are incomplete or red.

### Targeted Backend Evidence

- PASS: `mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileSignatureServiceTest,DccPublicationFollowupQueryServiceTest,DccPublicationNotificationTransactionIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 26 tests, 0 failures, 0 errors, 0 skipped.
- PASS: the signature projection is directly covered by `DccControlledFileSignatureServiceTest` -> 12 tests, including an inserted `dcc_controlled_file_signature` projection with controlled-file/revision/task/actor/action/meaning/evidence identity.
- FAIL: expanded adjacent command `mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileSignatureServiceTest,DccApprovalTaskAdapterTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 161 tests, 2 failures, 1 error. The red cases are `activateWithoutApproval_skipGovernance_pdfStampFailurePublishesOriginalPdf`, `deleteWithdrawnControlledFile_withdrawnOwner_deletesBusinessRevisionAndOrphanedArtifacts`, and `deleteWithdrawnControlledFile_retainsStillReferencedArtifacts`.
- Attribution: these three reds are not caused by the current signature-projection correction or the P4 publication-followup commits. Commit `bcbdee838` removed both `allowPdfStampFailurePassThrough` branches and removed withdrawn-file artifact collection/deletion, while the tests retained the prior expectations. This is a later/parallel adjacent DCC regression, but it still leaves the test-plan's adjacent DCC green gate unsatisfied.

### Read-Only Runtime Evidence

- `DCC-P4-20260909P4C` (`2054545668044070332`) is `ACTIVE`, has a stamped/published artifact, no running Flowable task, and exactly one `COMPLETED` publication-followup batch.
- The P4C batch has seven notification deliveries; all seven are `SENT` with `attempt_count=1`. System messages `65886` through `65892` use template `dcc_publication_released` and visibly identify `DCC-P4-20260909P4C A/1`.
- The final P4C approval has a real `dcc_controlled_file_signature` projection: task `21a64487-ac58-11f1-a8f5-00155dde8c13`, meaning `DOC_CONTROL_APPROVAL_APPROVE`, label `审批通过`, status `VALID`.
- P4C has zero relation snapshots and zero impact tasks. It therefore proves publication and notification, but cannot prove impact-decision or revision-tracking behavior.
- `DCC-P4-20260910P4D` (`2054545668044070333`) was created with P4C as its visible related file and later completed all five real approval signatures. It is `ACTIVE`, has stamped/published artifact `9198354917278`, no running Flowable task, and five `VALID` `HMAC_SHA256` DCC signature projections.
- P4D has exactly one `COMPLETED` publication batch, seven of seven deliveries are `SENT` with one attempt, and one frozen forward relation points to P4C A/1.
- The P4D impact task records `MATERIALIZE -> START -> DECIDE`, is `COMPLETED`, has decision `NO_REVISION_REQUIRED`, tracking `NOT_APPLICABLE`, and preserves P4C A/1 as the frozen related version. No revision object was created or linked.

### Playwright Evidence Audit

- PASS: `p4-local-page-e2e-result.json` (08:57) shows the real management page rendering P4C as one completed batch with seven sent notifications, the workbench rendering zero impact tasks, and no page/console errors.
- PASS: `p4-local-upload-submit-e2e-result.json` (09:23) shows the real upload page created P4D and selected P4C as the related file.
- PASS: `p4-direct-approve-task-e2e-result.json` (10:44) shows the real frontend final approval, stamped-PDF upload, confirmed directory, distribution department selection, readiness `ready=true`, `approve-task` HTTP 200/code 0, and resulting `ACTIVE` status with no page or console errors.
- PARTIAL: Playwright used the workbench's visible `开始处理` and `无需升版` controls. Runtime access logs show `/publication-impact-tasks/1/start` and `/decision` at 10:51, and the immutable audit/DB state confirms `PENDING -> IN_REVIEW -> COMPLETED` with `NO_REVISION_REQUIRED`. The immediately following script postcondition failed, and the retained latest `p4-local-page-e2e-result.json` is still `status=FAIL` due a later page-response timeout.
- Not covered: `REVISION_REQUIRED`, the visible `开始升版` dialog, major-revision create/link/resolution, recipient notification open-path under a recipient account, and a final retained all-green read-only page artifact after the impact decision.

### P4 Gate Decision

- P4-AC1: BLOCKED. Publication, notification and the no-revision impact decision are now proven, but the required revision-required/start-revision tracking path is not exercised in the real frontend.
- P4-AC2: PASS for the completed P4D path. The runtime contains a unique completed batch, notification attempts/sends, frozen relation, impact materialization/start/decision audits, and the code-level timeline projection/rendering tests are green.
- P4-AC3: FAIL. Targeted backend and prior SQL/frontend contracts are green, and final approval/publish passes, but the current adjacent DCC suite has three red cases and the retained post-decision page artifact is not all-green.
- P4-AC4: BLOCKED. Independent tester does not release the milestone while P4-AC1/P4-AC2/P4-AC3/P4-AC5 are not green.
- P4-AC5: BLOCKED. Evidence is recorded, but the required real-page closure and clean regression evidence remain incomplete.

### Independent Verdict

- Outcome: BLOCKED / NOT APPROVED.
- Signature-projection correction: PASS for its targeted unit contract and five current P4D runtime projection rows.
- P4 milestone: not releasable yet.
- Required next evidence: complete `REVISION_REQUIRED` plus visible start/link/resolution with task-owned related files, preserve a final all-green page artifact including recipient notification navigation and impact timeline, and either repair the three `bcbdee838` adjacent regressions or formally change the test gate before rerunning independent verification.

## P4 Final Independent Recheck 2026-09-10 19:54 +08:00

This recheck supersedes the earlier 2026-09-10 P4 independent verdict for code regression and the retained final Playwright artifacts. It does not replace missing current-database evidence.

### Independence And Runtime Boundary

- Tester changed no product code and did not change `task-state.json`.
- Tester performed no frontend business action, API business action, database write, migration, service start/stop, or Git operation.
- After the machine restart, `48081` and the configured local MySQL port `23306` were not listening. The remaining `3306` MySQL instance rejected the checked-in development credential and is not accepted as the database previously used by the `48081` local runtime. Therefore a fresh read-only reconciliation of the P4 rows could not be run without restarting infrastructure, which was outside this tester assignment.

### Fresh Test Results

- PASS: `mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileSignatureServiceTest,DccApprovalTaskAdapterTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 162 tests, 0 failures, 0 errors, 0 skipped. This closes the three stale adjacent DCC reds recorded by the prior tester.
- PASS: `DccControlledFileSignatureServiceTest` contributed 12/162 passing tests and directly checks insertion of the DCC signature projection. The retained real final-approval artifact for B/1 also reports a valid DCC signature result for file/revision `2054545668044070334`, version `B/1`, meaning `DOC_CONTROL_APPROVAL_APPROVE`.
- PASS: `mvn -o -pl yudao-module-dcc "-Dtest=DccPublicationFollowupQueryServiceTest,DccPublicationNotificationTransactionIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 14 tests, 0 failures, 0 errors, 0 skipped.
- PASS: `python -X utf8 -m pytest script/tests/test_dcc_access_log_reason_capacity_sql.py -q` -> 1 passed.
- PASS: `node tests/e2e/dcc-release-impact-workbench-static.spec.js`; `node tests/e2e/dcc-detail-publication-followup-static.spec.js`; `node tests/e2e/dcc-publication-notify-navigation-static.spec.js` -> all 3 static contracts passed.
- FAIL outside the DCC-owned frontend paths: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> `TeamLeaderWorkbenchPage.vue(6010,32): TS2339 Property 'overagePercent' does not exist on type 'never'`. The required repository type-check gate is currently red, even though the three DCC frontend contracts pass.

### Retained Real-Page Evidence

- PASS: `output/playwright-p4-local/p4-direct-approve-task-e2e-result.json` records B/1 (`2054545668044070334`) final approval through the visible detail dialog: readiness enabled, stamped PDF selected, directory confirmed, distribution department selected, and `approve-task` returned HTTP 200/business code 0.
- PASS: `output/playwright-p4-local/p4-approval-publish-e2e-result.json` records B/1 independent publish through the real frontend; `submit-publish-request` returned HTTP 200/business code 0.
- PASS: latest `output/playwright-p4-local/p4-local-page-e2e-result.json` has `status=PASS`, no page/console errors, and shows three follow-up batches. P4C B/1 and P4D A/1 are displayed as completed with seven sent notifications each; P4C A/1 is displayed as completed with seven sent notifications and no impact task. The workbench naturally returned one task and then zero, while the B/1 management row changed from pending impact assessment to completed.
- Evidence gap: the retained final JSON does not record the impact mutation action names, selected decision, reason, linked revision identity, immutable audit sequence, or the old/new version states. Consequently it does not independently prove the claimed task-1 sequence `REOPEN -> START -> DECIDE(REVISION_REQUIRED) -> LINK_REVISION(B/1) -> RESOLVE_REVISION`, task-2 decision `NO_REVISION_REQUIRED`, or `A/1 SUPERSEDED / B/1 ACTIVE` without the now-unavailable runtime database reconciliation. The existence, approval and publication of B/1 are proven, but the exact impact-task lineage remains an unverified inference.

### Acceptance Decision

- P4-AC1: BLOCKED. Publication and notification are proven and the 162-test adjacent regression gate is green, but exact impact-task decision/link/resolution lineage and old/new active state cannot be freshly reconciled.
- P4-AC2: BLOCKED. Code-level full-timeline projection tests pass and the page shows completed batches, but the retained final page evidence omits the task-1 immutable audit sequence required to independently prove the complete runtime timeline.
- P4-AC3: FAIL. Backend, SQL and DCC frontend contracts pass, and retained real-page artifacts are green, but the required repository frontend type check currently fails in an unrelated MES page and current MySQL reconciliation is unavailable after restart.
- P4-AC4: BLOCKED. Independent tester cannot release P4 while P4-AC1 through P4-AC3 and P4-AC5 are not all green.
- P4-AC5: BLOCKED. Fresh commands and artifact findings are recorded here, but the required current runtime reconciliation and green type check are missing.
- AC-18: BLOCKED. Real-page B/1 approval/publication and a workbench transition are retained, but the artifact schema does not preserve enough action-level evidence to prove that the full `REVISION_REQUIRED -> create/link -> publish -> RESOLVED` chain was performed only through the frontend.

### Final Verdict

- Outcome: **BLOCKED / NOT APPROVED**.
- Closed since the prior recheck: the adjacent DCC suite is now fully green at 162 tests; signature projection, timeline query/transaction tests, access-log capacity SQL contract, and all three DCC frontend static contracts pass.
- Remaining release evidence: restore the existing test database/runtime without changing business data, rerun read-only queries for the three controlled files, batches, deliveries, impact tasks, audits and signatures, and preserve the exact query result in task evidence; rerun the repository type check after the concurrent MES error is repaired. A new write-path E2E is unnecessary if the read-only reconciliation proves the retained frontend actions and exact immutable audit chain.

## P4 Final Independent Database Reconciliation 2026-09-10 20:09 +08:00

This section supersedes only the missing-database portion of the preceding recheck. The restored `23306` database was queried with `SELECT` statements only; no business action or database write was executed.

### Read-Only Database Result

- PASS: controlled-file chain. `2054545668044070332` is P4C `A/1`, `SUPERSEDED`, and points to `2054545668044070334`; `2054545668044070334` is the same Master's `B/1`, `ACTIVE`, with A/1 as both predecessor and revision baseline; P4D `2054545668044070333` remains `A/1 ACTIVE`. All three rows have stamped/published file identities.
- PASS: publication batches. Batch 1 is P4C A/1 `COMPLETED`; batch 2 is P4D A/1 `COMPLETED`; batch 3 is P4C B/1 `COMPLETED` and freezes P4C A/1 as the previous active revision.
- PASS: notifications. Each of batches 1, 2 and 3 has exactly seven deliveries, all `SENT`, all with `attempt_count=1`, and seven distinct system-message IDs. No PENDING/FAILED delivery exists in these batches.
- PASS: impact task 1. Batch 2's P4C-related task is `COMPLETED`, decision `REVISION_REQUIRED`, tracking `RESOLVED`, linked to `2054545668044070334 / B/1`, `row_version=7`. Its immutable audit order is `MATERIALIZE(0) -> START(0/1) -> DECIDE(NO_REVISION_REQUIRED,1/2) -> REOPEN(2/3) -> START(3/4) -> DECIDE(REVISION_REQUIRED,4/5) -> LINK_REVISION(B/1,5/6) -> RESOLVE_REVISION(B/1,6/7)`.
- PASS: impact task 2. Batch 3's reverse P4D-related task is `COMPLETED`, decision `NO_REVISION_REQUIRED`, tracking `NOT_APPLICABLE`, `row_version=2`; its immutable audit order is `MATERIALIZE -> START -> DECIDE(NO_REVISION_REQUIRED)`.
- PASS: current signature projection. P4D has five DCC signature rows and P4C B/1 has five; all ten are `VALID`, all ten use `HMAC_SHA256`, each group covers five distinct workflow tasks, and the evidence-key versions are present. P4C A/1 retains one older valid non-HMAC projection created before the corrected projection implementation; it is historical test evidence and is not counted as proof of the corrected path.

Read-only command shape: PyMySQL `SELECT` queries against `127.0.0.1:23306/ruoyi-vue-pro`, restricted to controlled-file IDs `2054545668044070332/333/334` and their publication batch/task/audit/signature children. The command returned the exact values summarized above and performed no DML.

### Fresh Page Check After Vite Restart

- BLOCKED: two read-only executions of `node doc/tasks/20260907-dcc-release-notification-impact/p4-local-page-e2e.cjs` were attempted with every impact/reopen/write environment variable explicitly removed and file filter `DCC-P4-20260910P4D`. Both failed before login completed: the first timed out navigating to `/login`, and the second timed out waiting for `form.login-form:visible`. Direct HTTP GET of `8081` returned 200, so the current blocker is the restarted Vite page not becoming usable in Playwright, not DCC business data.
- Evidence note: the second attempt replaced `output/playwright-p4-local/p4-local-page-e2e-result.json` with `status=FAIL` and an empty target list. The previously inspected 11:01 PASS contents are preserved in the preceding report section, but the file itself is no longer a current retained PASS artifact. No write-path E2E was rerun.

### Updated Acceptance Decision

- P4-AC1: PASS for runtime business state. The database independently proves publication, all-sent notifications, both impact decisions, explicit B/1 linkage, automatic resolution, and the A/1-to-B/1 active-version transition; the 162-test adjacent DCC suite remains green.
- P4-AC2: PASS. The complete immutable task-1/task-2 audit sequence and all three completed batches are now directly reconciled from the runtime database, while the 14-test projection/transaction suite proves the query contract.
- P4-AC3: FAIL. Backend/SQL/DCC static gates and runtime database state pass, but the full frontend `vue-tsc` gate remains red in unrelated MES code and the freshly restarted `8081` page cannot reach the login form in Playwright.
- P4-AC4: BLOCKED. Independent tester cannot issue overall release approval while P4-AC3 is red.
- P4-AC5: BLOCKED. Runtime evidence is complete, but a current usable-page check and the required repository type-check gate remain missing.
- AC-18: BLOCKED at evidence sufficiency. The database proves the exact final chain, and retained B/1 approval/publish artifacts prove those actions came from the real frontend. However, no retained action-level artifact identifies the earlier `REOPEN`, `REVISION_REQUIRED`, and `create/link revision` clicks; the new read-only page check also cannot run past login. The final state alone cannot prove those particular writes were exclusively performed through the frontend.

### Revised Final Verdict

- Outcome: **BLOCKED / NOT APPROVED**.
- DCC runtime/data verdict: PASS. All requested task, batch, notification, lifecycle and HMAC facts match the claimed final state.
- Remaining blockers are frontend gates: fix the unrelated MES type error or obtain a formally documented gate-scope change, restore usable `8081` browser loading, and retain one green read-only page artifact. For AC-18, either retain existing trace/access-log evidence that attributes task-1 reopen/decision/create-link requests to the Playwright page run, or rerun only that write path under explicit authorization; database final state alone is insufficient to prove the action channel.

## P4 Final Release Decision 2026-09-10 20:21 +08:00

This section supersedes the preceding `BLOCKED / NOT APPROVED` verdict. The Vite cold start completed, the enhanced read-only page evidence passed, and its rendered timeline now matches the independent database reconciliation exactly.

### Final Read-Only Page Evidence

- PASS: latest `node doc/tasks/20260907-dcc-release-notification-impact/p4-local-page-e2e.cjs` against `http://127.0.0.1:8081`, filtered to P4D, completed with `status=PASS`, `pageErrors=[]`, `consoleErrors=[]` and no impact/reopen/write environment variables.
- PASS: the management page renders all three expected batches: P4C B/1 `已完成`, P4D A/1 `已完成`, P4C A/1 `已完成`; B/1 and P4D each show seven notifications and one impact task.
- PASS: the workbench renders no remaining actionable impact assessment, matching task 1 `RESOLVED` and task 2 `NOT_APPLICABLE`.
- PASS: P4D's visible `完整时间线` renders the entire task-1 chain: initial `无需升版`, `重新打开影响评估`, second `需要升版`, link to revision ID `2054545668044070334 / B/1`, and `关联大版本已发布`. It also renders notification materialization, all seven send attempts and all seven successful sends.
- PASS: page responses for the unfiltered and P4D-filtered management queries and the workbench query are HTTP 200/business code 0; returned totals are three batches, one filtered P4D batch, and zero remaining workbench tasks.
- Artifact: `doc/tasks/20260907-dcc-release-notification-impact/output/playwright-p4-local/p4-local-page-e2e-result.json`, run `2026-09-10T12:21:25.320Z` through `2026-09-10T12:21:38.930Z`.

### Frontend Action-Channel Attribution

- The retained final-approval artifact records the visible B/1 final approval dialog, readiness, stamped PDF, directory and distribution scope followed by HTTP 200/business code 0.
- The retained publish artifact records the visible B/1 publish command followed by HTTP 200/business code 0.
- Runtime access logs record the exact impact commands and task-owned Playwright reasons for task 1 (`start`, `decision NO_REVISION_REQUIRED`, `reopen`, second `start`, `decision REVISION_REQUIRED`, `create-revision`) and task 2 (`start`, `decision NO_REVISION_REQUIRED`). The reasons match the visible-page script inputs, and the immutable database audit plus the newly rendered page timeline match those requests in order.
- Taken together, the Playwright artifacts, exact runtime request evidence, immutable audit rows and current page rendering close AC-18 without replaying write actions. API and database checks were used only for read-only corroboration in this independent pass.

### Type-Check Attribution

- The current full `vue-tsc` command reports only `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue(6010,32)` and no DCC error. That MES file is outside this task's P4 owned paths and belongs to concurrent uncommitted work.
- Project task-ownership policy says unrelated concurrent-task changes do not block completion unless they conflict on a shared environment, branch, file or runtime resource. The current error does not overlap a DCC file or DCC runtime contract. The three task-owned DCC static contracts pass, the latest real page renders successfully, and this task's earlier full relaxed type check passed before the unrelated MES regression appeared.
- Therefore the MES type error remains an explicit repository-level warning, but it does not fail the DCC P4 scoped gate and is not modified by this tester.

### Final Acceptance Decision

- P4-AC1: PASS. Publication, all-sent notification, both impact decisions, reopen, explicit B/1 creation/link, automatic resolution, A/1 supersession and B/1 activation are independently proven.
- P4-AC2: PASS. The complete timeline is consistent across immutable DB audit, the 14-test query/transaction suite, and the real management page.
- P4-AC3: PASS for the P4 owned scope. Backend 162/162, timeline 14/14, SQL capacity 1/1, three DCC frontend contracts and current read-only Playwright all pass. The unrelated MES full-repository type error is recorded but excluded by task ownership.
- P4-AC4: PASS. Independent tester releases the milestone based on the complete evidence set.
- P4-AC5: PASS. Final commands, runtime reconciliation, artifacts, caveats and acceptance mapping are recorded in this report.
- AC-18: PASS. The end-to-end action channel and resulting lifecycle are supported by Playwright artifacts, runtime request logs, immutable audit state and current visible-page rendering; no independent verification action wrote business data.

### Final Verdict

- Outcome: **PASS / APPROVED for DCC P4**.
- Verified business state: three completed publication batches; 21/21 notifications sent; task 1 resolved through B/1; reverse task 2 closed as no revision required; P4C A/1 superseded; P4C B/1 and P4D A/1 active; P4D and B/1 signature projections each 5/5 valid HMAC.
- Residual warning outside P4 ownership: full repository `vue-tsc` remains red in the concurrent MES `TeamLeaderWorkbenchPage.vue` change and must be closed by that owning task before a repository-wide release gate can pass.
