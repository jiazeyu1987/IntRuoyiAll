# Frontend Feature Evidence

## Feature Goal

在 DCC 受控文件上传页中，用户选择 DCC 项目和文件分类后，文件名称可从同项目同分类的已有文件中下拉选择，也可手动输入。手动输入默认 `V1.0`，选择已有文件默认下一大版本，生效日期默认当天。

## Non-goals

- 不改变 DCC 项目代码作为产品编号来源的统一口径。
- 不放宽文件类别上传权限、目录绑定校验或后端提交校验。
- 不引入产品主数据匹配、默认编号或临时编号。

## Entry Points

- Page: `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue`
- API wrapper: `IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts`
- Static contract: `IntRuoyiFronted/tests/e2e/dcc-upload-name-version-autofill-static.spec.js`

## Acceptance

- 选择 DCC 项目和文件分类后，文件名称下拉只显示该组合下已有文件。
- 文件名称输入框支持选择下拉项，也支持用户手动输入。
- 手动输入新文件名称时，版本号默认 `V1.0`。
- 选择已有文件名称时，版本号默认当前版本下一大版本，例如 `V1.0 -> V2.0`。
- 生效日期默认当天。

## BDD

- `BDD: 手动输入新文件名称默认 V1.0 -> Given 用户已选择 DCC 项目和文件分类 / When 用户在文件名称框手动输入不存在的文件名 / Then 版本号默认 V1.0，生效日期默认当天。`
- `BDD: 选择已有文件名称默认下一大版本 -> Given 当前系统存在同 DCC 项目和文件分类下的文件 / When 用户从文件名称下拉中选择该文件 / Then 版本号默认当前版本大版本 +1，例如 V1.0 到 V2.0，生效日期默认当天。`
- `BDD: 文件名称选项按项目和分类过滤 -> Given 系统存在不同 DCC 项目或不同分类的文件 / When 用户选择某一 DCC 项目和文件分类 / Then 下拉只显示该组合下已有文件名称。`

## RED / GREEN

- `RED: pnpm e2e:dcc:upload-name-version-autofill:static -> FAIL, package.json 尚未注册任务专用静态契约脚本。`
- `GREEN: pnpm e2e:dcc:upload-name-version-autofill:static -> PASS。`
- `GREEN: pnpm e2e:dcc:upload-project-taxonomy-revision:static -> PASS。`
- `GREEN: pnpm e2e:dcc:upload-product-autofill:static -> PASS。`
- `GREEN: pnpm e2e:dcc:upload-current-version:static -> PASS。`
- `GREEN: pnpm ts:check -> PASS。`

## Verification

- Static E2E: `pnpm e2e:dcc:upload-name-version-autofill:static` -> PASS.
- Adjacent upload contracts: `pnpm e2e:dcc:upload-project-taxonomy-revision:static`、`pnpm e2e:dcc:upload-product-autofill:static`、`pnpm e2e:dcc:upload-current-version:static` -> PASS.
- Type check: `pnpm ts:check` -> PASS.

## Checks

- Loading/error: 历史文件名称接口失败仍展示真实错误提示，不吞异常。
- Empty state: 没有同项目同分类文件时，下拉为空但仍允许手动输入。
- Permission: 文件类别上传权限和目录绑定仍走既有校验。

## Blockers

- Git closeout blocked by mixed ahead commit `f56fc825`; frontend implementation itself已通过验证。
