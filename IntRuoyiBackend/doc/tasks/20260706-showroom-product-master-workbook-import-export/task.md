# 任务：展厅导入导出合并产品主数据

## 任务目标
- 展厅产品导出资源包继续保持 zip，但 workbook 增加 `产品主数据` sheet。
- `产品列表` sheet 增加 `旧产品编号` 列，使用现有 `legacy_product_code`。
- 展厅导入时先导入产品主数据，再导入展厅产品、奖项、讲解、关键词等数据；任一阶段失败则整体失败。

## 经验门禁
- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；本任务中文命令输出与文档读写使用显式 UTF-8。
- 后端 API：已读取 `backend-api-delivery` 与 `references/backend-contract.md`；需要记录 API/数据契约、权限、失败语义与 RED/GREEN。
- 数据库/持久化：已读取 `database-schema-delivery` 与 `references/database-contract.md`；本任务不新增表字段，复用现有 MDM 与展厅字段。
- 收尾清理：已读取 `task-closeout-cleanup` 与 `references/closeout-rules.md`；完成后先跑 cleanup preview。

## 设计约束检查
- `是否引入 fallback/降级/吞异常`：否；旧格式缺少 `产品主数据` sheet 时失败，不做兼容降级。
- `是否从根因和长期维护角度解决`：是；将产品主数据导入纳入展厅资源包正式契约，并在导入编排中先写主数据再写展厅数据。
- `是否存在临时补丁或绕过`：否。

## BDD 场景
- `BDD: 导出展厅资源包包含旧产品编号 -> Given 展厅产品存在 legacyProductCode / When 导出展厅产品资源包 / Then 产品列表 sheet 包含旧产品编号列并写出正确值。`
- `BDD: 导出展厅资源包包含产品主数据 -> Given 展厅产品已绑定产品主数据 / When 导出资源包 / Then workbook 包含产品主数据 sheet 且行内容可用于再次导入。`
- `BDD: 导入先导入产品主数据再导入展厅数据 -> Given workbook 同时包含产品主数据和产品列表 / When 导入 / Then MDM 产品先 upsert，展厅产品再绑定 productMasterId 并保存旧产品编号。`
- `BDD: 主数据缺失时失败 -> Given 产品列表引用的展品编码不在产品主数据 sheet 或 MDM 中 / When 导入 / Then 返回明确错误且不写入展厅产品。`

## 里程碑
1. M1：已完成。补充展厅与 MDM 回归测试，锁定缺少旧产品编号列、主数据 sheet 和导入顺序的问题。
2. M2：已完成。扩展 MDM API / Service，提供展厅 workbook 主数据导出与导入能力。
3. M3：已完成。扩展展厅 workbook/zip 构建与解析，增加 `产品主数据` sheet。
4. M4：已完成。调整展厅导入编排，主数据先导入并映射 productMasterId 后再导入展厅数据。
5. M5：已完成。运行定向测试、编译、证据校验与 cleanup preview。
6. M6：已完成。更新任务文档与命令记录，仅提交本任务相关改动。

## 预期验证
- `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -pl yudao-module-mdm -Dtest=*Product*Import*,*Product*Excel* -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -pl yudao-module-showroom,yudao-module-mdm -DskipTests compile`
- backend evidence 校验与 task-closeout-cleanup preview。

## 当前状态
- completed：展厅资源包导入导出已纳入产品主数据 sheet；产品列表已包含 `旧产品编号`；标准导入和底表导入均先导入 MDM 产品主数据，再导入展厅数据。

## 完成记录
- 产品列表 Excel 契约：`旧产品编号` 固定为第二列，后续列顺延。
- 产品主数据 sheet：workbook 固定包含 `产品主数据`，列为 `产品编码 / DCC产品编号 / 中文名称 / 英文名称 / 型号规格 / 产品分类`。
- 导入顺序：controller 解包 workbook 后先读取并导入 `产品主数据`，返回 `productCode -> productMasterId` 映射后再进入展厅产品、奖项、讲解和关键词导入。
- 失败语义：缺少主数据、主数据导入失败、产品列表无法解析到主数据 ID 时均 fail fast；不兼容旧格式缺少主数据 sheet 的导入文件。
- 测试基线：同步 H2 `showroom_product` 测试 schema 的 `frozen_flag / frozen_hall_count` 字段，使其与既有 DO/Mapper 契约一致。

## 最终验证
- GREEN: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#importBaseWorkbookShouldResolveLegacyProductCodeToCurrentIntProduct -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- GREEN: `mvn --% -pl yudao-module-mdm -Dtest=MdmProductServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，4 tests passed。
- GREEN: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，53 tests passed。
- GREEN: `mvn --% -pl yudao-module-mdm -Dtest=*Product*Import*,*Product*Excel* -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，1 test passed。
- GREEN: `mvn --% -pl yudao-module-showroom,yudao-module-mdm -DskipTests compile` -> PASS。

## Cleanup Keep
- `doc/tasks/20260706-showroom-product-master-workbook-import-export/backend-api-evidence.md`
