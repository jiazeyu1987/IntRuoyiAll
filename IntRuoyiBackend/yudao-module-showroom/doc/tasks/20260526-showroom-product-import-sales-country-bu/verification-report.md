# 独立验证报告

## 目标

验证展厅产品导入与发布链路完成字段语义替换：`core_selling_points(_en)` 作为 `在售国家 / Countries on Sale`，`pipeline_layout(_en)` 作为 `BU`；支持验收 Excel `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版.xlsx` 的 `产品列表` 新表头；管理前端与 Website 不再把旧业务标签作为目标展示。

## 需求到产物

| 需求 | 产物/证据 | 结论 |
| --- | --- | --- |
| Excel 新表头导入 | `ShowroomProductExcelVO`、`ShowroomApiRuntime`、`ShowroomProductExcelImportExportIntegrationTest` | PASS |
| `BU -> pipeline_layout`，`在售国家 -> core_selling_points` | 导入集成测试、真实 Excel 只读检查 | PASS |
| 持证公司不静默忽略 | 导入 mismatch 测试 | PASS |
| 管理端基础信息、列表状态、批量入口改语义 | `yudao-ui-admin-vue3` 代码与 48 个脚本测试 | PASS |
| 后端发布字段标签和值 | `ShowroomFieldDisplaySupport`、发布详情/发布服务测试 | PASS |
| Website mock 与前台展示 | Website Vitest 73 tests、Playwright 2 tests | PASS |
| 真实测试租户从 `http://localhost:8081` 导入并发布 | Playwright 上传 `产品资料修改版.xlsx`，164 行全部成功发布 | PASS |
| 导入不生成音频/封面副作用 | 导入集成测试 + 后端日志检查 | PASS |
| 管理端全仓类型总检 | `pnpm ts:check` with 16GB heap | PASS |

## 已运行验证

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomFoundationContractTest,ShowroomReleaseProductDetailAssemblyTest,ShowroomReleasePublisherServiceTest,ShowroomVersionCenterServiceTest,ShowroomProductNarrationRegressionTest" test` -> 40 tests passed。
- PASS: `node --test scripts/showroom-admin-product-company-field-layout.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-frontend.test.mjs` -> 48 tests passed。
- PASS: `node tests\e2e\showroom-product-toolbar-layout.spec.js` -> PASS。
- PASS: `pnpm test -- --run` in `Website` -> 8 files / 73 tests passed。
- PASS: `npx playwright test kiosk-detail.spec.js` in `Website` -> 2 tests passed。
- PASS: `pnpm ts:check` in `yudao-ui-admin-vue3` with `NODE_OPTIONS=--max-old-space-size=16384` -> PASS。
- PASS: `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package` -> BUILD SUCCESS。
- PASS: backend health `http://127.0.0.1:48081/actuator/health` -> 200，runtime jar `output/runtime/backend-20260527-showroom-sales-country-bu.jar`。
- PASS: Playwright real import from `http://localhost:8081` with test tenant `测试租户` / `aoteman` and `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版.xlsx` -> `totalRows=164`、`successCount=164`、`skippedCount=0`、`failureCount=0`。
- PASS: backend log for the real import -> `/admin-api/showroom/product/import-excel` completed in 21348 ms; no `product-*-ruoxi.wav`, no `Native memory allocation`, no `SHOWROOM_AUDIO_GENERATION_FAILED`.
- PASS: `git diff --check` in all three worktrees -> no whitespace errors; only line-ending warnings.
- READ: `产品资料修改版.xlsx` -> sheets `产品列表`、`奖项`、`原材料`; `产品列表` row 1 matches the new 13-column contract.

## 阻塞

- 当前无阻塞。前置阻塞已解除：本任务 worktree 的 8081 管理端和 48081 后端均可用；管理端 `pnpm ts:check` 已通过。

## Gate 结论

Reviewer Gate 3: PASS。

代码切片、目标自动化测试、真实测试租户导入和副作用日志检查均满足放行条件。导入发布路径为满足 Website 版本包契约会沿用已发布讲解稿文本和音频引用到新产品版本，但不会生成新音频、不改封面、不调用 AI 补写字段。
