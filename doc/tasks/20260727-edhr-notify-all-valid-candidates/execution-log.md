# 执行日志

## 用户意图

用户明确要求：站内信应发给所有有效候选人，并按该口径进行设计、开发和验证。

2026-07-27 范围变更：用户在“以目标回归放行”与“修复全部 MES 回归失败”之间明确选择后者。变更请求已记录至 `docs/changes/20260727-mes-full-regression-green.md`，并通过 change-request validator。

PLANNING: `request-analysis.md` / `prd.md` -> APPROVED。规划明确 41 个失败测试套件、2509 tests / 58 failures / 78 errors / 31 skipped 基线、两个真实 fixture 硬阻塞、并发文件冲突约束和 AC-01..AC-17。

PLANNING BLOCKER: 分解代理调用返回 `403 Forbidden`，原因是代理服务余额和订阅额度不足，未产出 `dev-plan.md` / `test-plan.md`。用户在该阻塞后明确要求“继续”；主线程据此完成任务图和测试计划，保留同等验收标准，不切换测试命令、不跳过失败、不伪造 fixture。

PLAN: `dev-plan.md` / `test-plan.md` -> APPROVED。任务图包含 T0..T9，最终验收固定为 `mvn -pl yudao-module-mes test`。

## T1：完整失败清单

状态：completed

- 41 个失败测试套件已逐一映射到 T2-T7，未遗漏、未重复。
- 清单：`failure-inventory.md`。
- 当前可执行任务：T2 静态契约、T3 schema/DB、T6 排产。
- 当前硬阻塞：T0 Word `.doc` 权威原件未发现；Excel 发现两个同哈希候选副本但尚未确认权威性。
- 并发冲突：`IntRuoyiFronted/src/router/modules/remaining.ts` 等前端文件正在被其他任务修改，T2 不得直接覆盖。

## 初始状态

- 工作目录：`E:\IntRuoyi`
- 后端模块：`IntRuoyiBackend\yudao-module-mes`
- 分支：`int_main`
- 既有脏工作区已按项目门禁独立保存为基线提交：`868893b0`
- 基线包含既有发布脚本、前端改动、其他任务文档和静态 E2E 文件；不包含本任务文件。
- `origin`：`https://github.com/jiazeyu1987/IntRuoyiAll.git`

## BDD 场景

BDD: 填写任务通知全部有效候选人 -> Given 一个待办填写任务的候选快照包含多个有效账号，When 创建该工作任务，Then 每个有效候选账号各收到一条填写任务站内信。

BDD: 审核任务通知全部有效候选人 -> Given 一个待办审核任务的候选快照包含多个有效账号且当前任务有一个实际 assignee，When 创建该审核任务，Then 候选快照中的每个有效候选账号各收到一条审核任务站内信。

BDD: 同一任务候选账号去重 -> Given 一个任务候选快照重复包含同一账号，When 发送任务通知，Then 该账号只收到一条站内信。

BDD: 候选来源不混淆 -> Given 填写任务和审核任务拥有不同候选快照，When 分别创建任务，Then 每个任务只按自己的候选快照通知，不把两个任务的候选人合并。

## 里程碑 1：现状与影响范围确认

### 命令意图

- 定位批次创建、工作任务创建和站内信发送入口。
- 核对工艺路线表单槽位、批记录表单和候选快照的现有优先级。
- 核对 Git 状态并保存既有脏改动基线。

### 结果

- 批次创建最终调用 `createInitialFillTask`。
- 工作任务统一在 `MesProEdhrWorkTaskServiceImpl#createTask` 中调用 `sendNotify`。
- 原实现只把 `task.assigneeUserId` 设置为通知收件人。
- 任务已创建，既有脏改动已独立提交为 `868893b0`。

## 里程碑 2：测试先行

状态：completed

RED: mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 3 个用例均因 `sendNotify` 仍只按单一 `assigneeUserId` 调用 `sendSingleMessageToAdmin` 失败：填写任务期望 2 次实际 1 次；审核多候选任务期望 3 次实际 2 次；重复候选去重场景期望 2 次实际 1 次。

## 里程碑 3：后端实现

状态：completed

### 完成内容

- `MesProEdhrWorkTaskServiceImpl#sendNotify` 改为从 `candidateUserSnapshot` 解析候选账号集合。
- 复用既有 `parseCandidateUserIds` / `MesProEdhrWorkTaskAuthorization.parseRequiredCandidateSnapshotUserIds` 逻辑，保持任务内去重和候选快照缺失时 fail-fast。
- 每个候选账号单独调用既有 `notifyMessageSendApi.sendSingleMessageToAdmin`，模板编码、模板参数和 `workTaskId/actionUrl` 保持不变。

## 里程碑 4：验证

状态：blocked

GREEN: mvn -pl yudao-module-mes org.apache.maven.plugins:maven-surefire-plugin:3.5.3:test "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=true" -> PASS, 3 tests run, 0 failures, 0 errors。

GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 3 tests run, 0 failures, 0 errors；Surefire 报告更新时间为 2026-07-27 18:41:27。

REGRESSION: mvn -pl yudao-module-mes org.apache.maven.plugins:maven-surefire-plugin:3.5.3:test "-Dtest=MesProEdhrWorkTaskServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=true" -> PASS, 66 tests run, 0 failures, 0 errors。

COMPILE: mvn -pl yudao-module-mes -am "-DskipTests" compile -> PASS, MES 及依赖模块生产代码编译通过。

CHECK: git diff --check -- IntRuoyiBackend\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProEdhrWorkTaskServiceImpl.java IntRuoyiBackend\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProEdhrWorkTaskServiceImplTest.java doc\tasks\20260727-edhr-notify-all-valid-candidates -> PASS，仅输出 Git 行尾转换 warning，无 whitespace error。

BLOCKED: mvn -pl yudao-module-mes -am test -> FAIL, 上游 `yudao-module-infra` 共 415 tests，38 failures、1 error、10 skipped；MES 模块被 Maven reactor 标记为 SKIPPED。

BLOCKED: mvn -pl yudao-module-mes test -> TIMEOUT after 15 minutes，未产出 2026-07-27 18:44:55 之后的新完整 Surefire 报告；确认进程属于本任务后已终止 PID 59468，未触碰其他服务或任务进程。

BLOCKED: mvn -pl yudao-module-mes test -> FAIL after 38:34，命令于 2026-07-27 20:17:20 完整结束；MES 模块共 2509 tests、58 failures、78 errors、31 skipped。失败涉及排产契约、缺少 `C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`、缺少 `D:\ocr2\resource\球囊扩张导管工序(1).xlsx`、数据库测试上下文及其他既有测试。目标 `MesProEdhrWorkTaskServiceImplTest` 在本次完整运行中为 66 tests、0 failures、0 errors、0 skipped，Surefire 报告更新时间为 2026-07-27 19:46:50。

GREEN: stale blocker revalidation -> `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS at 2026-07-27 20:40:03，3 tests、0 failures、0 errors、0 skipped；24 个 reactor 模块全部 `BUILD SUCCESS`，总耗时 15:35。

EVIDENCE: bug regression validator -> PASS；backend API validator -> PASS。

EVIDENCE: 完整 MES 回归证据更新后再次运行 bug regression validator 与 backend API validator -> PASS；`git diff --check` -> PASS，仅有 Git 行尾转换 warning。

EXPERIENCE: 复核 `docs/experience-index.md` 与 `docs/backend-development.md`，现有“Word fixture 缺失必须阻塞”和“Windows Maven 超时/进程归属”门禁已覆盖本次可复用经验，不新增或修改长期经验文档。

## 里程碑 5：收尾

状态：blocked

### 集成状态

- 并发任务基线提交：`f18927b9e3682a8a66d44d535b24c75b824b40e2`，提交时间 `2026-07-27 18:41:23 +08:00`，主题 `chore: baseline pre-existing dirty worktree`。
- 该提交包含本任务 `MesProEdhrWorkTaskServiceImpl.java`、`MesProEdhrWorkTaskServiceImplTest.java` 以及当时的任务目录文件，并已推送到 `origin/int_main`。
- 本次完整回归结束后的复核确认本地 `HEAD` 与 `origin/int_main` 已对齐，且 `f18927b9` 是两者祖先；共享分支仍由并发任务持续推进，因此不把易过时的后续提交号作为任务完成依据。
- 本次完整回归后的证据文档仍为未提交状态；由于完整模块回归失败，不创建本任务收尾提交或推送。

## 阻塞项

- `-am test` 被非本任务上游 infra 失败阻断，MES 未执行。
- MES 全量单模块测试已形成完整结论，但存在 58 failures、78 errors，完整模块回归未通过。
- 当前共享分支上的目标行为已再次通过标准 reactor 生命周期；该结果不能替代完整模块回归，也不能静默放宽任务门禁。
- 按项目门禁，本任务不提交、不推送、不标记 `ready_for_closeout` 或 `completed`。

## T2：修复静态契约与工程路径漂移

- `task_id`: T2
- `acceptance_ids`: AC-04、AC-05、AC-13、AC-14、AC-16、AC-17
- 改动路径：
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProEdhrFormFillLogMenuContractTest.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProEdhrTemplateConfigMenuRemovalContractTest.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProBatchRecordVersionPhaseTwoMigrationContractTest.java`

BDD: 静态菜单契约读取当前正式前端工程 -> Given MES 静态契约需要读取前端路由和共享批记录组件，When 在当前工作区执行契约测试，Then 测试必须从 `IntRuoyiFronted` 完整读取既有目标文件并保留全部原断言。

BDD: 草稿重新上传契约匹配当前正式识别重载 -> Given 正式 `recognizeUploadedRoute` 调用包含目标版本参数槽位，When 静态契约核对重新上传调用，Then 必须精确匹配 `oldVersion.getSourceVersionId(), null, productNames, true, List.of(), productNames`，不得改为宽松包含或减少参数约束。

RED: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrFormFillLogMenuContractTest,MesProEdhrTemplateConfigMenuRemovalContractTest,MesProBatchRecordVersionPhaseTwoMigrationContractTest" test` -> FAIL，2026-07-27 21:37:12 +08:00 完成；10 tests、2 failures、1 error、0 skipped。`MesProEdhrFormFillLogMenuContractTest` 因读取不存在的 `E:\IntRuoyi\yudao-ui-admin-vue3\src\router\modules\remaining.ts` 报 `NoSuchFileException`；`MesProEdhrTemplateConfigMenuRemovalContractTest` 因旧根目录下找不到 `DesignerWrapper.vue` 断言失败；`MesProBatchRecordVersionPhaseTwoMigrationContractTest` 因仍断言旧参数序列、缺少 `null` 目标版本参数而失败。

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrFormFillLogMenuContractTest,MesProEdhrTemplateConfigMenuRemovalContractTest,MesProBatchRecordVersionPhaseTwoMigrationContractTest" test` -> PASS，2026-07-27 21:42:06 +08:00 完成；10 tests、0 failures、0 errors、0 skipped，`BUILD SUCCESS`，总耗时 04:22。

- 完成内容：两个静态前端工程根目录契约由废弃的 `yudao-ui-admin-vue3` 精确更新为当前正式根目录 `IntRuoyiFronted`；扫描目标、文件存在性检查和菜单/路由/共享组件断言均未减少。
- 完成内容：重新上传静态源码断言精确更新为当前正式重载参数序列，保留完整 `contains` 字符串匹配，未改为拆分或宽松断言。
- 风险/阻塞：当前前端文件存在并发改动，本任务仅只读验证其正式路径，未修改任何前端文件；T2 定向契约无阻塞。完整 MES 回归及其他 T0/T3-T9 阻塞不属于本执行代理范围，不能由本次 10 个契约测试替代。

## T3：修复 schema、Spring 测试上下文和 H2 隔离

- `task_id`: T3
- `acceptance_ids`: AC-05、AC-06、AC-07、AC-13、AC-14、AC-17

BDD: 正式迁移与 H2 schema 同步覆盖执行表 -> Given `MesProBatchRecordExecutionDO` 已引用批记录定义、版本及后续运行态字段，When schema 契约聚合正式非破坏性 migration 和 H2 测试 schema，Then 两侧必须完整覆盖 DO 字段且 migration 不包含删除 MES 数据或破坏性表操作。

BDD: Spring DB 测试显式装配新增依赖 -> Given 被测服务新增了正式协作服务依赖，When 启动各自最小 Spring 测试上下文，Then 测试必须通过精确 `@MockitoBean` 提供该依赖，不得使用 lenient 新桩、fallback 或扩大生产装配范围。

BDD: 删除后重导不残留活动 BATCH 流程配置 -> Given 同一批记录已导入、删除并释放其路线流程配置，When 在同一测试及连续独立测试进程中重新导入，Then 活动 BATCH 配置唯一键不得碰撞且两次运行均通过。

RED: `mvn -pl yudao-module-mes "-Dtest=MesBatchRecordBaseSchemaTest,BalloonProcessDeviceMappingImportServiceImplTest,MesProBatchRecordExecutionFieldAuditQueryExportServiceTest,MesProBatchRecordReportRenameServiceImplDbTest,MesProBatchRecordReportServiceImplDbTest,ThirdPartyFeedbackImportServiceImplDbTest,IntGyRouteMarkdownImportServiceImplDbTest,Sheet1RouteExcelImportServiceImplDbTest" test` -> FAIL，2026-07-27 21:51:57 +08:00 完成；131 tests、1 failure、22 errors、0 skipped。schema 契约首个失败为 runtime 聚合未识别 `mes_pro_batch_record_execution.batch_record_definition_id`；六个 Spring 测试上下文分别缺少 `MesProRouteProcessService`、`MesProEdhrPreReleaseEditabilityService`、`MesProBatchRecordRouteGenerationService`、`MesProRouteOwnerPermissionService`。`MesProBatchRecordReportServiceImplDbTest` 在本次干净组合中通过，唯一键问题按跨用例污染继续做连续运行验证，不关闭或放宽唯一约束。

### T3 实施与验证结果

- schema 核对：`MesProBatchRecordExecutionDO.batchRecordDefinitionId` 已由正式 migration `sql/mysql/20260708_mes_batch_record_version_phase_one.sql` 通过幂等 helper 非破坏性新增，H2 `src/test/resources/sql/create_tables.sql` 已包含同名列；未创建重复 migration，未修改真实数据库。
- schema 契约修复：`MesBatchRecordBaseSchemaTest` 纳入 `20260708_mes_batch_record_version_phase_one.sql`、`20260720_mes_batch_shared_form_binding.sql`、`20260722_mes_recordbook_batch_controlled_sync.sql`，并精确识别 `add_*_column_if_missing` / `add_*_column_if_table_exists` 的表名和列名参数；未删除 DO 字段断言或非破坏性检查。
- Spring 测试装配：为目标测试补齐精确接口级 `@MockitoBean`，包括 `MesProRouteProcessService`、`MesProEdhrPreReleaseEditabilityService`、`MesProEdhrGoldenFingerPermissionService`、`MesProEdhrRecordbookGlobalSettingService`、`MesProBatchRecordRouteGenerationService`、`MesProBatchRecordFormProfileRegistry`、`BusinessApprovalOrchestrator`、`MesProBatchRecordVersionBusinessApprovalEffectExecutor`、`MesProRouteOwnerPermissionService`；未新增 lenient 桩。
- 范围偏差纠正：曾短暂将 `Sheet1RouteExcelImportServiceImplDbTest` 改为程序生成最小 xlsx；收到用户纠正后已完整撤销该合成 fixture 改动。该文件最终仅保留正式路径和新增的 `MesProRouteOwnerPermissionService` 测试 Bean。
- 改动路径：
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesBatchRecordBaseSchemaTest.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/md/workstation/importer/BalloonProcessDeviceMappingImportServiceImplTest.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionFieldAuditQueryExportServiceTest.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportRenameServiceImplDbTest.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/importer/ThirdPartyFeedbackImportServiceImplDbTest.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/importer/IntGyRouteMarkdownImportServiceImplDbTest.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/importer/Sheet1RouteExcelImportServiceImplDbTest.java`

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesBatchRecordBaseSchemaTest,BalloonProcessDeviceMappingImportServiceImplTest,MesProBatchRecordReportRenameServiceImplDbTest,MesProBatchRecordReportServiceImplDbTest,ThirdPartyFeedbackImportServiceImplDbTest,IntGyRouteMarkdownImportServiceImplDbTest" test` -> PASS，2026-07-27 22:04:37 +08:00 完成；122 tests、0 failures、0 errors、0 skipped，`BUILD SUCCESS`。

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenDeletedBatchRecordReimported_startsFromV1Again" test` -> PASS，第一次于 2026-07-27 22:03:26 +08:00 完成；1 test、0 failures、0 errors、0 skipped。

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenDeletedBatchRecordReimported_startsFromV1Again" test` -> PASS，第二次于 2026-07-27 22:03:50 +08:00 完成；1 test、0 failures、0 errors、0 skipped。连续独立运行未出现 active BATCH route flow config 唯一键碰撞。

BLOCKED: `mvn -pl yudao-module-mes "-Dtest=Sheet1RouteExcelImportServiceImplDbTest" test` -> FAIL，2026-07-27 22:05:01 +08:00 完成；1 test、0 failures、1 error、0 skipped。Spring Context 已成功启动，当前唯一错误为权威路径 `D:\ocr2\resource\球囊扩张导管工序(1).xlsx` 不存在；该前置属于 T0，禁止使用合成文件、候选副本或跳过测试绕过。

BLOCKED: `MesProBatchRecordExecutionFieldAuditQueryExportServiceTest` 的 Spring Context 已成功启动，但 `responsibilityExportRejectsIncompleteOverallEvidence` 与 `responsibilityExportRejectsBlockedEvidenceAndPreservesUnknownSummaryOrigin` 仍失败。根因为范围外产品类 `MesProBatchRecordExecutionFieldResponsibilityService#export` 当前对 `EVIDENCE_MISSING` / `BLOCKED` 仍生成工作簿，未按既有强断言 fail-fast；T3 白名单不允许修改该产品文件，因此未弱化断言、未吞异常、未扩展范围。

REGRESSION: 原八类组合命令于 2026-07-27 22:05:49 +08:00 重跑 -> FAIL；131 tests、2 failures、1 error、0 skipped。其余 128 tests 通过，剩余失败精确为上述两个范围外导出 fail-fast 缺口和一个 T0 权威 Excel fixture 缺失。

CHECK: 任务文件 `git diff --check` 在并发 checkpoint 前通过，无 whitespace error。执行期间共享工作区被并发任务创建提交 `219169b7`（`chore: checkpoint concurrent workspace changes`），该提交包含本轮代码改动及其他任务文件；本执行代理未创建、未修改、未推送该提交。

状态：blocked。T3 不得标记 completed；解除条件为 T0 提供并确认权威 Excel fixture，以及获得范围授权后修复 `MesProBatchRecordExecutionFieldResponsibilityService#export` 的不完整/阻塞证据 fail-fast 行为。

## Supervisor：T3 独立门禁与根因重路由

- T3 独立测试确认 schema 契约、Spring 测试 Bean 装配和 H2 唯一键隔离均通过：可验证子集 122 tests 全绿，删除后重导用例连续两次 1/1 通过。
- `MesProBatchRecordExecutionFieldAuditQueryExportServiceTest` 的 Spring Context 已恢复，剩余两个失败属于 `MesProBatchRecordExecutionFieldResponsibilityService#export` 产品行为，正式转入 T4；原强断言保留。
- `Sheet1RouteExcelImportServiceImplDbTest` 的 Spring Context 已恢复，剩余唯一错误是权威 Excel 路径缺失，正式转入 T7；禁止合成 fixture、候选副本替代或跳过。
- 据此将 T3 标记 completed，并释放依赖 T3 的 T4、T5；任务整体仍受 T0 和后续 T4-T9 阻塞。

## T6：修复自动排产与排产订单契约簇

- `task_id`: T6
- `acceptance_ids`: AC-08、AC-11、AC-13、AC-17

BDD: Gantt 预览工单编码契约跟随正式转换边界 -> Given 自动排产预览由 `MesProAutoScheduleServiceImpl` 组织工单、由 `SchedulePlanner.PreviewStep` 转换任务行，When 静态契约核对工单编码传递，Then 必须分别验证调用方传入正式工单编码和真实转换方法写入 `workOrderCode`，不得为匹配字符串把转换代码搬回服务类。

BDD: 自动排产使用排产工单冻结路线版本 -> Given 排产工单已经冻结 `routeVersionId`，When 预览或应用自动排产，Then 测试必须精确装配对应激活路线版本及版本工序，不得回退到当前路线或缺失版本的默认成功。

BDD: 排产准入只读取启用工作站 -> Given 工序存在工作站资源准入检查，When 构建资源快照，Then Mapper 桩必须精确匹配 `selectListByProcessIds(processIds, ENABLE)`，不得使用 lenient 或宽 matcher 隐藏正式调用。

BDD: 过量报工保留数量证据且进度封顶 -> Given 已归属报工数量超过计划数量，When 同步排产工单进度，Then 工序快照必须保留 reported/overReported 数量，进度百分比必须精确封顶为 100。

BDD: 无默认资源配置继续 fail-fast -> Given 排产资源缺少正式班次小时或人员数量，When 排产或维护资源，Then 写入链路必须继续按正式错误码阻塞，资源查询服务按当前只读契约核对，不得引入默认配置或默认成功。

BDD: 手工覆盖产能优先于缺失人员数量 -> Given 工序使用正式手工覆盖小时产能且人员数量缺失，When 汇总工序在制负荷，Then 资源状态必须按手工覆盖语义保持 `NORMAL`，不得把缺失人员数量错误覆盖为 `CAPACITY_MISSING`。

RED: `mvn -pl yudao-module-mes "-Dtest=MesProTaskGanttWorkOrderCodeContractTest,MesProAutoScheduleAlgorithmContractTest,MesProAutoScheduleContractTest,MesProScheduleOrderAdmissionTest,MesProScheduleOrderFourRiskContractTest,MesProScheduleOrderNoDefaultConfigContractTest,MesProScheduleOrderServiceImplTest" test` -> FAIL，2026-07-27 22:18:41 +08:00 完成；99 tests、5 failures、22 errors、0 skipped。失败根因分别为 Gantt 契约仍在错误的服务类中查找 `PreviewStep` 转换签名；算法测试未装配冻结 `routeVersionId`/版本对象；契约测试缺少精确 `routeVersionMapper`；准入测试仍桩旧的一参数工作站查询；过量报工仍期待 170% 而正式实现封顶 100%；无默认配置契约仍要求只读资源服务包含已移除错误码；手工覆盖产能被缺失人员数量错误标记为 `CAPACITY_MISSING`。

## T4：修复工艺路线与 eDHR 契约簇

- `task_id`: T4
- `acceptance_ids`: AC-08、AC-09、AC-12、AC-13、AC-17

BDD: 路线服务测试精确装配正式依赖 -> Given 路线版本、复制和展示服务已经依赖路线负责人权限服务及受控内容平台适配器，When 在严格 Mockito 下运行路线契约，Then 测试必须按当前构造器和字段边界精确提供依赖，不得使用 lenient、宽泛 matcher 或 mock success。

BDD: 责任证据不完整时导出 fail-fast -> Given 批记录责任证据汇总状态为 `EVIDENCE_MISSING` 或 `BLOCKED`，When 请求生成责任证据导出工作簿，Then 服务必须使用既有错误码立即拒绝，不得继续创建工作簿或吞掉证据缺口。

BDD: eDHR legacy 与当前执行均满足正式冻结前置 -> Given 批次执行需要租户、冻结路线快照、附件负责人、填写规则和逐工序批记录表单绑定，When 打开 legacy 或当前 eDHR 执行链路，Then 测试必须显式提供这些正式前置，缺失配置继续 fail-fast，不得默认成功。

BDD: 三类工艺路线来源保持独立 -> Given 工序开始上传人、工序设置中的逐工序批记录表单、表单槽位 `formBindings` 分别存在，When 生成路线显示字段或批次执行快照，Then 每条链路只读取自己的正式来源，不得互相替代、补齐或推断。

BDD: eDHR 通知继续覆盖全部有效候选人 -> Given 每个工作任务的 `candidateUserSnapshot` 包含一个或多个有效候选账号且可能重复，When 创建任务并发送站内信，Then 每个任务内的全部有效候选账号各收到一次通知，且不跨任务合并候选人。

RED: `mvn -pl yudao-module-mes "-Dtest=MesProRouteVersionAndCopyTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProEdhrBatchExecutionLegacyProcessTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrBatchExecutionTaskGateTest,MesProEdhrRehearsalReadinessServiceTest,MesProEdhrWorkTaskLegacyProcessTest,MesProBatchRecordExecutionFieldAuditQueryExportServiceTest,MesProRouteProcessServiceImplBatchRecordBindingTest,MesProRouteServiceImplDisplayFieldsTest,MesProRouteServiceImplTest" test` -> FAIL，2026-07-27 22:41:15 +08:00 完成；242 tests、11 failures、17 errors、0 skipped。首个真实根因为 `MesProRouteVersionAndCopyTest` 未按当前服务依赖注入 `MesProRouteOwnerPermissionService`，导致创建/复制路线时空指针；其余失败包括 `platformAdapter` 缺失、两处不再使用的 strict stubs、legacy/eDHR 测试缺失租户/冻结附件负责人/填写规则/逐工序批记录绑定前置、责任证据 `EVIDENCE_MISSING`/`BLOCKED` 未 fail-fast，以及归档、任务门禁、路线快照和详情字段的当前行为断言漂移。

## T6：GREEN 与高风险回归证据

- `task_id`: T6
- `acceptance_ids`: AC-08、AC-11、AC-13、AC-17

RED: `mvn -pl yudao-module-mes "-Dtest=MesProAutoScheduleAlgorithmContractTest#preview_shouldBlockNightShiftProcessWhenOnlyDayCapacityExists+preview_shouldBlockNightShiftProcessWhenLineHasOnlyDayShift+apply_shouldRefreshAndPersistNightShiftFromProductRouteConfigBeforeScheduling,MesProScheduleOrderAdmissionTest#createFromWorkOrder_shouldRejectMissingShiftHours" test` -> FAIL，2026-07-27 22:32:45 +08:00 完成；4 tests、2 failures、1 error、0 skipped。两个夜班用例实际被计划产能自动扩展耗尽后的通用“向后搜索 3660 天”消息覆盖，冻结路线版本刷新用例的容量指标桩错误地包含了手工有限产能工作站，Admission 缺班次小时 fail-fast 用例已通过。

GREEN: 同一 4 方法命令 -> PASS，2026-07-27 22:37:58 +08:00 完成；4 tests、0 failures、0 errors、0 skipped。计划产能扩展耗尽后，夜班工序保留精确“夜班工序缺少可用夜班班次或夜班产能”根因；普通工序仍保留搜索上限消息。冻结路线版本刷新精确装配版本工序、启用工作站、班次小时和无限公式容量指标，并确保 preview/apply 分别读取新的冻结工序快照。

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProTaskGanttWorkOrderCodeContractTest,MesProAutoScheduleAlgorithmContractTest,MesProAutoScheduleContractTest,MesProScheduleOrderAdmissionTest,MesProScheduleOrderFourRiskContractTest,MesProScheduleOrderNoDefaultConfigContractTest,MesProScheduleOrderServiceImplTest" test` -> PASS，2026-07-27 22:38:24 +08:00 完成；99 tests、0 failures、0 errors、0 skipped，`BUILD SUCCESS`。

REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesProAutoScheduleAlgorithmContractTest" test` -> PASS，2026-07-27 22:38:47 +08:00 完成；18 tests、0 failures、0 errors、0 skipped。

REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesProScheduleOrderAdmissionTest" test` -> PASS，2026-07-27 22:39:10 +08:00 完成；8 tests、0 failures、0 errors、0 skipped。

REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest,MesProScheduleOrderFourRiskContractTest" test` -> PASS，2026-07-27 22:39:32 +08:00 完成；59 tests、0 failures、0 errors、0 skipped。

### T6 完成内容

- Gantt 静态契约从真实承载 `PreviewStep#toGanttDataRespVO` 的 `SchedulePlanner.java` 核对 `workOrderCode`，未搬迁生产转换代码。
- 自动排产算法与契约测试显式装配激活路线版本 `700L`、冻结 `routeVersionId`、版本工序、工作站和容量指标；未新增 lenient 或宽 matcher。
- 排产准入工作站查询精确匹配 `selectListByProcessIds(ids, ENABLE)`；缺少班次小时按 `PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED` 拒绝，且不写入排产工单或工序。
- 过量报工保留 `reportedQuantity=170`、`overReportedQuantity=70`，工序及汇总进度精确封顶 `100`。
- 无默认配置门禁按当前只读 `MesProRouteResourceService` 和 GET-only Controller 契约核对，保留资源缺班次小时、缺人员数量错误码。
- 手工覆盖小时产能在人员数量缺失时保持 `MANUAL_OVERRIDE / NORMAL / 16`，未改成 `CAPACITY_MISSING` 掩盖正式手工覆盖语义。
- 计划产能扩展达到搜索上限时，夜班工序保留精确夜班产能阻塞根因；非夜班工序继续使用既有搜索上限说明。

### T6 改动路径

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/task/MesProTaskGanttWorkOrderCodeContractTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleAlgorithmContractTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleContractTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/scheduleorder/MesProScheduleOrderAdmissionTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/scheduleorder/MesProScheduleOrderFourRiskContractTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/scheduleorder/MesProScheduleOrderNoDefaultConfigContractTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/scheduleorder/MesProScheduleOrderServiceImplTest.java`
- `doc/tasks/20260727-edhr-notify-all-valid-candidates/execution-log.md`

CHECK: `git diff --check`（上述 T6 白名单路径）-> PASS，2026-07-27 22:40 +08:00；仅有既有 LF/CRLF 转换提示，无 whitespace error。新增差异未包含 `lenient`、`@Disabled`、测试跳过、fallback、默认成功或弱化断言。

风险/阻塞：T6 范围内无剩余阻塞；未修改 `task-state.json`、规划文件、`test-report.md`、前端或其他范围外产品文件。任务整体仍由 T0/T4-T9 的独立状态决定，本次 99 tests GREEN 不替代最终完整 MES 模块验收。

## T4：GREEN 与通知回归证据

- `task_id`: T4
- `acceptance_ids`: AC-08、AC-09、AC-12、AC-13、AC-17

### 实施结果

- `MesProBatchRecordExecutionFieldResponsibilityService#export` 在责任证据整体状态不是 `COMPLETE` 时使用既有 `PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED` 错误码 fail-fast；`EVIDENCE_MISSING` 和 `BLOCKED` 均不再生成工作簿。
- 待放行批次的动作锁按具体任务状态判断：未完成普通表单继续锁定；已提交且已批准的普通表单保留既有打开能力，不扩大其他任务动作权限。
- 路线版本、复制和展示测试按当前服务边界精确补齐 `MesProRouteOwnerPermissionService`、`platformAdapter`；删除两处已不再发生真实调用的 strict stubs，未新增 lenient 或宽泛 matcher。
- legacy/eDHR 测试显式补齐 tenant、冻结附件负责人、填写规则、已发布版本和逐工序批记录表单绑定；缺失正式配置仍按原规则阻塞。
- 工序开始上传人、工序设置逐工序批记录表单、表单槽位 `formBindings` 继续分别读取自身正式来源，未新增互相替代、补齐或推断。
- 同一工序内顺序表单必须等待前一表单达到 `APPROVED`；通知行为继续按每个任务的 `candidateUserSnapshot` 向全部有效候选账号各发送一次。

### 改动路径

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionFieldResponsibilityService.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProRouteVersionAndCopyTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionArchiveServiceImplTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionLegacyProcessTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionTaskGateTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrRehearsalReadinessServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskLegacyProcessTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteProcessServiceImplBatchRecordBindingTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteServiceImplDisplayFieldsTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteServiceImplTest.java`
- `doc/tasks/20260727-edhr-notify-all-valid-candidates/execution-log.md`

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProRouteVersionAndCopyTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProEdhrBatchExecutionLegacyProcessTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrBatchExecutionTaskGateTest,MesProEdhrRehearsalReadinessServiceTest,MesProEdhrWorkTaskLegacyProcessTest,MesProBatchRecordExecutionFieldAuditQueryExportServiceTest,MesProRouteProcessServiceImplBatchRecordBindingTest,MesProRouteServiceImplDisplayFieldsTest,MesProRouteServiceImplTest" test` -> PASS，2026-07-27 23:05:47 +08:00 完成；242 tests、0 failures、0 errors、0 skipped，`BUILD SUCCESS`。

REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrWorkTaskServiceImplTest" test` -> PASS，2026-07-27 23:04:07 +08:00 完成；66 tests、0 failures、0 errors、0 skipped，`BUILD SUCCESS`。每任务向 `candidateUserSnapshot` 中全部有效候选人各发一次的通知语义无回归。

CHECK: T4 白名单执行 `git diff --check` -> PASS，仅有既有 LF/CRLF 转换提示，无 whitespace error；范围审查确认 T4 差异只涉及上述白名单文件，新增差异未包含 `lenient`、`@Disabled`、skip、fallback、吞异常、mock success 或宽化断言。

风险/阻塞：T4 范围内无剩余阻塞；未修改 `task-state.json`、规划文件、`test-report.md`、T6 排产源码/测试或其他任务文档。未提交、未推送；任务整体状态仍由 supervisor 和后续 T5、T7-T9 独立门禁决定。

## Supervisor：T0 Word fixture 权威性核验

- 仓库固定资源：`IntRuoyiBackend/yudao-module-mes/src/test/resources/fixtures/pressure-pump-record.doc`。
- 此前用户明确指定并用于真实回归的源文件：`C:\Users\BJB110\Desktop\文档\批记录压力泵.doc`。
- 两者长度均为 `905800` 字节，SHA-256 均为 `830A89A2E116ACA4AB9ECD63A9345F5A288998DD1DDE4A434A612B7BA57C103E`。
- Git 证据：该仓库资源自基线提交 `c67686a5` 即存在；本轮未复制、转换、合成或重命名 Word 文件。
- 结论：Word fixture 已具备可追溯来源和项目内稳定位置，可由 T7 将遗留绝对路径测试切换到该固定资源并执行真实回归。
- 剩余 T0 blocker：Excel 仅发现两个字节一致的候选副本，尚未获得用户对权威性的明确确认，因此不得复制、改名或接入测试资源。
