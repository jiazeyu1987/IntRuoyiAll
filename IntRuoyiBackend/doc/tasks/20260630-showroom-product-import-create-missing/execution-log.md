# Execution Log：产品 Excel 导入支持缺失即创建（后端）

- `2026-06-30 任务创建`：建立后端任务文档，目标是把现有产品 Excel 导入扩展为“缺失即创建，存在即更新”。
- `BDD: 导入行缺失目标 productCode 时创建新产品 -> Given Excel 行里的 productCode 在当前租户不存在 / When 调用产品 Excel 导入 / Then 系统创建新产品与首个发布版本，并把 productCode 记入 successProductCodes。`
- `BDD: 导入行命中已存在 productCode 时继续更新发布 -> Given 当前租户已经存在该 productCode / When 调用产品 Excel 导入 / Then 系统沿用既有更新/发布能力，不破坏跳过/覆盖语义。`
- `BDD: 奖项页签导入合同保持兼容 -> Given Excel 同时包含奖项页签 / When 执行产品 Excel 导入 / Then 奖项导入仍按现有合同成功，不因新增产品创建路径回退。`
- `INFO: task-switch-resolved -> 用户已恢复本任务，继续推进后端导入链路实现。`
- `RED: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldPublishChangedRowsSkipUnchangedRowsAndPreserveMediaFields" -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL, ShowroomAdminController 新增 ShowroomHallConfigPackageService 依赖后，旧测试上下文缺少该 Bean，无法装配。`
- `RED: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldPublishChangedRowsSkipUnchangedRowsAndPreserveMediaFields" -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL, 补齐 @MockBean 后，原断言仍要求缺失产品 IMPORT-404 导入失败且 successCount=1；新合同要求创建成功，因此继续保持红灯。`
- `GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldPublishChangedRowsSkipUnchangedRowsAndPreserveMediaFields" -Dsurefire.failIfNoSpecifiedTests=false test -> PASS`，缺失产品 IMPORT-404 已创建成功并计入成功导入，同时既有产品更新与媒体字段保留语义未回退。`
- `GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReviveSoftDeletedProductWithSameCode+importProductExcelShouldOnlyReadProductListSheet" -Dsurefire.failIfNoSpecifiedTests=false test -> PASS`，锁定软删同码复活与“只读取产品列表 sheet”回归语义。`
- `INFO: root-cause-closed -> 真实导入 `209` 行的根因是旧实现把 `奖项` sheet 当成产品行读取；真实导入 `INT-166` 失败的根因是目标租户同码产品软删后仍占用 `(tenant_id, product_code)` 唯一键。`
- `GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" -Dsurefire.failIfNoSpecifiedTests=false test -> PASS`，`40` 个导入集成测试全部通过。`
- `GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package -> PASS`，后端最新可执行包已重新构建，可支撑根任务真实回导验收。`
- `GREEN: root-task-real-import-verification -> PASS`，根任务真实测试租户导入结果为产品 `164/164` 成功、奖项 `46/46` 成功，证明本次后端合同扩展已在本机运行态闭环。`
RED: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldPublishChangedRowsSkipUnchangedRowsAndPreserveMediaFields" -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL, 补齐 @MockBean 后原断言仍要求缺失产品 IMPORT-404 导入失败，和“缺失即创建”新合同冲突。
GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" -Dsurefire.failIfNoSpecifiedTests=false test -> PASS, 40 个导入集成测试全部通过并覆盖缺失即创建、软删复活、只读产品列表 sheet。
