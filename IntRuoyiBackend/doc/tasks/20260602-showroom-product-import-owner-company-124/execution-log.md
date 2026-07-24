# Execution Log

BDD: 正式版产品导入不应因当前产品过期所属公司 ID 阻塞 -> Given 当前产品 `product_001` 的 `owner_company_id` 指向已不存在的公司 ID `124`，且导入 Excel 第 2 行明确填写可识别的持证公司 `瑛泰` / When 用户导入正式版产品资料并发布变更 / Then 系统应使用导入行的持证公司完成可验证校验并发布文本变更，不能因旧的当前产品公司 ID 反查缺失失败。

## Evidence

- INFO: 2026-06-02 已确认上一个后端任务 `20260602-runtime-console-build-deploy-test-media` 状态为 `completed`。
- INFO: 初步定位报错来自 `ShowroomApiRuntime.resolveOwnerCompanyExcelContract()` 对当前产品 `owner_company_id` 执行 `contentService.getCompany(Long.valueOf(...))`，当前 ID `124` 不存在时转换为 `当前产品所属公司不存在，无法导入发布：124`。
- RED: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldResolveOwnerCompanyFromFormalRowWhenCurrentOwnerIdIsStale' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, 新增回归测试复现 `ShowroomProductImportFailureRespVO[rowNo=2, productCode=product_001, reason=当前产品所属公司不存在，无法导入发布：124]`，期望导入成功但 `successCount=0`。
- GREEN: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldResolveOwnerCompanyFromFormalRowWhenCurrentOwnerIdIsStale' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，1 test。
- GREEN: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，27 tests。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260602-showroom-product-import-owner-company-124\execution-log.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-showroom-product-import-owner-company-124 --mode preview` -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`。

## Bug Regression Summary

Bug: 导入 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料正式版.xlsx` 时，第 2 行 `product_001` 返回 `当前产品所属公司不存在，无法导入发布：124`。

Expected: 当正式版 Excel 第 2 行明确提供 `持证公司=瑛泰` 时，导入应以当前租户公司主数据解析该持证公司，发布产品文本变更，并将产品草稿的 `owner_company_id` 写为解析后的有效公司 ID。

Reproduction: 新增回归测试构造当前产品 `product_001` 的旧 `owner_company_id=124` 且公司表中不存在 124，Excel 行填写 `持证公司=瑛泰`；修复前导入结果 `successCount=0`，失败原因与用户反馈一致。

Root Cause: 产品导入把 Excel 的“持证公司”只作为当前产品所属公司的校验文本，校验前必须先用当前产品旧 `owner_company_id` 反查公司；当旧 ID 已不再存在于公司主数据时，导入无法利用 Excel 中的有效持证公司修正产品主数据。

Regression Test: `ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldResolveOwnerCompanyFromFormalRowWhenCurrentOwnerIdIsStale` 覆盖旧 `owner_company_id=124`、正式版 `持证公司=瑛泰` 的导入场景，并断言发布后产品 `owner_company_id` 被写为当前公司 ID。

Verification: targeted 回归测试 1 条通过，整组 `ShowroomProductExcelImportExportIntegrationTest` 27 条通过。

Risk and Regression Scope: 影响范围为展厅产品 Excel 导入时的持证公司解析。导入行持证公司为空时仍保留当前产品字段；持证公司非空时必须能在当前租户公司主数据中唯一解析，否则明确失败，不引入 fallback、降级或吞异常。

Blockers and Follow-up: 无阻塞。未操作服务器或远程环境；本次仅完成本机后端代码与测试验证。
