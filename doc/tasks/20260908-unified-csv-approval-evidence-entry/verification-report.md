# Verification Report

## Scope
- CSV 质量包页签统一呈现并执行审批/审查证据导出入口。
- 旧正式页面不再显示证据导出/下载按钮；正式页面保留查看、查询、校验、打印、生成等非导出动作。

## Changed Files Verified
- `IntRuoyiFronted/src/views/signature-governance/components/CsvPackageGovernanceListPane.vue`
- `IntRuoyiFronted/tests/e2e/signature-governance-csv-evidence-entrypoints-static.spec.cjs`
- `IntRuoyiFronted/src/views/dcc/controlled-file/signatures/index.vue`
- `IntRuoyiFronted/src/views/infra/runtime-control/index.vue`
- `IntRuoyiFronted/src/views/system/backup-plan/index.vue`
- `IntRuoyiFronted/src/views/system/user/index.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr/FieldAuditPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`

## Results
- PASS: `node IntRuoyiFronted\tests\e2e\signature-governance-csv-evidence-entrypoints-static.spec.cjs`
- PASS: real Playwright E2E on `http://127.0.0.1:8081` verified `/signature-governance/csv-package` renders exactly 9 approval evidence entrypoints and 9 visible action buttons.
- PASS: real Playwright E2E sampled legacy pages and confirmed old approval/evidence export labels are not visible on `/signature-governance/signature-records`, `/infra/runtime-control`, `/system/backup-plan`, `/system/user`, `/mes/pro/feedback/edhr-field-audit`, and `/mes/pro/feedback/edhr-batch-history`.
- PASS: backend restart recheck on 2026-09-09: backend health returned `status=UP`; real Playwright login with test tenant `芋道源码/admin` rechecked `/signature-governance/csv-package` and confirmed exactly 9 approval evidence entrypoints and 9 action buttons; sampled legacy pages still have no old approval/evidence export labels.
- PASS: `pnpm ts:check`
- PASS: Node API ESLint changed frontend files，检查 16 个当前改动前端源文件和 E2E 静态脚本，`errorCount=0`、`warningCount=0`。
- PASS: route flow route-owned device parameter frontend/backend static contracts confirmed the “设备参数”字段入口、路线工序自有面板和 `saveRouteProcessDeviceParameterRule` 保存调用。
- PASS: `pnpm ts:check` rerun after route-owned device parameter/frontend parser fixes.
- PASS: Node API ESLint targeted rerun for `flowconfig.ts`、`form-center/parser/index.vue`、`routeProcessSettingsColumns.ts`、`RouteFlowGraphDesigner.vue`，均 `errorCount=0`、`warningCount=0`。
- PASS: production material list ERP columns Python contract.
- PASS: QA regulation common tab and common binding control static contracts.
- PASS: DCC controlled file high-priority data validation contract test，2 tests.
- PASS: MES/ERP reactor compile with dependencies，`mvn -pl yudao-module-mes,yudao-module-erp -am "-DskipTests" compile`.
- PASS: final `git diff --check` and branch runtime port guard.
- PASS: residual scan confirmed the 9 former pages no longer contain prohibited evidence export/download button labels or handlers.
- PASS: `git diff --check` returned no diff-check errors.
- INFO: `eslint.cmd` / `pnpm exec eslint` 包装命令在本机长时间无输出；已用同一 ESLint 包的 Node API 完成定向 lint，避免把包装层卡住误判为业务文件 lint 失败。

## Real E2E Evidence

- Result JSON: `output/playwright/signature-csv-entrypoints/result.json`
- Screenshot: `output/playwright/signature-csv-entrypoints/csv-package-entrypoints.png`
- Restart recheck JSON: `output/playwright/signature-csv-entrypoints-after-restart/result.json`
- Restart recheck screenshot: `output/playwright/signature-csv-entrypoints-after-restart/csv-package-entrypoints.png`
- Runtime: frontend `8081` PID `41736`, backend `48081` PID `38100`; backend health returned `{"status":"UP"}`.
- Identity label: `芋道源码/admin`，通过真实登录页登录；未使用 API、fetch 或数据库替代页面断言。

## Requirement Coverage
- DCC 签名证据 PDF: CSV 页签统一入口已覆盖，导出动作调用正式 DCC 签名证据导出 API。
- 可信时间戳证据 ZIP: CSV 页签统一入口已覆盖，导出动作执行正式巡检后下载可信时间证据 ZIP。
- 备份审查证据 ZIP: CSV 页签统一入口已覆盖，导出动作调用正式备份审查证据 API。
- 账号安全与通用账号证据: CSV 页签统一入口已覆盖，导出动作调用正式用户清单和通用账号不合规清单 API。
- eDHR 签名记录: CSV 页签统一入口已覆盖，作为查看类入口跳转 `/mes/pro/feedback/edhr-signatures`。
- eDHR 字段审计证据: CSV 页签统一入口已覆盖，导出动作调用正式字段审计链导出 API。
- 审批中心顺序证据: CSV 页签统一入口已覆盖，作为查看类入口跳转 `/approval-center/done`。
- eDHR 批记录归档: CSV 页签统一入口已覆盖，导出动作取已归档批记录并调用正式归档下载 API。
- eDHR 权限矩阵证据: CSV 页签统一入口已覆盖，作为查看类入口跳转 `/mes/pro/feedback/edhr-permission-matrix`。
- 权限约束: 每个入口保留对应权限码，不使用 mock、不使用 fetch 绕过正式 API。
- 旧入口清理: DCC 签名记录、运行控制台、备份计划、用户管理、字段审计、批记录历史、批记录列表、批记录详情、eDHR 执行页的证据导出/下载按钮已清理。

## Remaining Closeout
- 主实现提交：`bcbdee838 feat: consolidate compliance evidence exports`。
- 任务状态：completed。
- `eslint.cmd` / `pnpm exec eslint` 包装命令在本机长时间无输出；源码 lint 已使用同一 ESLint 包的 Node API 定向完成。
