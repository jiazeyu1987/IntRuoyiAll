# Execution Log

## Intent

用户反馈“现在报工列表还是空的”。当前只读核验显示接口不是空：`/mes/pro/feedback/page` 返回 `total=144`，但列表前几行的 `excelProductCode/excelProductName/excelProcessCode/excelProcessName` 为 `null`，同时正式字段 `itemCode/itemName/processCode/processName/workOrderCode` 有值，导致页面只绑定 `excel*` 列时视觉上像空表。

## BDD

BDD: 正式报工列表展示普通正式报工字段 -> Given 报工分页返回的行没有导入 Excel 展示字段但包含正式报工 canonical 字段；When 用户打开报工管理的“报工”列表；Then 产品代码、产品名称、工序编码、工序名称、人员名称和日期列必须显示正式字段，不应呈现为空白表格。

## Evidence

- API: `/mes/pro/feedback/page?pageNo=1&pageSize=10` -> `code=0`, `total=144`, `listCount=10`。
- UI: Playwright 登录 `芋道源码/admin` 打开 `/mes/pro/feedback?tab=feedback` -> 请求 `total=144`, 页面行数 `20`，但首屏关键列大量为空。
- Root cause: `IntRuoyiFronted/src/views/mes/pro/feedback/index.vue` 的正式报工表格列直接绑定 `excelProductCode/excelProductName/excelProcessCode/excelProcessName/...`，未展示普通正式报工的 `itemCode/itemName/processCode/processName/feedbackUserNickname/feedbackTime`。

## TDD

- RED: `node tests/e2e/mes-feedback-list-canonical-display-static.spec.js` -> FAIL，缺少 `resolveFeedbackProductCode` 等显示解析函数，当前表格只绑定 `excel*` 字段。
- Implementation: `IntRuoyiFronted/src/views/mes/pro/feedback/index.vue` 为产品、工序、人员名称和日期列增加显示解析函数；导入来源优先显示 `excel*` 快照字段，普通正式报工显示 `itemCode/itemName/processCode/processName/feedbackUserNickname/feedbackTime`。
- GREEN: `node tests/e2e/mes-feedback-list-canonical-display-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-feedback-list-excel-columns-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-feedback-unified-list-template-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- Real UI: Playwright 只读打开 `/mes/pro/feedback?tab=feedback`，报工分页接口返回 `total=144/count=20`；页面首屏 20 行，首行显示 `AW.107.02.01.2010 / 球囊扩张压力泵 / ER1A05996F5AA2 / 清洗工序 / 刘悦悦 / 2026-08-07 13:30:46`；无 `暂无数据`、无 `系统异常`、无 console/page error。
