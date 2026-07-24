BDD: 先提交再发布时后端发布输入必须来自已完成任务 -> Given 用户已选择先提交主工作区再发布 / When 后端仓准备提交当前改动 / Then 只允许纳入已经完成、具备 RED/GREEN 证据且验证通过的任务产物。
BDD: 发布输入收口不新增未验证业务行为 -> Given 当前后端改动来自 showroom / SRM / 本地运行态排查任务 / When 重新执行本轮关键验证 / Then 构成发布输入的测试必须全部通过。

GREEN: experience-preflight -> PASS，已按门禁读取 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md` 与 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`，确认本轮只做后端发布输入收口，不新增未验证的业务改动。

GREEN: staged-scope-check -> PASS，已复核当前 staged 改动仅对应以下已完成任务：
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-showroom-product-excel-audio-keyword-roundtrip`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-srm-nas-locator-blacklist-button-missing`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-srm-nas-locator-blacklist-srm-admin-binding`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-srm-nas-locator-wildcard-search-error`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-local-admin-api-48081-connection-refused`

RED: pre-commit-hook-without-task-dir -> FAIL，首次直接提交触发 `.git/hooks/pre-commit` 阻塞：`TDD compliance failed: set TDD_TASK_DIR to the task directory path before committing.`，证明后端仓提交必须绑定明确任务目录。

GREEN: python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_d7_d10_sql_contract.py -k nas_locator -q -> PASS。
GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am "-Dtest=SrmNasLocatorWildcardTenantSqlRegressionTest,SrmNasLocatorServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS。
GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test -> PASS。
