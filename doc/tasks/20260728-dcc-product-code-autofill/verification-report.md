# Verification Report

## Scope

- DCC 受控文件提交页红框“产品编号”自动带出。
- 权威来源：DCC 项目代码 `projectCode`。

## Commands

- `pnpm e2e:dcc:upload-product-autofill:static` -> PASS。
- `pnpm e2e:dcc:product-category-rule:static` -> PASS。
- `pnpm e2e:dcc:upload-project-taxonomy-revision:static` -> PASS。
- `pnpm e2e:dcc:upload-current-version:static` -> PASS。
- `node tests/e2e/dcc-optional-product-binding-static.spec.js` -> PASS。
- `mvn -pl yudao-module-dcc -am "-DskipTests" compile` -> PASS。
- `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_dhfCategoryUsesDccProjectCodeAsProductNumber+submitControlledFile_dhfCategoryRequiresProjectCodeProductNumber" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_success+submitControlledFile_rejectsInvalidProductCode+submitControlledFileWithoutApproval_allowsEmptyProductBinding" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `scripts/preflight/login-preflight.mjs --target-path /dcc/controlled-file/upload` -> PASS。
- `inline Playwright readonly DCC project-code product-number E2E` -> PASS。
- `pnpm ts:check` -> BLOCKED，无关 MES 历史类型错误。

## Result

- 前端上传页已改为只读红框产品编号，选择 DCC 项目后自动显示 `DccProjectCodeRespVO.projectCode`。
- 后端受控上传 + DHF/DMR 类别已使用 `DccProjectCodeDO.projectCode` 落库为 `productCode`，并清空 `productMasterId`。
- 真实页面 E2E 通过：本机 `http://127.0.0.1:8081` 登录 `芋道源码/admin`，选择项目 `按压式球囊扩充压力泵 / IDI` 和类别 `DCC_FVM_DHF_001 / 市场调研报告` 后，红框产品编号显示 `IDI`。
- 真实页面 E2E 观测：DCC 写请求 `0`，其它业务主数据选项请求 `0`，浏览器 console error `0`。

## Remaining Risk

- `pnpm ts:check` 未通过，阻塞点为无关 MES 组件 `BatchRecordCellRulesConfirmDialog.vue` 中 `assistPreviewRows` 类型缺失；未在本任务范围内修复。
- 本任务不再存在“需要匹配其它产品样本”的前置，因为用户已确认 DCC 项目代码是唯一权威来源。
