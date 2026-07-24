# DCC Release Gate E2E 补齐执行日志

BDD: upload fixed purpose E2E contract -> Given 当前 DCC 上传页没有可填写的 purpose 控件且上传预览固定传 `SOURCE` When 真实 E2E 配置 `DCC_E2E_TC006_FIXED_PURPOSE_VALUE`、`DCC_E2E_TC007_FIXED_PURPOSE_VALUE`、`DCC_E2E_TC008_FIXED_PURPOSE_VALUE` Then 前置检查不得继续要求不存在的 `*_PURPOSE_SELECTOR` / `*_PURPOSE_VALUE`，但也不得在未显式声明固定 purpose 时静默放行。

STATE: 创建前端任务档案。最近前端任务 `20260528-nas-transfer-large-task-resume-performance` 已标记 `completed`，可开始当前任务。

RED: `node tests\e2e\dcc-controlled-file-protection.contract.test.js` -> FAIL, 旧 E2E 即使配置 `DCC_E2E_TC006_FIXED_PURPOSE_VALUE`、`DCC_E2E_TC007_FIXED_PURPOSE_VALUE`、`DCC_E2E_TC008_FIXED_PURPOSE_VALUE`，仍阻塞在不存在的 `*_PURPOSE_SELECTOR` / `*_PURPOSE_VALUE`。

GREEN: `node tests\e2e\dcc-controlled-file-protection.contract.test.js` -> PASS，固定 purpose 模式不再要求缺失 UI selector，未显式配置固定 purpose 或真实 selector 时仍阻塞。

GREEN: `node --check tests\e2e\dcc-controlled-file-protection.e2e.js` -> PASS。

REGRESSION: `node tests\e2e\dcc-controlled-file-protection.e2e.js; Write-Output "EXIT:$LASTEXITCODE"` -> BLOCKED, exit `2`，真实 release gate/env 仍缺失时不加载 Playwright、不静默通过。

## 2026-05-30 int_main 前端融合验证

BDD: DCC 前端 E2E 合同融合后继续 fail-fast -> Given 最新 `int_main` 已合入 eDHR 真实 E2E 覆盖门禁 / When DCC release gate 前端脚本同步到融合分支 / Then DCC upload purpose 合同与 E2E 脚本语法必须继续通过，缺少真实 release gate/env 时不得静默通过。

BDD: eDHR 前端覆盖门禁与 DCC E2E 共存 -> Given 最新 `int_main` 新增 eDHR 归档、追溯、权限矩阵和发布覆盖脚本 / When 与 DCC 前端保护分支融合 / Then eDHR API/UI/E2E 静态合同和发布覆盖 gate 必须通过，且不覆盖 DCC E2E 脚本。

GREEN: `node tests\e2e\dcc-controlled-file-protection.contract.test.js` -> PASS，DCC 固定 purpose 合同仍通过。

GREEN: `node --check tests\e2e\dcc-controlled-file-protection.e2e.js` -> PASS，DCC 真实 E2E 脚本语法通过。

GREEN: `node scripts\edhr-domain-trace-api-contract.test.mjs`、`node scripts\edhr-domain-trace-ui-contract.test.mjs`、`node scripts\edhr-domain-trace-e2e-contract.test.mjs`、`node scripts\edhr-release-e2e-coverage-contract.test.mjs` -> PASS，分别为 2、4、7、11 tests passed。

GREEN: `node scripts\edhr-execution-list-e2e-contract.test.mjs`、`node scripts\edhr-field-audit-e2e-contract.test.mjs`、`node scripts\edhr-permission-tenant-matrix-contract.test.mjs`、`node scripts\edhr-tracking-signature-e2e-contract.test.mjs` -> PASS，分别为 6、4、3、6 tests passed。

GREEN: `node scripts\edhr-archive-export.test.mjs` -> PASS，7 tests passed；`node tests\e2e\runtime-control-edhr-archive-health-static.spec.js` -> PASS；`node --check tests\e2e\runtime-control-edhr-archive-health.e2e.js` -> PASS。

RED: `node scripts\edhr-release-e2e-coverage-gate.mjs --mode check` -> FAIL, expected reason: 发布覆盖 gate 不接受 `--mode` 参数，必须显式使用 `--check` 或 `--run-real`，无静默降级。

GREEN: `node scripts\edhr-release-e2e-coverage-gate.mjs --check` -> PASS，features=10，checkScripts=7，syntaxFiles=7。

## 2026-05-30 本地全量 DCC E2E 验证

BDD: 本地真实前端必须覆盖 DCC release gate -> Given 用户要求不发布到服务器且主前端在 `http://127.0.0.1:8081`、主后端在 `http://127.0.0.1:48081` / When 使用 Playwright 登录测试租户并执行 `tests/e2e/dcc-controlled-file-protection.e2e.js` / Then TC-E2E-001 至 TC-E2E-017 必须覆盖字段收敛、直链阻断、受控预览、水印追溯、OnlyOffice 只读、上传策略、上传票据、临时文件生命周期、下载加密、下载 fail-closed、非前缀权限、审计授权、前端 fail-closed、截图证据和全量汇总。

CONFIG: 本地 E2E profile 保留 release gates opt-in，覆盖 URL 为 `DCC_E2E_BASE_URL=http://127.0.0.1:8081`、`DCC_E2E_API_BASE_URL=http://127.0.0.1:48081`，允许 URL 正则也限定到 `127.0.0.1`；未访问测试服发布入口。

RED: local full `node tests\e2e\dcc-controlled-file-protection.e2e.js` -> FAIL at `TC-E2E-007`, expected reason: 本地测试租户缺少 `906101/SOURCE` 上传大小策略，真实超限路径无法产生 `DCC_UPLOAD_SIZE_EXCEEDED` 审计。

GREEN: local selected `DCC_E2E_CASES=TC-E2E-007..TC-E2E-017` -> PASS for TC-E2E-007 至 TC-E2E-016；TC-E2E-017 按设计失败，因为汇总用例要求 TC-E2E-001 至 TC-E2E-016 在同一轮全部通过。

GREEN: local full `node tests\e2e\dcc-controlled-file-protection.e2e.js` -> PASS，TC-E2E-001 至 TC-E2E-017 全部通过；`test-results\dcc-controlled-file-protection\summary.json` 记录 17 个 passedCases，截图证据包含 `TC-E2E-003-preview.png`、`TC-E2E-004-watermark.png`、`TC-E2E-004-audit.png`、`TC-E2E-005-onlyoffice.png`、`TC-E2E-010-encrypted-download.png`、`TC-E2E-013-auditor.png`、`TC-E2E-015-1.png` 至 `TC-E2E-015-3.png`。
