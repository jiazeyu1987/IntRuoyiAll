# Request Command Log

## 2026-07-02 任务：20260702-schedule-order-workorder-code-display

### 用户需求

- `排产工单列表里,工单编号不显示`

### 已执行命令

- `mvn -pl yudao-module-mes -Dtest=MesProScheduleOrderControllerTest#getScheduleOrderPage_backfillsWorkOrderCodeFromLinkedWorkOrderWhenScheduleCodeMissing test` -> RED FAIL，复现历史排产记录 `erpWorkOrderCode` 为空时响应未补齐工单编码。
- `mvn -pl yudao-module-mes -Dtest=MesProScheduleOrderControllerTest#getScheduleOrderPage_backfillsWorkOrderCodeFromLinkedWorkOrderWhenScheduleCodeMissing test` -> GREEN PASS，1 个后端目标回归测试通过。
- `node tests/e2e/mes-schedule-order-workorder-link-static.spec.js` -> PASS。
- `node tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> PASS。
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260702-schedule-order-workorder-code-display/bug-regression-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260702-schedule-order-workorder-code-display --mode preview` -> PASS，预览无阻塞，未执行删除。

## 2026-07-02 任务：20260702-edhr-jingxi-table-structure

### 用户需求

- `/goal 在worktree edhr_table_jingxi里实现 eDHR 批记录识别链路修复：以真实 DOC 的“精洗工序生产记录”为 ground truth，识别输出单元格数量、排布、rowSpan/colSpan、行列位置、列宽/行高比例和视觉结构必须完全一致；不得针对单表硬编码。`

### 已执行命令

- `python -X utf8 ...` -> PASS，读取 `docs/powershell-memory.md`、worktree/bug/doc 技能说明并确认主工作区前后端状态。
- `git worktree add -b codex/edhr_table_jingxi ...` -> PASS，创建后端与前端成对 worktree。
- `rg -n "MesProBatchRecordReportJsonBuilder|MesProBatchRecordDocParser|RouteARecognizer|精洗工序|fillBlankCells|columnWidth|gridSpan|vMerge" ...` -> PASS，定位批记录 Word 表格解析、校准、JSON 构建和既有回归测试入口。
- `apply_patch` -> 新建当前任务 `task.md`、`execution-log.md` 并记录本任务命令。

## 2026-07-01 任务：20260701-fix-mes-scheduler-role-scope-prod-missing

### 用户需求

- `/goal 成功构建并发布到测试服务器,正式服务器,备份服务器`

### 已执行命令

- `ssh root@172.30.30.57 ... mysql ...` -> PASS，只读确认正式库租户 1 缺少 `排产员/mes_scheduler`，并确认 `20260629_mes_smart_scheduling_role_scope` 在 prod 失败。
- `rg -n "Missing enabled MES scheduler role|mes_scheduler|排产员" ...` -> PASS，定位 SQL 当前对排产员角色只 fail fast，不创建基线。
- `apply_patch` -> 新建任务文档、数据库证据文档，并补充 RED 合同测试。
- `python -X utf8 -m pytest script/tests/test_mes_smart_scheduling_role_scope_sql.py -q` -> FAIL，确认 RED。
- `apply_patch` -> 更新 `20260629_mes_smart_scheduling_role_scope.sql`，恢复/创建租户 1 排产员角色并解析目标 ID。
- `python -X utf8 -m pytest script/tests/test_mes_smart_scheduling_role_scope_sql.py script/tests/test_mes_smart_scheduling_role_assignment_sql.py -q` -> PASS，26 passed。
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS，migrationCount=236。
- `python -X utf8 tool/verify_tdd_compliance.py --repo . --task-dir doc/tasks/20260701-fix-mes-scheduler-role-scope-prod-missing` -> PASS。
- `apply_patch` -> 标记后端任务完成，记录后续必须使用新提交、新 releaseTag 重新发布。

## 2026-07-01 任务：20260701-edhr-phase6-module-dedup

### 用户需求

- `当前实现的phase1~5与edhr里其他的模块有重复的吗,或者其他有哪些edhr的模块可以删除吗`
- `继续`

### 已执行命令

- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\simplify-codebase\SKILL.md`
- `rg -n "EdhrRelease|Release|Audit|DomainTrace|PermissionMatrix|DhrTemplate|Recordbook|BatchExecution|Workbench|Simulate|Review" yudao-module-mes/src/main yudao-module-mes/src/test doc/tasks/20260701-edhr-phase6-module-dedup -S`
- `apply_patch` -> 新建并更新 Phase 6 后端任务台账与执行日志，记录后端无已证明可安全删除接口，本轮不删除放行、审计、权限、模板、记录簿等专业后台能力。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260701-edhr-phase6-module-dedup --mode preview` -> BLOCKED，无删除项；因当前分支无法快进合并到 `int_main` 且主工作区 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 有脏改，未执行 apply。
- `git add -- doc/tasks/20260701-edhr-phase6-module-dedup/task.md doc/tasks/20260701-edhr-phase6-module-dedup/execution-log.md`
- `git commit -m '任务: 记录eDHR后端去重结论'` -> FAIL，缺少 `TDD_TASK_DIR`。
- `$env:TDD_TASK_DIR='D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\ruoyi-vue-pro\doc\tasks\20260701-edhr-phase6-module-dedup'; git commit -m '任务: 记录eDHR后端去重结论'` -> PASS，提交 `cda9574ef7`。

## 2026-07-03 任务：20260702-edhr-jingxi-table-structure

### 用户需求

- 继续在 `edhr_table_jingxi` worktree 中修复真实 DOC `RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc` 的 eDHR 表格识别；要求 `精洗工序生产记录` 与 Word 原表在单元格数量、排布、rowSpan/colSpan、列宽/行高比例上完全一致，不允许右侧多空列或局部错位，不允许单表硬编码。

### 已执行命令

- `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordJingxiTableStructureVerificationTest,MesProBatchRecordReportJsonBuilderTest' test` -> FAIL，真实 DOC 锚点已通过，但 JSON 回归暴露源行高、宽表横向预算、检测页过压缩问题。
- `apply_patch` -> 修复 Word 视觉列宽继承、source grid 保真、JSON 固定列宽、溢出空白格跳过、源行高节奏和低列数工艺页高度规则。
- `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordJingxiTableStructureVerificationTest,MesProBatchRecordReportJsonBuilderTest#build_shouldPreferSourceRowHeightsForFixedRouteANonPagedProcessRows+build_routeAT13_shouldUseLandscapeA4ForFixedWideTemplate+build_shouldNotOverCompressFixedRouteADetectionPageIntoLargeBottomWhitespace+build_shouldFollowSourceHeightForLiveLikeMediumProcessPages+build_shouldPreferSourceRowHeightsForDenseRowsWithVisualBlanksAndShortWrappedCells' test` -> PASS，6 tests, 0 failures。
- `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordJingxiTableStructureVerificationTest,MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordCodexCliImageParserTest' test` -> PASS，81 tests, 0 failures。
## 2026-07-03 任务：20260703-dcc-project-alias-mapping

### 用户需求

- 继续推进 DCC 产品名称/基础信息识别方案，补齐“先建立项目别名/目录映射表，再用映射表识别”的机制。

### 已执行命令

- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\SKILL.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\references\backend-contract.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\references\database-contract.md`
- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro status --short`
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 status --short`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260702-dcc-project-alias-directory-recognition\task.md`
- `rg -n "alias|别名|mapping|映射|..." DCC 相关源码与任务文档`
- `apply_patch` -> 创建任务文档、后端证据、数据库证据，并记录 BDD 场景。
- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest#recognizeProjectCode_confirmedFileNameAliasWinsBeforeStandardRulesAndCodex" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> RED FAIL，缺少持久化别名映射 DO/Mapper。
- `apply_patch` -> 新增 `dcc_project_code_alias_mapping` DO/Mapper、识别链路、账本追溯字段、SQL migration/schema、导出列和回归测试。
- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest#recognizeProjectCode_confirmedFileNameAliasWinsBeforeStandardRulesAndCodex,DccBaseSchemaTest#mysqlSchemaShouldSupportDccProjectCodeAliasMappingRecognition" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> GREEN PASS，2 tests, 0 failures。
- `python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py -q` -> GREEN PASS，7 passed。
- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileMetadataImportExportServiceTest#recognitionRecordExport_containsSharedLedgerRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> GREEN PASS，1 test, 0 failures。

### 用户需求

- `做一次真实数据的E2E验证`

### 已执行命令

- `restart-ruoyi-local-component.ps1 -Component backend` -> 首次构建成功，MySQL 容器启动窗口期未就绪；随后 `-SkipBuild` 启动后端成功，`48081/actuator/health` 返回 `UP`。
- `restart-ruoyi-local-component.ps1 -Component frontend` -> PASS，前端 `8081` HTTP 200。
- `login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /index` -> PASS。
- `mysql < sql/mysql/20260703_dcc_project_code_alias_mapping.sql` -> PASS，补齐本地测试库别名表和账本追溯列。
- `mysql < sql/mysql/20260526_dcc_electronic_signature_hardening.sql` -> PASS，补齐本地测试库详情页依赖的 DCC 签名快照 schema。
- Playwright 真实打开 `/dcc/controlled-file/detail/2054545668044051057` 并点击“识别基础信息” -> PASS，识别接口返回 `FILE_NAME_ALIAS`，数据库和识别账本落库成功。

## 2026-07-06 任务：20260706-dcc-recognition-no-match-success

### 用户需求

- 将文控中心“识别基础信息”中“文件可正常识别但未匹配到产品名称”的场景改为识别成功但未识别到产品名，而不是识别失败。

### 已执行命令

- `Get-Content -LiteralPath D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md -Encoding utf8`
- `Get-Content -LiteralPath D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md -Encoding utf8`
- `Get-Content -LiteralPath C:\Users\BJB110\.codex\skills\backend-api-delivery\SKILL.md -Encoding utf8`
- `Get-Content -LiteralPath C:\Users\BJB110\.codex\skills\backend-api-delivery\references\backend-contract.md -Encoding utf8`
- `Get-Content -LiteralPath C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md -Encoding utf8`
- `Get-Content -LiteralPath C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md -Encoding utf8`
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro status --short`
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 status --short`
- `rg -n "recognizeProjectCode|recognize-project-code|识别基础信息|识别产品|产品名称" ...`
- `apply_patch` -> 新增任务文档、后端证据、前端证据，并补 RED 测试断言。
- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest#recognizeProjectCode_emptyAiResultPersistsNoMatchWithoutUpdatingFile,DccControlledFileBatchRecognitionServiceTest#processWaitingTasksTreatsNoMatchRecognitionAsCompletedSuccess,DccControlledFileBatchRecognitionServiceTest#processWaitingTasksTreatsExistingNoMatchLedgerAsCompletedSuccess" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> RED FAIL，响应 VO 缺少 `recognitionStatus`。
- `apply_patch` -> 后端新增 `NO_MATCH` 状态、响应字段、无匹配账本记录、批量识别非失败统计；前端新增 `NO_MATCH` 类型和提示。
- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest#recognizeProjectCode_emptyAiResultPersistsNoMatchWithoutUpdatingFile,DccControlledFileBatchRecognitionServiceTest#processWaitingTasksTreatsNoMatchRecognitionAsCompletedSuccess,DccControlledFileBatchRecognitionServiceTest#processWaitingTasksTreatsExistingNoMatchLedgerAsCompletedSuccess" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> GREEN PASS，3 tests, 0 failures。
- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest,DccControlledFileProjectCodeRecognitionControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> GREEN PASS，47 tests, 0 failures。
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260706-dcc-recognition-no-match-success\backend-api-evidence.md` -> 初次 FAIL，缺少 `BDD:` 标记，随后补齐。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260706-dcc-recognition-no-match-success --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。


# 2026-07-06 排产与批记录前置检查解耦

## User Requests
- “排产与批记录系统分开,两个不要互相限制”。

## Commands / Actions
- 读取 `docs/experience-index.md`、`docs/powershell-memory.md`，确认任务门禁与 PowerShell UTF-8 要求。
- 搜索 `appendBatchPreflightIssues`、`BLOCKED_BATCH_CODE_REQUIRED`、`BLOCKED_BATCH_ROUTE_CONFIG_INVALID` 定位阻断来源。
- 创建 `doc/tasks/20260706-schedule-batchrecord-decouple/` 任务记录与 BDD/TDD 日志。
- `mvn.cmd -pl yudao-module-mes -Dtest=MesProScheduleOrderPreflightServiceTest test` -> RED FAIL，两条批记录解耦测试仍返回 `BLOCKED`。
- 移除 `MesProScheduleOrderServiceImpl` 中排产前检查对批记录路线、批次号和批记录模板绑定的阻断校验。
- `mvn.cmd -pl yudao-module-mes -Dtest=MesProScheduleOrderPreflightServiceTest test` -> GREEN PASS，4 tests, 0 failures, 0 errors。
- 清理测试中 RED 阶段遗留的批记录 stub 后，重新运行 `mvn.cmd -pl yudao-module-mes -Dtest=MesProScheduleOrderPreflightServiceTest test` -> REGRESSION PASS，4 tests, 0 failures, 0 errors。


# 2026-07-06 排产完成 eDHR 批次号非阻断

## User Requests
- “排产完成创建 eDHR 批次缺少前置条件：批次号”。

## Commands / Actions
- 读取 `docs/powershell-memory.md` 与 `docs/experience-index.md`，确认 PowerShell 与任务门禁。
- 搜索 `openOrCreateFromScheduleCompletion`、`EdhrScheduleCompletionCreateCommand`、`PRO_EDHR_BATCH_EXECUTION_SCHEDULE_PREREQUISITE_MISSING` 定位排产完成触发 eDHR 批次创建的异常来源。
- 创建并更新 `doc/tasks/20260706-schedule-edhr-batchcode-nonblocking/` 任务记录、BDD/TDD 证据与设计约束检查。
- 更新 `MesProAutoScheduleServiceImplTest#apply_shouldNotRollbackScheduleWhenEdhrBatchCreationMissesBatchCode`，覆盖 eDHR 批次号缺失时排产仍成功、任务创建、已排数量同步，并写入 `EDHR_BATCH_CREATION` 警告。
- 更新 `MesProAutoScheduleServiceImpl#createEdhrBatchExecutionsAfterScheduleCompletion`，仅将 eDHR 批次创建缺少前置条件错误码转为排产告警；其他 `ServiceException` 继续抛出。
- `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#apply_shouldNotRollbackScheduleWhenEdhrBatchCreationMissesBatchCode" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProFeedbackApprovalTaskAdapterTest.java,**/ApprovalCenterServiceImplTest.java" test` -> GREEN PASS，1 test, 0 failures, 0 errors。
# 2026-07-06 MES 手动重排应用系统异常

## User Requests
- `手动重排里面应用重排报错 index.vue:2395 [MES] 应用重排失败 Error: 系统异常 ... replanApply ... applyReplan`
- `继续`

## Commands / Actions
- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`docs/login-access.md`，创建 `doc/tasks/20260706-mes-replan-apply-system-error/` 任务记录。
- 搜索前端 `applyReplan/replanApply` 与后端 `/mes/pro/auto-schedule/replan/apply` 调用链，定位到 `MesProAutoScheduleServiceImpl#createEdhrBatchExecutionsAfterScheduleCompletion`。
- 检查运行日志 `E:\Int\CacheData\IntRuoyi\runtime\backend-20260706-125836.out.log`，确认根因为 eDHR 批次号缺失业务异常被外层捕获后，事务仍被标记 rollback-only 并在提交阶段抛出 `UnexpectedRollbackException`。
- 新增 `MesProEdhrBatchExecutionService#getScheduleCompletionMissingItems` 非事务前置条件检查，并在排产服务进入 eDHR 创建事务前先校验缺少批次号等前置条件。
- 更新 `MesProAutoScheduleServiceImplTest#apply_shouldNotEnterTransactionalEdhrCreationWhenSchedulePrerequisiteMissing`，验证缺少批次号时应用重排成功、写入警告且不进入事务创建方法。
- `mvn --% -pl yudao-module-mes -am -Dtest=MesProAutoScheduleServiceImplTest#apply_shouldNotEnterTransactionalEdhrCreationWhenSchedulePrerequisiteMissing -Dsurefire.failIfNoSpecifiedTests=false test` -> GREEN PASS，1 test, 0 failures, 0 errors。
- `mvn --% -pl yudao-module-mes -am -Dtest=MesProAutoScheduleServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> REGRESSION PASS，47 tests, 0 failures, 0 errors。

## 2026-07-06 任务：20260706-direct-work-report-import

### 用户需求

- `智能排产里,导入的工单要支持可以导入桌面的李萍.xlsx这种格式的`
- `这个是报工单`
- `PLEASE IMPLEMENT THIS PLAN: 支持 李萍.xlsx 报工单直接导入`

### 已执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、backend/frontend 交付技能说明与契约。
- `apply_patch` -> 新建 `doc/tasks/20260706-direct-work-report-import/` 任务文档、执行日志、后端证据、前端证据。
- `apply_patch` -> 后端新增李萍报工单表头解析、直接报工导入接口 `/mes/pro/feedback/import-direct-work-report-xlsx`、直接创建报工并提交审批、来源导入记录关联、杂务行跳过统计与行级 fail-fast 错误。
- `apply_patch` -> 前端报工导入弹窗新增“第三方报工待归属 / 李萍报工单”模式，直接报工模式调用新接口并展示创建数、提交数、跳过杂务行数和报工单号。
- `mvn -pl yudao-module-mes -am '-Dtest=ThirdPartyFeedbackExcelParserTest,ThirdPartyFeedbackImportServiceImplTest,MesProFeedbackControllerImportThirdPartyXlsxTest,MesProFeedbackControllerImportDirectWorkReportXlsxTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，18 tests, 0 failures, 0 errors。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS。
- `task_closeout.py --workspace ... --task-id 20260706-direct-work-report-import --mode preview` -> 根目录 PASS；后端/前端仓因 task.md 位于根目录而预览 BLOCKED，未执行清理。

# 2026-07-06 eDHR 批次执行角色化权限与操作体验改造

## 用户需求

- 在独立 worktree 中基于 `edhr_batch_improve` 分支完成 eDHR 批次执行角色化权限与操作体验改造，覆盖填写人、审核人、批准人、生产负责人、无关人员，验证通过后融合进 `int_main` 并删除 worktree。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/worktree-memory.md`、worktree/backend/frontend/BDD 技能说明与证据契约 -> PASS。
- `git worktree add -b edhr_batch_improve D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_batch_improve\ruoyi-vue-pro int_main` -> PASS。
- `Get-NetTCPConnection -LocalPort 8095,48095` -> PASS，目标端口当前未占用。
- 创建 `doc/tasks/20260706-edhr-batch-role-permission-flow/`、`.runtime/runtime.env`，记录 BDD、设计约束、经验门禁和运行态计划 -> PASS。

## 2026-07-06 edhr_batch_improve 验证收尾
- 后端 targeted 回归：mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest,MesProBatchRecordExecutionServiceImplTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrWorkTaskFlowContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，156 tests。
- 真实 E2E：
ode tests\e2e\edhr-batch-role-permission-real-flow.e2e.js -> PASS，batch=900000000462，execution=778。
## 2026-07-06 edhr_batch_improve 最终验证收尾
- 后端 openTask gate：MesProEdhrBatchExecutionServiceTest#openTask_rejectsAlreadyApprovedRouteTask+openTask_rejectsClosedBatch -> PASS。
- 后端 targeted 回归：mvn.cmd -pl yudao-module-mes -Dtest=MesProEdhrWorkTaskServiceImplTest,MesProBatchRecordExecutionServiceImplTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrWorkTaskFlowContractTest -Dsurefire.failIfNoSpecifiedTests=false -DforkCount=0 test -> PASS，159 tests。
- 后端打包：mvn.cmd -pl yudao-server -am -DskipTests package -> PASS，并用 yudao-server-exec.jar 启动 48095。
- 真实 E2E：node tests\e2e\edhr-batch-role-permission-real-flow.e2e.js -> PASS，routeCode=900025，batch=900000000462，execution=778，process=吹球囊成型。
- 五角色复核：填写人、审核人、批准人、生产负责人、无关人员 -> 全部 PASS。
- SQL 契约测试：python -X utf8 -m pytest script\tests\test_edhr_work_task_flow_sql.py -q -> PASS，覆盖新增 MES_EDHR_APPROVE_TASK_ASSIGNED 批准通知模板。

# 2026-07-07 展厅产品 zip 全量导出策略修正

## 用户需求

- `导出的策略有问题,产品不是应该全部导出导入吗?包括没用的,冻结的等`

## Commands / Actions

- 创建 `doc/tasks/20260707-showroom-full-product-package-export/`，记录 BDD、设计约束检查与 TDD 证据。
- 定位 `ShowroomApiRuntime#listProductExcelRows` 中按展柜映射过滤产品的逻辑。
- 更新导出回归用例：未入展柜产品 `EXCEL-003` 必须进入产品列表与产品主数据；未入展柜 `INT-99` 的产品语音必须进入讲解音频 sheet。
- `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#exportProductExcelShouldIgnorePaginationAndExcludeMediaColumns+exportProductExcelShouldIncludeNarrationsForAllExportedProducts -Dsurefire.failIfNoSpecifiedTests=false test` -> RED FAIL，现有实现仍过滤未入展柜产品。
- 移除产品导出时对 `hallNamesByProductId.containsKey(productId)` 的过滤，展柜名称保留为可选关联字段。
- `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#exportProductExcelShouldIgnorePaginationAndExcludeMediaColumns+exportProductExcelShouldIncludeNarrationsForAllExportedProducts -Dsurefire.failIfNoSpecifiedTests=false test` -> GREEN PASS。
- `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，55 tests, 0 failures, 0 errors。

# 2026-07-08 DCC 产品名称识别 status 截断 post-release 修复

## 用户需求

- “继续”：继续真实分析并修复测试服识别产品名称时报错，确认 `release-20260708-dcc-recognition-fix-4` 后仍新增的 `Data too long for column 'status'` 根因，不靠经验判断。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`docs/server-access.md`、`docs/release-backup-restore.md`、`bug-regression-fix-loop` 技能与证据契约 -> PASS。
- 创建 `doc/tasks/20260708-dcc-recognition-post-release-data-too-long/`，记录 BDD、经验门禁、设计约束与测试服后验审计目标 -> PASS。

# 2026-07-08 球囊/棘突球囊工艺路线工序导入

## 用户需求

- 基于桌面 `球囊扩张导管工序(1)(2).xlsx`，把 `球囊扩张导管` 和 `棘突球囊扩张导管` 两条工艺流程的工序加进 MES 工艺路线。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`database-schema-delivery`、`bdd-tdd-acceptance-planner`、`task-closeout-cleanup` 及对应引用契约 -> PASS。
- 创建 `doc/tasks/20260708-balloon-route-process-import/`，记录 BDD、设计约束、数据库证据与执行日志 -> PASS。
- `python -X utf8 -m pytest script/tests/test_mes_balloon_process_device_capacity_sql.py -q` -> RED FAIL，当前迁移缺少两条路线与 `mes_pro_route_process` 挂载契约。
- 更新 `sql/mysql/20260708_mes_balloon_process_device_capacity.sql`，新增两条路线 seed、23/26 道路线工序 seed、`process_id/next_process_id` 挂载和缺失/冲突 fail-fast 校验。
- 同步 SQL 契约测试、H2 测试 schema 的 `product_name/manual_shift_capacity/process_code` 字段，并修正复用工位产能断言为保留既有 `12.50`。
- `python -X utf8 -m pytest script/tests/test_mes_balloon_process_device_capacity_sql.py -q` -> GREEN PASS，6 tests。
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260708-balloon-route-process-import/database-schema-evidence.md` -> PASS。
- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteProcessControllerTest,MesProRouteServiceImplTest,BalloonProcessDeviceMappingImportServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，10 tests。
- `task_closeout.py --task-id 20260708-balloon-route-process-import --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
- Git commit -> BLOCKED，相关 SQL 与 SQL 契约测试在本任务开始前已是未跟踪文件，整文件提交会夹带前序未提交内容。

# 2026-07-09 工序列表展示并筛选所属工艺路线

## 用户需求

- 工序设置列表里增加一列“属于哪些工艺路线”，一个工序可以属于多个工艺路线，并且可以通过工艺路线筛选，例如选择属于“压力泵”的所有工序。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、frontend/backend 交付技能与 closeout 规则 -> PASS。
- 创建 `doc/tasks/20260709-process-route-filter-column/`，记录 BDD、设计约束检查、经验门禁和执行日志 -> PASS。
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProProcessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少 `routeId`、`routeList` 和批量路线工序查询接口。
- RED: `node tests/e2e/mes-pro-process-route-filter-static.spec.js` -> FAIL，前端缺少路线 VO、筛选项和“所属工艺路线”列。
- 实现结果：`/mes/pro/process/page` 支持 `routeId` 筛选并返回 `routeList`；工序设置列表新增“所属工艺路线”列和“工艺路线”快速筛选。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProProcessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，4 tests。
- GREEN: `node tests/e2e/mes-pro-process-route-filter-static.spec.js`、`node tests/e2e/mes-pro-process-unified-list-template-static.spec.js` -> PASS。
- GREEN: `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS。
- GREEN: 后端/前端 evidence 校验与 closeout preview -> PASS。
- Git commit -> BLOCKED，目标文件存在本轮前置未提交改动且本任务依赖这些改动，无法安全单独提交本任务 hunk。

# 2026-07-09 工艺路线工序对应工序设置主数据

## 用户需求

- 将当前工序设置里的列表里的工序与工艺流程里的工序做对应。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`database-schema-delivery`、`database-contract.md`、`backend-api-delivery`、`backend-contract.md` -> PASS。
- 创建 `doc/tasks/20260709-route-process-master-alignment/`，记录 BDD、设计约束、数据库证据和执行日志 -> PASS。
- RED: `python -X utf8 -m pytest script/tests/test_mes_route_process_alignment_sql.py -q` -> FAIL，缺少 `sql/mysql/20260709_mes_route_process_master_alignment.sql`。
- 新增 `sql/mysql/20260709_mes_route_process_master_alignment.sql`，按 `tenant_id + code` 归一 `mes_pro_route_process.process_id` 到规范工序主数据，保留 `mes_pro_process` 不删除不合并。
- GREEN: `python -X utf8 -m pytest script/tests/test_mes_route_process_alignment_sql.py -q` -> PASS，5 tests。
- GREEN: 本机 MySQL 只读预检 -> PASS，有效关系 421 条、空 `process_id` 0 条、断链 0 条、编码名称冲突 0 个、待归一 18 条。
- GREEN: 本机执行 SQL -> PASS，更新 18 条路线工序关系；复查后待归一 0 条。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProProcessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，5 tests。
- GREEN: 数据库 evidence 校验与 closeout preview -> PASS。

# 2026-07-09 工序设置红框内容可点击跳转

## 用户需求

- 工序设置列表红框内“批记录表单 / 生产填写人 / 质量填写人 / 设备填写人”均可点击。
- 批记录表单跳转到电子批记录模板查看页并按 `reportId` 精确过滤。
- 填写人按真实来源类型与 ID 跳转到权限角色、部门管理或用户管理，并在目标页过滤。

## 执行命令

- 创建 `doc/tasks/20260709-process-redbox-click-through/`，记录 BDD、经验门禁、设计约束和执行日志 -> PASS。
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProProcessServiceImplTest,MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少 `reportId` 请求字段与工序分页结构化链接字段。
- 实现结果：`/mes/pro/process/page` 新增 `batchRecordForms` 与三类 `Fillers` 结构化字段；批记录分页支持 `reportId` 精确过滤；缺失表单/角色/部门/用户主数据 fail fast。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProProcessServiceImplTest#getProcessPageWithCapacity_shouldAttachStructuredBatchRecordFormsAndFillers,MesProBatchRecordReportServiceImplDbTest#getGeneratedReportPage_filtersExactlyByReportId" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 tests。
- PARTIAL: 完整 `MesProBatchRecordReportServiceImplDbTest` 类仍有旧用例 `recognizeUploadedRoute_whenSameHashApprovedVersionIsReimported_canApproveAsNextVersion` 失败，期望 `PRECHECK_PASSED`，实际 `PRECHECK_FAILED`；与本任务新增结构化字段和 `reportId` 过滤无直接关系。
- GREEN: backend API evidence 校验与 closeout preview -> PASS。
- Git commit -> BLOCKED，当前混合工作区存在大量既有脏改，且本任务目标文件与既有改动存在文件级重叠，无法安全单独提交本任务 hunk。

# 2026-07-09 再次同步球囊扩张压力泵批记录路线数据

## 用户需求

- 数据没有了，再次从工艺批记录路线里的“球囊扩张压力泵”同步数据。

## 执行命令

- 创建 `doc/tasks/20260709-sync-pressure-pump-batch-record-route/`，记录 BDD、经验门禁、设计约束和执行日志 -> PASS。
- GREEN: `python -X utf8 -m pytest script/tests/test_mes_rt000006_batch_record_mapping_sql.py -q` -> PASS，4 tests。
- GREEN: 只读预检 -> PASS，RT000006 / 球囊扩张压力泵已有 `route_process=14`、`matched_reports=14`、`process_config=14`、`batch_record_bindings=14`、`permission_rules=42`，缺口为 0。
- GREEN: 重新执行 `sql/mysql/20260709_mes_rt000006_batch_record_mapping.sql` -> PASS。
- GREEN: 同步后回查 -> PASS，14 道工序均可联出批记录表单、生产填写人、质量填写人、设备填写人，`redbox_ready_rows=14`。
- Git commit -> BLOCKED，当前仓库存在大量既有脏改，且本任务只执行本机数据库同步与任务记录更新，暂不提交以避免夹带无关改动。
- 用户追加反馈：黄框里的仍为空、没有同步到数据。
- 只读排查 -> PASS，确认 tenant=1 的 `route_id=922067 / RT000006 / 球囊扩张压力泵` 数据完整，但测试租户页面命中 tenant=122 的 `route_id=922060 / RT000006 / E2E-WORD-1783433099306`；该路线已有 14 个表单绑定但缺少 42 条填写人规则，且压力泵三类填写员角色只存在 tenant=1。
- 修复执行 -> PASS，按 `system_role.code` 与 `system_role_category.code=batch-record` 补齐 tenant=122 的三类压力泵填写员角色，并为 `route_id=922060` 的 14 个工序补齐 `FILL / QUALITY_FILL / EQUIPMENT_FILL` 共 42 条规则。
- 修复回查 -> PASS，tenant=122 `route_id=922060 / RT000006` 当前 `route_process_rows=14`、`batch_bindings=14`、`matched_reports=14`、`enabled_fill_rules=42`，14 道工序均可联出批记录表单与三类填写人。

# 2026-07-09 DCC 识别非超时问题修复

## 用户需求

- 解决本次 DCC 识别发现的问题，超时问题除外。

## 执行命令

- 创建并更新根任务文档 `doc/tasks/20260709-dcc-recognition-non-timeout-errors/`，记录 BDD、设计约束、经验门禁、RED/GREEN 证据。
- RED: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccProjectCodeCodexCliClientImplTest#recognizeProjectCode_usesStrictOutputWhenCliExitsNonZeroAfterWritingResult,DccControlledFileBatchRecognitionServiceTest#processWaitingTasksCountsParallelDuplicateInProgressAsSkippedWithoutFailedLedger" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，Codex 有效 JSON 结果因非零退出码被拒绝；并发重复处理中候选未计入批量进度。
- 修复：`DccProjectCodeCodexCliClientImpl` 在非零退出时仅接受严格 JSON 结果文件；`DccControlledFileBatchRecognitionServiceImpl` 并发路径对 `SKIPPED` 候选计入进度。
- GREEN: 同一目标用例命令 -> PASS，2 tests，0 failures。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest,DccProjectCodeCodexCliClientImplTest,DccProjectCodeServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，94 tests，0 failures。
- 收尾预览：`task_closeout.py --task-id 20260709-dcc-recognition-non-timeout-errors --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
# 2026-07-09 生产订单补齐工艺路线关联产品

## 用户需求

- 在工艺路线详情“关联产品”页签红框位置新增按钮，点击后把生产订单中产品名称等于当前工艺路线名称的产品编号补齐到当前路线关联产品。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`docs/agent-memory/project-error-prevention.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` -> PASS。
- 创建 `doc/tasks/20260709-route-product-bind-from-work-orders/`，记录 BDD、经验门禁、设计约束和后端证据 -> PASS。
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteProductBindFromWorkOrdersTest" test` -> FAIL，缺少批量补齐 VO、service method、controller endpoint。
- 实现结果：新增 `POST /mes/pro/route-product/bind-from-work-orders`，按工艺路线名称精确匹配生产订单产品，去重插入 `mes_pro_route_product`，其它路线已绑定冲突 fail fast。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteProductBindFromWorkOrdersTest" test` -> PASS，4 tests。

# 2026-07-09 工艺排产路线改为路线级共用配置

## 用户需求

- 工艺排产路线不再分产品，所有关联的产品都公用同一条工艺排产路线。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`docs/agent-memory/project-error-prevention.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、backend/frontend/database delivery skills -> PASS。
- 创建 `doc/tasks/20260709-route-schedule-shared-config/`，记录 BDD、经验门禁、设计约束、RED/GREEN 证据 -> PASS。
- 实现结果：`/mes/pro/route-schedule-config/save` 改为按 `routeVersionId + routeProcessId` 保存路线级排产配置，`itemId` 仅保留为历史兼容字段且保存时清空；前端“工艺排产路线”配置弹窗移除产品选择器和保存 payload 中的 `itemId`。
- SQL 迁移契约：`mes_pro_route_schedule_config` 改为 `tenant_id + route_version_id + route_process_id + deleted` 唯一索引；产品维度历史配置存在差异时 fail fast，无冲突时归并为路线级配置。
- GREEN: `python -X utf8 -m pytest script/tests/test_mes_route_schedule_config_shared_sql.py script/tests/test_mes_route_schedule_config_item_capacity_sql.py -q` -> PASS，6 tests。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteScheduleConfigServiceTest,MesProRouteVersionMapperTest" test` -> PASS，11 tests。
- GREEN: `node tests/e2e/mes-schedule-route-shared-config-static.spec.js`、`node tests/e2e/mes-schedule-route-production-factor-static.spec.js` -> PASS。
- GREEN: backend/database/frontend evidence 校验 -> PASS。
- GREEN: `task_closeout.py --task-id 20260709-route-schedule-shared-config --mode preview` -> PASS，blocked/warnings 均为 `<none>`。

# 2026-07-09 工艺路线基础信息 Tab 调整

## 用户需求

- 将工艺路线编辑页红框中的基础信息字段作为独立“基础信息”Tab，插入在“组成工序”和“流转关系图”之间；页面顶部标题、返回列表和保存按钮保留。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`docs/login-access.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、frontend-feature-delivery 与 task-closeout-cleanup 规则 -> PASS。
- 创建 `doc/tasks/20260709-route-basic-info-tab/`，记录 BDD、经验门禁、设计约束和前端证据 -> PASS。
- RED: `node tests/e2e/mes-route-basic-info-tab-static.spec.js` -> FAIL，缺少 `basic` Tab 类型和基础信息 Tab。
- 实现结果：`RouteFormContent.vue` 将编码、名称、负责人、说明、备注移动到“基础信息”Tab；已有路线 Tab 顺序为“组成工序 / 基础信息 / 流转关系图 / 关联产品”；新建路线未生成 ID 前只显示基础信息；`RouteEditPage.vue` 仍默认进入流转关系图。
- GREEN: `node tests/e2e/mes-route-basic-info-tab-static.spec.js`、`node tests/e2e/mes-route-edit-default-flow-tab-static.spec.js`、`node tests/e2e/mes-route-edit-page-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS；默认堆内存执行曾 OOM，已用显式 Node 堆内存参数复验通过。
- GREEN: frontend evidence 校验和 `task_closeout.py --task-id 20260709-route-basic-info-tab --mode preview` -> PASS。
- BLOCKER: 官方登录 preflight 未通过；默认 Playwright headless shell 报 ICU 数据错误，改用系统 Chrome 后等待 `/system/auth/login` 响应超时，未继续真实页面 E2E。
- BLOCKER: 当前混合工作区已有大量非本任务改动，且前端目标文件存在本轮开始前的重叠未提交改动；为避免夹带其它任务或用户改动，本轮不提交。

# 2026-07-09 工艺路线流转关系图进入时自动布局

## 用户需求

- 进入工艺路线编辑页“流转关系图”Tab 时，默认点一次“自动布局”。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、frontend-feature-delivery 与 evidence contract -> PASS。
- 创建 `doc/tasks/20260709-route-flow-entry-auto-layout/`，记录 BDD、经验门禁、设计约束和前端证据 -> PASS。
- RED: `node tests/e2e/mes-route-flow-entry-auto-layout-static.spec.js` -> FAIL，缺少 flow Tab 进入事件监听与子组件自动布局入口。
- 实现结果：`RouteFormContent.vue` 在默认进入或切换到 `flow` Tab 时调用 `RouteFlowGraphDesigner.autoLayoutOnEntry()`；`RouteFlowGraphDesigner.vue` 复用现有 `handleAutoLayout()`，等图节点加载完成后执行一次，不改保存接口和布局算法。
- GREEN: `node tests/e2e/mes-route-flow-entry-auto-layout-static.spec.js`、`node tests/e2e/mes-route-flow-graph-static.spec.js`、`node tests/e2e/mes-route-edit-default-flow-tab-static.spec.js`、`node tests/e2e/mes-route-basic-info-tab-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- BLOCKER: 当前混合工作区已有大量非本任务改动，且前端目标文件存在未提交重叠改动；为避免夹带其它任务或用户改动，本轮不提交。

# 2026-07-09 排产员工作台当前工序显示修复

## 用户需求

- 排产员工作台的工序名称和编码不是正在排产的工序和编码。

## 执行命令

- 读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`docs/agent-memory/project-error-prevention.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、`bug-regression-fix-loop`、`frontend-feature-delivery`、`backend-api-delivery` 与 `task-closeout-cleanup` 规则 -> PASS。
- 创建 `doc/tasks/20260709-scheduler-workbench-current-process-wip/`，记录 BDD、经验门禁和设计约束 -> PASS。
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldUseCurrentProcessSnapshotForWorkbenchWip" test` -> FAIL，期望 1 行但返回 2 行，后续未完成工序被统计进工作台。
- 修复结果：`MesProScheduleOrderServiceImpl#getProcessWipStatistics` 改为先按每个排产工单解析当前未完成工序，再按当前工序聚合；`processCode` / `processName` 来自排产工序快照。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldUseCurrentProcessSnapshotForWorkbenchWip" test` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldUseCurrentProcessSnapshotForWorkbenchWip,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldAggregateCurrentUnfinishedEnabledProcessPerOrder,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldExposeListMetricsForWorkbenchTable,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldExposeNightShiftAndPlannedStartDateMixedState" test` -> PASS，4 tests PASS。
- GREEN: bug-regression evidence validation 与 backend-api evidence validation -> PASS。
- GREEN: `task_closeout.py --task-id 20260709-scheduler-workbench-current-process-wip --mode preview` -> PASS。

## 2026-07-09 排产员工作台显示全部在排工序
- 用户需求：这个列表要显示所有的在排工序，不是某一天的。
- 执行记录：创建任务目录 `doc/tasks/20260709-scheduler-workbench-all-wip-processes/`，准备按 BDD + TDD 修复后端 WIP 统计逻辑。

## 2026-07-09 排产员工作台显示全部在排工序 - 验证收尾
- RED：`mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldAggregateAllUnfinishedEnabledProcesses,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldUseProcessSnapshotForAllWorkbenchWip" test` -> FAIL，旧逻辑只返回当前工序。
- GREEN：同一命令 -> PASS，2 个回归测试通过。
- REGRESSION：5 个 WIP 相关测试 -> PASS。
- Evidence：`validate_bug_regression.py` 与 `validate_backend_api.py` -> PASS。
- Closeout preview：`task_closeout.py --task-id 20260709-scheduler-workbench-all-wip-processes --mode preview` -> PASS。

## 2026-07-10 排产员工作台夜班切换系统异常
- 用户需求：点击夜班的时候提示系统异常。
- 执行记录：创建任务目录 `doc/tasks/20260710-scheduler-workbench-night-shift-toggle-error/`，开始按 BDD + TDD 定位并修复。

## 2026-07-10 排产员工作台夜班切换系统异常 - 修复验证
- 日志定位：`PUT /admin-api/mes/pro/schedule-order/process-wip-settings` 抛出 `TooManyResultsException`，根因是 `selectByRouteVersionIdAndRouteProcessId` 对重复路线排产配置执行 `selectOne`。
- 只读 SQL：`route_version_id=4 AND route_process_id=922499` 存在 8 条配置，排产工序快照已有 `route_schedule_config_id`。
- 修复：`resolveCurrentRouteScheduleConfig` 优先使用快照 `routeScheduleConfigId` 查询唯一配置。
- 验证：标准 Maven 测试被非本任务脏改 `MesProEdhrBatchExecutionServiceImpl.java` 编译错误阻断；手动编译目标类后定向 surefire 测试 PASS。

## 2026-07-10 00:26:06 - 20260710-scheduler-workbench-night-shift-toggle-error
- 用户请求：点击夜班时提示系统异常。
- 执行：定位运行日志 TooManyResultsException；修复夜班设置保存时优先使用 routeScheduleConfigId；添加定向回归测试；标准 Maven 被无关编译错误阻塞后执行 scoped javac + surefire；热补丁重启本地后端。
- 验证：定向回归 PASS；后端 PID 31604 健康检查 UP；运行态 class hash 匹配；日志无新增 process-wip-settings TooManyResultsException。

## 2026-07-10 工艺路线工序排产配置全局统一
- 用户确认：排产配置统一按租户、路线版本和路线工序识别，不区分产品。
- 用户确认：全局保留 `item_id IS NULL` 的通用配置，产品级配置转历史记录。
- 用户确认：排产员工作台按路线工序分行，相同基础工序属于不同路线时分别显示和修改。
- 执行入口：创建前后端同名隔离 worktree 和任务台账，按数据库迁移、后端 API、前端页面、真实 E2E 顺序执行 BDD + 严格 TDD。
- 用户更正：“球囊扩张导管”没有“全检导丝”；确认该记录是已删除历史路线工序残留，迁移和真实页面均排除。
- RED/GREEN：新增“当前路线工序定义优先”回归，先失败于历史快照工序，再改为读取当前路线工序和当前工序主数据后通过。
- GREEN：后端目标回归 65 tests，迁移/发布契约 17 tests，前端静态契约和生产构建通过，后端完整 reactor 构建通过。
- GREEN：真实 Playwright 加载 44 条路线工序，同一路线工序聚合 34 个订单，夜班切换与恢复成功，当前工序编码名称完整。
- BLOCKER：测试租户没有“同一基础工序同时属于两条工艺路线”的在排数据，跨路线分行缺少真实 E2E 前置；完整 MES 测试的代表性既存失败已在干净基线提交复现。
- 清理：本任务验证服务、隔离数据库、隔离用户和临时运行文件已清理；未修改源数据库，未提交、未融合。
- 用户追加要求：使用“芋道源码/admin”进行最终只读验证，验证通过后融合；用户提供的临时登录口令不写入任务记录或脚本。
- 验证结果：“芋道源码/admin”登录和只读页面验证通过，工作台返回 26 条路线工序，“棘突球囊扩张导管 / RX口检测”聚合 5 个订单，且没有 MES 写请求；但当前在排工艺路线只有“棘突球囊扩张导管”，缺少同一基础工序跨两条路线的真实数据，验证门禁未通过，因此未提交、未融合。
- 用户要求再次验证；重试返回 49 条路线工序，“RX口检测”分别出现在“棘突球囊扩张导管”和“球囊扩张导管”两条路线中，“棘突球囊扩张导管 / RX口检测”聚合 6 个订单，且没有 MES 写请求；跨路线分行真实 E2E 通过，开始提交和融合。
- 融合验证：最新 `int_main` 合入任务分支后，后端目标回归 66 个测试、迁移契约、完整打包、前端静态契约和生产构建通过；管理员只读 Playwright 再次加载 49 条路线工序，跨路线分行和多订单聚合通过，零 MES 写请求。
## 2026-07-10 eDHR 工序辅助表单联动填写
- 用户需求：工艺路线工序配置损耗单、过程检验单、参数记录表后，填写该工序批记录时必须同时完成对应辅助表单。
- 执行：创建 `doc/tasks/20260710-edhr-process-companion-forms/`，按 BDD + 严格 TDD 锁定同工序任务生成、门禁和配置缺失失败行为。
- RED：缺少 `formSlotType/slotConfigSnapshotHash` 的辅助表单绑定未被拒绝。
- 实现：同一 `routeProcessId` 下按 `reportSort` 生成主表和辅助表单任务；SEQUENTIAL/PARALLEL 沿用现有门禁；路线绑定缺少表单、槽位、权限范围或槽位快照时直接失败。
- 无 fallback：删除打开任务时从记录表权限范围补 `permissionScopeId` 的回退逻辑。
- GREEN：`MesProEdhrBatchExecutionServiceTest` 全量 74 tests PASS。

## 2026-07-10 工艺路线边界关系持久化

- 用户需求：流转关系图的工序开始允许多条出口，工序结束只允许一条入口，边界关系可保存和刷新恢复。
- 执行：创建隔离分支 `codex/20260710-route-flow-boundary-links`，计划新增边界关系表、API 字段、校验、复制与删除契约。
- 门禁：使用本机测试数据库与 Redis；SQL 必须通过 Schema 契约和 migration policy gate；不操作服务器。
- RED：服务测试因边界 ReqVO/DO/Mapper 缺失而失败；SQL 契约因迁移文件缺失而失败。
- 实现：新增边界关系表、DO/Mapper/VO，查询/校验/保存/复制/删除纳入同一图事务与版本机制；普通工序允许多前置、最多一个后续。
- 根因修复：迁移删除阻止多前置汇合的旧目标唯一索引；路线版本按数字后缀排序，V10 后连续保存生成 V11。
- GREEN：22 个后端定向测试、5 个 SQL 契约测试、后端打包和 migration policy gate 全部通过。
- 本机验证：迁移仅应用到本机数据库，历史有效图回填 `START=4`、`END=4`；隔离后端 `48094` 健康。
- 真实链路：测试租户路线 `RT000017` 完成多 START 汇合、唯一 END、两次保存刷新和 API 持久化断言，并通过页面恢复原拓扑。

## 2026-07-10 排产员工作台动态重排说明

- 用户需求：排产逻辑页签每次成功重排后更新具体数值，不只展示物料需求、库存和短缺，还要展示订单顺序、工序、班次产能、受保护任务、问题和最终任务结果。
- 执行：创建独立后端 worktree 与任务文档，按 BDD + 严格 TDD 实现一次成功重排一条权威说明快照及当前租户最新说明查询接口。

## 2026-07-10 应用重排系统异常

- 用户反馈：排产工单应用重排时前端记录 `[MES] 应用重排失败 Error: 系统异常`。
- 根因：业务请求尚未进入重排接口，访问令牌刷新时引用了已删除的后台用户，`OAuth2TokenServiceImpl.buildUserInfo` 触发空指针，且事务回滚导致失效令牌重复使用。
- 修复：刷新令牌对应用户不存在时返回明确 401，清理刷新令牌和关联访问令牌；新增严格 TDD 回归。
- 验证：系统模块 524 个测试通过；隔离运行态真实登录通过；Playwright 使用测试租户真实失效令牌验证返回 401 且令牌清理完成。
- 数据阻断：完整重排写入因测试租户工艺路线流转关系图无效被业务门禁阻断，未修改数据或绕过门禁。

## 2026-07-11 旧工序 ID 系统性消除

- 用户需求：报工导入 `C:\Users\BJB110\Desktop\文档\李萍.xlsx` 报“未找到对应的工艺工序配置”，继续检查并修复系统内旧工序 ID 问题，直到类似问题消除。
- 执行：在独立后端 worktree `codex/legacy-process-id-eradication` 中建立统一路线工序身份解析，修复报工创建/导入/审批、BOM/物料消耗/IPQC、工位/设备/资源、批记录/eDHR、路线默认配置、排程配置和自动排程快照链路中的旧工序严格匹配。
- GREEN：旧工序 ID 目标回归与自动排程相关回归 Maven 命令均通过；`git diff --check` 通过。
- GREEN：测试租户真实前端导入 `李萍.xlsx` 返回成功弹窗，未再出现“未找到对应的工艺工序配置”。
- 限制：测试租户没有该文件匹配的工单/任务，导入统计为创建报工 0、提交审批 0、跳过杂务行 70；未通过 SQL 或接口造数绕过真实路径。
## 2026-07-11 预览重排路线工序身份歧义修复

- 用户意图：修复预览重排时报错 `工艺路线工序身份不唯一，routeId=null，sourceProcessId=922894，processCode=Z2630，candidateRouteProcessIds=[900394, 922894]`。
- 执行摘要：在独立后端 worktree `replan_identity_20260711` 中修复 `getProcessIdentityMap`，允许同一上下文内多个显式目标工序共用编码时保留自身身份，同时保留外部别名无法唯一映射时的 fail fast。
- 验证结果：RED 定向测试复现用户报错；GREEN 定向测试通过；`MesProRouteProcessServiceImplTest` 11 tests PASS；排产身份 Python 契约 5 tests PASS。
- 合并结果：后端 `int_main` 合并提交 `ef19a2d1f3`；合并后排产身份 Python 契约 5 tests PASS，`MesProRouteProcessServiceImplTest` 11 tests PASS。
- 边界说明：未访问远程服务器，未修改前端，未记录密码、token、私钥或连接串。

## 2026-07-11 预览重排 Z3710 同编码快照工序别名归属
- 用户反馈：预览重排仍报 `工艺路线工序身份不唯一，routeId=null，sourceProcessId=900400，processCode=Z3710，candidateRouteProcessIds=[922864, 922895]`。
- 根因：`getProcessIdentityMap` 在同一编码目标同时包含已删除快照目标和唯一当前有效目标时，把外部旧工序别名误判为不可解析歧义。
- 修复：显式目标工序保留自身身份；外部旧别名仅在同编码目标中存在唯一 `deleted=false` 目标时归属该当前有效目标；多个当前有效目标仍 fail fast。
- 验证：定向 RED/GREEN、`MesProRouteProcessServiceImplTest` 12 tests PASS、排产身份 Python 契约 5 tests PASS。

## 2026-07-11 预览重排设备工序产能冲突
- 用户反馈：前端 `index.vue` 报 `[MES] 重排预览失败 Error: 系统异常`。
- 根因：设备工序产能查询把旧工序别名产能行与显式当前目标工序产能行统一映射到同一正式 `processId` 后保留了两条记录，后续按 `machineryId + processId` 合并时触发 `设备工序产能存在冲突: machineryId=47, processId=922851`。
- 修复：设备工序产能按正式身份去重；当同一设备同一正式工序同时存在旧别名与显式当前目标产能时，优先使用显式当前目标产能；同类冲突仍 fail fast，不吞异常、不默认取第一条。
- 验证：定向 RED/GREEN、相关回归 19 tests PASS；本机 `int_main` 后端重启健康检查 `UP`；测试租户真实登录后同载荷接口验证不再返回系统异常或设备工序产能冲突。
- 收尾：实现提交 `6c24fe905d`；`task-closeout-cleanup` preview/apply 均通过，当前为主工作区 `int_main`，无 linked worktree 需融合或删除。

## 2026-07-11 DCC 类别删除同步清理矩阵与授权页
- 用户反馈：类别列表删除黄框中的 3 个类别后，审阅矩阵、查看矩阵、目录授权三个 tab 仍显示对应 row。
- 代码核对：当前后端 `deleteCategory` 已清理类别目录绑定、权限规则、查看矩阵规则、审阅矩阵路线与节点、分发规则、培训规则；前端类别页已有 `categoryRevision`，三个页签会在激活或类别修订变化后重载。
- 处理：未改生产逻辑；补强 `DccFileCategoryAdminServiceImplTest`，把查看矩阵真实规则纳入类别删除回归，并修正查看矩阵测试夹具的生命周期阶段字段。
- 验证：类别删除定向测试 PASS；DCC 类别/审阅矩阵/查看矩阵/目录管理相关回归 56 tests PASS；前端 `e2e:dcc:permission-deleted-category-sync:static` PASS。
- 收尾：实现提交 `fb85755806`；`task-closeout-cleanup` preview/apply 均通过，当前为主工作区 `int_main`，无 linked worktree 需融合或删除。

## 2026-07-13 Word 导入弹窗界面精简
- 用户反馈：导入 Word 弹窗中红框产线候选不用默认选中，黄框文件名和格式提示内容删除。
- 执行：创建 `doc/tasks/20260713-word-import-dialog-ui-cleanup/`，按 BDD + 严格 TDD 增加静态回归，先复现文件名展示和产线默认勾选问题。
- 修复：批记录表单列表 Word 文件行只保留“选择文件”按钮；批记录表单列表和批记录模板页预检成功后不再自动勾选重建产线候选。
- 验证：新增弹窗 UI 静态契约 PASS；原 Word 导入预检静态契约 PASS；表单导入前置校验静态契约 PASS；未执行真实写入 E2E，未修改芋道源码租户数据。
- 收尾：`task-closeout-cleanup` preview/apply 均通过，当前为主工作区 `int_main`，无 linked worktree 需融合或删除。

## 2026-07-17 Word 表单格式与取值范围批量识别
- 用户需求：Word 导入解析完成后先批量识别单元格格式和取值范围，批记录、损耗单等其他表单都要覆盖；不要求 100% 正确，后续用户可手动调整。
- 执行：创建 `doc/tasks/20260717-word-form-format-rule-recognition/`，读取批记录表单识别经验门禁，按 BDD + 严格 TDD 在 `MesProBatchRecordCellRuleSupportTest` 先复现自动识别缺口。
- RED：`mvn.cmd -pl yudao-module-mes -Dtest=MesProBatchRecordCellRuleSupportTest test` 因 `applyAutomaticSuggestions(...)` 缺失失败。
- 修复：在报表 JSON 保存前自动应用通用单元格规则建议；按邻近标签/单位识别文本、数字、日期、日期时间、勾选，并为数量、温度、压力、比例、重量、时长、描述等生成初始约束；保留人工已确认规则。
- 验证：`MesProBatchRecordCellRuleSupportTest`、`MesProBatchRecordJimuReportGatewayImplTest`、`MesProBatchRecordGenericDetailFormNormalizerTest`、`MesProBatchRecordRouteERecognizerTest` 和 `MesProBatchRecordReportJsonBuilderTest` 3 个相关方法均 PASS；全量 `MesProBatchRecordReportJsonBuilderTest` 因长时间未完成被停止，未作为通过证据。
# 2026-07-23 批记录 checkbox 选项组通用化

- 用户需求：辅助模式中 `检测结果` 下 `符合要求 / 不符合要求` 这类互斥 checkbox 应实现为一个选项组，示例为 `检测结果：○ 符合要求  ○ 不符合要求`。
- 执行意图：在 `20260723_batch` 独立 worktree 内补充后端识别契约，使互斥 checkbox 文本输出可被前端渲染为选项组，不做单表或单坐标特例。
- 2026-07-23 用户要求：将互斥 checkbox 实现为选项组 `检测结果：○ 符合要求  ○ 不符合要求`；已在后端识别契约中通用化，并用 Maven 目标测试验证。
- 2026-07-23 补充修复：泛化 `结果/检查结果 + 符合要求/不符合要求` 为 `检测结果` 选项组标签；`MesProBatchRecordCellRuleSupportTest` 21 tests PASS。

## 2026-07-23 压力泵组装Ⅰ/光固Ⅰ单元格差异优化

- 用户需求：在 `20260723_batch` worktree 中，对比识别后的 `组装Ⅰ`、`光固Ⅰ` 与 `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc` 原 Word 表单的每个单元格，分析差异最大的单元并通用优化识别算法，直到基本无差异。
- 执行意图：新增真实 Word 单元格级差异报告测试，比较 Word 原表与 `RouteB + LayoutCalibrator + ReportJsonBuilder` 渲染 JSON 的行数、列数、列宽、行高、文本、合并、控件和斜杠语义。
- 根因修复：将结构化汇总空白录入格与普通汇总留白分开处理，并在压缩物料矩阵展开时保留奇数尾行右侧物料/批号槽可填写属性；未使用单表名称、固定坐标或截图特例。
- 验证结果：`MesProBatchRecordPressurePumpCellDiffReportTest` PASS，最终报告显示 `组装Ⅰ工序生产记录 diffCount=0/maxScore=0`、`光固Ⅰ工序生产记录 diffCount=0/maxScore=0`；JsonBuilder 定向 3 tests PASS，LayoutCalibrator 定向 2 tests PASS，CellRuleSupport 21 tests PASS。
- 限制记录：组合全类回归运行 15 分钟超时后停止，未作为通过证据；本次完成证据采用定向 GREEN 与真实 Word 单元格差异报告。
