# Execution Log

BDD: 产品导出不应因历史瑛泰所属公司 ID 阻塞 -> Given 产品管理存在历史产品 `product_001`，其 `owner_company_id=124` 已不在当前公司主数据中，但 `product_owner_type=YINGTAI` 表明它属于瑛泰正式公司 / When 用户在产品管理点击导出 / Then 系统应导出产品 Excel，并在 `持证公司` 列写入当前租户唯一瑛泰公司正式名称，不能抛出导入发布错误。

## Evidence

- INFO: 2026-06-02 已确认上一个后端任务 `20260602-showroom-images-broken` 状态为 `completed`。
- INFO: 初步定位报错来自 `ShowroomApiRuntime.toProductExcelRow()` 导出行组装时调用 `resolveOwnerCompanyExcelText()`，历史 `owner_company_id=124` 反查失败后抛出导入发布文案。
- RED: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest#exportProductExcelShouldResolveYingtaiOwnerCompanyWhenCurrentOwnerIdIsStale' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, 新增导出回归测试复现 `java.lang.IllegalStateException: 当前产品所属公司不存在，无法导入发布：124`，失败栈为 `ShowroomApiRuntime.toProductExcelRow()` 调用 `resolveOwnerCompanyExcelText()`。
- GREEN: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest#exportProductExcelShouldResolveYingtaiOwnerCompanyWhenCurrentOwnerIdIsStale' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，1 test。
- GREEN: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，30 tests。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260602-showroom-product-export-owner-company-124\execution-log.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-showroom-product-export-owner-company-124 --mode preview` -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`。

## Bug Regression Summary

Bug: 产品管理点击导出时，历史产品 `owner_company_id=124` 已不在当前公司主数据中，导出行组装抛出 `当前产品所属公司不存在，无法导入发布：124`。

Expected: 产品导出应使用导出专用所属公司文本解析。有效公司 ID 按公司主数据输出；历史瑛泰产品在当前租户存在唯一瑛泰主公司时导出该正式公司名称；无法确定的历史脏数据继续明确失败，且错误语义应为导出产品资料。

Reproduction: 新增回归测试构造当前租户唯一瑛泰公司，并创建 `EXCEL-STALE-OWNER` 产品，字段为 `owner_company_id=124`、`product_owner_type=YINGTAI`；修复前点击导出路径在 `ShowroomApiRuntime.toProductExcelRow()` 抛出用户反馈错误。

Root Cause: 导出复用了导入所属公司解析方法。该方法通过产品旧 `owner_company_id` 反查公司主数据，并在反查失败时抛出导入发布文案；导出路径没有根据产品归属类型处理历史瑛泰产品的旧 ID。

Regression Test: `ShowroomProductExcelImportExportIntegrationTest#exportProductExcelShouldResolveYingtaiOwnerCompanyWhenCurrentOwnerIdIsStale` 覆盖历史瑛泰产品导出成功；`#exportProductExcelShouldFailWhenNonYingtaiOwnerCompanyIdIsStale` 覆盖非瑛泰历史脏 ID 继续失败，避免隐式兜底。

Verification: targeted 导出回归测试 1 条通过，整组 `ShowroomProductExcelImportExportIntegrationTest` 30 条通过。

Risk and Regression Scope: 影响范围为展厅产品 Excel 导出 `持证公司` 列。导入发布解析未改动；导出对非数字 ID、非瑛泰缺失 ID、瑛泰公司不存在或不唯一仍 fail fast。

Blockers and Follow-up: 无阻塞。未操作服务器或远程环境；本次仅完成本机后端代码与测试验证。
