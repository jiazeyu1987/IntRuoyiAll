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
