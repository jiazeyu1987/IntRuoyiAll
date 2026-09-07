# Execution Log

`BDD: 二阶段规划可执行 -> Given 一阶段已生成发布事实、关联方范围和关联文件快照，When 产品与工程规划二阶段，Then 每个通知和影响评估行为都有明确角色、状态、失败边界、验收标准和严格 TDD 路径`

Status: in_progress

## Change Triage

`GREEN: validate_change_request.py -> PASS, decision=accept and split; metadata repair and phase-two feature remain separate tasks`

## Planning Validation

`GREEN: validate_product_requirements.py -> PASS, PRD/user flows/acceptance criteria complete`

`GREEN: validate_acceptance_plan.py -> PASS, BDD/TDD/E2E/test-data plans complete`

`GREEN: validate_node_dev_plan.py -> PASS, task/prd/development-plan/test-plan/task-state package complete`

`GREEN: AC mapping audit -> PASS, AC-01 through AC-18 each appear in PRD, development plan, test plan and task state`

Main-agent review corrections:

- “能看见”冻结正式权限来源及发布时解析用户明细；“必须通知”只取责任明确的收件人，两者不互相替代。
- 当前文件和相关文件责任人统一取对应正式版本 `requesterId`，缺失或停用进入 UNASSIGNED。
- 发布后续完成不阻塞 ACTIVE，但后续账本和快照必须与发布事务原子保存，避免成功发布却永久丢失后续工作。
- 正向关联和反向引用均纳入，按相关 Master 去重；需要升版只复用现有 major-revision，不自动建版。

Status: planning approved; implementation remains pending at P1.

## P1 Executor: 发布后续账本与冻结快照

`BDD: 发布与后续账本原子提交 -> Given B/1 已批准且 A/1 为当前正式版，When 文控发布 B/1，Then 发布事务内建立唯一后续批次并冻结可见主体、通知候选和关系快照；任一快照写入失败时发布事务失败且不记录完成事件`

`BDD: 当前真实 VIEW 口径冻结 -> Given ACTIVE 文件的正式可见主体来自 requesterId、CURRENT_VIEW_MATRIX 和 PUBLIC_FOLDER 电子分发收件人，When 建立发布快照，Then 逐条保存规则来源及发布时用户/部门明细；项目、分类、目录只作为上下文身份，项目分配硬范围、目录规则和 PRODUCT_GROUP 操作权限不冒充 VIEW 授权`

`BDD: 正反向关系按 Master 冻结 -> Given X 正向关联 Y、Y 当前正式版也反向引用 X 且 Z 反向引用 X，When X 发布，Then Y、Z 各保存一个相关 Master 快照，Y 同时保留 FORWARD 和 REVERSE 方向明细`

`BDD: 重复发布与权限变更不改历史 -> Given 发布后续批次已完整存在，When 同一发布回调重放且当前查看矩阵已经变化，Then 返回已有批次且不追加、不更新任何冻结明细`

`BDD: 小版本检入无发布后续 -> Given A/1 检出后生成 A/2，When 小版本服务被调用，Then 不创建发布批次、可见范围、通知候选或关系快照`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationFollowupServiceTest,DccPublicationFollowupSchemaTest,DccControlledFileFinalizationServiceImplTest#applyApprovedPublishControlledFile_followupSnapshotFailureFailsPublicationBeforeCompletionEvent' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL at testCompile, expected P1 publication-followup service, DO and Mapper contracts do not exist`

`RED: python -X utf8 -m pytest script/tests/test_dcc_publication_followup_sql.py -q -> FAIL, 3 tests / 3 failures, expected migration 20260907_dcc_publication_followup.sql is missing`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccControlledFileAssignmentScopeServiceTest,DccPublicationFollowupServiceTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL at testCompile, expected extracted DccControlledFileAssignmentScopeService and publication snapshot contracts do not exist`

P1 implementation:

- Added one immutable batch per `tenant_id + published_controlled_file_id`, with structured rule/user, notification candidate/reason, related Master and relation-direction tables. The migration contains no historical DCC DML.
- Extracted the existing project-assignment hard-scope resolver into `DccControlledFileAssignmentScopeService`; browser/detail query logic and publication snapshot filtering use the same service. Project assignment remains a constraint and is never stored as a VIEW grant.
- The business-visible snapshot contains only requester, `CURRENT_VIEW_MATRIX` and `PUBLIC_FOLDER` recipients after assignment-scope filtering. Project/category/directory/taxonomy leaf are frozen only as context. Directory manager and lifecycle-management technical access remain real-time and outside the associated-party snapshot.
- Forward and current-formal reverse relations are frozen by related Master. Multiple rows and both directions share one related-Master snapshot while preserving every source direction row.
- `activateRevision` invokes the ledger inside the existing finalization transaction before the lifecycle completion event. Batch or child insert failure propagates so the publication transaction rolls back; repeated callbacks use the batch unique key and immutable creation token to return without rewriting snapshots. Published B/2 or later rework iterations still create a batch; check-in has no publication-followup dependency.

`GREEN: mvn -o -pl yudao-module-dcc '-Dtest=DccControlledFileAssignmentScopeServiceTest,DccPublicationFollowupServiceTest,DccPublicationFollowupSchemaTest,DccControlledFileFinalizationServiceImplTest#applyApprovedPublishControlledFile_followupSnapshotFailureFailsPublicationBeforeCompletionEvent+applyApprovedPublishControlledFile_readyCandidateStartsFinalizationAndActivates,DccControlledFileRelatedFileServiceTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> PASS, 21 tests / 0 failures`

`GREEN: mvn -o -pl yudao-module-dcc '-Dtest=DccControlledFileAssignmentScopeServiceTest,DccPublicationFollowupServiceTest,DccPublicationFollowupSchemaTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileRelatedFileServiceTest,DccControlledFileWorkflowServiceImplTest,DccControlledFilePublicationFlowTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> PASS, 245 tests / 0 failures`

`GREEN: python -X utf8 -m pytest script/tests/test_dcc_publication_followup_sql.py -q -> PASS, 3 tests`

`GREEN: python script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file <12-file dependency closure through 20260907_dcc_publication_followup.sql> -> PASS, migrationCount=12`

`GREEN: mvn -o -pl yudao-module-dcc -DskipTests compile -> PASS`

`GREEN: task-owned git diff --check -> PASS (line-ending warnings only)`

P1 runtime note: per current authorization, no database write, service restart, or E2E was executed. Migration first/repeat execution remains a P4 runtime gate and is not claimed here.

## P1 Runtime Permission Baseline Correction

`BDD: 冻结真实 VIEW 来源 -> Given 当前 ACTIVE 浏览授权只来自 requester、CURRENT_VIEW_MATRIX 和 PUBLIC_FOLDER 收件人，When P1 保存发布时可见清单，Then 只冻结这三类实际授权和解析用户，项目/分类/目录只保存上下文，目录规则与 PRODUCT_GROUP 操作权限不得伪装成 VIEW`

Main-agent decision: accept executor evidence and align P1 with current authorization semantics. No permission expansion is authorized.

Follow-up clarification: project assignment is a visibility constraint rather than a grant. P1 must apply it when producing the final resolved-user snapshot and may record it as a filter reason, but must not label it as VIEW grant or include users excluded by the hard scope.

P1 technical design decision: extract the existing private assignment-scope logic into one DCC service and make QueryServiceImpl plus publication snapshot reuse it. The frozen list is explicitly the business audience; directory/document-control management overrides remain dynamic governance access and are not notification stakeholders.

## P1 Executor Handoff

Latest evidence after correcting the migration dependency to `20260906_dcc_new_file_lifecycle_p4`:

- Focused P1 GREEN: 21 tests, 0 failures.
- Adjacent DCC regression GREEN: 245 tests, 0 failures, including full finalization, query, related-file, workflow and publication-flow classes.
- SQL contract GREEN: 3 tests; release migration policy gate GREEN with the complete 12-file dependency closure.
- Backend compile and task-owned `git diff --check` GREEN.

Task-owned files:

- `IntRuoyiBackend/sql/mysql/20260907_dcc_publication_followup.sql`
- `IntRuoyiBackend/script/tests/test_dcc_publication_followup_sql.py`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccPublicationFollowupBatchDO.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccPublicationVisibilityRuleSnapshotDO.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccPublicationVisibilityUserSnapshotDO.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccPublicationNotificationCandidateDO.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccPublicationNotificationCandidateReasonDO.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccPublicationRelationSnapshotDO.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccPublicationRelationDirectionSnapshotDO.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccPublicationFollowupBatchMapper.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccPublicationVisibilityRuleSnapshotMapper.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccPublicationVisibilityUserSnapshotMapper.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccPublicationNotificationCandidateMapper.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccPublicationNotificationCandidateReasonMapper.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccPublicationRelationSnapshotMapper.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccPublicationRelationDirectionSnapshotMapper.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFileRelatedFileMapper.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileAssignmentScopeService.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupService.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupServiceImpl.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRelatedFileService.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRelatedFileServiceImpl.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/DccPublicationFollowupSchemaTest.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileAssignmentScopeServiceTest.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupServiceTest.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImplTest.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFilePublicationFlowTest.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRelatedFileServiceTest.java`
- `doc/tasks/20260907-dcc-release-notification-impact/execution-log.md`

Blocker: no P1 code blocker remains. Per authorization, the migration was not applied to a database and first/repeat runtime execution was not claimed; that evidence remains for the P4 runtime gate. No Git, DB, E2E, service restart, `task-state.json`, or `test-report.md` action was performed by this executor.

## P1 Review Corrections

`BDD: 返工迭代发布仍建后续批次 -> Given 大版本 B/1 审批后返工形成 B/2，When B/2 最终正式发布，Then B/2 建立自己的唯一发布后续批次；只有 check-in 路径不依赖后续服务`

`BDD: 重复回调身份必须一致 -> Given 同一 publishedControlledFileId 已有批次，When 回调携带不同 Master、版本、被替代正式版或发布事实，Then fail fast，不得把冲突事件当幂等成功`

`BDD: 发布时间单一事实 -> Given finalization 把版本切为 ACTIVE，When 同事务建立后续批次，Then 受控文件 publishedTime 与批次 publishedAt 使用完全相同的秒级时间`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationFollowupServiceTest#recordPublishedRevision_publishedB2StillCreatesFollowupBatch+recordPublishedRevision_duplicatePublishedFileWithConflictingIdentityFailsFast+recordPublishedRevision_freezesActualViewSourcesCandidatesAndBidirectionalRelationsByMaster,DccControlledFileFinalizationServiceImplTest#applyApprovedPublishControlledFile_readyCandidateStartsFinalizationAndActivates' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL, 4 tests / 4 failures: B/2 was skipped, conflicting identity was silently accepted, batch time used a new clock value, and finalization did not pass its publishedTime to the follow-up snapshot`

Correction: removed the iteration-number guard because a formally published B/2 or B/3 is still a publication; check-in remains side-effect-free because `DccControlledFileQueryServiceImpl` has no publication-followup dependency. Existing batches now compare tenant, published file, Master, previous ACTIVE, project/category/directory/taxonomy context, file identity, version and published time before idempotent return. Finalization creates one second-normalized `publishedAt`, writes it to the ACTIVE version and passes the same value to the batch.

`GREEN: focused P1 review correction command -> PASS, 9 tests / 0 failures`

`GREEN: mvn -o -pl yudao-module-dcc '-Dtest=DccControlledFileAssignmentScopeServiceTest,DccPublicationFollowupServiceTest,DccPublicationFollowupSchemaTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileRelatedFileServiceTest,DccControlledFileWorkflowServiceImplTest,DccControlledFilePublicationFlowTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> PASS, 252 tests / 0 failures`

Final P1 blocker: none at code/static-contract level. Runtime migration first/repeat execution remains intentionally deferred because this executor was not authorized for database writes.

## P1 Independent Tester Transaction-Gate Correction

`BDD: 后续子快照失败回滚发布 -> Given H2 中 A/1 ACTIVE、B/1 READY_TO_PUBLISH 且 Master 指向 A/1，When 实际 applyApprovedPublishControlledFile 在真实事务中更新替代关系后被注入子快照插入失败，Then 主事务回滚，A/1 仍 ACTIVE、Master 仍指 A/1、B/1 由独立失败事务标为 FINALIZATION_FAILED、后续批次和子快照零残留且 completion event 不发送`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationFollowupTransactionIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL before scenario setup, BaseDbUnitTest does not auto-register JdbcTemplate; corrected the test harness to construct JdbcTemplate from the real injected DataSource before capturing the target RED`

`RED: same transaction integration command -> FAIL, H2 controlled-file Master fixture lacks the new lifecycle identity columns and all publication-followup tables, so real persistence cannot reach the injected child failure; expected fixture gap confirms the prior test suite could not prove transaction rollback`

Harness progression: after adding the H2 lifecycle/follow-up fixture, the real path exposed a null-department lookup defect before the injected child failure (`Map.of().get(null)` in visibility enrichment). The minimal production correction treats an absent formal department ID as an absent department snapshot; it does not invent a department or relax any visibility rule.

### Bug

P1 previously had only mock-based transaction evidence, so it did not directly prove that the production finalization transaction rolls back A/1 supersession, B/1 activation, Master pointer replacement and the partially inserted follow-up ledger together.

### Expected

A child snapshot insert failure must leave A/1 ACTIVE, keep the Master pointer on A/1, leave no follow-up batch or child rows, avoid the completion event, and allow only the existing independent failure transaction to mark B/1 `FINALIZATION_FAILED`.

### Reproduction

`mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationFollowupTransactionIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=true' test`

### Root Cause

The production boundary was already programmatic-transaction backed, but the P1 H2 fixture lacked both the Windchill Master identity columns and seven follow-up tables, and no test invoked the real finalization service with real persistence. After adding that harness, an independent null-department enrichment bug prevented reaching the injected child failure and was corrected without inventing department data.

### GREEN:

`GREEN: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationFollowupTransactionIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> PASS, 1 test / 0 failures; injected visibility-user insert failure reached and every rollback assertion passed`

`GREEN: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationFollowupTransactionIntegrationTest,DccControlledFileAssignmentScopeServiceTest,DccPublicationFollowupServiceTest,DccPublicationFollowupSchemaTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileRelatedFileServiceTest,DccControlledFileWorkflowServiceImplTest,DccControlledFilePublicationFlowTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> PASS, 253 tests / 0 failures`

`GREEN: mvn -o -pl yudao-module-dcc -DskipTests compile -> PASS`

`GREEN: python -X utf8 -m pytest script/tests/test_dcc_publication_followup_sql.py -q -> PASS, 3 tests`

### Verification

- The harness uses the real H2 `PlatformTransactionManager`, `TransactionTemplate`, controlled-file Mapper, Master Mapper, follow-up batch Mapper and child snapshot Mappers.
- Only external document, permission, identity and audit ports are mocked; the visibility-user Mapper delegates to real persistence except for the deterministic injected failure.
- Database assertions cover A/1, B/1, Master and all seven follow-up tables, and the completion adapter is verified not called.
- Added files: `DccPublicationFollowupTransactionIntegrationTest.java`; updated H2 `create_tables.sql` and `clean.sql`; minimally corrected `DccPublicationFollowupServiceImpl.java` null-department enrichment.

### Blockers

No P1 code or local test blocker remains. No Git, real database, E2E, or service restart action was performed.

`GREEN: bug-regression evidence validator -> PASS`

`GREEN: transaction-correction task-owned git diff --check -> PASS (line-ending warnings only)`

## P1 Independent Gate

`GREEN: independent tester rerun -> PASS, transaction integration 1/1; P1 suite 253 tests / 0 failures; SQL contract 3 passed; 12-file migration closure passed; compile and scoped diff-check passed`

`GREEN: P1-AC1 through P1-AC5 -> completed; business AC-01, AC-02, AC-03, AC-06, AC-07, AC-14 and AC-16 verified`

Project experience consolidation: added the reusable publication-followup transaction and business-audience snapshot gate to `docs/backend-development.md`; no new long-term document was created.

`GREEN: git commit/push -> PASS, P1 implementation, tests, migration, independent evidence, task state and consolidated experience committed as b1e08ba6b and pushed to origin/int_main; unrelated concurrent changes were excluded`
