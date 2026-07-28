# Frontend Feature Evidence

## Feature Goal

- DHF/DMR 类受控文件提交时，“产品编号”只读显示所选 DCC 项目的 `projectCode`。
- 用户纠正后的权威来源是 DCC 项目代码，不匹配、不选择、不依赖其它业务数据源。

## Non-Goals

- 不新增业务数据源。
- 不改动非 DHF/DMR 类别的既有可选绑定路径。
- 不新增提交页手动输入或临时编号生成能力。

## Requirements And Acceptance

- 选择 DCC 项目后，`formData.productCode` 必须等于 `selectedProjectCode.projectCode.trim()`。
- DHF/DMR 类别下，如果所选 DCC 项目没有项目代码，提交前必须提示“请选择包含项目代码的 DCC 项目”。
- 上传页不得加载产品选项、不得展示产品选择器、不得出现匹配其它数据源的提示。
- 提交 payload 中 `productMasterId` 清空，`productCode` 由 DCC 项目代码带出。

## UI And Owned Files

- 页面入口：`/dcc/controlled-file/upload`。
- 组件：`IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue`。
- 提交器：`IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts`。
- 静态合同：`IntRuoyiFronted/tests/e2e/dcc-upload-product-autofill-static.spec.js`、`IntRuoyiFronted/tests/e2e/dcc-product-category-rule-static.spec.js`。

## API Contracts And Data States

- DCC 项目候选来自 `/dcc/project-codes/page`，字段为 `DccProjectCodeRespVO.projectCode`。
- 文件类别仍来自 `/dcc/file-categories`，DHF/DMR 必填口径仍按 `DCC_FVM_DHF_` / `DCC_FVM_DMR_` 类别编码前缀。
- 前端不调用 `/dcc/controlled-files/product-options` 或其它产品选项接口完成红框产品编号。

## BDD Scenarios

- `BDD: DCC 项目代码自动带出产品编号 -> Given 用户选择启用 DCC 项目 / When 项目包含 projectCode / Then 产品编号只读字段显示该 projectCode。`
- `BDD: DHF/DMR 类别缺项目代码 fail-fast -> Given 用户选择 DHF/DMR 类别 / When 当前 DCC 项目 projectCode 为空 / Then 页面阻止提交并提示选择包含项目代码的 DCC 项目。`
- `BDD: 不查询其它业务数据源 -> Given 产品编号权威来源是 DCC 项目代码 / When 用户选择 DCC 项目和文件类别 / Then 页面不加载产品选项接口。`

## RED / GREEN

- `RED: pnpm e2e:dcc:upload-product-autofill:static -> FAIL, 旧上传页仍依赖产品选择器，未使用 DCC 项目代码。`
- `RED: pnpm e2e:dcc:product-category-rule:static -> FAIL, 旧校验仍要求产品选择。`
- `GREEN: pnpm e2e:dcc:upload-product-autofill:static -> PASS。`
- `GREEN: pnpm e2e:dcc:product-category-rule:static -> PASS。`

## Verification

- `pnpm e2e:dcc:upload-project-taxonomy-revision:static` -> PASS。
- `pnpm e2e:dcc:upload-current-version:static` -> PASS。
- `node tests/e2e/dcc-optional-product-binding-static.spec.js` -> PASS。
- `scripts/preflight/login-preflight.mjs --target-path /dcc/controlled-file/upload` -> PASS。
- `inline Playwright readonly DCC project-code product-number E2E` -> PASS，`selectedProject=按压式球囊扩充压力泵 / IDI`，`selectedCategory=DCC_FVM_DHF_001 / 市场调研报告`，`productNumber=IDI`，`writeRequestCount=0`，`productMasterRequestCount=0`，`consoleErrorCount=0`。
- `pnpm ts:check` -> BLOCKED，无关 MES 历史类型错误 `assistPreviewRows`。

## Responsive, Accessibility, Loading, Empty, Error, Permission

- UI 控件从选择器改为只读输入框，避免用户误选其它来源。
- 来源说明显示“来源：DCC 项目代码 项目名称 / 项目代码”。
- 缺项目代码时保留可见错误提示，不吞异常、不默认成功。
- 类别上传权限仍沿用当前 `canUpload` 投影和后端类别权限校验。

## Blockers

- 无当前 DCC 产品编号功能阻塞。
- 全量类型检查存在无关 MES 历史阻塞，已在执行日志中记录。
