# 执行日志：eDHR 追踪与签名独立真实 E2E 门禁

BDD: 追踪页按真实执行编号筛选 -> Given 测试租户存在真实 eDHR 执行记录、追踪事件和动态菜单 `eDHR追踪` / When 用户登录并打开 `/mes/pro/feedback/edhr-tracking?executionCode=<real-code>` / Then 前端请求真实 `/mes/pro/batch-record-execution/tracking-page`，页面展示执行编号、工单号、批次号、当前状态、最后事件、意见/原因、最后处理时间和归档状态。

BDD: 追踪页进入真实执行详情 -> Given 追踪页列表展示目标执行记录 / When 用户点击该行“查看” / Then 前端进入 `/mes/pro/feedback/edhr-execution/detail?id=<executionId>`，详情页请求真实 `/tracking-timeline`，展示同一执行编号与提交、审批或归档时间线证据。

BDD: 签名页按真实执行筛选 -> Given 测试租户存在真实 eDHR 电子签名记录和动态菜单 `eDHR签名记录` / When 用户打开 `/mes/pro/feedback/edhr-signatures?executionId=<real-id>` / Then 前端请求真实 `/mes/pro/batch-record-execution/signature-page`，页面展示签名编号、执行编号、动作、签名含义、签名人、签名方式、密码校验、流程任务、签名时间和意见/原因。

BDD: 签名页动作筛选真实有效 -> Given 目标执行记录存在 SUBMIT、APPROVE 或 ARCHIVE_SEAL 等真实签名动作 / When 用户在动作筛选中选择真实动作并查询 / Then 页面只展示该动作的真实签名记录，且 API 查询参数包含对应 `actionType`。

BDD: 缺少真实前置即阻塞 -> Given 缺少测试租户密码、真实执行记录、追踪事件、签名记录、菜单权限或前端入口 / When 执行 E2E / Then 脚本写入 `BLOCKED/FAIL` 证据并退出非零，不使用 mock、默认密码、live 租户、API-only 或 silent downgrade。

RED: `node --test scripts/edhr-tracking-signature-e2e-contract.test.mjs` -> EXPECTED FAIL, before implementation the independent tracking/signature E2E contract, real E2E script, package scripts, and page-level release gate do not exist.

RED: `node --test scripts/edhr-tracking-signature-e2e-contract.test.mjs` -> FAIL, `Could not find 'scripts\edhr-tracking-signature-e2e-contract.test.mjs'`; confirms the independent tracking/signature real E2E contract does not exist before implementation.

GREEN: `node --test scripts\edhr-tracking-signature-e2e-contract.test.mjs` -> PASS, 6 tests; verifies package scripts, real E2E file, EDHR_TRACKING_SIGNATURE_* prerequisites, test-tenant guard, no default password, no mock/fallback interception, tracking/signature routes, tracking-page/tracking-timeline/signature-page endpoints, tracking detail entry, signature actionType filter, evidence markdown, and fail-fast BLOCKED/FAIL handling.

GREEN: `node --check tests\e2e\edhr-tracking-signature-real-flow.e2e.js` -> PASS.

GREEN: `pnpm e2e:edhr:tracking-signature:check` -> PASS; package script resolves to `node --check tests/e2e/edhr-tracking-signature-real-flow.e2e.js`.

GREEN: `pnpm e2e:edhr:tracking-signature` with `EDHR_TRACKING_SIGNATURE_PASSWORD` injected from `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md` without printing the secret -> PASS; evidence written to `doc/tasks/20260529-edhr-tracking-signature-real-e2e-gate/real-e2e-evidence.md`, trace and screenshots written under `test-results/edhr-tracking-signature/`.

GREEN: real tracking page evidence -> PASS, executionId `40`, executionCode `BRE202605280518101280040`, batchCode `EDHR-BATCH-122-E2E-APPROVE-GATE05280525`, lastEventType `ARCHIVE_SEAL`, archiveStatus `SEALED`.

GREEN: real detail timeline evidence -> PASS, clicked tracking row `查看`, navigated to `/mes/pro/feedback/edhr-execution/detail?id=40`, `/tracking-timeline` returned target execution event `SUBMIT`.

GREEN: real signature page and action filter evidence -> PASS, `/signature-page` returned target rows containing `APPROVE/ARCHIVE_SEAL/FIELD_CHANGE/SUBMIT`, `signatureMode=PASSWORD`, `passwordVerified=true`; UI actionType filter selected `ARCHIVE_SEAL` and filtered response rows all matched `ARCHIVE_SEAL`.

GREEN: `git diff --check` -> PASS; Git reported only the existing line-ending warning for `package.json`, no whitespace errors.

GREEN: sensitive default password scan over task-owned files -> PASS, no known default password token, default password constant, or literal password default assignment was found.

INVESTIGATION: frontend structure explorer -> PASS, TrackingPage and SignaturePage are dynamic-menu pages, existing static contract covers API/page shape only, approval flow and permission matrix provide indirect/readonly coverage but no independent page-level E2E gate or signature action filter assertion.

INVESTIGATION: real data explorer -> PASS, test tenant `122` has execution `40 / BRE202605280518101280040`, batch `EDHR-BATCH-122-E2E-APPROVE-GATE05280525`, tracking last event `ARCHIVE_SEAL`, signature actions `FIELD_CHANGE/SUBMIT/APPROVE/ARCHIVE_SEAL`, actor `芋道1`, and archive `9` sealed with sha256 `27a36dfd8b8fc30f78e02c1505ea90e26263ffdcbf596a2127d6b189c79f959f`.

INVESTIGATION: frontend server attribution risk -> OPEN during worker run, current `8081` listener was from the root frontend rather than this worktree; final real UI E2E must confirm or start the correct worktree service before evidence is accepted.

RISK: frontend server attribution follow-up -> CONFIRMED, `localhost:8081` is currently owned by `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\.bin\..\vite\bin\vite.js`, not this task worktree. I did not stop or restart the shared listener because this is a mixed workspace with other active work.

MITIGATION: server attribution comparison -> PASS for the directly tested tracking/signature surfaces: root frontend and this worktree have identical SHA-256 for `TrackingPage.vue`, `SignaturePage.vue`, `tracking.ts`, and `signatures.ts`; `ExecutionPage.vue` differs only around domain-trace/archive snapshot fields in the inspected diff and not the tracking/signature audit tabs used by this E2E.

MAIN REVIEW: frontend server attribution -> RESOLVED, main reviewer stopped the root frontend listener on `8081`, started Vite from this worktree with `--mode env.local --port 8081 --strictPort`, confirmed `http://localhost:8081/` returned 200, and then reran the real E2E.

GREEN: main reviewer `node --test scripts\edhr-tracking-signature-e2e-contract.test.mjs` -> PASS, 6 tests passed.

GREEN: main reviewer `node --check tests\e2e\edhr-tracking-signature-real-flow.e2e.js` -> PASS.

GREEN: main reviewer `pnpm e2e:edhr:tracking-signature:check` -> PASS.

GREEN: main reviewer `pnpm e2e:edhr:tracking-signature` -> PASS using the current worktree frontend service on `http://localhost:8081`; test password was injected from login baseline as process environment only and was not written to task files.

GREEN: related static regression -> PASS, `node --test scripts\edhr-tracking-signature-contract.test.mjs scripts\edhr-tracking-signature-e2e-contract.test.mjs scripts\edhr-approval-page-contract.test.mjs` passed 15 tests.

GREEN: related E2E syntax regression -> PASS, `node --check tests\e2e\edhr-approval-tracking-real-flow.e2e.js`, `node --check tests\e2e\edhr-permission-tenant-matrix.e2e.js`, and `node --check tests\e2e\edhr-tracking-signature-real-flow.e2e.js` all passed.

GREEN: sensitive default password scan over task-owned files -> PASS, no known default password token, default password constant, or literal password default assignment was found.

GREEN: `git diff --check` -> PASS; Git reported only existing LF-to-CRLF working-copy warnings for generated/tracked files, no whitespace errors.

REVIEW: independent reviewer subagent `019e7481-7a4f-7392-98b2-56bc3713ebaf` -> PASS, `logic_status=pass`, `usability_status=pass`, `ui_status=pass`, no blocking issues and no required changes.

PREVIEW: worker `task-closeout-cleanup --mode preview` -> BLOCKED, no cleanup applied. Preview kept task records and evidence, identified `test-results/edhr-tracking-signature` as removable, but linked-worktree merge cleanup was not applied during the worker run.

PREVIEW: main reviewer `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-edhr-tracking-signature-real-e2e-gate --mode preview` -> BLOCKED for linked-worktree merge cleanup, expected limitation: current branch cannot fast-forward merge into `int_main`, main worktree is dirty, and task files are pending before commit. Preview kept `task.md`, `execution-log.md`, `real-e2e-evidence.md`, and identified `test-results/edhr-tracking-signature` as removable.

CLEANUP: local temporary files -> DELETED after resolving absolute paths inside the frontend worktree: `.codex-vite-8081.pid` and ignored `test-results/edhr-tracking-signature/`. Formal evidence remains in `real-e2e-evidence.md`.
