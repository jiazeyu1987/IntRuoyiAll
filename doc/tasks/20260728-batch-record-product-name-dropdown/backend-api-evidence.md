# Backend API Evidence

## Scope

批记录表单列表新增只读候选接口：`GET /admin-api/mes/pro/batch-record-report/product-name-options`。

## Contract

- API contract：请求参数为 `keyword?: string`、`latestVersionOnly?: boolean`，响应为 `string[]`。
- Data contract：候选只来自批记录表单列表同口径可见记录中的非空 `productName`，去重排序后返回。
- Auth and permissions：接口与相邻 `/page`、`/batch-record-names` 读接口保持同一 Controller 边界，不新增菜单、SQL 或 schema 迁移。
- Error behavior：不捕获并吞掉异常，不返回 mock、默认成功或 fallback 候选；运行态未加载路由时真实暴露业务 `404`。

## Validation

- 服务实现：`MesProBatchRecordReportService#getProductNameOptions(keyword, latestVersionOnly)` 与 `MesProBatchRecordReportServiceImpl#getProductNameOptions(...)`。
- 口径复用：通过 `getGeneratedReportList(optionsReqVO)` 复用可见视图、Jimu 报表存在性过滤、版本产品拆行、`latestVersionOnly`、产品名包含匹配。
- 测试覆盖：Controller 映射与 service 委托；DB 测试覆盖候选去重排序、keyword 过滤、版本产品拆行、隐藏已清理 Jimu 报表、latest-only 口径。

## BDD

- BDD: 点击产品名称输入框展示候选 -> Given 批记录表单目录存在多个产品名称 / When 用户点击产品名称筛选输入框 / Then 下拉展示当前批记录表单目录实际存在的产品名称候选。
- BDD: 点击候选立即过滤 -> Given 候选下拉中存在目标产品名称 / When 用户点击该候选 / Then 快速筛选写入 `productName` 并立即请求列表过滤，无需点击查询按钮。
- BDD: 手动输入查询过滤 -> Given 用户手动输入或复制产品名称 / When 用户点击查询按钮 / Then 列表按输入文本作为 `productName` 过滤。

## RED

- RED: `node -e "<dfc71011^ snapshot assertions>"` -> FAIL, expected reason: task-start parent source lacked batch-record `product-name-options` API, `triggerOnFocus`, and productName autocomplete filter contract.

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 119 tests, 0 failures, 0 errors.

## Verification

- Controller mapping verified: `/product-name-options` exposes `keyword` and `latestVersionOnly=false` default.
- Service DB behavior verified: returns visible distinct sorted products, filters by keyword, respects latest-only version scope, excludes cleared Jimu reports.
- Runtime E2E supporting check: source and JUnit pass, but current `48081` runtime returns business `404` for the new endpoint, so the deployed local process has not loaded this backend route.

## Blockers

- 本机真实 E2E blocked：`http://127.0.0.1:48081` health 为 `UP`，但新增接口在运行态返回业务码 `404`；需重建/重启当前 `int_main` 后端到包含本提交的 Jar 后才能完成真实页面验收。
