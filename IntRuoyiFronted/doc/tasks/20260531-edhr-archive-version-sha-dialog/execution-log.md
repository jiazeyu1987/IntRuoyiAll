# 执行日志：归档版本弹窗 sha256 证据

## Bug

- 归档版本弹窗未展示真实归档响应里的 `sha256`，导致用户无法在版本列表核对归档下载摘要，真实 release E2E 在 `归档版本可查看` 步骤失败。
- 字段审计 E2E 在定位执行记录后使用未初始化的 `config.executionCode`，把 `undefined` 传入 Playwright `getByText()`。

## Feature

- eDHR 执行详情“归档版本”弹窗展示每个归档版本的 `SHA-256`。
- 字段审计真实 E2E 定位执行记录步骤使用列表接口返回的真实 `row.executionCode`。

## Expected

- 用户打开归档版本弹窗时能看到同一归档的 `SHA-256`，并可与下载文件重算摘要比对。
- E2E 脚本缺少文本断言目标时必须 fail fast，不能暴露 Playwright 内部 undefined selector 异常。

## Acceptance

- AC1: 归档版本表格包含 `SHA-256` 列并绑定 `row.sha256`。
- AC2: 真实 release gate 的归档版本步骤能在弹窗文本中找到本轮归档 `sha256`。
- AC3: 字段审计定位执行记录使用列表 API 返回的 `row.executionCode` 并断言详情页同一编号。
- AC4: 静态合同、release check、真实 release gate、格式检查全部通过。

## Reproduction

- `node scripts\edhr-release-e2e-coverage-gate.mjs --run-real --report D:\ProjectPackage\Int\IntRuoyi\output\local-edhr-int-main-verify\edhr-release-real-report.json`

## Root Cause

- `src/views/mes/pro/edhr/ExecutionPage.vue` 的归档版本弹窗只展示版本、格式、状态、归档编号、文件名、时间、失败原因和操作，没有展示已由接口返回的 `sha256` 字段。
- `tests/e2e/edhr-field-audit-real-flow.e2e.js` 的 `collectConfig()` 不设置 `executionCode`，但 `openExecutionFromFieldAuditList()` 使用 `config.executionCode` 过滤行并断言文本。

BDD: 归档版本弹窗展示 sha256 -> Given 已关闭 eDHR 记录通过真实归档接口生成 SEALED PDF 归档 / When 用户点击执行详情“查看版本”打开归档版本弹窗 / Then 弹窗必须展示同一归档版本的 `sha256`，便于人工和 E2E 核对受控下载文件摘要。

RED: `node scripts\edhr-release-e2e-coverage-gate.mjs --run-real --report D:\ProjectPackage\Int\IntRuoyi\output\local-edhr-int-main-verify\edhr-release-real-report.json` -> FAIL, `归档版本弹窗未展示本轮归档 sha256`；真实接口已返回 archive id=21、sha256=53f14c22972951ec034e5c3cf8b06088ee547bee7d0687852ec477465830e578，但弹窗文本只包含版本、格式、状态、归档编号、文件名、时间和操作。

RED: `node --test scripts\edhr-archive-export.test.mjs` -> FAIL, `archive versions dialog should render a visible SHA-256 column bound to row.sha256`。

GREEN: `node --test scripts\edhr-archive-export.test.mjs` -> PASS, 8 tests；归档版本弹窗静态合同已覆盖 `SHA-256` 列与 `row.sha256` 绑定。

RED: `node scripts\edhr-release-e2e-coverage-gate.mjs --run-real --report D:\ProjectPackage\Int\IntRuoyi\output\local-edhr-int-main-verify\edhr-release-real-report.json` -> FAIL, `e2e:edhr:field-audit` 在定位执行记录后把未初始化的 `config.executionCode` 传给 `waitForText`，触发 Playwright `Cannot read properties of undefined (reading 'unicode')`。

RED: `node --test scripts\edhr-field-audit-e2e-contract.test.mjs` -> FAIL, `定位执行记录 helper 必须接收字段审计列表真实 row，不能依赖未初始化的 config.executionCode。`

GREEN: `node --test scripts\edhr-field-audit-e2e-contract.test.mjs` -> PASS, 5 tests；定位执行记录使用真实列表行 `row.executionCode`，`waitForText` 对空值 fail fast。

GREEN: `node tests\e2e\edhr-field-audit-real-flow.e2e.js` -> PASS, 使用测试租户 executionId=56 完成字段审计列表、详情、校验链、导出和定位执行记录。

GREEN: `node scripts\edhr-release-e2e-coverage-gate.mjs --run-real --report D:\ProjectPackage\Int\IntRuoyi\output\local-edhr-int-main-verify\edhr-release-real-report.json` -> PASS, scripts=e2e:edhr:approval-tracking, e2e:edhr:execution-list, e2e:edhr:tracking-signature, e2e:edhr:field-audit, e2e:edhr:domain-trace, e2e:edhr:permission-matrix, e2e:edhr:archive-health；本地测试租户 fresh 输入后缀 `0531002024`。

## Verification

- GREEN: `node --test scripts\edhr-archive-export.test.mjs` -> PASS, 8 tests。
- GREEN: `node --test scripts\edhr-field-audit-e2e-contract.test.mjs` -> PASS, 5 tests。
- GREEN: `node scripts\edhr-release-e2e-coverage-gate.mjs --check --report D:\ProjectPackage\Int\IntRuoyi\output\local-edhr-int-main-verify\edhr-release-coverage-check-report.json` -> PASS, features=10, checkScripts=7, syntaxFiles=7。
- GREEN: `node scripts\edhr-release-e2e-coverage-gate.mjs --run-real --report D:\ProjectPackage\Int\IntRuoyi\output\local-edhr-int-main-verify\edhr-release-real-report.json` -> PASS, 7 个真实 E2E package scripts 全部通过。
- GREEN: `git diff --check` -> PASS，仅有 CRLF 提示，无 whitespace error。

## Blockers

- none.
