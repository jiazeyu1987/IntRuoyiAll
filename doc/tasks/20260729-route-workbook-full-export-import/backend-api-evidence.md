# Backend API Evidence

## Scope

- Endpoint: `GET /mes/pro/route/export-import-xlsx`
- Import endpoint: `POST /mes/pro/route/import-workbook-xlsx`
- Services: `MesProRouteWorkbookExportServiceImpl`, `MesProRouteWorkbookImportServiceImpl`

## API Contract

- 导出返回 `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`。
- 导出不使用当前列表筛选裁剪路线，导出现有全部工艺路线。
- 导入继续使用同一个 Excel 数据包，缺 Sheet、缺主数据、字段无效时返回既有显式错误。

## Data Contract

- 必须覆盖路线基础、路线工序、流转关系、边界关系、布局、产品绑定、工序 BOM、路线版本、排产配置、流程用途配置、工序用途配置、批记录表单、表单槽位。
- 不得用 `formBindings` 替代批记录表单，不得用默认 MAIN 或空值掩盖缺失正式来源。

## BDD:

- BDD: 全量路线导出 -> Given 工艺路线列表存在筛选条件和多条现有路线 When 点击导出 Then 后端不按当前筛选裁剪，导出全部现有路线。
- BDD: 全量路线数据包 -> Given 工艺路线存在流转关系图、布局、产品、BOM、排产配置、BATCH/SCHEDULE 用途配置、批记录表单和表单槽位 When 导出 Excel Then 工作簿包含可回放这些正式数据的 Sheet。
- BDD: 全量路线导入 -> Given 使用全量导出工作簿 When 重新导入 Then 路线基础、工序、关系图、布局、产品、BOM 和配置数据按正式来源重建，缺少主数据时显式失败。

## Verification

- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteWorkbookExportServiceTest,MesProRouteWorkbookImportServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，当前 Mapper/服务未支持全量配置 Sheet。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteWorkbookExportServiceTest,MesProRouteWorkbookImportServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，8 tests。
- Regression: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteWorkbookExportServiceTest,MesProRouteWorkbookImportServiceTest,MesProRouteControllerWorkbookExcelTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，11 tests。

## Auth, Permissions, Validation, And Errors

- Permission remains `mes:pro-route:export` for export and `mes:pro-route:create` for import.
- Workbook import requires all formal sheets and exact headers; missing sheets, missing route/process/product master references, duplicate rows, invalid numbers, invalid status, and invalid graph references fail fast.
- No fallback/default success path was introduced.

## Observability

- Existing API access logging remains on export via `@ApiAccessLog(operateType = EXPORT)`.
- Import failures continue to surface through existing `ServiceException` error codes.

## Blockers

- None.
