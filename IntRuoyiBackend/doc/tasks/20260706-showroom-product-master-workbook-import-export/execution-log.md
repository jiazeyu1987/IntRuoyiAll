# 执行日志：展厅导入导出合并产品主数据

## BDD
- BDD: 导出展厅资源包包含旧产品编号 -> Given 展厅产品存在 legacyProductCode / When 导出展厅产品资源包 / Then 产品列表 sheet 包含旧产品编号列并写出正确值。
- BDD: 导出展厅资源包包含产品主数据 -> Given 展厅产品已绑定产品主数据 / When 导出资源包 / Then workbook 包含产品主数据 sheet 且行内容可用于再次导入。
- BDD: 导入先导入产品主数据再导入展厅数据 -> Given workbook 同时包含产品主数据和产品列表 / When 导入 / Then MDM 产品先 upsert，展厅产品再绑定 productMasterId 并保存旧产品编号。
- BDD: 主数据缺失时失败 -> Given 产品列表引用的展品编码不在产品主数据 sheet 或 MDM 中 / When 导入 / Then 返回明确错误且不写入展厅产品。

## Evidence
- GREEN: experience-preflight -> PASS, 已读取 `docs/powershell-memory.md`、后端 API / 数据库契约技能引用与 task-closeout-cleanup 规则；本任务不执行服务器写入、不发布、不改真实环境数据。

## RED
- RED: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, expected reason: 新增展厅 workbook 主数据契约后，旧测试依赖与 workbook helper 尚未包含 MDM workbook DTO、`产品主数据` sheet 和 `旧产品编号` 表头顺序。
- RED: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, actual reason: 导入阶段已拿到 productMasterId 后，内容服务按真实逻辑调用 MDM `getProduct(id)`，测试桩未返回产品主数据详情，触发 `SHOWROOM_TARGET_NOT_FOUND: product master not found`。
- RED: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, actual reason: 测试 workbook 的 `产品主数据` sheet 使用占位名称，导入后主数据名称覆盖产品列表名称，旧业务断言失败。

## GREEN
- GREEN: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#importBaseWorkbookShouldResolveLegacyProductCodeToCurrentIntProduct -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS；覆盖底表导入旧展品编码时，`产品主数据` sheet 返回 workbook 原始产品编码仍可绑定当前展厅产品主数据。
- GREEN: `mvn --% -pl yudao-module-mdm -Dtest=MdmProductServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，4 tests passed；覆盖展厅 workbook 主数据 upsert 仅影响当前 sheet 行，不执行全量导入停用。
- GREEN: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，53 tests passed；覆盖旧产品编号列、产品主数据 sheet、资源包导出、标准导入、底表导入、讲解/关键词/奖项联动。
- GREEN: `mvn --% -pl yudao-module-mdm -Dtest=*Product*Import*,*Product*Excel* -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，1 test passed。
- GREEN: `mvn --% -pl yudao-module-showroom,yudao-module-mdm -DskipTests compile` -> PASS。

## 实施记录
- MDM API 新增 `exportForShowroomWorkbook(Collection<String>)` 和 `importFromShowroomWorkbook(List<MdmProductShowroomWorkbookRowDTO>)`，DTO 使用展厅 workbook 主数据列契约。
- MDM Service 展厅导入不复用全量 `previewImport/confirmImport`，改为复用行级标准化与校验后只 upsert 当前 workbook 行，避免误停用未包含产品。
- 展厅导出资源包 workbook 新增 `产品主数据` sheet；产品列表新增第二列 `旧产品编号`。
- 展厅导入 controller 先读取 `产品主数据` sheet 并调用 MDM API，成功后把 `productCode -> productMasterId` 传入 runtime，再导入展厅数据。
- Runtime 导入产品行时要求每条展品编码能解析到刚导入/已有的 MDM 主数据 ID，缺失时报 `SHOWROOM_PRODUCT_MASTER_DATA_MISSING`。
- Runtime 对底表导入旧展品编码增加双键解析：优先当前展厅产品编码，其次 workbook 原始展品编码，避免 MDM 主数据导入返回原始编码时误判缺失。
