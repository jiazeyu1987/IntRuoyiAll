# Backend API Evidence

## Scope

- Service scope：`DccControlledFileWorkflowServiceImpl.prepareSubmitContext` 与 `insertControlledFile`。
- Behavior slice：受控上传 + DHF/DMR 类别提交时，产品编号来自 DCC 项目代码。

## API And Data Contract

- 请求仍使用 `DccControlledFileSubmitReqVO.dccProjectCodeId`、`categoryId`、`productCode`、`productMasterId`。
- 对受控上传的 DHF/DMR 类别，后端以 `dccProjectCodeId` 读取启用的 `DccProjectCodeDO`。
- 落库字段：`dcc_controlled_file.product_code = DccProjectCodeDO.projectCode`，`product_name = DccProjectCodeDO.projectName`，`product_master_id = null`。
- 非受控上传或非 DHF/DMR 类别的既有可选绑定路径保持不变。

## Auth, Permissions, Validation, Error Behavior

- 类别上传权限仍通过 `DccControlledFileCategoryPermissionSupport` 校验。
- DHF/DMR 类别缺少启用 DCC 项目或项目代码为空时，抛出 `CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING`。
- 后端不吞异常、不默认补值、不切换到其它数据源。

## Required Config, Services, Fixtures, Migrations

- 无表结构迁移。
- 单元测试通过 mock `projectCodeMapper.selectById(3000L)` 提供启用 DCC 项目代码。
- 无新增外部服务依赖。

## BDD Scenarios

- `BDD: DHF/DMR 提交使用 DCC 项目代码 -> Given 受控上传选择 DHF/DMR 类别和启用 DCC 项目 / When 提交受控文件 / Then 落库 productCode 为该项目 projectCode，productMasterId 为空。`
- `BDD: DHF/DMR 缺项目代码 fail-fast -> Given 受控上传选择 DHF/DMR 类别但 DCC 项目 projectCode 为空 / When 提交受控文件 / Then 后端拒绝提交并不插入文件。`
- `BDD: 相邻旧路径保持 -> Given 非 DHF/DMR 或无审批路径允许可选绑定 / When 提交 / Then 既有成功、无效编号、空绑定回归不被破坏。`

## RED / GREEN

- `RED: mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_dhfCategoryUsesDccProjectCodeAsProductNumber" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 旧服务仍要求 productMasterId 并抛出 CONTROLLED_FILE_PRODUCT_MASTER_INVALID。`
- `GREEN: mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_dhfCategoryUsesDccProjectCodeAsProductNumber+submitControlledFile_dhfCategoryRequiresProjectCodeProductNumber" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，Tests run: 2, Failures: 0, Errors: 0。`

## Contract And Integration Verification

- `mvn -pl yudao-module-dcc -am "-DskipTests" compile` -> PASS，`BUILD SUCCESS`。
- `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_success+submitControlledFile_rejectsInvalidProductCode+submitControlledFileWithoutApproval_allowsEmptyProductBinding" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 3, Failures: 0, Errors: 0。
- `inline Playwright readonly DCC project-code product-number E2E` -> PASS，只读页面路径验证前端展示，不触发提交写入。

## Observability Touchpoints

- 失败通过既有业务异常码返回。
- 单元测试用 `verify(productApi, never()).getEnabledDccProduct(any())` 证明 DHF/DMR 受控上传路径不调用其它数据源。

## Blockers

- 无当前 DCC 后端阻塞。
