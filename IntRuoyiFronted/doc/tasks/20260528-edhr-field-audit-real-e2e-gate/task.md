# 20260528-edhr-field-audit-real-e2e-gate

## Task Goal

为 eDHR 字段审计补齐当前 worktree 的真实用户路径 E2E 门禁，使 reviewer 能证明字段审计列表、详情、链校验和导出都经由前端真实页面、真实测试租户、真实后端 API 完成验证。

本任务不得使用 mock 数据、测试专用 UI、API-only 替代路径、静默跳过或 fallback。若当前环境缺少字段审计真实数据、权限、后端运行时或前端入口，必须 fail fast 并记录具体阻塞与影响。

## Scope

- `tests/e2e/edhr-field-audit-real-flow.e2e.js`
- `scripts/edhr-field-audit-e2e-contract.test.mjs`
- `package.json`
- `doc/tasks/20260528-edhr-field-audit-real-e2e-gate/`

除非真实 E2E 暴露后端或页面缺陷，本任务不改业务页面与 API 合同；如必须改，需先补充 RED 证据并更新本文档。

## BDD Scenarios

- 字段审计列表可追溯：Given 测试租户存在带 `FIELD_CHANGE` 字段审计的真实执行记录 / When 用户登录并打开 `/mes/pro/feedback/edhr-field-audit?executionId=<id>` / Then 页面调用真实 `/field-audit/page`，列表展示执行编号、字段路径、旧值、新值、原因、修改人、签名和 hash 状态。
- 字段审计详情可核验：Given 列表中存在字段审计行 / When 用户点击“详情” / Then 页面进入 `/mes/pro/feedback/edhr-field-audit/detail` 并展示 items 字段路径/标识、旧值、新值、原因、修改人、签名或审计 hash，且 hash 状态来自真实详情 API。
- 字段审计链可校验：Given 当前执行记录有完整字段审计链 / When 用户点击列表或详情中的链校验按钮 / Then 前端调用真实 `/field-audit/verify-chain`，校验结果为 `VALID` 时页面显示通过，非 `VALID` 必须失败暴露。
- 字段审计链可导出：Given 当前执行记录字段审计链可校验 / When 用户点击“导出审计链” / Then 前端调用真实 `/field-audit/export`，响应包含 fileName、contentType、sha256、recordCount、hashVerification 和非空 content，并触发真实下载事件或等价浏览器下载证据。

## TDD Plan

- RED: 新增静态合同测试，证明当前缺少字段审计真实 E2E 脚本、package script、列表/详情/校验/导出断言。
- GREEN: 增加最小真实 E2E 脚本与 package scripts，复用现有登录、租户、响应捕获、截图和 fail-fast 模式。
- REGRESSION: 运行字段审计 API/UI 静态合同、E2E 合同、语法检查和真实 E2E。

## Expected Verification

- `node --test scripts/edhr-field-audit-api-contract.test.mjs scripts/edhr-field-audit-ui-contract.test.mjs scripts/edhr-field-audit-e2e-contract.test.mjs`
- `node --check tests/e2e/edhr-field-audit-real-flow.e2e.js`
- `pnpm e2e:edhr:field-audit:check`
- `$env:EDHR_FIELD_AUDIT_BASE_URL='http://localhost:8081'; $env:EDHR_FIELD_AUDIT_TENANT='测试租户'; $env:EDHR_FIELD_AUDIT_USERNAME='aoteman'; $env:EDHR_FIELD_AUDIT_PASSWORD='<test-password-from-login-baseline>'; $env:EDHR_FIELD_AUDIT_EXECUTION_ID='<real-id>'; pnpm e2e:edhr:field-audit`

真实 E2E 必须使用测试租户真实用户路径。最终只允许使用 API 响应作为页面操作后的交叉校验证据，不得绕过前端直接调用接口完成主验证。
默认 evidence 输出到 `doc/tasks/20260528-edhr-field-audit-real-e2e-gate/real-e2e-evidence.md`，该文件是可提交任务证据；`test-results/edhr-field-audit/` 下截图、trace、result.json 与下载文件不提交。

## Milestones

- [completed] M1: 创建任务文档与 BDD/TDD/E2E 放行口径。
- [completed] M2: 子 agent 增加 RED 静态 E2E 合同。
- [completed] M3: 子 agent 实现真实字段审计 E2E 与 package scripts。
- [completed] M4: worker 修复独立 reviewer 阻塞项，补齐列表旧/新值 UI 可见断言、详情 items 关键审计内容 UI 可见断言、证据路径说明和静态合同覆盖。
- [completed] M5: 主 reviewer 复跑静态合同、语法检查、package check、真实 E2E，并确认独立 reviewer 阻塞项已修复。

## Current Status

- status: completed
- owner: main reviewer
- reviewer mode: main reviewer coordinates worker/explorer subagents; worker may edit only scoped files; reviewer decides release.
- worker update: 字段审计真实 E2E、静态合同测试和 package scripts 已完成；本次 review-fix 已补齐列表页 oldValueDisplay/oldValueJson 与 newValueDisplay/newValueJson 页面正文可见断言，详情页 items 的 fieldPath/fieldKey、old/new、reasonText/reasonCategory、actorName、signatureId/auditHash 页面正文可见断言，并明确默认 evidence 文件可提交。
- final reviewer verification: 静态合同、语法检查、package check、`git diff --check` 和真实字段审计 E2E 均已由主 reviewer 复跑通过。

## Known Preconditions

- 前端入口默认 `http://localhost:8081`。
- 后端需提供当前 eDHR 字段审计 API，当前本地后端通常为 `http://localhost:48098` 经前端代理访问。
- 测试租户账号默认：租户 `测试租户`，用户 `aoteman`；密码必须由 `EDHR_FIELD_AUDIT_PASSWORD` 或登录基线注入，不写入脚本默认值或证据文件。
- 若默认 executionId 不存在字段审计数据，E2E 必须 fail fast，并提示设置 `EDHR_FIELD_AUDIT_EXECUTION_ID` 或先通过真实用户路径创建字段审计数据。

## Cleanup Keep

- `doc/tasks/20260528-edhr-field-audit-real-e2e-gate/real-e2e-evidence.md`
- `scripts/edhr-field-audit-e2e-contract.test.mjs`
- `tests/e2e/edhr-field-audit-real-flow.e2e.js`
- `package.json`
