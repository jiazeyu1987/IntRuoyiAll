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

## P2 Executor: 影响评估任务与升版跟踪

`BDD: 发布事务物化唯一任务 -> Given 发布批次已冻结多个相关 Master，When P1 后续账本在发布事务内完成物化，Then 每个 batch + related Master 只有一个影响任务；有效 requester 为 PENDING，缺失或停用 requester 为 UNASSIGNED，任一任务或审计写入失败使发布事务回滚`

`BDD: 负责人处理决定 -> Given 当前用户是任务负责人且 expectedVersion 匹配，When 开始任务并提交必填原因的 NO_REVISION_REQUIRED 或 REVISION_REQUIRED，Then 任务通过 CAS 完成并写不可变审计；无需升版不改变相关文件，需要升版只进入 NOT_STARTED 而不自动建版`

`BDD: 文控转派与更正 -> Given 操作者同时具有 doc_control 角色和 approve 权限，When 以匹配版本转派有效用户或重新打开已完成任务并填写原因，Then 状态按规则更新且旧决定只保留在审计；缺任一权限、停用用户、空原因或旧版本均零副作用`

`BDD: 复用正式大版本合同 -> Given REVISION_REQUIRED 任务的负责人也是相关文件 requester，When 关联同 Master 唯一开放大版本或调用现有 major-revision 创建后关联，Then 只保存显式版本链接；已有开放大版本时现有 workflow 阻止第二条，跨 Master 或越权关联失败`

`BDD: 关联修订发布后解决 -> Given 任务已关联具体大版本，When 该版本正式发布为 ACTIVE，Then 跟踪状态以 CAS 变为 RESOLVED 并写审计；驳回、撤回或 FINALIZATION_FAILED 不解决任务`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccRelatedFileImpactAssessmentServiceTest,DccPublicationImpactAssessmentSchemaTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL at testCompile, expected P2 impact task/audit DO, Mapper, service methods, relation batch query and error contracts do not exist`

`RED: python -X utf8 -m pytest script/tests/test_dcc_publication_impact_assessment_sql.py -q -> FAIL, 3 tests / 3 failures, expected migration 20260907_dcc_publication_impact_assessment.sql is missing`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccImpactRevisionCommandServiceTest,DccImpactAssessmentArchitectureTest,DccRelatedFileImpactAssessmentServiceTest#materializeForPublicationBatch_lostUniqueInsertRaceDoesNotDuplicateMaterializeAudit+linkExistingMajorRevision_activeOrFailedVersionIsNotAnOpenRevision' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL at testCompile, expected cycle-free revision coordinator did not exist`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationImpactAssessmentControllerTest#managementEndpointsRequireDocControlRoleAndApprovePermission' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL, create-revision only required authentication and did not preserve the existing dcc:controlled-file:submit gate`

### Scope And Contract

P2 adds two structured entities: a versioned impact task unique by publication batch and related Master, and an append-only audit row for every materialize/start/decision/reassign/reopen/link/resolve action. Command endpoints expose start, decision, reassign, reopen, link-existing and create-through-existing-major-revision workflow. No message delivery, frontend page, scheduler or automatic version creation was added.

The state contract is `PENDING/UNASSIGNED -> IN_REVIEW -> COMPLETED`; decisions are `NO_REVISION_REQUIRED/REVISION_REQUIRED`; revision tracking is `NOT_APPLICABLE/NOT_STARTED -> REVISION_LINKED -> RESOLVED`. Every mutation uses request `expectedVersion` plus an SQL CAS predicate.

### Validation And Authorization

- Materialization uses the immutable relation snapshot `responsibleUserIdSnapshot + responsibleUserStatusSnapshot`; it does not reread current user status or silently assign missing/disabled users.
- Start, decision and link require the current task assignee in the service. Reassign/reopen require both formal `doc_control` role and `dcc:controlled-file:approve` permission in Controller and Service; the new assignee must currently exist and be enabled.
- Create-revision preserves `dcc:controlled-file:submit`, uses a cycle-free coordinator, checks task/master/requester/version first, invokes the existing `createMajorRevision` contract, then explicitly links the concrete created revision.
- Existing revision links accept exactly one open workflow revision for the same Master/requester. ACTIVE, REJECTED, WITHDRAWN, SUPERSEDED, OBSOLETE and FINALIZATION_FAILED are not linkable.

### Data, Migration, Safety And Rollback

`20260907_dcc_publication_impact_assessment.sql` is additive, depends on `20260907_dcc_publication_followup`, contains no historical DML, and defines `uk_dcc_pub_impact_master`, row-version CAS data and linked-revision lookup. The H2 fixture mirrors both tables and cleanup removes audit before tasks and tasks before the P1 ledger.

Task materialization is invoked inside the P1 follow-up transaction. The unique insert uses an immutable creation token; only the transaction whose token survives the unique-key race writes the `MATERIALIZE` audit. Existing task identity is compared before idempotent return. Real H2 transaction tests prove audit failure rolls back both task materialization and start-state CAS. A separate real transaction harness proves link CAS failure rolls back the revision row created by the workflow participant. Recovery is rollback of the enclosing transaction; no fallback, compensation, historical backfill or partial-success path exists.

### GREEN:

`GREEN: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationImpactAssessmentSchemaTest,DccRelatedFileImpactAssessmentServiceTest,DccImpactRevisionCommandServiceTest,DccImpactAssessmentArchitectureTest,DccImpactAssessmentTransactionIntegrationTest,DccPublicationFollowupServiceTest,DccPublicationFollowupTransactionIntegrationTest,DccPublicationImpactAssessmentControllerTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> PASS, 36 tests / 0 failures`

`GREEN: mvn -o -pl yudao-module-dcc '-Dtest=DccControlledFileAssignmentScopeServiceTest,DccPublicationFollowupServiceTest,DccPublicationFollowupSchemaTest,DccPublicationFollowupTransactionIntegrationTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileRelatedFileServiceTest,DccControlledFileWorkflowServiceImplTest,DccControlledFilePublicationFlowTest,DccPublicationImpactAssessmentSchemaTest,DccRelatedFileImpactAssessmentServiceTest,DccImpactRevisionCommandServiceTest,DccImpactAssessmentArchitectureTest,DccImpactAssessmentTransactionIntegrationTest,DccPublicationImpactAssessmentControllerTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> PASS, 281 tests / 0 failures`

`GREEN: python -X utf8 -m pytest script/tests/test_dcc_publication_followup_sql.py script/tests/test_dcc_publication_impact_assessment_sql.py -q -> PASS, 6 tests`

`GREEN: release migration policy gate with complete dependency closure through 20260907_dcc_publication_impact_assessment.sql -> PASS, migrationCount=13`

`GREEN: mvn -o -pl yudao-module-dcc -DskipTests compile -> PASS`

### Verification

P2 tests cover unique task materialization, duplicate/lost-race idempotence, snapshot-status assignment, assignee and doc-control authorization, invalid/blank decisions, stale CAS, both decisions, zero controlled-file writes for no-revision, no automatic version for revision-required, cross-Master and closed-version rejection, existing/new major revision linking, publish-only resolution, immutable audit and real transaction rollback.

Task-owned implementation files include the new migration and SQL contract; impact task/audit DO and Mappers; impact state service/interface; cycle-free revision coordinator; command Controller and six request VOs; P1 follow-up/finalization hooks; error codes; H2 fixtures; schema, service, controller, architecture and transaction tests.

### Blockers

No P2 code or local verification blocker remains. The migration was not applied to a real database and no E2E, service restart, Git commit or push was performed under this executor authorization.

Final P2 correction evidence:

- Eliminated the Spring dependency cycle by keeping the impact state service independent of Workflow/Finalization and introducing `DccImpactRevisionCommandService` as the one-way coordinator for `Impact + Workflow`.
- Added transactional annotations to every state/audit mutation and real H2 rollback tests for start/audit, materialize/audit, and workflow-created revision/link failure.
- Materialization now assigns strictly from the frozen relation status and uses creation-token winner comparison before writing the single `MATERIALIZE` audit.
- Closed revisions, including ACTIVE and FINALIZATION_FAILED, cannot be linked as future work; create-revision retains the existing submit permission plus requester/Master checks.

`GREEN: final P1+P2 adjacent rerun -> PASS, 281 tests / 0 failures`

`GREEN: backend-api evidence validator against execution-log.md -> PASS`

`GREEN: database-schema evidence validator against execution-log.md -> PASS`

`GREEN: tracked P2 diff-check and untracked trailing-whitespace scan -> PASS (line-ending warnings only)`

## P2 Resolve Lost-Race Correction

`BDD: 自动解决合法竞争不阻塞发布 -> Given 关联任务在 resolve 查询后被另一 resolver 抢先标记 RESOLVED，或被文控并发 reopen 后不再关联当前修订，When 当前大版本继续完成 ACTIVE 发布，Then CAS update=0 经 tenant-scoped 当前任务复核后视为合法竞争，不重复写 audit且发布事务正常提交；若任务仍原样 REVISION_LINKED 到同一修订则继续 fail fast`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccRelatedFileImpactAssessmentServiceTest#resolveLinkedRevisionAfterPublication_concurrentResolverWinnerDoesNotThrowOrDuplicateAudit+resolveLinkedRevisionAfterPublication_concurrentReopenDoesNotThrowOrWriteAudit+resolveLinkedRevisionAfterPublication_unexplainedCasMissStillFailsFast,DccPublicationFollowupTransactionIntegrationTest#applyApprovedPublishControlledFile_resolveLostRaceStillCommitsActivePublication' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL, 4 tests: both legal CAS lost-races threw PUBLICATION_IMPACT_VERSION_CONFLICT and the real H2 outer publication rolled back into FINALIZATION_FAILED instead of committing ACTIVE`

Root Cause: automatic resolution treated every conditional-update miss as a fatal stale command. Unlike an interactive command, this publication-side projection can legitimately lose to an already committed resolver or reopen. Propagating that expected race through `recordPublishedRevision` incorrectly converted a valid document publication into finalization failure.

Correction: keep the conditional CAS update. On update count zero, perform one tenant-scoped current-task read. Return without audit only when the task is already `RESOLVED` for the same revision or no longer links that revision. A missing task, Master mismatch, or task still `REVISION_LINKED` to the same revision remains `PUBLICATION_IMPACT_VERSION_CONFLICT`; Mapper, database and audit exceptions continue to propagate.

`GREEN: same focused lost-race command -> PASS, 4 tests / 0 failures; includes real H2 finalization showing A/1 SUPERSEDED, B/1 ACTIVE, Master -> B/1, follow-up batch committed and no duplicate impact audit`

`GREEN: P1+P2 adjacent suite with lost-race coverage -> PASS, 285 tests / 0 failures`

`GREEN: P1/P2 SQL contracts -> PASS, 6 tests; 13-file migration dependency closure -> PASS; DCC module compile -> PASS; scoped diff-check -> PASS`

Risk and regression scope: the change applies only to automatic linked-revision resolution after a successful ACTIVE publication. Interactive stale CAS behavior for start, decision, reassign, reopen and link remains fail-fast. No notification, UI, schema, historical data or authorization behavior changed.

Blockers: none at code/local-test level. No Git, real database, E2E or service restart action was performed.

### MySQL Current-Read Hardening

`BDD: 合法竞争复核读取最新已提交状态 -> Given MySQL 默认 REPEATABLE READ 下 resolve 的初次查询已建立事务快照，When CAS update=0 后复核并发 resolver/reopen 的结果，Then 必须使用 tenant-scoped SELECT ... FOR UPDATE 当前读看到最新已提交行；同一 REVISION_LINKED 且仍链接当前修订的异常 CAS miss 继续 fail fast`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationImpactAssessmentSchemaTest#resolveRaceCurrentReadUsesTenantScopedForUpdateQuery' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL, NoSuchMethodException: DccPublicationImpactTaskMapper.selectByIdAndTenantForUpdate; 普通 select 在 MySQL REPEATABLE READ 下不能证明会跳出旧快照`

Correction: CAS update=0 后通过 `DccPublicationImpactTaskMapper.selectByIdAndTenantForUpdate` 执行带 `tenant_id + id + deleted = 0` 条件的 `SELECT ... FOR UPDATE` 当前读。仅已 RESOLVED、已 reopen 或已改链等可解释的更高 rowVersion 状态视为合法竞争；仍为同一 `REVISION_LINKED`、Master 不一致、记录缺失或 rowVersion 未推进均继续抛版本冲突。Mapper/数据库/audit 异常不吞并。

`GREEN: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationImpactAssessmentSchemaTest#resolveRaceCurrentReadUsesTenantScopedForUpdateQuery,DccRelatedFileImpactAssessmentServiceTest#resolveLinkedRevisionAfterPublication_concurrentResolverWinnerDoesNotThrowOrDuplicateAudit+resolveLinkedRevisionAfterPublication_concurrentReopenDoesNotThrowOrWriteAudit+resolveLinkedRevisionAfterPublication_unexplainedCasMissStillFailsFast,DccPublicationFollowupTransactionIntegrationTest#applyApprovedPublishControlledFile_resolveLostRaceStillCommitsActivePublication' '-Dsurefire.failIfNoSpecifiedTests=true' test -> PASS, 5 tests / 0 failures`

`GREEN: P1+P2 adjacent suite after locking-current-read correction -> PASS, 286 tests / 0 failures`

Final correction-owned files: `DccPublicationImpactTaskMapper.java`, `DccRelatedFileImpactAssessmentServiceImpl.java`, `DccPublicationImpactAssessmentSchemaTest.java`, `DccRelatedFileImpactAssessmentServiceTest.java`, `DccPublicationFollowupTransactionIntegrationTest.java`, and this execution log. Blockers: none. No Git, real database, E2E or service restart action was performed.

## P2 Independent Gate

`GREEN: independent tester corrective rerun -> PASS, corrective suite 25 tests; P2 suite 41 tests; P1+P2 adjacent suite 286 tests; all 0 failures/errors`

`GREEN: P1/P2 SQL contracts -> PASS, 6 tests; 13-file migration dependency closure -> PASS; DCC compile and scoped diff checks -> PASS`

`GREEN: backend API evidence validator -> PASS; database schema evidence validator -> PASS`

`GREEN: P2-AC1 through P2-AC5 and business AC-06 through AC-13 -> completed; current phase advanced to P3`

Project experience consolidation: extended the existing publication-followup gate in `docs/backend-development.md` with automatic-projection CAS race classification and MySQL locking-current-read verification; no new long-term document was created.

P2 runtime note: real MySQL first/repeat migration, E2E and service restart remain intentionally deferred to P4 and are not claimed here.
