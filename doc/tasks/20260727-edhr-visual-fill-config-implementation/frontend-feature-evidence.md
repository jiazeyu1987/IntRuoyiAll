# Frontend Feature Evidence

## Feature Goal And Non-goals

- Goal: 在现有批记录单元格规则弹窗中完成“填写配置”，支持字段类型纠错、下拉选项、签名单元格提示、辅助行描述、单元格归属和辅助行填写人分配维护。
- Non-goal: 不新增独立辅助设计器、不新增辅助布局表、不新增独立 assistDraftFieldValues。

## Entry Points And Owned Files

- Entry: `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue` 的“填写配置”。
- Editor: `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`。
- Execution: `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`。
- API types: `batchrecordreport/index.ts`、`processFormPermissionRule.ts`、`feedback/index.ts`、`batchExecution.ts`。

## Acceptance

- AC-T08-1: “填写配置”入口复用现有单元格规则弹窗。
- AC-T08-2: 弹窗保存 cell rules 时同步提交 assistRows。
- AC-T08-3: 弹窗按辅助行配置员工/角色，并复用 `save-by-report` 提交 `fillAssignments`。
- AC-T08-4: 执行页辅助模式优先使用当前用户 assistRows，缺少配置显示“未配置辅助模式”。

## API Contracts And Data States

- `cell-rules` 请求/响应携带 `assistRows`。
- `save-by-report` 请求/响应携带 `fillAssignments`，每个辅助行使用一个 `scopeKey` 配置员工或角色。
- 辅助模式优先使用 openTask/executionPageQuery 返回的当前用户 `assistRows`。
- 缺少辅助行时显示“未配置辅助模式”，不从字段推导辅助行。
- 两种填写模式继续复用 `draftFieldValues[field.fieldIdentity]`。

## BDD Scenario

- BDD: T08 visual fill config -> Given 管理员打开批记录表单“填写配置”。
- When 管理员把可填写单元格归入辅助行并维护描述、字段类型、下拉选项、签名类型和辅助行填写人。
- Then 保存请求同时提交人工确认规则、`assistRows` 和 `fillAssignments`，员工打开执行页时只看到后端返回的当前用户辅助行。

## RED And GREEN

- RED: `node tests\e2e\edhr-visual-fill-config-static.spec.js` -> FAIL，入口文案和辅助行配置能力缺失。
- RED: `node tests\e2e\edhr-visual-fill-config-static.spec.js` -> FAIL，填写配置弹窗缺少辅助行填写人分配和 `save-by-report` 保存链路。
- RED: `node tests\e2e\edhr-visual-fill-config-static.spec.js` -> FAIL，真实 E2E 仍把批记录夹具名当作 DCC 路线产品名，缺少 `routeProductName` 分离合同。
- GREEN: `node tests\e2e\edhr-visual-fill-config-static.spec.js` -> PASS。

## Verification

- Static contract and adjacent regressions were run locally with Node.
- `pnpm ts:check` -> PASS。
- `node --check tests\e2e\edhr-visual-fill-config-real-flow.e2e.js` -> PASS。
- `node tests\e2e\edhr-visual-fill-config-real-flow.e2e.js` -> BLOCKED，缺少正式路线产品夹具 `fixture.routeProductName`，脚本在写入冲突数据前停止。

## Regression

- `node tests\e2e\edhr-cell-rules-static.spec.js` -> PASS。
- `node tests\e2e\edhr-batch-record-form-list-filler-static.spec.js` -> PASS。
- `node tests\e2e\batch-record-cell-rule-editor-mode-static.spec.js` -> PASS。
- `node tests\e2e\edhr-assist-fill-mode-static.spec.js` -> PASS。

## Blockers

- T09 real E2E runtime prerequisites are now available on 8083/48083.
- T09 real E2E no longer depends on `EDHR_VISUAL_FILL_*` environment variables; test tenant/account credentials and write authorization are supplied through ignored local config.
- T09 real E2E is blocked by missing formal route-product fixture: current consistent route candidates `IDI`、`ID`、`CODXVFC20260726` are not valid Kingdee materials, while `A001.02.092.60011` conflicts with MES item name/batch binding.
