# 执行日志：eDHR 执行列表真实 E2E 门禁

BDD: 执行列表可查询 -> Given 测试租户存在真实 eDHR 执行记录和动态菜单 `eDHR执行列表` / When 用户登录并打开 `/mes/pro/feedback/edhr-execution?batchCode=<real-batch>` / Then 前端请求真实 `/mes/pro/batch-record-execution/page`，页面展示执行编号、生产工单、批次号、执行状态、绑定状态、打开能力和上下文证据。

BDD: 最新归档状态可见 -> Given 目标执行记录已有真实 `SEALED` PDF 归档 / When 执行列表加载完成 / Then 前端请求真实 `/mes/pro/batch-record-execution-archive/latest`，页面展示 `已封存`、`V1`、`PDF` 或等价归档证据。

BDD: 列表归档可下载 -> Given 用户具备归档下载权限且目标归档已封存 / When 用户点击列表行的“下载归档” / Then 前端调用真实 `/mes/pro/batch-record-execution-archive/download?id=<archiveId>`，浏览器下载文件，下载 SHA-256 与真实归档接口/数据库证据一致。

BDD: 列表进入详情 -> Given 目标执行记录在列表中可见 / When 用户点击列表行“详情” / Then 前端进入 `/mes/pro/feedback/edhr-execution/detail?id=<executionId>&fromList=1`，详情页展示同一执行编号与只读/关闭态证据。

BDD: 缺少真实前置即阻塞 -> Given 缺少测试租户密码、真实执行记录、归档、权限或前端入口 / When 执行 E2E / Then 脚本写入 `BLOCKED/FAIL` 证据并退出非零，不使用 mock、默认密码、live 租户、API-only 或 silent downgrade。

RED: `node --test scripts/edhr-execution-list-e2e-contract.test.mjs` -> EXPECTED FAIL, before implementation the contract file, real E2E script, package scripts, and execution-list-specific real path gate do not exist.

RED: `node --test scripts/edhr-execution-list-e2e-contract.test.mjs` -> FAIL, `Could not find 'scripts/edhr-execution-list-e2e-contract.test.mjs'`; confirms the execution-list static contract did not exist before implementation.

INVESTIGATION: current package scripts -> PASS, eDHR approval tracking/domain trace/field audit/archive health/permission matrix exist; execution-list real E2E script is missing.

INVESTIGATION: real data baseline -> PASS, local test tenant `122` has execution `40 / BRE202605280518101280040`, batch `EDHR-BATCH-122-E2E-APPROVE-GATE05280525`, archive `9`, status `SEALED`, artifact `PDF`, sha256 `27a36dfd8b8fc30f78e02c1505ea90e26263ffdcbf596a2127d6b189c79f959f`.

INVESTIGATION: dynamic menu baseline -> PASS, `system_menu.id=900023` maps `eDHR执行列表` to path `feedback/edhr-execution`, component `mes/pro/edhr/ExecutionListPage`, permission `mes:pro-batch-record-execution:query`.

GREEN: `node --test scripts/edhr-execution-list-e2e-contract.test.mjs` -> PASS, 6 tests passed; static contract covers package scripts, login/test-tenant protection, dynamic list route, execution page API, latest archive API, download API, detail navigation, SHA-256 verification, evidence markdown, and fail-fast prerequisites.

GREEN: `node --check tests/e2e/edhr-execution-list-real-flow.e2e.js` -> PASS, execution-list real E2E script syntax is valid.

GREEN: `pnpm e2e:edhr:execution-list:check` -> PASS, package script executes the syntax gate.

RED: `pnpm e2e:edhr:execution-list` -> FAIL, expected script-defect reason: latest archive response for archive `9` does not expose `canDownloadArchive=true`; the UI download button is gated by row-level `canDownloadArchive` or archive-level `canDownloadArchive`, so requiring archive-level permission in the E2E assertion was stricter than the page behavior.

GREEN: E2E assertion fix -> PASS, latest archive now proves SEALED/V1/PDF/SHA evidence while actual download permission is proven by the visible row action, real download API response, browser download event, and SHA-256 match.

GREEN: `node --test scripts/edhr-execution-list-e2e-contract.test.mjs` -> PASS, 6 tests passed after assertion fix.

GREEN: `node --check tests/e2e/edhr-execution-list-real-flow.e2e.js` -> PASS after assertion fix.

GREEN: `pnpm e2e:edhr:execution-list:check` -> PASS after assertion fix.

GREEN: `pnpm e2e:edhr:execution-list` -> PASS, main reviewer injected the test-tenant password from `docs/login-access.md` as process environment only. Real UI flow completed list query, latest archive state, archive download, SHA-256 verification, and detail navigation for execution `40 / BRE202605280518101280040`; evidence written to `real-e2e-evidence.md` without password.

GREEN: password literal scan -> PASS, execution-list task files contain no committed continuous test password literal.

GREEN: related static regression -> PASS, `node --test scripts\edhr-v1-feedback-entry.test.mjs scripts\edhr-archive-export.test.mjs scripts\edhr-approval-archive-gate.test.mjs scripts\edhr-execution-list-e2e-contract.test.mjs` passed 20 tests.

GREEN: `git diff --check` -> PASS, no whitespace errors; Git reported the existing `package.json` LF-to-CRLF working-copy warning only.

REVIEW: independent reviewer subagent `019e743d-9bd8-7a91-863c-8e5becc8026e` -> PASS, `logic_status=pass`, `usability_status=pass`, `ui_status=pass`, no blocking issues and no required changes.

PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-edhr-execution-list-real-e2e-gate --mode preview` -> BLOCKED for apply, expected linked-worktree closeout limitation: current branch cannot be fast-forward merged into `int_main` and the task files are still pending before the current task commit. No cleanup apply was run.

CLEANUP: ignored temporary directory `test-results/edhr-execution-list/` -> DELETED after resolving the absolute path and confirming it is inside the frontend worktree. Formal evidence remains in `real-e2e-evidence.md`.
