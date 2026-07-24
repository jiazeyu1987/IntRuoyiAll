# Execution Log

BDD: 测试服 SQL 快应用 -> Given 操作者在运行控制台选择一个本机 SQL 文件 / When 提交 `测试服数据库快应用` / Then 系统只将该 SQL 应用到测试服 `ruoyi-vue-pro` 数据库，并记录操作日志和健康检查结果。

BDD: 缺少前置条件直接失败 -> Given SQL 文件不存在、目标主机不是测试服、测试库缺少基础表或 MySQL 执行失败 / When 执行快应用 / Then 动作必须失败并输出明确原因，不得返回默认成功。

BDD: 快应用不替代发布 -> Given SQL 快应用执行成功 / When 用户查看发布包状态 / Then 不应标记发布包为测试通过，也不应同步业务数据、MinIO 或 NAS 产物。

GREEN: experience-preflight -> PASS，仅实现受控入口和本地静态/单元验证；本轮不实际连接测试服务器、不执行真实远程 SQL、不重启、不发布、不操作正式服。

RED: python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py -q -> FAIL，缺少 `apply-test-db-sql` 动作和 `script/deploy/apply-test-db-sql.ps1`。

RED: mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#executeApplyTestDbSqlShouldDispatchOnlyTestServerScript+executeApplyTestDbSqlShouldRequireSqlPath+executeNonDbQuickApplyActionShouldRejectSqlPath test -> FAIL，`RuntimeControlActionReqVO` 缺少 `setSqlPath(String)`。

RED: node tests\e2e\runtime-control-test-db-quick-apply-static.spec.js -> FAIL，前端运行控制台缺少测试服数据库快应用入口。

GREEN: python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py -q -> PASS，6 passed。

GREEN: mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#executeApplyTestDbSqlShouldDispatchOnlyTestServerScript+executeApplyTestDbSqlShouldRequireSqlPath+executeNonDbQuickApplyActionShouldRejectSqlPath test -> PASS，3 tests。

GREEN: node tests\e2e\runtime-control-test-db-quick-apply-static.spec.js -> PASS。

GREEN: mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test -> PASS，65 tests。

GREEN: node tests\e2e\runtime-control-static.spec.js -> PASS。

GREEN: node tests\e2e\runtime-control-release-package-static.spec.js -> PASS。

BLOCKER: pnpm ts:check -> FAIL，第一次失败于 Node 默认堆内存上限；`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` 后暴露无关既有类型错误：`src/views/mes/pro/route-use/RouteUsePage.vue` 保存批记录报表缺少 `recordCategory` / `validationProfile`。本次未修改该文件。

GREEN: powershell scriptblock parse -> PASS，`script/deploy/apply-test-db-sql.ps1` 语法可解析。

GREEN: database-schema-evidence-validator -> PASS。

GREEN: ci-cd-environment-evidence-validator -> PASS。

GREEN: task-closeout-cleanup preview -> PASS，keep `task.md`、`execution-log.md`、`database-schema-evidence.md`，delete/blocked/warnings 均为 `<none>`。

GREEN: git diff --check -> PASS，仅 Git 自动 CRLF 提示，无空白错误。
