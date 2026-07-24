# Execution Log

BDD: 同一份直接报工文件可重复导入 -> Given 同一 Excel 行已存在历史导入记录且排产任务仍可报工 When 用户再次上传同一份直接报工 Excel Then 系统再次创建新的报工单、提交审批，并通过既有报工链路累加对应任务/工单进度。

BDD: 重排后同一工序存在多条任务 -> Given 同一排产工序下存在多条任务 When Excel 行带有明确任务单号 Then 直报导入优先匹配该任务单号创建报工单；若未精确命中，则按任务数量降序、ID 升序稳定选择任务，避免返回 0。

GREEN: experience-preflight -> PASS, 已读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`docs/login-access.md`；本阶段只做本机代码、本机数据库和本机接口验证，不连接测试服/正式服。

RED: mvn.cmd -pl yudao-module-mes '-Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_whenSameSourceRowAlreadyImported_shouldCreateAnotherFeedback' test -> FAIL, expected importedCount=1 but duplicate source fingerprint returned importedCount=0。

GREEN: mvn.cmd -pl yudao-module-mes '-Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_whenSameSourceRowAlreadyImported_shouldCreateAnotherFeedback' test -> PASS，移除直报导入源行重复跳过后，同文件同源行可再次创建报工单。

GREEN: mvn.cmd -pl yudao-module-mes '-Dtest=ThirdPartyFeedbackImportServiceImplDbTest#importRecordTable_shouldAllowRepeatedSourceRowForDirectWorkReportRetest' test -> PASS，测试 schema 放开 `source_file_sha256 + sheet_name + row_no` 唯一约束。

RED: mvn.cmd -pl yudao-module-mes '-Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_shouldMatchTaskCodeWhenScheduleProcessLinksMultipleTasks' test -> FAIL, expected importedCount=1 but same schedule process linked multiple tasks caused importedCount=0。

GREEN: mvn.cmd -pl yudao-module-mes -DskipTests compile -> PASS。

BLOCKER: targeted-junit -> 无关未跟踪测试 `MesProEdhrReleaseServiceImplTest` 编译失败，访问私有常量 `EXECUTION_STATUS_APPROVED`，导致当前模块 testCompile 无法继续执行目标 JUnit；本任务不修改该外部文件。

GREEN: mvn.cmd -pl yudao-server -am -Dmaven.test.skip=true package -> PASS，本机后端重启为 `E:\Int\CacheData\IntRuoyi\runtime\backend-20260707-182253.jar`，健康检查 `UP`。

GREEN: real-direct-import -> PASS，使用芋道源码/admin 本机 API 上传 `C:\Users\BJB110\Desktop\文档\李萍.xlsx`，返回 `code=0`、`importedCount=18`、`submittedCount=18`、`skippedRows=52`，生成 `FB-000241` 至 `FB-000258`。

GREEN: progress-accumulation -> PASS，工序 `2973` 从 `396 / 39.6%` 增至 `528 / 52.8%`，`2975` 从 `318 / 31.8%` 增至 `424 / 42.4%`，`3085` 从 `540 / 54.0%` 增至 `720 / 72.0%`。