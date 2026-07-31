# Frontend Feature Evidence

## Feature Goal

将批记录表单列表工具栏中的“批量删除”按钮替换为“最新版本”开关；开关开启后列表只查询并显示最新版本表单。

## Non-Goals

- 不改动批记录表单导入、单条删除、填写人配置或预览逻辑。
- 不引入前端本地分页后过滤，避免跨页漏数。
- 不新增 mock、fallback、默认成功或吞异常路径。

## Requirements And Acceptance

- AC-01: 工具栏截图位置不再展示批量删除按钮，改为 Element Plus switch。
- AC-02: 开启“最新版本”后，分页接口发送 `latestVersionOnly=true`。
- AC-03: 后端在分页前按定义级最新版本和可见产品/批记录/表单类型分组过滤，只返回最新版本表单。
- AC-04: 关闭开关后不发送 `latestVersionOnly` 条件，恢复默认列表查询。

## UI Entry Points

- Route: `/mes/pro/batch-record-form-list`
- Component: `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue`
- API wrapper: `IntRuoyiFronted/src/api/mes/pro/batchrecordreport/index.ts`

## API Contracts

- Request type: `BatchRecordReportPageReqVO.latestVersionOnly?: boolean`
- Backend request VO: `BatchRecordReportPageReqVO.latestVersionOnly`
- Backend behavior: `MesProBatchRecordReportServiceImpl#getGeneratedReportPage` filters latest version rows before pagination; after product expansion and product/version filters, it keeps only the highest version per visible product/batch-record/form-slot group.

## Data States

- Off: `latestVersionOnly` is omitted, preserving current list behavior.
- On: `latestVersionOnly=true`, rows without latest version ownership are excluded.
- Pending latest: latest version is selected by version ordering, so pending approval latest versions are included when they are newest.

## BDD Scenarios

- `BDD: latest version switch filters form list -> Given 用户位于表单列表页, When 开启“最新版本”开关, Then 列表查询只请求并显示最新版本表单。`
- `BDD: latest version switch restores default list -> Given 用户已开启“最新版本”开关, When 关闭开关, Then 列表按默认筛选条件重新查询。`

## RED Evidence

- `RED: node IntRuoyiFronted/tests/e2e/batch-record-form-latest-version-switch-static.spec.js -> FAIL, toolbar lacked the latest-version switch container.`
- `RED: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportServiceImplDbTest#getGeneratedReportPage_latestVersionOnlyKeepsNewestVersionPerDefinition' '-Dsurefire.failIfNoSpecifiedTests=false' test -> FAIL, BatchRecordReportPageReqVO lacked setLatestVersionOnly(boolean).`

## GREEN Evidence

- `GREEN: node IntRuoyiFronted/tests/e2e/batch-record-form-latest-version-switch-static.spec.js -> PASS`
- `GREEN: pnpm ts:check -> PASS`
- `GREEN: mvn -pl yudao-module-bpm -am '-Dmaven.test.skip=true' install -> PASS, refreshed sibling main artifacts without compiling unrelated tests.`
- `GREEN: mvn -pl yudao-module-mes '-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportServiceImplDbTest#getGeneratedReportPage_latestVersionOnlyKeepsNewestVersionPerDefinition' '-Dsurefire.failIfNoSpecifiedTests=false' test -> PASS`
- `GREEN: node IntRuoyiFronted/tests/e2e/batch-record-title-actions-layout-static.spec.js -> PASS`
- `GREEN: node IntRuoyiFronted/tests/e2e/batch-record-force-unbind-delete-static.spec.js -> PASS`
- `GREEN: mvn -pl yudao-module-mes "-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportServiceImplDbTest#getGeneratedReportPage_latestVersionOnlyExcludesOlderDuplicateDefinitionRows" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS`

## Verification

- Static contract: latest-version switch placement, request field propagation, and backend service filter markers are covered.
- Frontend type check: `pnpm ts:check` passed with project memory settings.
- Backend target behavior: DB tests confirm `latestVersionOnly=true` returns the newest definition version and excludes obsolete same-product duplicate definition rows.

## Responsive Accessibility Loading Empty Error Permission

- Responsive: switch uses inline-flex toolbar container and keeps whitespace stable next to the import button.
- Accessibility: switch is visible with adjacent text label `最新版本`; no hidden-only control was introduced.
- Loading: existing list loading state is reused when toggling the switch.
- Empty: backend returns an empty page if no latest-version rows match current filters.
- Error: existing `getList` error branch remains unchanged and still surfaces API failures in `listErrorMessage`.
- Permission: no permission, route, menu, or role contract was changed.

## Blockers

- Full `-am` backend test run is blocked by unrelated `yudao-module-system` Codex Runner test compile state in the dirty workspace; target MES test passed after sibling main artifact isolation.
- Final commit/push is blocked by the pre-existing dirty/ahead workspace unless the project baseline commit workflow is explicitly executed for all existing dirty changes.
