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
