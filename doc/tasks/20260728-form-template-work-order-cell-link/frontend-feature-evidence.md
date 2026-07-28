# Frontend Feature Evidence

## Feature Goal

在表单中心模板预览工具栏增加“链接”入口，并把当前模板 `templateId + versionNo` 传入批记录单元格链接工作台。

## Non-Goals

- 不把表单中心模板伪装成批记录表单。
- 不新增或依赖 `batchRecordBindingStatus` / `batchRecordReportId`。
- 不改变“打开 / 编辑 / 填写 / 填写配置”的既有交互边界。

## Entry Points

- `IntRuoyiFronted/src/views/form-center/template/index.vue`
- `IntRuoyiFronted/src/views/mes/pro/batchrecordcelllink/index.vue`
- `IntRuoyiFronted/src/api/mes/pro/batchrecordcelllink/index.ts`
- `IntRuoyiFronted/src/router/modules/remaining.ts`

## API Contract

- `/mes/pro/batch-record-cell-link/workbench-context` 支持可选 `templateId` 与 `versionNo`。
- 表单模板入口跳转 `/mes/pro/batch-record-cell-link`，query 包含 `templateId`、`versionNo`、`returnTo`、`returnLabel`。

## Acceptance

- 表单模板预览红框工具栏出现“链接”按钮。
- 点击“链接”进入批记录单元格链接工作台。
- 工作台可使用当前模板版本作为目标表单，并保留返回表单模板页面的 query。

## BDD

- BDD: 表单模板链接入口 -> Given 已选中可交互表单模板 When 用户点击预览工具栏“链接” Then 进入批记录单元格链接工作台并携带 `templateId + versionNo`。
- BDD: 链接工作台返回 -> Given 从表单模板进入链接工作台 When 用户点击返回 Then 回到原表单模板页面并显示“返回表单模板”语义。

## RED / GREEN

- RED: `node tests/e2e/form-center-static.spec.js` -> FAIL，缺少 `openSelectedTemplateCellLinks`。
- RED: `node tests/e2e/mes/batch-record-cell-link-static.spec.js` -> FAIL，API 参数缺少 `templateId?: number` / `versionNo?: string`。
- GREEN: `node tests/e2e/form-center-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/form-template-button-interaction-parity-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/form-template-independent-button-actions-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes/batch-record-cell-link-static.spec.js` -> PASS。

## Verification

- `node tests/e2e/form-center-static.spec.js`
- `node tests/e2e/form-template-button-interaction-parity-static.spec.js`
- `node tests/e2e/form-template-independent-button-actions-static.spec.js`
- `node tests/e2e/mes/batch-record-cell-link-static.spec.js`

## Checks

- Loading / empty / permission: 复用现有模板按钮可用性和 `v-hasPermi` 模式；链接按钮只在模板可交互时出现。
- Error behavior: 工作台继续依赖后端 fail-fast 响应，不在前端吞异常或伪造默认成功。
- Route regression: 补齐现有 `form-center/policy` 路由 activeMenu，使表单中心静态合同回归通过。

## Blockers

- 无当前任务阻塞。
