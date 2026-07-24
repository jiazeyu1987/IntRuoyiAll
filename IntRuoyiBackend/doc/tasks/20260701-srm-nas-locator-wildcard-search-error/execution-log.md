BDD: *MO13*.pdf 在真实查询合同下返回 PDF 命中 -> Given 最新成功快照中存在 MO13 PDF 与非 PDF 文件 / When 服务按 *MO13*.pdf 查询分页 / Then 仅返回 PDF 命中文件。
BDD: SQL LIKE ESCAPE 合同兼容真实 MySQL -> Given 通配关键字里包含被转义的 % 或 _ / When Mapper 执行通配分页查询 / Then 语句使用真实 MySQL 可接受的 ESCAPE 写法且按字面量匹配。
BDD: 普通关键字查询排序保持原样 -> Given 查询关键字不含 * / When 服务分页查询 / Then 仍走原有关键字优先级排序。
INFO: previous-task-completed -> PASS，后端上一任务已完成，可直接开始本轮回归。
GREEN: experience-preflight -> PASS，已命中并读取 `docs/powershell-memory.md` 与 `docs/login-access.md`；本轮真实验证限定为本机只读 NAS定位 查询与本机 Docker MySQL 方言探针，不触发跨环境写入。
RED: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am "-Dtest=SrmNasLocatorWildcardTenantSqlRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL，`mapperXml_shouldUseMysqlSafeEscapeLiteral` 失败，证明 mapper XML 的显式 `ESCAPE` 写法不满足真实 MySQL 合同。
RED: runtime-log-proof -> FAIL，`D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260701-104031.out.log` 记录 `BadSqlGrammarException`，SQL 为 `... LIKE UPPER(?) ESCAPE '\' AND tenant_id = 122`。
GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am "-Dtest=SrmNasLocatorWildcardTenantSqlRegressionTest,SrmNasLocatorServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS
GREEN: powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main -> PASS，本机 48081 切换到 `backend-runtime-control-20260701-134637.jar`。
