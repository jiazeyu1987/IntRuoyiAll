# eDHR 主数据追溯列表真实 E2E 门禁 Execution Log

BDD: 主数据追溯列表可查询 -> Given 测试租户存在真实 eDHR 执行记录和主数据追溯结果, When 执行人通过前端打开 `/mes/pro/feedback/edhr-domain-trace` 并按执行编号查询, Then 页面展示目标执行编号、追溯状态、domainTraceHash、blockerCount 和 itemCount。

BDD: 主数据追溯列表进入详情 -> Given 目标执行记录已经出现在主数据追溯列表, When 用户点击列表中的执行编号或详情入口, Then 前端进入 `/mes/pro/feedback/edhr-domain-trace/detail` 并继续展示该执行记录的 canonical 详情证据。

BDD: 主数据追溯详情校验保持有效 -> Given 用户已经从列表进入详情页, When 用户触发主数据追溯校验, Then 前端仍然发起真实 `/domain-trace/verify` 请求，最终 `status`、`domainTraceHash`、`blockers[]` 和 `items[]` 与后端详情 API 一致。

BDD: 主数据追溯列表 E2E 缺前置即阻塞 -> Given 缺少真实前端入口、测试租户、账号、执行记录、分页接口响应或 Playwright runtime, When E2E 启动或打开列表页, Then 脚本必须 fail fast 写入 evidence markdown，不得使用 mock、API 替代列表路径或 silent downgrade。

GREEN: M1 task package created before E2E script changes.

RED: `node --test scripts/edhr-domain-trace-e2e-contract.test.mjs` -> FAIL, 新增列表真实路径合同测试失败于 `E2E 脚本必须声明主数据追溯列表默认路由常量。`；当前脚本仍然登录后直接打开详情页，缺少列表路由、分页 endpoint、列表 helper 和 `domain-trace-list` 证据标记。

GREEN: 实现主数据追溯列表真实路径 -> PASS, E2E 脚本登录后先打开 `/mes/pro/feedback/edhr-domain-trace?executionCode=...&executionId=...`，等待真实 `/mes/pro/batch-record-execution/domain-trace/page` 响应，解析 rows，按 `executionId` 或 `executionCode` 找目标行，断言 `status`、`domainTraceHash`、`blockerCount`、`itemCount` 和页面关键证据，然后记录 `domain-trace-list` 截图步骤并从列表进入详情。

GREEN: `node --test scripts/edhr-domain-trace-e2e-contract.test.mjs` -> PASS, 6 tests passed / 0 failed；新增列表真实路径合同和既有 expected status / blocker count / evidence 任务 ID 合同均通过。

GREEN: `pnpm e2e:edhr:domain-trace:check` -> PASS, `node --check tests/e2e/edhr-domain-trace-real-flow.e2e.js` 完成。

CHECK: `git diff --check` -> PASS, only CRLF normalization warnings were reported for touched tracked JS files.

HANDOFF: `pnpm e2e:edhr:domain-trace` -> NOT RUN by worker per instruction；真实账号、真实租户、真实前端路径执行由主 reviewer 负责。

CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-edhr-domain-trace-list-e2e-gate --mode preview` -> blocked, delete candidates `<none>`；preview blocked because linked worktree closeout cannot fast-forward merge `codex/20260527-edhr-prod-doc-code-subagent-review` into `int_main` and pending task changes are not committed by this worker. No cleanup apply was run and no files were deleted.

RED: Reviewer real E2E `pnpm e2e:edhr:domain-trace` with test tenant execution `40 / BRE202605280518101280040` -> FAIL, `主数据追溯列表目标行 缺少 itemCount 字段。` Root cause: worker E2E 过严要求分页行直接返回 `itemCount` 字段，未按现有前端/API 合同 `row.itemCount ?? row.items?.length ?? 0` 读取真实 UI 计数来源；但也不能在两个来源都缺失时接受默认 0。

RED: `node --test scripts/edhr-domain-trace-e2e-contract.test.mjs` -> FAIL, 新增列表计数来源合同失败于 `主数据追溯列表目标行 缺少 itemCount 字段。`；该合同要求 `itemCount` 可由 `itemCount` 或 `items.length` 得到，`blockerCount` 可由 `blockerCount` 或 `blockers.length` 得到，缺来源必须 fail fast，目标记录 `itemCount` 必须大于 0。

GREEN: 修复列表计数来源 -> PASS, `summarizeDomainTraceListRow` 按真实 UI 合同从 `blockerCount` 或 `blockers.length`、`itemCount` 或 `items.length` 读取；两个来源都缺失时 fail fast，目标列表记录 `itemCount <= 0` 时 fail fast，页面断言继续匹配实际展示的 `blockerCount=<computed>` 与 `items=<computed>`。

GREEN: `node --test scripts/edhr-domain-trace-e2e-contract.test.mjs` -> PASS, 7 tests passed / 0 failed；新增计数来源回归合同通过，并防止回退到“必须直接有 itemCount 字段”或“无来源接受 0”。

GREEN: `pnpm e2e:edhr:domain-trace:check` -> PASS, `node --check tests/e2e/edhr-domain-trace-real-flow.e2e.js` 完成。

HANDOFF: Reviewer RED repair complete for static scope. `pnpm e2e:edhr:domain-trace` real run remains NOT RUN by worker per instruction；等待主 reviewer 使用真实账号复跑。

CHECK: `git diff --check` -> PASS, only CRLF normalization warnings were reported for touched tracked JS files.

CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-edhr-domain-trace-list-e2e-gate --mode preview` -> blocked, delete candidates `<none>`；preview remains blocked because linked worktree closeout cannot fast-forward merge `codex/20260527-edhr-prod-doc-code-subagent-review` into `int_main` and pending task changes are not committed by this worker. No cleanup apply was run and no files were deleted.

RED: Reviewer real E2E rerun after frontend count-source repair -> FAIL, `主数据追溯列表目标行 缺少 itemCount 来源：必须提供 itemCount 或 items.length。` Impact: the frontend E2E correctly exposed that `/domain-trace/page` did not provide a verifiable list item count source, so release evidence could not be accepted from the list page.

GREEN: Backend reviewer repair `/domain-trace/page itemCount` -> PASS, backend adds `itemCount` to `MesProBatchRecordDomainTracePageRespVO` and fills it from persisted DomainTrace item rows when a real snapshot exists; rows without snapshot keep missing evidence semantics instead of fake success.

GREEN: `node --test scripts/edhr-domain-trace-e2e-contract.test.mjs` -> PASS, 7 tests passed / 0 failed after backend contract repair remained compatible with strict list count-source parsing.

GREEN: `pnpm e2e:edhr:domain-trace:check` -> PASS, `node --check tests/e2e/edhr-domain-trace-real-flow.e2e.js` completed.

GREEN: `pnpm e2e:edhr:domain-trace` with `EDHR_E2E_BASE_URL=http://localhost:8081`, tenant `测试租户`, executor `aoteman`, execution `40 / BRE202605280518101280040` -> PASS. Evidence recorded list step `01-domain-trace-list.png`, detail step `02-domain-trace-detail.png`, verified step `03-domain-trace-verified.png`; list status `VERIFIED`, list blocker count `0`, list item count `8`, final status `VERIFIED`, final blocker count `0`, final item count `8`, hash `2c7c5aa13178e7c452697672e86ca1efa2c22ca00ada1e2ff22da0e19dd72a79`.

GREEN: Final reviewer `node --test scripts/edhr-domain-trace-e2e-contract.test.mjs` -> PASS, 7 tests passed / 0 failed.

GREEN: Final reviewer `pnpm e2e:edhr:domain-trace:check` -> PASS, `node --check tests/e2e/edhr-domain-trace-real-flow.e2e.js` completed.

GREEN: Final reviewer `git diff --check` -> PASS, no whitespace errors; Git reported only LF-to-CRLF normalization warnings for touched JS files.

GREEN: Final reviewer real E2E `pnpm e2e:edhr:domain-trace` -> PASS. Evidence file generated at `test-results/edhr-domain-trace/evidence.md` with current status `PASS`, list/final `status=VERIFIED`, `blockerCount=0`, `itemCount=8`, hash `2c7c5aa13178e7c452697672e86ca1efa2c22ca00ada1e2ff22da0e19dd72a79`.

REVIEW: Independent reviewer -> PASS, no blocking findings. Reviewer confirmed target can be implemented without side effects, BDD/TDD/subagent-driven evidence is present, logic and interfaces are clear, and the E2E uses the real list-to-detail path.

GREEN: Final reviewer rebuilt backend jar, restarted current backend on `48098` with explicit local DCC E2E signature evidence config required by startup fail-fast, and reran `pnpm e2e:edhr:domain-trace` -> PASS. The final browser path again recorded list/final `status=VERIFIED`, `blockerCount=0`, `itemCount=8`, hash `2c7c5aa13178e7c452697672e86ca1efa2c22ca00ada1e2ff22da0e19dd72a79`.

CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-edhr-domain-trace-list-e2e-gate --mode preview` -> blocked, delete candidates `<none>`. Cleanup apply was not run because the linked worktree cannot be fast-forward merged into `int_main` and task changes are still pending for the current commit.
