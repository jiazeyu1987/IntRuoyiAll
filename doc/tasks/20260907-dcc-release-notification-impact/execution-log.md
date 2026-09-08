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

`CORRECTION: backend-api validator run against execution-log.md was only a local format check and is not formal backend-api evidence; formal evidence remains main-Agent owned.`

`CORRECTION: database-schema validator run against execution-log.md was only a local format check and is not formal database-schema evidence; formal evidence remains main-Agent owned.`

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

`GREEN: Git implementation commit/push -> PASS, P2 implementation, migration, tests, independent evidence, task state and consolidated experience committed as 2d3149d70 and pushed to origin/int_main; unrelated working-tree changes were excluded`

## P3 Executor Pass: Idempotent Notification And Frontend Entry

`BDD: 发布提交后按责任收件人独立发送 -> Given 发布事务已写入 ACTIVE 通知候选及多个收件原因且另有 INACTIVE/MISSING 审计候选，When 主事务提交，Then 只为 ACTIVE 候选建立一人一条 delivery 并在 afterCommit 调用非事务 orchestrator；每个收件人的 attempt、平台幂等发送和 DCC ACK 分属独立提交边界，一个失败不阻塞其他收件人且不改变文件 ACTIVE`

`BDD: 平台消息落库后 ACK 失败可幂等恢复 -> Given 平台已按 batch+user 稳定 businessKey 提交站内信，但 DCC SENT ACK 独立事务失败，When 文控填写原因重试同一 PENDING/FAILED delivery，Then 再次调用 sendSingleMessageIdempotentlyToAdmin 返回同一 messageId，平台消息仍只有一条，DCC 最终 CAS 为 SENT；SENT 不可再次发送或回退`

`BDD: 通知状态和关键动作可审计 -> Given delivery 处于 PENDING 或 FAILED，When 自动发送、失败或文控重试，Then 尝试次数、发送时间、脱敏错误、平台 messageId 和 rowVersion 由条件更新维护，attempt/sent/failed/retry 写入不可变 audit；空原因、旧版本、越权或非法状态零副作用`

`BDD: 发布后续查询沿用当前文件权限 -> Given 用户从站内通知或文件详情查询某发布批次，When 当前 VIEW 仍允许，Then 后端按批次聚合可见来源、收件原因、关系方向和影响任务并返回；权限已撤销时复用 controlled-file detail 授权明确拒绝，消息与历史快照不成为授权来源`

`BDD: 我的评估与文控全量查询权限隔离 -> Given 普通负责人和文控管理员分别打开工作台或发布后续管理页，When 查询分页，Then 普通用户只看到 assignee 为自己的影响任务，文控全量入口同时通过 Controller 注解及 Service 的 doc_control+approve 双校验；分页一行一任务或一批次且一对多原因/方向由后端聚合`

`BDD: 前端入口保留字符串身份和完整状态 -> Given 后端返回超过 JavaScript 安全整数范围的 batch/task/file/message ID，When 用户在详情“发布后续”、工作台“我的影响评估”或文控管理页查看、筛选、重试和跳转，Then API 类型、路由、比较和 payload 全程使用十进制 string；页面明确展示 loading/empty/error/permission，桌面与窄屏内容不重叠且操作具备可访问名称`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationNotificationServiceTest,DccPublicationNotificationDispatchOrchestratorTest,DccPublicationFollowupQueryServiceTest,DccPublicationFollowupControllerTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL at testCompile, expected notification delivery/audit DO and Mapper, independent transaction worker, non-transactional orchestrator, materialization/retry service, follow-up query service/API and formal error contracts did not exist`

`RED: python -X utf8 -m pytest script/tests/test_dcc_publication_notification_sql.py -q -> FAIL, 3 tests / 3 failures, expected 20260907_dcc_publication_notification.sql delivery/audit schema plus template/menu/permission migration did not exist`

`RED: node tests/e2e/dcc-release-impact-workbench-static.spec.js -> FAIL, ENOENT for publicationFollowup.ts; expected my-impact workbench API/section and publication-followup management view did not exist`

`RED: node tests/e2e/dcc-detail-publication-followup-static.spec.js -> FAIL, ENOENT for publicationFollowup.ts; expected detail follow-up API/section did not exist`

`GREEN: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationNotificationServiceTest,DccPublicationNotificationDispatchOrchestratorTest,DccPublicationFollowupQueryServiceTest,DccPublicationFollowupControllerTest,DccPublicationFollowupServiceTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> PASS, initial backend slice 15 tests / 0 failures`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationNotificationTransactionIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL, H2 reported dcc_publication_notification_delivery missing before P3 fixture; expected real RE behavior could not execute until delivery/audit fixtures existed`

`RED: python -X utf8 -m pytest script/tests/test_dcc_publication_notification_sql.py -q -> FAIL, 2 tests / 2 failures after contract hardening: migration lacked platform business-key dependency, used invalid ${...}/non-JSON template syntax and created an orphan tenant package instead of extending real DCC packages`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationNotificationPostCommitSchedulerTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL, 2 tests: no-transaction scheduling sent immediately instead of failing fast, and afterCommit infrastructure failure propagated to the publication caller`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationFollowupStatusServiceTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL at testCompile, expected authoritative PENDING/PROCESSING/READY/PARTIAL_FAILED/COMPLETED batch status derivation service did not exist`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationFollowupQueryServiceTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL at testCompile with 10 expected contract gaps: database pages/all filters, current-page child aggregation, candidate name/department, structured visibility users and relation directions were missing`

`RED: node tests/e2e/dcc-publication-notify-navigation-static.spec.js -> FAIL, existing station-message navigation had no DCC publication target, did not validate detail/:id and had no string-safe viewer route or inbox quick action`

`RED: python -X utf8 -m pytest script/tests/test_dcc_publication_notification_sql.py -q -> FAIL, 1 of 3 tests after authorization hardening: role-menu migration still copied every category-management role instead of tenant-scoped active doc_control roles already holding approve permission`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationNotificationDispatchOrchestratorTest,DccPublicationNotificationSchemaTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL, 4 tests: null dispatch list was silently accepted and batch/delivery/task locking current-read methods did not exist`

### P3 Implementation

Feature: P3 delivers idempotent DCC publication notifications plus the controlled-file detail, personal impact-assessment workbench and document-control publication-followup management entry points. Non-goals remain historical backfill, external channels, schedulers and automatic related-file version creation.

Acceptance: P3-AC1 through P3-AC5 and business AC-03, AC-04, AC-05, AC-09, AC-14, AC-15 and AC-17 are covered by the backend, schema, transaction, static UI and type evidence below. Real runtime acceptance AC-18 remains P4.

- Added an additive delivery/audit ledger only for P1 `resolutionStatus=ACTIVE` candidates. INACTIVE/MISSING candidates remain immutable audit candidates and never become PENDING deliveries.
- The publication transaction materializes the unique batch+candidate delivery and registers an afterCommit callback. Scheduling outside an active publication transaction fails fast. The callback invokes a non-transactional orchestrator; attempt, SENT ACK and FAILED ACK use a separate bean with `REQUIRES_NEW`, so platform commit and DCC ACK cannot accidentally join the publication transaction or each other.
- Every platform call uses `sendSingleMessageIdempotentlyToAdmin` with stable `DCC_PUBLICATION:{batchId}:USER:{userId}`. Tests cover platform commit followed by ACK failure, FAILED rowVersion progression, retry with the same business key/message ID, and SENT no-replay.
- Batch status recomputation first locks tenant+batch, then uses locking current reads for all deliveries/tasks. Notification ACK/failure and P2 task transitions lock the batch before changing a child, eliminating stale MySQL RR aggregation and lock-order inversion. Real H2 concurrency proves all-SENT becomes COMPLETED and any FAILED becomes PARTIAL_FAILED.
- Follow-up APIs use database pagination and all approved filters. Only current-page batch/task IDs feed child queries. Detail authorization calls the existing controlled-file detail service before returning immutable visibility rules/users, candidate identity and reasons, directions and impact tasks. Management APIs enforce doc_control + approve + publication-followup manage in Controller and Service.
- The detail panel, workbench impact section and publication-followup management page expose loading, empty, error and responsive states. DCC IDs remain decimal strings. Station-message navigation accepts only the same-origin `/dcc/controlled-file/detail/<decimal>?viewer=1&from=notification` target and passes IDs without Number conversion.
- Migration depends on both P2 and the platform message business-key schema, uses the formal `{param}` template syntax/JSON params/nickname, verifies menu ID/path ownership, grants the menu only to active tenant doc_control roles already holding approve, and appends the stable menu ID only to existing packages containing the formal DCC source menu.

### P3 GREEN

`GREEN: mvn -o -pl yudao-module-dcc '-Dtest=<P1+P2+P3 23-class suite>' '-Dsurefire.failIfNoSpecifiedTests=true' test -> PASS, 306 tests / 0 failures / 0 errors`

`GREEN: mvn -o -pl yudao-module-system '-Dtest=NotifyMessageBusinessKeyIdempotencyTest,NotifyMessageSendApiImplTest,NotifySendServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> PASS, 26 platform idempotency tests / 0 failures`

`GREEN: python -X utf8 -m pytest script/tests/test_dcc_publication_followup_sql.py script/tests/test_dcc_publication_impact_assessment_sql.py script/tests/test_dcc_publication_notification_sql.py -q -> PASS, 9 tests`

`GREEN: release migration policy gate through 20260907_dcc_publication_notification.sql, including 20260815 platform idempotency dependency -> PASS, migrationCount=15`

`GREEN: node tests/e2e/dcc-release-impact-workbench-static.spec.js; node tests/e2e/dcc-detail-publication-followup-static.spec.js; node tests/e2e/dcc-publication-notify-navigation-static.spec.js -> PASS, 3 frontend contracts`

`GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json -> PASS`

`GREEN: pnpm exec eslint <P3 API/navigation/detail/workbench/management/router files> -> PASS`

`GREEN: scoped git diff --check -> PASS, line-ending warnings only`

`CORRECTION: the prior claim that backend-api, database-schema and frontend-feature validators against execution-log.md constituted formal evidence is withdrawn. The three formal evidence files remain main-Agent owned and pending refresh; execution-log.md is not a substitute.`

### P3 Scope And Remaining Gate

Task-owned files are the new publication notification migration/SQL contract; delivery/audit DO and Mappers; notification materialization, scheduler, non-transactional orchestrator, REQUIRES_NEW worker, authoritative batch-status and follow-up query services; follow-up Controller/VOs/errors; the minimal P1/P2 status hooks and H2 fixtures/tests; frontend API, detail panel, workbench section, management page, route and station-message navigation; three static contracts; and this log.

No P3 code or local verification blocker remains. Per authorization, no Git operation, real database migration, Playwright/E2E, server restart or remote action was performed. P4 must still apply/verify migrations in real MySQL, run the real page workflow and independent acceptance; those deferred checks are not claimed by P3.

## P3 Review Correction

`BDD: 升版入口由服务端返回权威选项 -> Given 影响任务已决定需要升版，When 负责人打开“开始升版”，Then 后端按同一相关 Master 和当前大版本族返回可选 A/1、A/2 等来源迭代以及唯一开放大版本；存在开放大版本时页面只能填写原因并关联该版本，不存在时才允许选择来源创建，跨 Master、旧 task version 和非负责人继续由 P2 服务拒绝`

`BDD: 我的影响评估默认未完成且可完整翻页 -> Given 当前负责人有超过一页任务且包含流程状态 COMPLETED 的升版跟踪工作，When 不传 taskStatus 打开工作台，Then 后端分页返回 PENDING/UNASSIGNED/IN_REVIEW，以及 COMPLETED+NOT_STARTED/REVISION_LINKED；排除 COMPLETED+NOT_APPLICABLE/RESOLVED，页面提供页码和总数，不静默截断`

`BDD: 页面写动作正确区分取消失败成功 -> Given 用户开始、决定、创建、关联或重试，When 取消原因输入、API 返回业务错误/CAS 冲突或成功，Then 取消不提示系统异常也不清空行，失败在当前区域显示真实错误并保留行，按钮 loading 防止重复点击，只有成功才刷新列表`

`BDD: 发布后续只显示规范中文 -> Given API 返回批次、通知、任务、跟踪、来源和方向内部码，When 详情、工作台或管理页渲染，Then 使用统一中文映射；未知码显示“未知状态（code）”，页面不直接暴露内部码，管理页不展示功能说明副标题`

`BDD: 重复物化必须校验不可变身份 -> Given 同 candidate 已存在 delivery，When 再次物化，Then tenant/batch/candidate/user/businessKey/template 全部一致才幂等返回且不追加 MATERIALIZE audit；任一身份冲突立即失败且不调度发送`

`BDD: 非法管理筛选显式拒绝 -> Given assigneeUserId 非正十进制 Long 或批次/通知/任务/跟踪状态不在正式枚举，When 调用分页 API，Then Bean Validation 或 Service 校验返回参数错误，不把字符串交给 MySQL 隐式转换，也不返回空成功`

`BDD: ACK 丢失后的真实 CAS 演进 -> Given 第一次 attempt 已提交且平台消息 ID 已提交，When DCC SENT ACK 失败后写入 FAILED，再以实际 rowVersion 重试，Then H2 事实依次为 FAILED/version=2 和 SENT/version=4，平台两次请求使用同一 businessKey 并返回同一 messageId；错误字段和日志不包含原始 token/password/secret/Bearer/authorization/密码内容`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccRelatedFileImpactAssessmentServiceTest,DccPublicationNotificationServiceTest,DccPublicationFollowupFilterValidationTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL at testCompile, expected authoritative revision-options domain/API and duplicate-delivery identity verification did not exist`

`RED: python -X utf8 -m pytest script/tests/test_dcc_publication_notification_sql.py -q -> FAIL, 1 of 3 tests, template body still rendered the raw followupUrl instead of keeping it as a hidden navigation parameter`

`RED: node tests/e2e/dcc-release-impact-workbench-static.spec.js; node tests/e2e/dcc-detail-publication-followup-static.spec.js -> FAIL, shared Chinese presentation contract, revision-options/create-or-link dialog, pagination and cancellation/error/loading guards were missing`

### Review Correction Implementation

- Added assignee-authorized revision options. The backend returns either the current revision family's selectable iterations such as A/1 and A/2, or one authoritative open major revision. `create-revision` now revalidates the selected source against those options; an existing open revision blocks creation and the existing P2 same-Master/requester/link contract remains authoritative.
- Workbench revision handling now loads options into a modal. No open major allows explicit source-iteration selection and create; an open major exposes only link-existing. All IDs remain strings and the modal preserves errors without losing the task row.
- My-impact database pagination defaults to non-COMPLETED tasks and the workbench exposes total/page controls. Management and mine filter VOs now validate every status and positive decimal assignee ID; the service converts the ID to Long before SQL and rejects overflow rather than relying on MySQL coercion.
- Start, decision, create, link and retry actions now have per-row loading/duplicate-click guards. Prompt cancellation is recognized without a global error; API/CAS failures remain in the current page/dialog; success is the only branch that refreshes. P3 API calls use local-error ownership to avoid duplicate global errors.
- Added one shared Chinese mapping for batch, notification, task, decision, tracking, VIEW source, relation direction and controlled-file option statuses. Unknown values render `未知状态（code）`. Detail, workbench, management filters/tables and revision modal use the mappings; the management explanatory subtitle was removed.
- The notification template body is now readable Chinese without the raw URL. `followupUrl` remains present only in hidden template parameters and is still strictly validated by notification navigation.
- Duplicate delivery materialization now compares tenant, batch, candidate, user, business key and template before idempotent return. A matching duplicate writes no new MATERIALIZE audit; a conflict fails before scheduling.
- The H2 ACK-loss integration now uses the production REQUIRES_NEW worker: first platform success plus injected ACK failure commits FAILED at rowVersion 2, retry reuses the same platform business key/message ID and commits SENT at rowVersion 4. Persisted error summaries and dispatch logs retain safe failure types only.

### Review Correction GREEN

`GREEN: focused revision-options/filter/materialization/ACK correction suite -> PASS, 42 tests / 0 failures / 0 errors`

`GREEN: P1+P2+P3 adjacent suite after review corrections -> PASS, 312 tests / 0 failures / 0 errors`

`GREEN: platform idempotency suite -> PASS, 26 tests / 0 failures / 0 errors`

`GREEN: P1/P2/P3 SQL contracts -> PASS, 9 tests`

`GREEN: complete migration dependency closure -> PASS, migrationCount=15`

`GREEN: dcc-release-impact-workbench-static + dcc-detail-publication-followup-static + dcc-publication-notify-navigation-static -> PASS, 3 contracts`

`GREEN: vue-tsc relaxed project check -> PASS; targeted P3 ESLint -> PASS; scoped git diff --check -> PASS`

`CORRECTION: the repeated execution-log validator claim is withdrawn for the same reason. Formal backend-api/database-schema/frontend-feature evidence remains pending main-Agent update and is not claimed by this executor.`

One Maven attempt overlapped an unrelated `yudao-server -am package` process, which replaced shared module output during testCompile and produced widespread missing-class errors. No source was changed in response; after that package completed, the identical focused command passed 42 tests and the final adjacent suite passed 312 tests.

Review correction blocker: none. P4 real MySQL migration, service restart, Playwright workflow and independent runtime acceptance remain intentionally unexecuted under the current authorization. No Git operation was performed.

## P3 Final Backend Boundary Correction

`BDD: 负责人筛选必须是严格正 Long -> Given 管理查询传入 Long.MAX_VALUE、Long.MAX_VALUE+1、0、负数或非数字，When Bean Validation 和 Service 解析筛选，Then 只有 1..9223372036854775807 可到达 Long 类型 Mapper；其它输入显式参数错误且不执行 SQL`

`BDD: 升版操作按 Master 当前正式版本族 -> Given 任务冻结 A/1 后 A/1 已 SUPERSEDED，Master 当前正式版本已是 B/1且存在 B/2 WORKING，When 负责人打开 revision-options 或选择 B/2 创建下一大版本，Then 冻结 A/1 只保留展示，操作权威来源为 Master 当前 B/1，选项返回 B/1+B/2且允许现有 workflow 创建 C/1；仍校验同 Master、当前 requester 和唯一开放修订`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationFollowupFilterValidationTest,DccPublicationFollowupQueryServiceTest,DccRelatedFileImpactAssessmentServiceTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL, 30 tests with 2 expected failures: Bean Validation accepted 9999999999999999999, and revision-options still read frozen controlledFileId=200 instead of Master currentActiveControlledFileId=300`

Correction: `DccPublicationFollowupPageReqVO` now validates the decimal string by actual `Long.parseLong` in addition to its positive-digit shape, while `DccPublicationFollowupQueryServiceImpl` keeps the independent service-side positive Long parse before calling a Long-typed Mapper. Tests cover Long.MAX_VALUE, overflow, zero, negative and nonnumeric values.

Correction: revision-options now loads `DccControlledFileMaster.currentActiveControlledFileId`, validates that current row is ACTIVE for the same Master and current requester, then derives the selectable revision family from that current version. The frozen relatedActiveControlledFileId remains unchanged for display/audit. A service test proves frozen A/1 SUPERSEDED with current B/1 and B/2 WORKING returns B/1+B/2, and the existing workflow service creates C/1 from selected B/2.

`GREEN: same focused boundary suite -> PASS, 30 tests / 0 failures / 0 errors`

`GREEN: P1+P2+P3 adjacent final rerun after backend boundary corrections -> PASS, 315 tests / 0 failures / 0 errors`

`GREEN: final frontend gates -> PASS, 3 P3 static contracts and relaxed vue-tsc`

Final backend boundary blockers: none. No Git, database, E2E, restart or remote action was performed.

## P3 Default Work Predicate Correction

`BDD: 已决定需要升版的任务继续留在我的工作 -> Given 负责人提交 REVISION_REQUIRED 后任务为 COMPLETED+NOT_STARTED，或已关联开放大版本后为 COMPLETED+REVISION_LINKED，When 工作台按默认条件刷新，Then 两类任务继续分页返回并分别提供“开始升版”或“等待关联版本发布”；只有 COMPLETED+NOT_APPLICABLE 和 COMPLETED+RESOLVED 从默认工作中消失，显式 taskStatus=COMPLETED 仍按原语义返回全部完成任务`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationNotificationSchemaTest#myImpactPageDefaultsToUnfinishedTasksAtDatabaseBoundary,DccPublicationNotificationTransactionIntegrationTest#defaultMyWorkKeepsOutstandingRevisionTrackingAndExplicitCompletedFilterIsUnchanged' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL, 2 tests / 2 expected failures: Mapper contract lacked revision tracking predicate and real H2 default page returned only PENDING id=20 instead of ids 20/21/22`

Correction: default assignee page predicate is now `(task_status <> 'COMPLETED' OR revision_tracking_status IN ('NOT_STARTED','REVISION_LINKED'))`. Explicit `taskStatus` continues to use exact equality, so `taskStatus=COMPLETED` still returns all completed decisions, including NOT_APPLICABLE and RESOLVED history.

`GREEN: same focused default-work predicate suite -> PASS, 2 tests / 0 failures / 0 errors`

`GREEN: final P1+P2+P3 adjacent regression -> PASS, 316 tests / 0 failures / 0 errors`

`GREEN: frontend default-work/static flow -> PASS, 3 P3 contracts; relaxed vue-tsc -> PASS; scoped diff-check -> PASS`

Default-work predicate blocker: none. No Git, database, E2E, restart, evidence-file or task-state/test-report change was performed.

## P4 Executor Pass: Timeline And Runtime Preflight

`BDD: 发布后续完整时间线按服务端事实排序 -> Given 发布批次包含通知 materialize/attempt/retry/sent/failed 和影响任务 materialize/start/reassign/decide/reopen/link/resolve 审计，When 有权限用户打开文件详情或文控展开批次，Then 后端按当前页 batch IDs 聚合全部事件，以 occurredAt、事件来源和审计主键稳定排序，返回字符串事件/对象 ID、中文动作/状态/决定/关系方向及已脱敏错误摘要；前端只渲染响应，不自行拼接历史`

`BDD: 时间线查询继续执行当前权限 -> Given 普通用户通过通知打开文件详情，When 当前 VIEW 已撤销，Then timeline 查询先由 controlled-file detail 授权拒绝且不读取审计；文控管理页仍需 Controller 和 Service 的 doc_control+approve+manage 三重校验`

`BDD: 时间线页面状态完整 -> Given 时间线加载中、无事件、服务端失败或窄屏设备，When 用户查看详情发布后续或管理批次展开区，Then 页面分别显示 loading/empty/error，事件文本不暴露内部码，移动端内容可滚动且不重叠`

`BDD: P4 真实验收只走页面 -> Given 干净 detached worktree 已构建并完成正式 migration/restart、测试租户账号与两份 DCC-P4-<timestamp> 新文件前置有效，When 后续获准运行 Playwright，Then 发布、站内信查看、两类决定、小版本升大版或关联开放版、文控查看/重试全部由可见页面完成；API/DB 仅在动作完成后只读核验，脚本和证据不保存密码/token`

## P3 Independent Gate

`GREEN: independent tester final rerun -> PASS, P3 focused 27 tests; P1+P2+P3 adjacent 316 tests; platform idempotency 26 tests; all 0 failures/errors`

`GREEN: P1/P2/P3 SQL contracts -> PASS, 9 tests; 15-file migration dependency closure -> PASS`

`GREEN: 3 P3 frontend static contracts, relaxed vue-tsc, targeted ESLint, DCC compile and scoped diff checks -> PASS`

`GREEN: backend API evidence validator -> PASS; database schema evidence validator -> PASS; frontend feature evidence validator -> PASS`

`GREEN: P3-AC1 through P3-AC5 and business AC-03, AC-04, AC-05, AC-09, AC-14, AC-15 and AC-17 -> completed; current phase advanced to P4`

Project experience consolidation: extended the existing publication-followup and station-message idempotency gates in `docs/backend-development.md` with end-to-end work predicates, attempt/platform/ACK transaction separation, post-commit failure isolation and locking batch-status aggregation; no new long-term document was created.

P3 runtime note: real MySQL first/repeat migration, 48081 deployment and Playwright workflow remain P4 gates and are not claimed here. No Git operation was performed under the current authorization.

`GREEN: P3 implementation commit/push -> PASS, P3 backend/frontend/migration/tests, independent evidence, task state and consolidated experience committed as 45817f5c0 and pushed to origin/int_main; unrelated working-tree changes were excluded`

### P4 Timeline RED

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationFollowupQueryServiceTest#getFileFollowup_reusesCurrentDetailAuthorizationAndAggregatesReasonsOnBackend' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL at testCompile, expected timeline response model and tenant-scoped notification/impact audit batch queries did not exist`

`RED: node tests/e2e/dcc-detail-publication-followup-static.spec.js -> FAIL, expected the controlled-file detail follow-up panel and API contract to expose the server-projected timeline`

`RED: node tests/e2e/dcc-release-impact-workbench-static.spec.js -> FAIL, expected the document-control management batch expansion to render the server-projected timeline`

### P4 Timeline Implementation

- Added a backend timeline projection that merges batch creation, notification audits and impact-assessment audits for current-page batch IDs only. Events use string IDs, Chinese action/status/decision/direction labels, persisted sanitized error summaries and stable `occurredAt + source order + audit ID` ordering.
- Reused the current controlled-file detail authorization before any timeline audit read. Existing document-control management Controller and Service permission checks remain unchanged.
- Added one shared frontend timeline component and used it in controlled-file detail and publication-followup management expansion. The component renders the backend sequence without reconstructing history and includes empty, error-summary accessibility and narrow-screen layout states.
- Existing audit tables already contain every required P4 fact, so no additive schema migration was necessary for this slice.
- Added a sanitized runtime preflight at `doc/tasks/20260907-dcc-release-notification-impact/p4-runtime-preflight.md`. It records the future clean-detached-worktree build/migration/restart prerequisites, formal page entry points, `DCC-P4-<timestamp>` data shape, UI-only Playwright path, read-only post-action checks and credential/token redaction gates. No runtime action or Playwright execution occurred.

### P4 Timeline GREEN

`GREEN: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationFollowupQueryServiceTest#getFileFollowup_reusesCurrentDetailAuthorizationAndAggregatesReasonsOnBackend,DccPublicationNotificationTransactionIntegrationTest#timelineQueryUsesRealAuditMappersCurrentViewGuardAndStableCrossSourceOrdering' '-Dsurefire.failIfNoSpecifiedTests=true' test -> PASS, 2 tests / 0 failures / 0 errors`

`GREEN: P1+P2+P3+P4 adjacent DCC suite -> PASS, 318 tests / 0 failures / 0 errors`

`GREEN: mvn -o -pl yudao-module-system '-Dtest=NotifyMessageBusinessKeyIdempotencyTest,NotifyMessageSendApiImplTest,NotifySendServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> PASS, 26 tests / 0 failures / 0 errors`

`GREEN: python -X utf8 -m pytest script/tests/test_dcc_publication_followup_sql.py script/tests/test_dcc_publication_impact_assessment_sql.py script/tests/test_dcc_publication_notification_sql.py -q -> PASS, 9 tests`

`GREEN: release migration dependency closure -> PASS, migrationCount=15`

`GREEN: node tests/e2e/dcc-release-impact-workbench-static.spec.js; node tests/e2e/dcc-detail-publication-followup-static.spec.js; node tests/e2e/dcc-publication-notify-navigation-static.spec.js -> PASS, 3 frontend static contracts`

`GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json -> PASS`

`GREEN: targeted timeline/frontend ESLint -> PASS; scoped git diff --check -> PASS, line-ending warnings only`

`GREEN: P4 preflight redaction and UI-only contract scan -> PASS; no password, token, API-write helper or database-write instruction is persisted`

P4 code-gate blocker: none. Runtime acceptance remains intentionally pending a reviewed commit and clean detached worktree, real MySQL migration, owned runtime restart and UI-only Playwright execution. This executor did not modify runtime data, start or stop services, execute E2E, or perform Git operations. Formal task state, test report, verification report and evidence files remain main-Agent owned and were not modified by this executor.

## P4 Timeline Context Correction

`BDD: 通知时间线保留每次发送次数 -> Given 通知审计包含 ATTEMPT、RETRY、SENT 或 FAILED 的 attemptCount，When 用户查看发布后续时间线，Then 每条对应事件返回并显示本次累计尝试次数；MATERIALIZE 等无发送尝试语义的事件保持为空，不把审计初始化值伪装为发送次数`

`BDD: 转派时间线保留原负责人和新负责人 -> Given REASSIGN 审计冻结 assigneeBefore 与 assigneeAfter，When 用户查看时间线，Then 两个 Long ID 均以字符串返回并明确显示“原负责人/新负责人”；START、DECIDE 等无转派语义事件保持为空`

`BDD: 关联与解决时间线保留精确版本身份 -> Given LINK_REVISION 或 RESOLVE_REVISION 审计冻结 linkedRevisionControlledFileId，When 用户查看时间线，Then 返回并显示精确字符串 ID；仅当任务冻结的关联 ID 与该审计 ID 一致时显示 linkedRevisionVersion，历史审计缺少可匹配版本快照时明确显示版本号未记录，不读取当前其它版本猜测`

`RED: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationFollowupQueryServiceTest#getFileFollowup_reusesCurrentDetailAuthorizationAndAggregatesReasonsOnBackend,DccPublicationNotificationTransactionIntegrationTest#timelineQueryUsesRealAuditMappersCurrentViewGuardAndStableCrossSourceOrdering' '-Dsurefire.failIfNoSpecifiedTests=true' test -> FAIL at testCompile, 22 expected errors because the timeline VO had no attemptCount, assigneeBefore, assigneeAfter, linkedRevisionControlledFileId or linkedRevisionVersion contract`

`RED: node tests/e2e/dcc-detail-publication-followup-static.spec.js; node tests/e2e/dcc-release-impact-workbench-static.spec.js -> FAIL at the first expected contract assertion because the timeline API and shared UI did not expose attempt count, reassignment identities or linked revision identity/version context`

Root cause: the immutable audit rows already stored notification `attemptCount`, reassignment `assigneeBefore/assigneeAfter` and linked revision ID, but the P4 timeline projection only copied common status/reason/message fields. The frontend contract therefore had no way to show the action-specific audit context.

Correction:

- Timeline responses now expose `attemptCount`, `assigneeBefore`, `assigneeAfter`, `linkedRevisionControlledFileId` and `linkedRevisionVersion`. Every Long identity is converted to a decimal string.
- Only ATTEMPT/RETRY/SENT/FAILED project attempt count; MATERIALIZE leaves it null. Only REASSIGN projects assignees and requires a recorded new assignee; START/DECIDE leave reassignment fields null.
- Only LINK_REVISION/RESOLVE_REVISION project the immutable audit revision ID. The task's frozen version string is reused only when its linked ID exactly equals that audit ID; a historical mismatched ID keeps the exact ID and null version instead of guessing from the task's newer linkage.
- The shared timeline UI displays “发送尝试次数”, “原负责人/新负责人” and “关联版本 ID”. Legitimately absent old assignee or linked version is shown as “未记录/版本号未记录”; unrelated events render no fabricated context.
- The real H2 timeline case covers MATERIALIZE, FAILED, SENT, START, REASSIGN, DECIDE, LINK_REVISION and RESOLVE_REVISION, including IDs above JavaScript's safe integer and a historical linked ID whose version must remain null. The service contract directly covers ATTEMPT, RETRY, FAILED and SENT attempt counts.

`GREEN: mvn -o -pl yudao-module-dcc '-Dtest=DccPublicationFollowupQueryServiceTest#getFileFollowup_reusesCurrentDetailAuthorizationAndAggregatesReasonsOnBackend,DccPublicationNotificationTransactionIntegrationTest#timelineQueryUsesRealAuditMappersCurrentViewGuardAndStableCrossSourceOrdering' '-Dsurefire.failIfNoSpecifiedTests=true' test -> PASS, 2 tests / 0 failures / 0 errors`

`GREEN: P1+P2+P3+P4 adjacent DCC suite after the final context assertions -> PASS, 318 tests / 0 failures / 0 errors`

`GREEN: mvn -o -pl yudao-module-system '-Dtest=NotifyMessageBusinessKeyIdempotencyTest,NotifyMessageSendApiImplTest,NotifySendServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=true' test -> PASS, 26 tests / 0 failures / 0 errors`

`GREEN: python -X utf8 -m pytest script/tests/test_dcc_publication_followup_sql.py script/tests/test_dcc_publication_impact_assessment_sql.py script/tests/test_dcc_publication_notification_sql.py -q -> PASS, 9 tests`

`GREEN: complete release migration dependency closure through 20260907_dcc_publication_notification -> PASS, migrationCount=15`

`GREEN: node tests/e2e/dcc-release-impact-workbench-static.spec.js; node tests/e2e/dcc-detail-publication-followup-static.spec.js; node tests/e2e/dcc-publication-notify-navigation-static.spec.js -> PASS, 3 frontend static contracts`

`GREEN: pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json -> PASS; targeted P4 frontend ESLint -> PASS; mvn -o -pl yudao-module-dcc -DskipTests compile -> PASS`

Regression risk is limited to the read-only timeline projection and shared rendering component. No schema change was needed because the formal audit rows already contain the facts. Runtime Playwright acceptance and independent tester re-review remain the P4 gates; no Git, database write, restart or E2E action was performed.
