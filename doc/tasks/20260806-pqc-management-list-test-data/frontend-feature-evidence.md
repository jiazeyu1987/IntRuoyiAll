# PQC 管理列表前端交付证据

## Feature Goal

- 修复本机 `芋道源码/admin` 打开 `PQC组长 > PQC管理` 后列表为空的问题。
- 调整 PQC 管理展示口径：列表不显示样本值，详情保留样本值，并将详情抽屉宽度翻倍。
- 非目标：不改后端接口、不造 mock 数据、不隐藏 API 错误、不扩大到复核/汇集/生产列池重构。

## Requirements

- `PQC管理` 列表必须使用后端必填的 `submitDate`。
- 默认 `submitDate` 必须是今天，格式为 `YYYY-MM-DD`。
- 默认日期必须作为标准多条件筛选中的可见条件存在，不能作为隐藏查询参数。
- 切换到 `PQC管理` 页签时必须加载正式提交列表。
- `逐件/样本值` 不作为 PQC 管理列表列展示，也不能出现在用户列配置池中。
- 详情中的 PQC 项目明细必须继续显示样本值。
- 详情抽屉宽度必须从原 `620px` 翻倍为 `1240px`。
- 详情不显示 `结构化报工内容` 和 `原始提交内容`。
- 详情左侧描述标签列宽必须是 `400px`，避免截图中的竖向换行。

## Acceptance

- Acceptance: `PQC管理` 页签首个正式列表请求包含 `leaderType=PQC&submitDate=2026-08-06`。
- Acceptance: 页面表格显示测试工单 `RRM-20260801-PP-MO-001` 和工序 `清洗工序`。
- Acceptance: 日期默认值通过可见多条件筛选条件维护，不通过隐藏参数或 mock 数据维护。
- Acceptance: `PQC管理` 列表不渲染 `逐件/样本值` / `pieceSampleValues` 列。
- Acceptance: 详情弹框仍通过 `formatPqcSnapshotSampleValues(row)` 显示 PQC 样本值。
- Acceptance: 详情抽屉使用 `size="1240px"`。
- Acceptance: 详情弹框不渲染 `结构化报工内容` 或 `原始提交内容`。
- Acceptance: 详情弹框左侧描述标签列真实宽度为 `400px`。

## UI Entry Points And Owned Files

- Route: `/mes/pro/process-pool/pqc-leader`.
- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`.
- Regression test: `IntRuoyiFronted/tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs`.
- Regression test: `IntRuoyiFronted/tests/e2e/pqc-leader-sample-values-detail-only-static.spec.cjs`.
- Real verification script: `doc/tasks/20260806-pqc-management-list-test-data/verify-pqc-management-list-real.e2e.cjs`.

## API Contract And Data States

- API: `GET /admin-api/mes/pro/process-pool/team-leader/submission/page`.
- Required query: `leaderType=PQC`, `submitDate=2026-08-06`.
- Expected data: event `160`, work order `RRM-20260801-PP-MO-001`, process `清洗工序`, total `1`.
- Empty/error state before fix: no visible date condition and no list load on `PQC管理` tab switch.

## BDD Scenarios

- BDD: PQC 管理显示当天提交 -> Given 今天存在 admin/PQC 可见提交事件 / When 用户切换到 `PQC管理` / Then 请求带 `submitDate=2026-08-06` 并显示该提交。
- BDD: 日期条件可见 -> Given 后端要求提交日期 / When 页面自动设置默认日期 / Then 标准筛选条件中包含 `submitDate` 条件，不使用隐藏参数冒充成功。
- BDD: PQC 样本值只在详情展示 -> Given PQC 管理列表存在带逐件样本值的提交 / When 用户停留在列表页 / Then 列表不显示逐件/样本值列；When 用户点击详情 / Then 详情中的 PQC 项目明细仍显示样本值，并且详情抽屉宽度为原 `620px` 的 2 倍。
- BDD: PQC 详情只展示业务摘要和项目明细 -> Given 用户打开 PQC 管理提交详情 / When 详情抽屉展示提交内容 / Then 不显示 `结构化报工内容` 和 `原始提交内容`；And 左侧详情标签列宽为 `400px`。

## RED Evidence

- RED: `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs` -> FAIL, expected reason missing shared `formatDate(..., 'YYYY-MM-DD')` default submit date and missing `PQC管理` tab load.
- `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs` -> FAIL.
- Expected reason: missing shared `formatDate(..., 'YYYY-MM-DD')` default submit date and missing `PQC管理` tab load.
- RED: `node tests/e2e/pqc-leader-sample-values-detail-only-static.spec.cjs` -> FAIL, expected reason list still rendered `逐件/样本值` / `pieceSampleValues` before the display adjustment.
- RED: `node tests/e2e/pqc-leader-sample-values-detail-only-static.spec.cjs` -> FAIL, expected reason detail still rendered `结构化报工内容` and parsed `detail.originalPayloadJson`.
- RED: `node doc/tasks/20260806-pqc-management-list-test-data/verify-pqc-management-list-real.e2e.cjs` -> FAIL, expected reason label width was still `146px` after prop-only implementation.

## GREEN Evidence

- GREEN: `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/pqc-leader-sample-values-detail-only-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/pqc-leader-item-snapshot-static.spec.js` -> PASS.
- GREEN: `node doc/tasks/20260806-pqc-management-list-test-data/verify-pqc-management-list-real.e2e.cjs` -> PASS.
- `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs` -> PASS.
- `node tests/e2e/pqc-leader-sample-values-detail-only-static.spec.cjs` -> PASS.
- `node tests/e2e/pqc-leader-item-snapshot-static.spec.js` -> PASS.
- `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> PASS.
- `node doc/tasks/20260806-pqc-management-list-test-data/verify-pqc-management-list-real.e2e.cjs` -> PASS.
- `pnpm ts:check` -> PASS.
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/pqc-leader-sample-values-detail-only-static.spec.cjs doc/tasks/20260806-pqc-management-list-test-data` -> PASS.

## Verification

- Browser verification used real local route `/mes/pro/process-pool/pqc-leader`, real local `芋道源码/admin`, and real API response from `48081`.
- Captured response total was `1`; no target console or page errors were recorded.
- Static detail/list verification confirmed list-hidden/detail-visible sample values and `1240px` detail drawer width.
- Real browser detail verification clicked event `160`, measured detail drawer width as `1240px`, measured detail label width as `400px`, confirmed hidden structure/raw blocks, and confirmed detail sample values include seeded `53.00`.

## UI State Checks

- Loading state remains `loading.value`.
- Empty state is no longer reached for the seeded row because `PQC管理` triggers the list request.
- Error state still surfaces through `loadError` and `ElMessage.error`; no exception is swallowed.
- Permission path uses real local `芋道源码/admin`; no API-only page claim.

## Blockers And Follow-Up

- Not a blocker for this bug: older/static adjacent contracts still conflict on list structure and default empty-filter policy.
- Follow-up candidate: clean the PQC/production submission column contracts so they consistently reflect the latest structured-list requirements.
