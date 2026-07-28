# Test Report

## Current Status

PASS / completed. 用户已明确取消 Sheet1 Excel 真实样本覆盖要求；该缺失 fixture 不再作为验收前置。当前保留的 Sheet1 parser 合成 fail-fast/契约测试通过。完整 `mvn -pl yudao-module-mes test` 于 08:53 通过：2530 tests、0 failures、0 errors、18 skipped；并发回归修复后于 12:41 再次通过：2537 tests、0 failures、0 errors、18 skipped，`BUILD SUCCESS`。

当前工作区最新复验：2026-07-28 13:29:43 +08:00 执行 `mvn -pl yudao-module-mes test`，结果 `BUILD SUCCESS`，2540 tests、0 failures、0 errors、18 skipped。

## Baseline

- Command: `mvn -pl yudao-module-mes test`
- Result: FAIL
- Summary: 2509 tests, 58 failures, 78 errors, 31 skipped, 41 failing suites.
- Historical baseline only: this blocker was resolved by subsequent fixes and the user-approved Sheet1 scope change.

## T2 Independent Verification

- Task ID: `20260727-edhr-notify-all-valid-candidates`
- Test case: `TC-03`
- Acceptance mapping: `AC-04`, `AC-05`, `AC-13`, `AC-14`, `AC-16`, `AC-17`
- Expected: 三个静态契约测试只对齐当前正式前端根目录 `IntRuoyiFronted` 和 `recognizeUploadedRoute` 正式精确参数序列；不得删除断言、缩小扫描范围或使用宽松匹配；定向测试与差异检查通过。
- Actual: 三个测试文件各仅有 1 行替换，共 3 insertions/3 deletions。两个菜单契约只将废弃根目录 `yudao-ui-admin-vue3` 替换为 `IntRuoyiFronted`，原 SQL、路由、文件存在性及禁止项断言均保留；版本迁移契约只在原精确字符串序列中加入正式参数 `null`，与生产代码调用 `oldVersion.getSourceVersionId(), null, productNames, true, List.of(), productNames` 一致。未发现断言删除、扫描范围缩小、正则放宽或模糊匹配。
- Result: `PASS`
- Test summary: `10 tests`, `0 failures`, `0 errors`, `0 skipped`, `BUILD SUCCESS`
- Test command: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrFormFillLogMenuContractTest,MesProEdhrTemplateConfigMenuRemovalContractTest,MesProBatchRecordVersionPhaseTwoMigrationContractTest" test`
- Diff command: `git diff --check -- "yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProEdhrFormFillLogMenuContractTest.java" "yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProEdhrTemplateConfigMenuRemovalContractTest.java" "yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProBatchRecordVersionPhaseTwoMigrationContractTest.java"`
- Diff result: exit code `0`，无空白错误；仅输出 Git 既有 LF/CRLF 转换提示。
- Unresolved issues: T2 范围内无未解决问题；任务整体仍受权威 fixture 缺失及其他 MES 完整回归失败簇阻塞，本次 PASS 不代表 TC-09 或完整任务通过。

## T3 Independent Verification

- Task ID: `20260727-edhr-notify-all-valid-candidates`
- Test case: `TC-04`
- Acceptance mapping: `AC-05`, `AC-06`, `AC-07`, `AC-13`, `AC-14`, `AC-17`
- Expected: schema 契约只纳入仓库中已存在的正式 migration，并精确识别正式 helper；Spring 测试仅补齐当前生产服务实际依赖的精确 `@MockitoBean`；可验证 Spring/DB 子集和删除后重导唯一键用例可重复通过；不得使用合成 Excel、跳过测试、放宽断言或默认成功；不完整或阻塞的责任证据必须拒绝导出。
- Commit review: `219169b70a17461d160d4aa47cd9295f604a4ed6` 中 T3 七个测试文件共 `39 insertions/2 deletions`。`MesBatchRecordBaseSchemaTest` 只新增读取三个已在父提交中存在的正式 migration：`20260708_mes_batch_record_version_phase_one.sql`、`20260720_mes_batch_shared_form_binding.sql`、`20260722_mes_recordbook_batch_controlled_sync.sql`；helper 匹配仅扩展为 `ensure_*_column` 和 `add_*_column_if_missing|table_exists`，与 migration 中的 `add_mes_edhr_column_if_missing`、`add_mes_edhr_column_if_table_exists` 精确对应。其余六个测试只增加生产类当前实际依赖的类型导入和精确 `@MockitoBean`，未改变业务断言、测试发现范围或 Mockito 严格性。
- Fixture review: `Sheet1RouteExcelImportServiceImplDbTest` 在提交前后都通过 `Files.readAllBytes(FIXTURE)` 读取 `D:\ocr2\resource\球囊扩张导管工序(1).xlsx`；提交只增加 `MesProRouteOwnerPermissionService` 的精确 `@MockitoBean`，没有 `XSSFWorkbook`、`createSheet`、`createRow` 或其他合成/伪造 fixture 逻辑。
- Verifiable subset command: `mvn -pl yudao-module-mes "-Dtest=MesBatchRecordBaseSchemaTest,BalloonProcessDeviceMappingImportServiceImplTest,MesProBatchRecordReportRenameServiceImplDbTest,MesProBatchRecordReportServiceImplDbTest,ThirdPartyFeedbackImportServiceImplDbTest,IntGyRouteMarkdownImportServiceImplDbTest" test`
- Verifiable subset result: `PASS`，`122 tests`, `0 failures`, `0 errors`, `0 skipped`, `BUILD SUCCESS`。
- Repeatability command, run 1: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenDeletedBatchRecordReimported_startsFromV1Again" test`
- Repeatability result, run 1: `PASS`，`1 test`, `0 failures`, `0 errors`, `0 skipped`, `BUILD SUCCESS`。
- Repeatability command, run 2: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenDeletedBatchRecordReimported_startsFromV1Again" test`
- Repeatability result, run 2: `PASS`，`1 test`, `0 failures`, `0 errors`, `0 skipped`, `BUILD SUCCESS`；未复现 H2 唯一键污染。
- Responsibility export command: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionFieldAuditQueryExportServiceTest" test`
- Responsibility export result: `BLOCKED`，`8 tests`, `2 failures`, `0 errors`, `0 skipped`, `BUILD FAILURE`。失败精确为 `responsibilityExportRejectsIncompleteOverallEvidence` 和 `responsibilityExportRejectsBlockedEvidenceAndPreservesUnknownSummaryOrigin`：汇总状态分别已正确得到 `EVIDENCE_MISSING`、`BLOCKED`，但调用导出时均未抛出期望的 `PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED`。只读代码审查确认 `MesProBatchRecordExecutionFieldResponsibilityService#export` 在 `computeSummary` 后直接生成工作簿，未对 `overallStatus` 执行 fail-fast；这是两个产品行为缺口，不是 Spring Bean 装配失败。
- Excel fixture command: `mvn -pl yudao-module-mes "-Dtest=Sheet1RouteExcelImportServiceImplDbTest" test`
- Excel fixture result: `BLOCKED`，`1 test`, `0 failures`, `1 error`, `0 skipped`, `BUILD FAILURE`。唯一错误为 `NoSuchFileException: D:\ocr2\resource\球囊扩张导管工序(1).xlsx`；独立 `Test-Path` 结果为 `False`，未发现其他测试或产品错误。
- Diff command: `git diff --check 219169b7^ 219169b7 -- <T3 seven test files>`
- Diff result: `PASS`，exit code `0`，无 whitespace error。当前七个文件与提交 `219169b7` 完全一致。
- Overall result: `BLOCKED`。`AC-05`、`AC-06` 和 `AC-07` 在当前可验证范围内通过；T3 命令未出现 skipped，提交未新增跳过、测试过滤配置、宽松 mock、放宽断言、fallback 或伪 fixture。`AC-13` 仍因上述两个产品 fail-fast 缺口和权威 Excel 缺失未满足，因此本结果不构成完整 MES 回归或任务完成放行。
- Unresolved issues: 在 `MesProBatchRecordExecutionFieldResponsibilityService` 正式实现中补齐 `EVIDENCE_MISSING/BLOCKED` 导出拒绝逻辑并通过原两项严格断言；从权威来源取得并治理 `D:\ocr2\resource\球囊扩张导管工序(1).xlsx` 对应真实 Excel 后重新运行 Sheet1 用例。独立测试未修改任何产品代码、测试代码、`task-state.json`、`execution-log.md` 或规划文件。

## T6 Independent Verification

- Task ID: `20260727-edhr-notify-all-valid-candidates`
- Test case: `TC-07`
- Acceptance mapping: `AC-08`, `AC-11`, `AC-13`, `AC-17`
- Expected: Gantt 契约检查真实 `SchedulePlanner` 转换边界；冻结路线版本和启用工作站调用被精确装配；夜班计划产能搜索耗尽后保留精确夜班阻塞原因，非夜班仍保留 3660 天搜索上限消息；缺班次小时 fail-fast 且不写排产订单；过量报工数量证据保留且进度封顶 100；只有正式 `MANUAL_OVERRIDE` 可在人员数量缺失时保持 `NORMAL`；不得新增 lenient、宽泛桩、跳过、fallback、默认成功或弱化断言。
- Diff review: T6 白名单共 8 个文件，`127 insertions/39 deletions`。生产代码差异仅有 `MesProAutoScheduleServiceImpl.java` 一处：计划产能扩展搜索耗尽时按 `nightShiftEnabled` 选择既有 `buildLineCapacityInsufficientMessage` 或既有 `buildLineCapacitySearchLimitMessage`，未改变搜索天数、容量计算、写入链路或其他产品行为。其余差异均为 7 个目标测试文件。
- Strictness review: 新增差异中 `lenient=0`、`@Disabled/@Ignore/assumption skip=0`、fallback/默认成功/吞异常/弱断言模式 `=0`。新增的两处 `any(...)` 只位于 `verify(..., never()).insert(any(...))`，用于证明缺班次小时后任何排产订单/工序实体都不得写入，不是宽泛 stubbing，也不会隐藏参数不匹配；新增 stubbing 均按正式路线版本、工作站状态和实际调用参数精确匹配。已有测试基线中的历史 `lenient` 未在本轮增加。
- Main command: `mvn -pl yudao-module-mes "-Dtest=MesProTaskGanttWorkOrderCodeContractTest,MesProAutoScheduleAlgorithmContractTest,MesProAutoScheduleContractTest,MesProScheduleOrderAdmissionTest,MesProScheduleOrderFourRiskContractTest,MesProScheduleOrderNoDefaultConfigContractTest,MesProScheduleOrderServiceImplTest" test`
- Main result: `PASS`，2026-07-27 22:47:39 +08:00 完成；`99 tests`, `0 failures`, `0 errors`, `0 skipped`, `BUILD SUCCESS`。
- High-risk command 1: `mvn -pl yudao-module-mes "-Dtest=MesProAutoScheduleAlgorithmContractTest" test`
- High-risk result 1: `PASS`，2026-07-27 22:47:59 +08:00 完成；`18 tests`, `0 failures`, `0 errors`, `0 skipped`, `BUILD SUCCESS`。
- High-risk command 2: `mvn -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest,MesProScheduleOrderFourRiskContractTest" test`
- High-risk result 2: `PASS`，2026-07-27 22:48:18 +08:00 完成；`59 tests`, `0 failures`, `0 errors`, `0 skipped`, `BUILD SUCCESS`。
- Behavior review: 两个夜班阻塞用例均精确断言唯一 `CAPACITY/BLOCKING` 问题的工单、工序和完整消息为“夜班工序缺少可用夜班班次或夜班产能”；产品分支对非夜班仍调用 `buildLineCapacitySearchLimitMessage`，其中 `LINE_CAPACITY_SEARCH_DAY_LIMIT=3660`。缺班次小时用例精确断言 `PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED`、完整错误消息及两个 Mapper 均无 insert。过量报工用例精确保留 `reportedQuantity=170`、`overReportedQuantity=70`，并断言工序和汇总进度为 `100`。人员数量缺失用例显式使用 `MANUAL_OVERRIDE` 和小时产能 `2`，断言来源为 `MANUAL_OVERRIDE`、状态为 `NORMAL`、班次产能为 `16`；未把 `RESOURCE_CALCULATED` 当作隐式 fallback。Gantt 契约限定读取 `SchedulePlanner.PreviewStep#toGanttDataRespVO` 所在区间并检查 `workOrderCode` 入参与 VO 赋值。
- Diff command: `git diff --check -- <T6 eight whitelist paths>`
- Diff result: `PASS`，exit code `0`，无 whitespace error；仅有 Git 既有 LF/CRLF 转换提示。测试前后 8 个白名单文件 SHA-256 一致，验证期间未发生并发改写。
- Overall result: `PASS`。T6 / TC-07 / AC-08、AC-11、AC-13、AC-17 在指定独立验证范围内满足放行条件。
- Unresolved issues: T6 范围内无未解决问题。任务整体仍由 T0、T4、T5、T7-T9 和最终 `mvn -pl yudao-module-mes test` 决定；本次 T6 PASS 不代表完整 MES 回归或任务整体完成。独立测试未修改任何产品代码、测试代码、`task-state.json`、`execution-log.md` 或规划文件，未提交、未推送。

## T4 Independent Verification

- Task ID: `20260727-edhr-notify-all-valid-candidates`
- Test case: `TC-05`
- Acceptance mapping: `AC-08`, `AC-09`, `AC-12`, `AC-13`, `AC-17`
- Pre-read evidence: 已完整读取 `E:\IntRuoyi\AGENTS.md`、`docs\backend-development.md`、`docs\powershell-encoding.md`、本任务 `test-plan.md` 与 `execution-log.md`；`rg --files -g AGENTS.md E:\IntRuoyi\IntRuoyiBackend` 未发现后端目录下更近的 `AGENTS.md`。
- Expected: T4 范围只允许 2 个生产类和 10 个测试类；责任证据 `EVIDENCE_MISSING` / `BLOCKED` 导出必须 fail-fast，`COMPLETE` 仍可导出；待放行动作锁不得扩大权限；工序开始上传人、逐工序批记录表单、`formBindings` 三条来源独立；通知服务继续按每个任务 `candidateUserSnapshot` 的全部有效候选人逐人发送且去重；不得新增 lenient、宽泛 stubbing、`@Disabled`、skip、fallback、默认成功、吞异常或弱化断言。
- Diff scope review: T4 白名单差异精确为 12 个文件：`MesProBatchRecordExecutionFieldResponsibilityService.java`、`MesProEdhrBatchExecutionServiceImpl.java`、`MesProRouteVersionAndCopyTest.java`、`MesProBatchRecordExecutionArchiveServiceImplTest.java`、`MesProEdhrBatchExecutionLegacyProcessTest.java`、`MesProEdhrBatchExecutionServiceTest.java`、`MesProEdhrBatchExecutionTaskGateTest.java`、`MesProEdhrRehearsalReadinessServiceTest.java`、`MesProEdhrWorkTaskLegacyProcessTest.java`、`MesProRouteProcessServiceImplBatchRecordBindingTest.java`、`MesProRouteServiceImplDisplayFieldsTest.java`、`MesProRouteServiceImplTest.java`；全局工作区仍有并发非 T4 脏文件，本次仅按 T4 白名单审查。
- Diff behavior review: `MesProBatchRecordExecutionFieldResponsibilityService#export` 在 `computeSummary` 后仅新增 `overallStatus != COMPLETE` 时抛出既有 `PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED`，未引入新错误码或默认成功；`MesProEdhrBatchExecutionServiceImpl` 仅把待放行动作锁豁免收窄到已提交/已完成的普通路线表单任务且原 `OPEN_FORM` 已允许的场景。
- Test diff review: 路线与 eDHR 测试只补齐当前正式依赖、tenant、冻结附件负责人、填写规则、已发布批记录版本和逐工序批记录绑定；`MesProRouteProcessServiceImplBatchRecordBindingTest` 仅删除两处不再发生真实调用的 strict stub；新增差异中 `lenient=0`、`@Disabled=0`、`@Ignore=0`、`assumeTrue/assumeFalse=0`、`fallback=0`、`default success=0`、`mock success=0`、`吞异常=0`、`跳过=0`。审查未发现新增宽泛 stubbing、跳过配置、默认成功分支或弱化断言。
- Source independence review: 工序开始/附件负责人仍通过冻结快照 `batchRecordAttachmentOwners` 与特殊节点上传/完成链路验证；逐工序批记录表单仍通过 BATCH 工序绑定、report/version identity 和 `batchRecordReportId` 链路验证；表单槽位仍通过 `formBindings` / 动态表单中心绑定链路验证。T4 差异未把 `formBindings`、附件负责人或工序开始上传人替代为批记录表单来源，也未用批记录表单反推表单槽位。
- Main command: `mvn -pl yudao-module-mes "-Dtest=MesProRouteVersionAndCopyTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProEdhrBatchExecutionLegacyProcessTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrBatchExecutionTaskGateTest,MesProEdhrRehearsalReadinessServiceTest,MesProEdhrWorkTaskLegacyProcessTest,MesProBatchRecordExecutionFieldAuditQueryExportServiceTest,MesProRouteProcessServiceImplBatchRecordBindingTest,MesProRouteServiceImplDisplayFieldsTest,MesProRouteServiceImplTest" test`
- Main result: `PASS`，2026-07-27 23:18:22 +08:00 完成；`242 tests`, `0 failures`, `0 errors`, `0 skipped`, `BUILD SUCCESS`。
- Notification regression command: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrWorkTaskServiceImplTest" test`
- Notification regression result: `PASS`，2026-07-27 23:18:50 +08:00 完成；`66 tests`, `0 failures`, `0 errors`, `0 skipped`, `BUILD SUCCESS`。既有 66 个用例继续覆盖每任务按 `candidateUserSnapshot` 全部有效候选人逐人发送、任务内去重且不跨任务合并候选人。
- Responsibility export high-risk command: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionFieldAuditQueryExportServiceTest" test`
- Responsibility export high-risk result: `PASS`，2026-07-27 23:20:10 +08:00 完成；`8 tests`, `0 failures`, `0 errors`, `0 skipped`, `BUILD SUCCESS`。其中 `responsibilityExportRejectsIncompleteOverallEvidence` 与 `responsibilityExportRejectsBlockedEvidenceAndPreservesUnknownSummaryOrigin` 确认 `EVIDENCE_MISSING`、`BLOCKED` 均 fail-fast；`responsibilityExportCreatesCompleteSnapshotWorkbookWithoutSensitiveColumns` 与相邻完整证据用例确认 `COMPLETE` 仍可生成导出。
- Action-lock high-risk command: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionTaskGateTest,MesProEdhrBatchExecutionServiceTest#openTask_pendingReleaseAllowsApprovedOrdinaryFillCompletedBeforeClose+get_releasePendingApproval_locksNormalTaskActions" test`
- Action-lock high-risk result: `PASS`，2026-07-27 23:21:27 +08:00 完成；`8 tests`, `0 failures`, `0 errors`, `0 skipped`, `BUILD SUCCESS`。`openTask_pendingReleaseAllowsApprovedOrdinaryFillCompletedBeforeClose` 确认待放行状态下已完成/已批准的普通路线表单仍保留 `OPEN_FORM`；`get_releasePendingApproval_locksNormalTaskActions` 确认未完成普通任务 `allowedActions=[]` 且 disabled reason 为放行审批锁原因；`MesProEdhrBatchExecutionTaskGateTest` 确认顺序表单需前序达到 `APPROVED` 后才放行，未扩大后续任务权限。
- Diff command: `git diff --check -- <T4 twelve whitelist paths>`
- Diff result: `PASS`，exit code `0`，无 whitespace error；仅有 Git 既有 LF/CRLF 转换提示。T4 白名单文件 SHA-256 已记录用于验证期间并发改写核对。
- Overall result: `PASS`。T4 / TC-05 / AC-08、AC-09、AC-12、AC-13、AC-17 在指定独立验证范围内满足放行条件。
- Unresolved issues: T4 范围内无未解决问题。本次独立测试仅修改 `test-report.md`，未修改产品代码、测试代码、`task-state.json`、`execution-log.md` 或规划文档，未提交、未推送。任务整体仍需 T5、T7-T9 与最终 `mvn -pl yudao-module-mes test` 放行；本次 T4 PASS 不代表完整 MES 回归或任务整体完成。

## T5 Executor Verification

- Task ID: `20260727-edhr-notify-all-valid-candidates`
- Test case: `TC-06`
- Acceptance mapping: `AC-10`, `AC-13`, `AC-17`
- Expected: 非缺失 Excel 夹具依赖的批记录 JSON、布局、形状规则、路线候选治理、路线生成、Word 真实样本和相邻高风险服务用例全部通过；不得新增模板名特例、宽松断言、跳过、fallback、默认成功或吞异常。
- Main combo result: `PASS`。`mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordRouteCandidateGovernanceTest,MesProBatchRecordRouteGenerationCodeRuleTest" test` -> `152 tests`, `0 failures`, `0 errors`, `6 skipped`, `BUILD SUCCESS`。
- Word fixture result: `PASS`。`mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordRouteARecognizerTest,MesProBatchRecordRouteFRecognizerTest,FullWordTableInventoryProbeTest,MesProBatchRecordJingxiTableStructureVerificationTest,TmpPrintBatchRecordTableTest" test` -> `51 tests`, `0 failures`, `0 errors`；项目内 `fixtures/pressure-pump-record.doc` 已有 SHA-256 权威性证据。
- Targeted rerun result: `PASS`。`mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldKeepProductionBatchSummaryBehindRoughWashSideColumn,TmpPrintBatchRecordTableTest" test` -> 2026-07-28 01:11:02 +08:00 完成；`5 tests`, `0 failures`, `0 errors`, `0 skipped`, `BUILD SUCCESS`。
- Adjacent regression result: `PASS`。`Sheet1MachineryProcessExcelParserTest`、`MesProBatchRecordExecutionFieldResponsibilityServiceTest#export_failsFastWhenEvidencePackageIsBlocked`、`MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenUpgradingRoute_keepsStableProcessConnectionInfoOnActiveRoute` 均已通过。
- Full MES command: `mvn -pl yudao-module-mes test`
- Full MES result: `BLOCKED`，2026-07-28 01:15:20 +08:00 完成；`2511 tests`, `0 failures`, `4 errors`, `18 skipped`, `BUILD FAILURE`。四个 errors 均为 `java.nio.file.NoSuchFileException: D:\ocr2\resource\球囊扩张导管工序(1).xlsx`。
- Affected suites: `Sheet1RouteExcelParserTest`、`Sheet1RouteExcelImportServiceImplTest`、`Sheet1RouteExcelImportServiceImplDbTest`。
- Diff result: `PASS`。T5 目标路径 `git diff --check` 退出码 `0`，无 whitespace error；仅有 Git 既有 LF/CRLF 转换提示。
- Overall result: `BLOCKED`。T5 非 Excel 权威夹具阻塞范围满足验证条件；完整任务仍需用户确认权威 Excel 原件或提供项目内正式 fixture 后重新运行 T7/T9。

## T7 Executor Verification

- Task ID: `20260727-edhr-notify-all-valid-candidates`
- Test case: `TC-02`
- Acceptance mapping: `AC-01`, `AC-03`, `AC-13`, `AC-17`
- Expected: Sheet1 路线解析、导入和 DB 回滚测试不得依赖个人桌面或 `D:\ocr2` 固定盘符；必须读取项目内稳定资源路径，资源缺失时 fail-fast，不得合成、跳过、默认成功或使用未经确认的候选副本。
- Path governance result: `PASS`。`Sheet1RouteExcelParserTest`、`Sheet1RouteExcelImportServiceImplTest`、`Sheet1RouteExcelImportServiceImplDbTest` 已改为通过 `Sheet1RouteExcelTestFixtures` 读取 `fixtures/sheet1-route-balloon-catheter.xlsx`，移除三处 `D:\ocr2\resource\球囊扩张导管工序(1).xlsx` 硬编码。
- Targeted command: `mvn -pl yudao-module-mes "-Dtest=Sheet1RouteExcelParserTest,Sheet1RouteExcelImportServiceImplTest,Sheet1RouteExcelImportServiceImplDbTest" test`
- Targeted result: `BLOCKED`，2026-07-28 01:23:39 +08:00 完成；`8 tests`, `0 failures`, `4 errors`, `0 skipped`, `BUILD FAILURE`。四个 errors 均为 `java.nio.file.NoSuchFileException: src/test/resources/fixtures/sheet1-route-balloon-catheter.xlsx`，测试编译和 Spring context 边界正常。
- Candidate evidence: `C:\Users\BJB110\Desktop\球囊扩张导管工序(1)(2).xlsx` 与 `C:\Users\BJB110\Desktop\文档\球囊扩张导管工序(1)(2).xlsx` 均为 `17251` 字节，SHA-256 均为 `A7ACF4ADE2E09A00B68D80701B1FB86BC79B6F3CCDA55504B7C838AB85240354`，但仍缺用户明确权威性确认。
- Full MES rerun: `mvn -pl yudao-module-mes test` -> `BLOCKED`，2026-07-28 01:30:40 +08:00 完成；`2511 tests`, `0 failures`, `4 errors`, `18 skipped`, `BUILD FAILURE`。四个 errors 均为 `src/test/resources/fixtures/sheet1-route-balloon-catheter.xlsx` 缺失，无其他 failure/error。
- Overall result: `BLOCKED`。解除条件为确认权威 Excel 原件并加入 `src/test/resources/fixtures/sheet1-route-balloon-catheter.xlsx` 后重新运行 Sheet1 套件与完整 MES 回归。

## T7 Scope Change Verification

- User decision: Sheet1 Excel 真实样本覆盖“不需要覆盖这个”，缺失 `sheet1-route-balloon-catheter.xlsx` 不再作为本任务验收前置。
- Removed coverage: 删除依赖该缺失真实 Excel 的 `Sheet1RouteExcelImportServiceImplTest`、`Sheet1RouteExcelImportServiceImplDbTest` 和 `Sheet1RouteExcelTestFixtures`；删除 `Sheet1RouteExcelParserTest.parseFixture_returnsTwoRoutesWithFirstAppearanceDeduplicatedSteps`。
- Preserved coverage: `Sheet1RouteExcelParserTest` 保留 4 个合成 fail-fast/契约测试，覆盖缺少 Sheet1、表头无效、产品重复、产品块无工序。
- Targeted result: `mvn -pl yudao-module-mes "-Dtest=Sheet1RouteExcelParserTest" test` -> PASS；4 tests、0 failures、0 errors、0 skipped。
- Adjacent fix result: `MesProBatchRecordExecutionFieldAuditServiceTest` 和 `MesProEdhrWorkTaskLegacyProcessTest` 在完整套件中分别为 44 tests、3 tests，均 0 failures、0 errors。

## Final MES Verification

- Command: `mvn -pl yudao-module-mes test`
- Result: `PASS`，2026-07-28 08:53:25 +08:00 完成；2530 tests、0 failures、0 errors、18 skipped，`BUILD SUCCESS`。
- Concurrent regression RED: `mvn -pl yudao-module-mes test` -> `FAIL`，2026-07-28 12:19:18 +08:00 完成；2537 tests、4 failures、2 errors、18 skipped。失败原因为传统批记录打开链路误把 eDHR 批次任务 ID 当成 execution 的排产 `task_id`。
- Concurrent regression targeted GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProEdhrBatchExecutionServiceTest" test` -> `PASS`，2026-07-28 12:36:59 +08:00 完成；246 tests、0 failures、0 errors、0 skipped。
- Notification / adjacent regression: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrWorkTaskServiceImplTest,Sheet1RouteExcelParserTest,MesProBatchRecordCellLinkControllerTest,MesProBatchRecordCellLinkServiceImplTest" test` -> `PASS`，2026-07-28 12:37:35 +08:00 完成；81 tests、0 failures、0 errors、0 skipped。
- Latest command: `mvn -pl yudao-module-mes test`
- Latest result: `PASS`，2026-07-28 12:41:40 +08:00 完成；2537 tests、0 failures、0 errors、18 skipped，`BUILD SUCCESS`。
- Current-worktree RED: `mvn -pl yudao-module-mes test` -> `FAIL`，2026-07-28 13:13:48 +08:00 完成；2539 tests、1 failure、2 errors、18 skipped。失败仍为传统 execution 上下文误纳入 eDHR 批次任务 ID 的同类风险；清理 stale target class 并恢复源码正式口径后定向通过。
- Current-worktree GREEN: `mvn -pl yudao-module-mes clean test "-Dtest=MesProBatchRecordExecutionServiceImplTest#entryContextAndOpenOrCreateByContext_ignoreScheduleTaskFieldsForFutureExecutionContext+openOrCreateByContext_doesNotPersistScheduleTaskFieldsForNewExecution+openOrCreateByContext_reusesSubmittedExecutionForActiveStatus"` -> `PASS`，2026-07-28 13:17:57 +08:00 完成；3 tests、0 failures、0 errors。
- Current-worktree adjacent GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProEdhrBatchExecutionServiceTest" test` -> `PASS`，2026-07-28 13:18:53 +08:00 完成；247 tests、0 failures、0 errors。
- Current-worktree cell-link GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest" test` -> `PASS`，2026-07-28 13:25:22 +08:00 完成；5 tests、0 failures、0 errors。
- Current-worktree final command: `mvn -pl yudao-module-mes test`
- Current-worktree final result: `PASS`，2026-07-28 13:29:43 +08:00 完成；2540 tests、0 failures、0 errors、18 skipped，`BUILD SUCCESS`。
- Diff/skip review: 未使用 `@Disabled`、Maven excludes、assumptions、空夹具、合成 workbook 或桌面候选文件替代真实 fixture；删除的是用户明确取消的真实样本覆盖入口。
- Closeout state: 实现、验证、cleanup preview/apply、最终提交和推送均已完成。
