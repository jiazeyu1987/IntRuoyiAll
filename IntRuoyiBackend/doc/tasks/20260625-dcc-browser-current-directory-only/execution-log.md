# Execution Log：DCC 受控浏览目录仅显示当前层

BDD: 非递归目录浏览只查当前目录 -> Given 请求携带 directoryId 且未传 includeDescendantDirectories 或传 false / When 查询受控浏览列表 / Then 仅返回当前目录文件，不返回子目录文件。
BDD: 显式递归目录浏览继续包含子目录 -> Given 请求携带 directoryId 且 includeDescendantDirectories=true / When 查询受控浏览列表 / Then 返回当前目录及其子目录文件。
BDD: 最新版本聚合不受目录语义影响 -> Given 浏览页请求 latestVersionOnly=true / When 查询仅当前目录文件 / Then 仍按文件 master 聚合为最新版本一行。

INFO: task-created -> 后端任务文档已创建，等待 RED 回归与最小修复。
RED: `mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增非递归目录浏览合同暴露既有实现会默认递归包含子目录，且首次测试签名需按真实 mapper 调整。
INFO: `mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BLOCKED，首次尝试暴露本任务新测试调用 `selectBrowserSummaryList` 签名假设错误，已按真实 mapper 签名修正测试与实现。
BLOCKER: `mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`yudao-module-dcc` 当前被 `DccCategoryApprovalMatrix*` 相关 VO / Service 编译错误阻塞，Maven 尚未进入本任务定向单测执行阶段。
INFO: approval-matrix-vo-contract -> 已补齐 `DccCategoryApprovalMatrixRespVO`、`DccCategoryApprovalMatrixSaveReqVO`、`DccCategoryReviewMatrixRowRespVO` 缺失的兼容字段与链式 setter 契约，清除本任务外部编译阻塞。
GREEN: `mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
