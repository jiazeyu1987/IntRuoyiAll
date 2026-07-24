# Execution Log

BDD: all references listed -> Given 批记录 V1.0 无主表单但存在多类历史引用, When 用户重新导入 Word, Then 系统一次性列出所有引用位置和删除入口说明。
GREEN: experience-preflight -> PASS, 已读取 PowerShell 门禁、经验索引，并使用 backend-api-delivery 技能。
RED: mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenMultipleHistoricalReferencesRemain_listsAllCleanupEntrances" "-DfailIfNoTests=false" test -> FAIL, 当前只返回第一条历史引用，未一次性列出所有引用和删除入口。
GREEN: mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenMultipleHistoricalReferencesRemain_listsAllCleanupEntrances+recognizeUploadedRoute_whenNoMainReportsButExecutionExistsWithoutUpgrade_rejectsAsExistingBatchRecord+recognizeUploadedRoute_whenNoMainReportsButExecutionExists_rejectsVersionResetBlocked+recognizeUploadedRoute_whenExistingVersionWithoutMainReportsHasExecution_rejectsV1Reset+recognizeUploadedRoute_whenOnlyDefinitionVersionsRemain_cleansOrphanAndStartsFromV1" "-DfailIfNoTests=false" test -> PASS, 5 tests，覆盖多引用一次性清单与无引用 V1 重导回归。
