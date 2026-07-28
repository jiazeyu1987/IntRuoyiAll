# Frontend Feature Evidence

## Feature Goal

- DHF/DMR 类受控文件提交时，“产品编号”从正式产品主数据自动带出；不得临时生成产品编号。

## Non-Goals

- 不新增产品主数据。
- 不修改后端接口契约或数据库结构。
- 不改变非 DHF/DMR 类文件的可选产品绑定规则。

## Requirements And Acceptance

- DHF/DMR 类别要求产品主数据时，若当前 DCC 项目可唯一匹配启用且带 DCC 产品编号的产品主数据，则自动选中该产品并显示 DCC 产品编号。
- 若匹配不到或匹配到多个产品主数据，则不生成临时编号，并提示用户手动选择产品主数据。
- 手动选择产品和自动带出产品使用同一绑定路径，提交 payload 继续发送 `productMasterId` 和正式 `productCode`。

## UI And Owned Files

- 页面入口：受控文件提交页。
- 组件：`IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue`。
- 测试：`IntRuoyiFronted/tests/e2e/dcc-upload-product-autofill-static.spec.js`。
- 脚本入口：`IntRuoyiFronted/package.json` 中 `e2e:dcc:upload-product-autofill:static`。

## API Contracts And Data States

- 复用现有 `getDccProductOptions`，传入 `status=ENABLE`、`requireDccProductCode=true` 和 DCC 项目关键词。
- 候选关键词来自当前 DCC 项目的 `projectName`、`projectCode`、`docControlNo`。
- 唯一正式产品主数据命中才写入 `formData.productMasterId` 与 `formData.productCode`。

## BDD Scenarios

- `BDD: DHF/DMR 产品编号自动带出 -> Given 受控文件分类要求产品主数据且当前 DCC 项目存在唯一产品关联 / When 用户选择 DCC 项目或文件类别 / Then 系统自动填入对应正式 DCC 产品编号。`
- `BDD: 产品关联不唯一时不得默认生成 -> Given 分类要求产品主数据但无法唯一定位产品 / When 用户进入提交页或选择分类 / Then 系统提示选择产品主数据，不生成临时产品编号。`

## RED / GREEN

- `RED: pnpm e2e:dcc:upload-product-autofill:static -> FAIL, 缺少正式产品主数据自动带出逻辑。`
- `GREEN: pnpm e2e:dcc:upload-product-autofill:static -> PASS。`

## Verification

- `pnpm e2e:dcc:upload-project-taxonomy-revision:static` -> PASS。
- `pnpm e2e:dcc:upload-current-version:static` -> PASS。
- `pnpm e2e:dcc:product-category-rule:static` -> PASS。
- `node tests/e2e/dcc-optional-product-binding-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。

## Responsive, Accessibility, Loading, Empty, Error, Permission

- UI 结构未改，仅复用现有产品选择框和 loading 状态。
- 自动检索失败时显示真实错误提示；匹配不唯一时要求人工选择，不吞异常。
- 权限沿用现有产品选项接口权限和提交页权限。

## Blockers

- 无。
