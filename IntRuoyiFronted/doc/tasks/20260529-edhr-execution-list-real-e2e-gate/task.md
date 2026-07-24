# 20260529-edhr-execution-list-real-e2e-gate

## Task Goal

为 eDHR 执行列表补齐独立真实用户路径 E2E 门禁，证明用户可以从动态菜单路由 `/mes/pro/feedback/edhr-execution` 查询真实执行记录、查看归档状态、下载已封存归档，并从列表进入执行详情页。

本任务不得使用 mock 数据、测试专用 UI、API-only 替代路径、静默跳过或 fallback。若真实前端、真实后端、测试租户账号、目标执行记录、归档文件或下载权限缺失，必须 fail fast 并记录阻塞与影响。

## Scope

- `tests/e2e/edhr-execution-list-real-flow.e2e.js`
- `scripts/edhr-execution-list-e2e-contract.test.mjs`
- `package.json`
- `doc/tasks/20260529-edhr-execution-list-real-e2e-gate/`

除非真实 E2E 暴露页面或接口缺陷，本任务不修改业务页面与 API 合同；如必须修改，先补充 RED 证据并更新本文档。

## BDD Scenarios

- BDD: 执行列表可查询 -> Given 测试租户存在真实 eDHR 执行记录和动态菜单 `eDHR执行列表` / When 用户登录并打开 `/mes/pro/feedback/edhr-execution?batchCode=<real-batch>` / Then 前端请求真实 `/mes/pro/batch-record-execution/page`，页面展示执行编号、生产工单、批次号、执行状态、绑定状态、打开能力和上下文证据。
- BDD: 最新归档状态可见 -> Given 目标执行记录已有真实 `SEALED` PDF 归档 / When 执行列表加载完成 / Then 前端请求真实 `/mes/pro/batch-record-execution-archive/latest`，页面展示 `已封存`、`V1`、`PDF` 或等价归档证据。
- BDD: 列表归档可下载 -> Given 用户具备归档下载权限且目标归档已封存 / When 用户点击列表行的“下载归档” / Then 前端调用真实 `/mes/pro/batch-record-execution-archive/download?id=<archiveId>`，浏览器下载文件，下载 SHA-256 与真实归档接口/数据库证据一致。
- BDD: 列表进入详情 -> Given 目标执行记录在列表中可见 / When 用户点击列表行“详情” / Then 前端进入 `/mes/pro/feedback/edhr-execution/detail?id=<executionId>&fromList=1`，详情页展示同一执行编号与只读/关闭态证据。
- BDD: 缺少真实前置即阻塞 -> Given 缺少测试租户密码、真实执行记录、归档、权限或前端入口 / When 执行 E2E / Then 脚本写入 `BLOCKED/FAIL` 证据并退出非零，不使用 mock、默认密码、live 租户、API-only 或 silent downgrade。

## Strict TDD Plan

1. RED: 新增 `scripts/edhr-execution-list-e2e-contract.test.mjs`，先断言当前缺少执行列表真实 E2E 脚本、package scripts、登录/测试租户保护、列表路由、分页 API、latest archive、download、详情跳转和证据写入。
2. GREEN: 新增 `tests/e2e/edhr-execution-list-real-flow.e2e.js` 与 package scripts，使静态合同通过。
3. GREEN: 运行真实 Playwright E2E，使用测试租户真实用户、真实执行记录和真实归档，完成列表查询、归档状态、下载、详情跳转。
4. REGRESSION: 运行相关 eDHR 静态合同、E2E 语法检查、执行列表真实 E2E、`git diff --check`，并由独立 reviewer 子 agent 复审。

## Expected Verification

- `node --test scripts/edhr-execution-list-e2e-contract.test.mjs`
- `node --check tests/e2e/edhr-execution-list-real-flow.e2e.js`
- `pnpm e2e:edhr:execution-list:check`
- `$env:EDHR_EXECUTION_LIST_BASE_URL='http://localhost:8081'; $env:EDHR_EXECUTION_LIST_TENANT='测试租户'; $env:EDHR_EXECUTION_LIST_USERNAME='aoteman'; $env:EDHR_EXECUTION_LIST_PASSWORD='<test-password-from-login-baseline>'; $env:EDHR_EXECUTION_LIST_EXECUTION_ID='<real-id>'; $env:EDHR_EXECUTION_LIST_EXECUTION_CODE='<real-code>'; $env:EDHR_EXECUTION_LIST_BATCH_CODE='<real-batch>'; $env:EDHR_EXECUTION_LIST_ARCHIVE_ID='<real-archive-id>'; $env:EDHR_EXECUTION_LIST_ARCHIVE_SHA256='<real-sha256>'; pnpm e2e:edhr:execution-list`
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
  - archiveVersion `1`
  - artifactType `PDF`
  - archiveSha256 `27a36dfd8b8fc30f78e02c1505ea90e26263ffdcbf596a2127d6b189c79f959f`

## Current Status

- status: completed
- previous frontend task check: `20260528-edhr-field-audit-real-e2e-gate` and `20260528-edhr-role-tenant-e2e-gate` are marked completed.
- current gap evidence:
  - `package.json` currently has eDHR approval tracking, domain trace, field audit, archive health, and permission matrix E2E scripts.
  - No `e2e:edhr:execution-list` package script exists before this task.
  - `ExecutionListPage.vue` exists and calls real execution page, latest archive, download, generate archive, and detail navigation APIs.
  - `system_menu` has dynamic menu id `900023`, name `eDHR执行列表`, path `feedback/edhr-execution`, component `mes/pro/edhr/ExecutionListPage`, permission `mes:pro-batch-record-execution:query`.
  - This gap is now closed by the current task files, static contract, and real E2E evidence.

## Final Verification Result

- reviewer decision: PASS
- static contract: `node --test scripts/edhr-execution-list-e2e-contract.test.mjs` -> PASS, 6 tests
- syntax check: `node --check tests/e2e/edhr-execution-list-real-flow.e2e.js` -> PASS
- package gate: `pnpm e2e:edhr:execution-list:check` -> PASS
- real E2E: `pnpm e2e:edhr:execution-list` -> PASS
- regression: `node --test scripts/edhr-v1-feedback-entry.test.mjs scripts/edhr-archive-export.test.mjs scripts/edhr-approval-archive-gate.test.mjs scripts/edhr-execution-list-e2e-contract.test.mjs` -> PASS, 20 tests
- whitespace: `git diff --check` -> PASS

## Cleanup Keep

- `doc/tasks/20260529-edhr-execution-list-real-e2e-gate/task.md`
- `doc/tasks/20260529-edhr-execution-list-real-e2e-gate/execution-log.md`
- `doc/tasks/20260529-edhr-execution-list-real-e2e-gate/real-e2e-evidence.md`

## Cleanup Candidates

- `test-results/edhr-execution-list/`

## Reviewer Gate

主 reviewer 只在以下条件同时满足时放行：

- 子 agent 产物按本文档实现执行列表真实 E2E，且不引入 mock、fallback、默认密码、live 租户或测试专用 UI。
- 静态合同证明脚本覆盖列表查询、latest archive、下载、详情跳转、证据写入和 fail-fast 前置。
- 真实 E2E 通过并把非密证据写入 `real-e2e-evidence.md`。
- 独立 reviewer 子 agent 判定 logic/usability/UI 均无阻塞问题。
- 当前任务直接相关文件通过验证并单独提交。
