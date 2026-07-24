# 20260529-edhr-tracking-signature-real-e2e-gate

## Task Goal

为 eDHR 追踪页与电子签名页补齐独立真实用户路径 E2E 门禁，证明测试租户用户可以从动态菜单路由打开 `/mes/pro/feedback/edhr-tracking` 与 `/mes/pro/feedback/edhr-signatures`，按真实执行记录筛选并查看追踪、时间线与签名合规证据。

本任务不得使用 mock 数据、测试专用 UI、API-only 替代路径、静默跳过、默认密码或 fallback。若真实前端、真实后端、测试租户账号、目标执行记录、追踪事件、签名记录或页面入口缺失，必须 fail fast 并记录阻塞与影响。

## Scope

- `tests/e2e/edhr-tracking-signature-real-flow.e2e.js`
- `scripts/edhr-tracking-signature-e2e-contract.test.mjs`
- `package.json`
- `doc/tasks/20260529-edhr-tracking-signature-real-e2e-gate/`

除非真实 E2E 暴露页面、接口或菜单缺陷，本任务不修改业务页面与 API 合同；如必须修改，先补充 RED 证据并更新本文档。

## BDD Scenarios

- BDD: 追踪页按真实执行编号筛选 -> Given 测试租户存在真实 eDHR 执行记录、追踪事件和动态菜单 `eDHR追踪` / When 用户登录并打开 `/mes/pro/feedback/edhr-tracking?executionCode=<real-code>` / Then 前端请求真实 `/mes/pro/batch-record-execution/tracking-page`，页面展示执行编号、工单号、批次号、当前状态、最后事件、意见/原因、最后处理时间和归档状态。
- BDD: 追踪页进入真实执行详情 -> Given 追踪页列表展示目标执行记录 / When 用户点击该行“查看” / Then 前端进入 `/mes/pro/feedback/edhr-execution/detail?id=<executionId>`，详情页请求真实 `/tracking-timeline`，展示同一执行编号与提交、审批或归档时间线证据。
- BDD: 签名页按真实执行筛选 -> Given 测试租户存在真实 eDHR 电子签名记录和动态菜单 `eDHR签名记录` / When 用户打开 `/mes/pro/feedback/edhr-signatures?executionId=<real-id>` / Then 前端请求真实 `/mes/pro/batch-record-execution/signature-page`，页面展示签名编号、执行编号、动作、签名含义、签名人、签名方式、密码校验、流程任务、签名时间和意见/原因。
- BDD: 签名页动作筛选真实有效 -> Given 目标执行记录存在 SUBMIT、APPROVE 或 ARCHIVE_SEAL 等真实签名动作 / When 用户在动作筛选中选择真实动作并查询 / Then 页面只展示该动作的真实签名记录，且 API 查询参数包含对应 `actionType`。
- BDD: 缺少真实前置即阻塞 -> Given 缺少测试租户密码、真实执行记录、追踪事件、签名记录、菜单权限或前端入口 / When 执行 E2E / Then 脚本写入 `BLOCKED/FAIL` 证据并退出非零，不使用 mock、默认密码、live 租户、API-only 或 silent downgrade。

## Strict TDD Plan

1. RED: 新增 `scripts/edhr-tracking-signature-e2e-contract.test.mjs`，先断言当前缺少独立真实 E2E 脚本、package scripts、登录/测试租户保护、追踪页真实路由、签名页真实路由、timeline/API 监听、签名动作筛选和证据写入。
2. GREEN: 新增 `tests/e2e/edhr-tracking-signature-real-flow.e2e.js` 与 package scripts，使静态合同通过。
3. GREEN: 运行真实 Playwright E2E，使用测试租户真实用户、真实执行记录、真实追踪事件和真实签名记录，完成追踪筛选、进入详情时间线、签名筛选、动作筛选与证据写入。
4. REGRESSION: 运行既有追踪/签名静态合同、相关 eDHR E2E 语法检查、真实 E2E、`git diff --check`，并由独立 reviewer 子 agent 复审。

## Expected Verification

- `node --test scripts/edhr-tracking-signature-e2e-contract.test.mjs`
- `node --check tests/e2e/edhr-tracking-signature-real-flow.e2e.js`
- `pnpm e2e:edhr:tracking-signature:check`
- `$env:EDHR_TRACKING_SIGNATURE_BASE_URL='http://localhost:8081'; $env:EDHR_TRACKING_SIGNATURE_TENANT='测试租户'; $env:EDHR_TRACKING_SIGNATURE_USERNAME='aoteman'; $env:EDHR_TRACKING_SIGNATURE_PASSWORD='<test-password-from-login-baseline>'; $env:EDHR_TRACKING_SIGNATURE_EXECUTION_ID='<real-id>'; $env:EDHR_TRACKING_SIGNATURE_EXECUTION_CODE='<real-code>'; $env:EDHR_TRACKING_SIGNATURE_BATCH_CODE='<real-batch>'; pnpm e2e:edhr:tracking-signature`
- `git diff --check`

## Real Data Baseline

- 默认前端入口：`http://localhost:8081`
- 测试租户：`测试租户`
- 默认用户：`aoteman`
- 真实密码只能从当前登录基线或环境变量注入，不写入脚本默认值、文档证据或提交。
- 当前本地真实数据候选：
  - executionId `40`
  - executionCode `BRE202605280518101280040`
  - batchCode `EDHR-BATCH-122-E2E-APPROVE-GATE05280525`
  - archiveId `9`
  - archiveStatus `SEALED`
  - archiveSha256 `27a36dfd8b8fc30f78e02c1505ea90e26263ffdcbf596a2127d6b189c79f959f`

## Current Status

- status: completed
- previous frontend task check: `20260529-edhr-execution-list-real-e2e-gate` is marked completed and committed.
- current gap evidence:
  - `package.json` has eDHR approval tracking, domain trace, field audit, execution list, archive health, and permission matrix E2E scripts.
  - No `e2e:edhr:tracking-signature` package script existed before this task.
  - `scripts/edhr-tracking-signature-contract.test.mjs` verifies static page/API contracts but does not execute a dedicated real browser path for these pages.
  - `tests/e2e/edhr-approval-tracking-real-flow.e2e.js` touches tracking/signature as part of approval flow, and permission matrix opens them readonly, but neither was an independent page-level release gate.
  - This gap is now closed by the current task files, static contract, and real E2E evidence.

## Final Verification Result

- reviewer decision: PASS
- static contract: `node --test scripts\edhr-tracking-signature-e2e-contract.test.mjs` -> PASS, 6 tests
- syntax check: `node --check tests\e2e\edhr-tracking-signature-real-flow.e2e.js` -> PASS
- package gate: `pnpm e2e:edhr:tracking-signature:check` -> PASS
- real E2E: `pnpm e2e:edhr:tracking-signature` -> PASS using the current worktree frontend service on `http://localhost:8081`
- regression: `node --test scripts\edhr-tracking-signature-contract.test.mjs scripts\edhr-tracking-signature-e2e-contract.test.mjs scripts\edhr-approval-page-contract.test.mjs` -> PASS, 15 tests
- related E2E syntax regression: `node --check tests\e2e\edhr-approval-tracking-real-flow.e2e.js`, `node --check tests\e2e\edhr-permission-tenant-matrix.e2e.js`, and `node --check tests\e2e\edhr-tracking-signature-real-flow.e2e.js` -> PASS
- sensitive default password scan: PASS
- whitespace: `git diff --check` -> PASS
- independent reviewer subagent: PASS, `logic_status=pass`, `usability_status=pass`, `ui_status=pass`, no blocking issues and no required changes

## Cleanup Keep

- `doc/tasks/20260529-edhr-tracking-signature-real-e2e-gate/task.md`
- `doc/tasks/20260529-edhr-tracking-signature-real-e2e-gate/execution-log.md`
- `doc/tasks/20260529-edhr-tracking-signature-real-e2e-gate/real-e2e-evidence.md`

## Cleanup Candidates

- `test-results/edhr-tracking-signature/`

## Reviewer Gate

主 reviewer 只在以下条件同时满足时放行：

- 子 agent 产物按本文档实现追踪页与签名页独立真实 E2E，且不引入 mock、fallback、默认密码、live 租户或测试专用 UI。
- 静态合同证明脚本覆盖 package scripts、登录保护、测试租户保护、真实路由、真实 API 监听、追踪进入详情、签名动作筛选、证据写入和 fail-fast 前置。
- 真实 E2E 通过并把非密证据写入 `real-e2e-evidence.md`。
- 独立 reviewer 子 agent 判定 logic/usability/UI 均无阻塞问题。
- 当前任务直接相关文件通过验证并单独提交。
