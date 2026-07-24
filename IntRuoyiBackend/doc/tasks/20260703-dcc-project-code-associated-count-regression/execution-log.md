# Execution Log - DCC项目代码关联文件数回归修复

BDD: 项目代码列表统计主表已识别关联文件 -> Given 受控文件主表记录已关联 DCC 项目代码但明细历史记录未回填, When 用户打开 DCC 项目代码列表, Then 关联文件数应统计该主表关联并显示非 0。
BDD: 项目代码列表按真实关联文件数排序 -> Given 不同项目代码拥有不同主表关联文件数, When 用户按关联文件数排序, Then 列表按真实统计值升序或降序排列。

STATUS: task-doc -> CREATED
GREEN: experience-preflight -> PASS, PowerShell and bug-regression-fix-loop gates read; no high-risk E2E/server/database write executed.

RED: mvn -pl yudao-module-dcc "-Dtest=cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapperTest#selectAssociatedFileCounts_includesSuccessfulRecognitionLedgerWhenFileFieldMissing" test -> FAIL, expected project codes [1001, 1002] but current query returned [1002], proving successful recognition ledger associations were omitted when file field was not backfilled.
GREEN: mvn -pl yudao-module-dcc "-Dtest=cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapperTest#selectAssociatedFileCounts_includesSuccessfulRecognitionLedgerWhenFileFieldMissing" test -> PASS.
GREEN: mvn -pl yudao-module-dcc "-Dtest=cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapperTest#selectAssociatedFileCounts_mapsGroupedCountRows,cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeServiceImplTest#pageShouldIncludeAssociatedFileCountAndSortByCount" test -> PASS, 2 tests.
ROOT_CAUSE: associated file count only grouped `dcc_controlled_file.dcc_project_code_id`; files with successful recognition ledger rows but missing field backfill were counted as zero on the project-code list.
FIX: `DccControlledFileMapper.selectAssociatedFileCountsByProjectCodeIds` now uses file field first and falls back to each file's latest successful recognition ledger `matched_project_code_id`, grouped by effective project code.

STATUS: task -> COMPLETED
