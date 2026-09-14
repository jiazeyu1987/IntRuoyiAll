# Execution Log

BDD: 统一 CSV 页签展示审批证据导出入口 -> Given 具备电子签名治理查看权限的管理员进入 `/signature-governance/csv-package`, When 需要导出审查/审批证据, Then 页面在同一 CSV 质量包页签展示 DCC 签名证据、可信时间证据、备份审查证据入口，并说明来源页面和权限要求。

BDD: 统一入口不替代正式业务操作 -> Given 用户点击 CSV 页签中的某个证据入口, When 该入口对应已有正式页面能力, Then 系统跳转到正式页面完成导出操作，不在 CSV 页签中伪造下载、不使用 mock 或隐藏失败。

BDD: 权限和前置条件仍由正式页面执行 -> Given 用户缺少某类证据导出权限或尚无可导出证据, When 通过 CSV 页签入口进入正式页面, Then 正式页面继续按原权限和前置条件展示禁用、错误或阻断信息。

RED: node IntRuoyiFronted\tests\e2e\signature-governance-csv-evidence-entrypoints-static.spec.cjs -> FAIL, expected reason: CSV质量包页签尚未集中定义 approvalEvidenceEntrypoints。

GREEN: node IntRuoyiFronted\tests\e2e\signature-governance-csv-evidence-entrypoints-static.spec.cjs -> PASS。
GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm --dir IntRuoyiFronted exec vue-tsc --noEmit --pretty false -> PASS。
Experience: 更新 docs\frontend-development.md，记录 CSV 页签统一审批/签名/可信时间/备份审查证据导出入口规则。

BDD: CSV 页签继续收口账号和 eDHR 审查证据入口 -> Given 审查人员进入 CSV 质量包页签, When 需要核对 4.1 至 4.12 的账号、签名、字段审计、审批顺序、归档和权限证据, Then 页面集中展示账号安全证据、eDHR 签名记录、字段审计、审批详情、批记录归档和权限矩阵入口，并仍跳转正式页面执行导出或查看。

RED: node IntRuoyiFronted\tests\e2e\signature-governance-csv-evidence-entrypoints-static.spec.cjs -> FAIL, expected reason: CSV 质量包页签尚未覆盖账号安全与 eDHR 审查证据新增入口。

GREEN: node IntRuoyiFronted\tests\e2e\signature-governance-csv-evidence-entrypoints-static.spec.cjs -> PASS。
GREEN: pnpm --dir IntRuoyiFronted exec eslint --ext .vue src/views/signature-governance/components/CsvPackageGovernanceListPane.vue -> PASS。
GREEN: pnpm --dir IntRuoyiFronted ts:check -> PASS。
GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260908-unified-csv-approval-evidence-entry/frontend-feature-evidence.md -> PASS。

BDD: 所有导出按钮只能出现在 CSV 质量包 -> Given 用户进入 DCC 签名记录、运行控制台、备份计划、用户管理、eDHR 字段审计、eDHR 批记录历史、批记录列表、批记录详情或 eDHR 执行页, When 页面渲染证据相关操作区, Then 不得出现证据导出/下载按钮；Given 用户进入 `/signature-governance/csv-package`, When 点击导出类入口, Then CSV 质量包调用正式导出 API 并暴露失败，不使用 mock 或 fetch 绕过。

RED: node IntRuoyiFronted\tests\e2e\signature-governance-csv-evidence-entrypoints-static.spec.cjs -> FAIL, expected reason: clarified target requires `actionType` and CSV-owned download handler, and rejects old formal-page export/download buttons.

GREEN: node IntRuoyiFronted\tests\e2e\signature-governance-csv-evidence-entrypoints-static.spec.cjs -> PASS。
GREEN: rg residual scan for `下载证据 PDF|导出时间戳证据|导出审查证据|导出通用账户清单|责任证明导出|导出审计链|下载归档打印件|下载打印版 PDF` across the 9 former pages -> PASS, no matches.
GREEN: git diff --check -> PASS, whitespace warnings only for CRLF normalization; no diff-check errors.
BLOCKED: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm --dir IntRuoyiFronted exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false` -> interrupted after repeated 30s waits with no output; no PASS/FAIL available.
BLOCKED: `pnpm --dir IntRuoyiFronted exec eslint --ext .vue <modified-pages>` -> interrupted after repeated 30s waits with no output; no PASS/FAIL available.

## 2026-09-09 后端重启后继续验证

RED: real Playwright CSV entrypoint check -> FAIL, expected reason: CSV 质量包渲染 9 个入口标题但只有 8 个可见按钮；`备份审查证据ZIP` 的按钮被旧源页面权限 `system:backup-plan:evidence-export` 隐藏，无法满足“所有导出按钮只能出现在 CSV 质量包”的统一入口可见性要求。

GREEN: node IntRuoyiFronted\tests\e2e\signature-governance-csv-evidence-entrypoints-static.spec.cjs -> PASS。

GREEN: real Playwright CSV entrypoint check -> PASS，使用真实前端 `http://127.0.0.1:8081`、租户/账号标签 `芋道源码/admin` 登录并打开 `/signature-governance/csv-package`；页面显示且仅显示 9 个审批证据入口，按钮文本为 `导出, 导出, 导出, 导出, 查看, 导出, 查看, 导出, 查看`；抽查 `/signature-governance/signature-records`、`/infra/runtime-control`、`/system/backup-plan`、`/system/user`、`/mes/pro/feedback/edhr-field-audit`、`/mes/pro/feedback/edhr-batch-history`，未再出现旧审批证据导出/下载按钮文案。

GREEN: pnpm ts:check -> PASS，`IntRuoyiFronted` 下 `vue-tsc --noEmit -p tsconfig.relaxed.json` 完成且退出码 0。

BLOCKED: npx eslint --ext .ts,.vue src/views/signature-governance/components/CsvPackageGovernanceListPane.vue src/views/system/backup-plan/index.vue -> interrupted after repeated waits with no output; no PASS/FAIL claimed.

BLOCKED: .\node_modules\.bin\eslint.cmd --ext .ts,.vue src/views/signature-governance/components/CsvPackageGovernanceListPane.vue src/views/system/backup-plan/index.vue -> interrupted after repeated waits with no output; no PASS/FAIL claimed.

EVIDENCE: Playwright JSON result saved at `output/playwright/signature-csv-entrypoints/result.json`; screenshot saved at `output/playwright/signature-csv-entrypoints/csv-package-entrypoints.png`.

DIAGNOSIS: ESLint wrapper hang -> `eslint.cmd --print-config` 和 `.cmd` 单文件 lint 在本机无输出卡住；直接用 Node 加载 `require('eslint')` 可正常创建 `ESLint` 实例并 lint `src/views/system/backup-plan/index.vue`，说明卡点在 PowerShell/.cmd/pnpm 包装层，不是 ESLint 本体或业务文件规则。

GREEN: Node API ESLint changed frontend files -> PASS，检查 16 个当前改动前端源文件和 E2E 静态脚本，`errorCount=0`、`warningCount=0`。

## 2026-09-09 提交前红灯修复

BDD: 路线工序设备参数配置必须归属路线工序 -> Given 工艺路线设计人员在流转关系图选择某个路线工序, When 在字段明细中查看“设备参数”, Then 页面通过路线工序自有 API 读取并保存设备参数规则，不得使用生产组长配置接口或跨链路补齐。

RED: node IntRuoyiFronted\tests\e2e\route-flow-device-parameter-config-static.spec.cjs -> FAIL, expected reason: 前端未将 `deviceParameters` 注册为路线工序字段，流转关系图缺少 `route-process-device-parameter-config` 面板和路线自有保存调用。

GREEN: node IntRuoyiFronted\tests\e2e\route-flow-device-parameter-config-static.spec.cjs -> PASS。

GREEN: node IntRuoyiBackend\yudao-module-mes\src\test\js\route-flow-device-parameter-config-static.spec.cjs -> PASS。

GREEN: node IntRuoyiFronted\tests\e2e\form-parser-json-download-static.spec.cjs -> PASS。

GREEN: pnpm ts:check -> PASS，`IntRuoyiFronted` 下项目脚本完成且退出码 0。

GREEN: Node API ESLint targeted frontend files -> PASS，逐文件检查 `flowconfig.ts`、`form-center/parser/index.vue`、`routeProcessSettingsColumns.ts`、`RouteFlowGraphDesigner.vue`，均 `errorCount=0`、`warningCount=0`。

GREEN: python -X utf8 IntRuoyiBackend\script\tests\test_production_material_list_erp_columns_contract.py -> PASS。

GREEN: node IntRuoyiFronted\tests\e2e\qa-regulation-common-binding-control-static.spec.cjs -> PASS。

GREEN: node IntRuoyiFronted\tests\e2e\qa-regulation-common-tab-static.spec.cjs -> PASS。

GREEN: mvn -pl yudao-module-dcc "-Dtest=cn.iocoder.yudao.module.dcc.service.file.DccControlledFileHighPriorityDataValidationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，2 tests。

GREEN: mvn -pl yudao-module-mes,yudao-module-erp -am "-DskipTests" compile -> PASS。

GREEN: git diff --check -> PASS，仅 CRLF 工作区换行提示，无 diff-check 错误。

GREEN: powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1 -> PASS，`int_main` frontend 8081 / backend 48081。

Experience: 更新 docs\frontend-development.md 与 docs\experience-index.md，沉淀静态合同 cwd 和 ESLint wrapper 卡住时的 Node API 定向验证经验。

## 2026-09-09 后端重启后复验

GREEN: backend health check after restart -> PASS，`http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。

RED: real Playwright CSV entrypoint check after restart -> FAIL, expected reason: 初版脚本在登录页尝试操作租户下拉后，未找到可见用户名输入框；诊断截图/DOM 证明登录页实际存在用户名和密码输入框，失败属于测试脚本交互扰动，不是业务页面入口缺失。

GREEN: real Playwright CSV entrypoint check after restart -> PASS，使用真实前端 `http://127.0.0.1:8081`、测试租户 `芋道源码`、账号 `admin` 通过登录页登录；页面 `/signature-governance/csv-package` 显示 9 个审批证据入口，按钮文本为 `导出, 导出, 导出, 导出, 查看, 导出, 查看, 导出, 查看`；抽查 `/signature-governance/signature-records`、`/infra/runtime-control`、`/system/backup-plan`、`/system/user`、`/mes/pro/feedback/edhr-field-audit`、`/mes/pro/feedback/edhr-batch-history`，未出现旧审批证据导出/下载按钮文案。

EVIDENCE: 后端重启后复验 JSON 保存于 `output/playwright/signature-csv-entrypoints-after-restart/result.json`；截图保存于 `output/playwright/signature-csv-entrypoints-after-restart/csv-package-entrypoints.png`；登录页诊断保存于同目录 `login-debug.json` / `login-debug.png`。
