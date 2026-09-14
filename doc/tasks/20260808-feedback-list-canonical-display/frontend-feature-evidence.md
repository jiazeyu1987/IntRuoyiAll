# Feature

报工管理“报工”列表在普通正式报工行没有导入 Excel 快照字段时，仍展示正式业务字段。

## Acceptance

- 列表接口返回 `total > 0` 时，产品代码、产品名称、工序编码、工序名称、人员名称和日期列不能因为 `excel*` 字段为空而显示为空白。
- 导入来源记录仍优先展示导入快照字段，保留现有列顺序、列设置和导入记录页行为。
- 不改后端接口、不造数据、不隐藏错误、不删除重复工序编码数据。

## UI Entry

- Route: `/mes/pro/feedback?tab=feedback`
- Component: `IntRuoyiFronted/src/views/mes/pro/feedback/index.vue`
- API: `/mes/pro/feedback/page`

## BDD

BDD: 正式报工列表展示普通正式报工字段 -> Given 报工分页返回的行没有导入 Excel 展示字段但包含正式报工 canonical 字段；When 用户打开报工管理的“报工”列表；Then 产品代码、产品名称、工序编码、工序名称、人员名称和日期列必须显示正式字段，不应呈现为空白表格。

## RED

RED: `node tests/e2e/mes-feedback-list-canonical-display-static.spec.js` -> FAIL，缺少 `resolveFeedbackProductCode` 等显示解析函数，当前表格只绑定 `excel*` 字段。

## GREEN

GREEN: `node tests/e2e/mes-feedback-list-canonical-display-static.spec.js` -> PASS。
GREEN: `node tests/e2e/mes-feedback-list-excel-columns-static.spec.js` -> PASS。
GREEN: `node tests/e2e/mes-pro-feedback-unified-list-template-static.spec.js` -> PASS。
GREEN: `pnpm ts:check` -> PASS。

## Verification

- Playwright read-only `/mes/pro/feedback?tab=feedback` -> `/admin-api/mes/pro/feedback/page?pageNo=1&pageSize=20` 返回 `code=0`, `total=144`, `count=20`。
- 首屏第 1 行显示产品代码 `AW.107.02.01.2010`、产品名称 `球囊扩张压力泵`、工序编码 `ER1A05996F5AA2`、工序名称 `清洗工序`、人员名称 `刘悦悦`、日期 `2026-08-07 13:30:46`。
- 页面无 `暂无数据`、无 `系统异常`、`consoleErrors=[]`、`pageErrors=[]`。

## Blockers

- 无当前任务 blocker。
- 当前工作区存在大量无关脏改动；本任务只触碰报工页、本任务静态合同和本任务文档。
