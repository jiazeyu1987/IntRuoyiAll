# Frontend Feature Evidence

## Feature Goal

工艺路线列表当前“导出”按钮触发全量工艺路线数据包导出，“导入”按钮继续导入同一数据包。

## Non-Goals

- 不新增第二套导入导出入口。
- 不改造页面视觉布局。
- 不用前端拼装数据替代后端正式数据包。

## Acceptance

- 导出按钮保留在现有工艺路线列表工具区。
- 导出调用不传当前列表筛选条件。
- 导入按钮继续打开现有 Excel 工作簿导入弹窗。

## BDD:

- BDD: 全量路线导出按钮 -> Given 用户在工艺路线列表设置了快速过滤 When 点击导出 Then 前端调用全量导出 API 空参数并下载全量数据包。
- BDD: 保留导入入口 -> Given 用户需要回放全量工艺路线数据包 When 点击导入 Then 仍打开现有多 Sheet Excel 导入弹窗。

## UI Entry

- `IntRuoyiFronted/src/views/mes/pro/route/index.vue`
- `IntRuoyiFronted/src/api/mes/pro/route/index.ts`

## Verification

- RED: `node tests/e2e/mes-pro-route-unified-list-template-static.spec.js` -> FAIL，当前页面仍调用 `ProRouteApi.exportRouteImportWorkbook(queryParams)`。
- GREEN: `node tests/e2e/mes-pro-route-unified-list-template-static.spec.js` -> PASS。
- Permission/loading/error checks: `node tests/e2e/mes-pro-route-toolbar-remove-blue-actions-static.spec.js` -> PASS，导入/导出权限、loading 绑定和标准工具区按钮保留。

## API Contracts And States

- Export button calls `ProRouteApi.exportRouteImportWorkbook({})`, so current quick filters only affect list display, not export scope.
- Import button continues to open `RouteWorkbookExcelImportForm` and imports the same workbook contract.

## Responsive, Accessibility, Empty, Error

- No layout or visual structure was changed beyond the downloaded filename.
- Existing loading state remains on `exportLoading`.
- Existing error path still surfaces backend export failure via `message.error(getErrorMessage(...))`.

## Blockers

- None.
