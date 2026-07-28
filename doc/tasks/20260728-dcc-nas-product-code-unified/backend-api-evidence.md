# Backend API Evidence

## Scope

- Services: `DccControlledFileWorkflowServiceImpl`、`DccExternalFileReviewServiceImpl`、`DccControlledFileMetadataUpdateServiceImpl`、`DccControlledFileNasTransferServiceImpl`、`DccControlledFileMetadataImportExportServiceImpl`、`DccControlledFileFormEffectExecutor`。
- Controllers/VOs: DCC 受控文件 controller、NAS transfer/local folder import request VOs、DCC submit/metadata update request VOs。
- Removed runtime dependency: DCC/NAS 写链路不再通过 `MdmProductApi` 解析产品主数据。

## Contract

- DCC/NAS 写请求以 `dccProjectCodeId` 为正式项目编号来源。
- 后端按 `dccProjectCodeId` 读取启用的 `DccProjectCodeDO`，写入 `productCode=projectCode`、`productName=projectName`、`productMasterId=null`。
- `productCode` 即使前端传入也不是权威来源；后端重新按项目代码计算。
- DCC 响应 VO/DO 保留历史 `productMasterId` 读取兼容，但新写入、提交、更新、导入、NAS transfer 都清空。

## Validation

- 缺少 `dccProjectCodeId`、项目不存在、项目停用、项目代码或项目名称为空时 fail-fast，不引入默认编号、匹配、降级或临时编号。
- 外来评审转交 DCC workflow 时清空 `productMasterId` 并透传 `dccProjectCodeId`。
- NAS transfer、本地文件夹导入、识别迁移导入导出、表单效果执行器统一传入 DCC 项目代码 ID。
- 审计文案从用户可见“产品主数据”切换到 DCC 项目代码/历史产品 ID 口径。

## BDD

- `BDD: DCC/NAS 新写入使用 DCC 项目代码 -> Given 请求包含 dccProjectCodeId / When 提交、外来评审、元数据更新、NAS transfer 或导入 / Then 落库 productCode/productName 来自 DccProjectCodeDO，productMasterId 为 null。`
- `BDD: 后端不再解析产品主数据 -> Given DCC/NAS 写请求包含历史 productMasterId / When 后端处理 / Then 忽略该值且不调用 MdmProductApi。`

## RED

- `RED: target JUnit -> FAIL, workflow 重提交缺少项目代码 ID、识别迁移导出仍断言旧产品编号、外来评审旧断言仍期待 productMasterId=5000。`

## GREEN

- `GREEN: mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest,DccExternalFileReviewServiceImplTest,DccControlledFileMetadataUpdateServiceTest,DccControlledFileNasTransferServiceTest,DccControlledFileMetadataImportExportServiceTest,DccControlledFileFormEffectExecutorTest,DccControlledFileLocalFolderImportControllerTest,DccBaseSchemaTest" test -> PASS, Tests run: 198, Failures: 0, Errors: 0, Skipped: 0。`
- `GREEN: mvn -pl yudao-module-dcc -am "-DskipTests" compile -> PASS。`
- `GREEN: rg MdmProductApi scoped DCC main/test -> PASS, 0 命中。`

## Verification

- Contract tests cover controlled submit, external review submit, metadata update, NAS transfer, local folder import request compatibility, recognition migration import/export, form effect executor, and DCC schema fixture.
- Observability: 既有 service exception 和 error log 路径保留；缺项目代码继续 fail-fast，不吞异常。
- Integration boundary: MDM 模块自身和展厅不在本次修改范围内。

## Blockers

- None. 写入型真实页面 E2E 已通过，并验证新提交记录 `productMasterId=null`。
