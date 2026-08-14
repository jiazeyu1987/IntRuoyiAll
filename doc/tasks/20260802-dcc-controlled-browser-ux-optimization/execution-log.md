# Execution Log

## 2026-08-02

- Intent: 按用户要求优化 DCC 文控“受控浏览”前端体验，并完成真实 Playwright E2E 验证；不顺手修其它场景，不使用 admin、API-only 或 SQL 改状态。
- Rules read: `AGENTS.md`, `docs/frontend-development.md`, `docs/e2e-rules.md`, `docs/login-access.md`, `docs/powershell-encoding.md`, `docs/task-closeout-rules.md`, `docs/local-runtime.md`, `docs/database-rules.md`, `docs/powershell-memory.md`, `docs/experience-index.md`.
- Skills read: `frontend-feature-delivery`, `bdd-tdd-acceptance-planner`, `bug-regression-fix-loop`, `playwright` and their required references.
- Git preflight: `git status --short --branch` showed `int_main...origin/int_main [ahead 1]` with many unrelated modified/untracked files before this task; this task will only edit DCC controlled-browser UX files and its own task artifacts.
- BDD: 受控浏览列表展示当前有效版与发布状态 -> Given 有权限非 admin 用户进入受控浏览, When 按目录/分类/项目代码或文件编号定位目标 ACTIVE 文件, Then 列表行直接显示当前有效版、版本号、目录路径、发布文件状态、盖章文件状态和清晰入口文案。
- BDD: 预览页展示业务可读发布/盖章信息 -> Given 有权限非 admin 用户从受控浏览打开当前有效版预览, When 预览页加载完成, Then 页面显示发布文件、盖章文件、当前有效版来源、最终目录路径和高级 ID 信息。
- BDD: 无权限/无匹配反馈明确 -> Given 低权限非 admin 用户进入同一受控浏览路径或搜索同一文件编号, When 后端只返回可见文件集合且目标文件不可见, Then 页面提示无权限或无匹配当前有效文件，并展示当前筛选条件。
- BDD: 目录分类项目代码定位路径稳定 -> Given 用户通过目录、分类和项目代码筛选定位文件, When 列表刷新, Then 页面显示稳定面包屑和当前筛选条件，避免用户误判目录。
- BDD: 版本入口避免误点 -> Given 列表和预览存在预览、追溯、签核证据入口, When 用户查看操作按钮, Then 文案区分为预览当前有效版、查看版本追溯、查看签核证据。
- BDD: 上传审批前后形成发布闭环 -> Given 上传/提交和审批完成链路, When 用户查看预检或完成结果, Then 页面展示浏览权限范围、发布到哪个受控浏览目录、当前有效版本、发布/盖章文件和可见范围说明。

## RED/GREEN Evidence

- RED: `node tests\e2e\dcc-controlled-browser-ux-optimization-static.spec.js` -> FAIL, expected reason: 文件编号列缺少当前有效版元信息，真实 E2E 在用户列配置/固定列场景下无法确认 `发布文件：已生成`。
- GREEN: `node tests\e2e\dcc-controlled-browser-ux-optimization-static.spec.js` -> PASS after adding current-effective metadata to the file-number visible column.
- GREEN: `node tests\e2e\dcc-browser-version-summary-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\dcc-controlled-browser-viewer-linkage-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\dcc-upload-governance-ux-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS; rebuilt target jar to recover the earlier MES mapper artifact corruption.
- BLOCKED: real Playwright E2E -> BLOCKED before completing authorized/limited-account verification because the rebuilt backend runtime exited during startup with `APPROVAL_ADAPTER_DECLARED_BUT_NOT_REGISTERED: SHOWROOM`.
- Experience consolidation: merged the durable lesson into `docs\frontend-development.md#前端列表跨账号默认列布局统一门禁`: critical list-row acceptance information must not live only in hideable/fixed/scrolled columns and needs a stable visible summary column.

## Blockers

- Runtime blocker at `2026-08-02 21:35 +08:00`: after copying the newly built backend jar into `output\runtime\int_main`, `48081` failed to stay up. Backend log `output\runtime\int_main\logs\yudao-server.log` reports `java.lang.IllegalStateException: APPROVAL_ADAPTER_DECLARED_BUT_NOT_REGISTERED: SHOWROOM`.
- Impact: real Playwright E2E cannot complete the required authorized `wangsiyu` and lower-permission `pengyunfeng` browser paths. The last E2E result JSON records read-only DB target verification PASS, `targetNetworkFailures=[]`, `dccMutationRequests=[]`, and non-target tenant lookup failures caused by the backend runtime outage.
- Decision: did not use admin, API-only validation, SQL mutation, or an old backend jar to bypass this runtime blocker.
- Resolved at `2026-08-03 00:17 +08:00`: rebuilt and loaded `E:\IntRuoyi\output\runtime\int_main\backend\yudao-server-exec-20260803-001741.jar`; backend health returned `UP` on PID `35384`.

## 2026-08-03

- Runtime reload: `mvn -pl yudao-server -am -DskipTests package` -> PASS, generated `yudao-server-exec.jar`; copied to `E:\IntRuoyi\output\runtime\int_main\backend\yudao-server-exec-20260803-001741.jar`, SHA256 `2E841659CA322F843F6603A2AE57F2F9471E5F4BDC99700C6D242F74862E993B`, health `UP`.
- RED: `node tests\e2e\dcc-controlled-browser-ux-optimization-static.spec.js` -> FAIL, expected reason: viewer linkage used directory tree fallback only and `DccControlledFileRespVO` did not expose formal `directoryPath`.
- GREEN: `node tests\e2e\dcc-controlled-browser-ux-optimization-static.spec.js` -> PASS after projecting `directoryPath` from DCC query responses and preferring it in viewer linkage.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `mvn -pl yudao-module-dcc -am -DskipTests compile` -> PASS.
- GREEN: `node tests\e2e\dcc-browser-version-summary-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\dcc-controlled-browser-viewer-linkage-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\dcc-upload-governance-ux-static.spec.js` -> PASS.
- GREEN: real Playwright E2E with non-admin accounts `wangsiyu` and `pengyunfeng` -> PASS; result file `doc\tasks\20260802-dcc-controlled-browser-ux-optimization\dcc-controlled-browser-ux-real-e2e-result.json`.
- E2E result: authorized account saw target ACTIVE `V1.0` file `CODX-DCC-ORIG-20260802101521`, opened viewer preview `PDF` / `stamped-approval-sample.pdf`, saw published/stamped IDs `9198354916366`; lower-permission account saw zero target rows and explicit `无权限或无匹配当前有效文件`.
- Safety evidence: `targetNetworkFailures=[]`, `consoleErrors=[]`, `pageErrors=[]`, `dccMutationRequests=[]`, `targetLinkErrorCount=0`, `targetDccMutationRequestCount=0`.
- Secret scan: task directory credential-pattern scan for password, token, and private-key markers -> no matches.
- Experience consolidation: added DCC viewer E2E wait rule to `docs\e2e-rules.md`; viewer linkage assertions must wait for target directory path and published/stamped IDs, not only for container visibility.
- Closeout note: repository still has unrelated modified/untracked files; no commit or push performed to avoid mixing unrelated task changes.
- Final report refresh: `verification-report.md` updated from stale runtime BLOCKED state to final PASS using `dcc-controlled-browser-ux-real-e2e-result.json`; task status normalized to `ready_for_closeout` without backticks for cleanup tooling.
- Cleanup keep decision: preserve the task-owned real Playwright script and result JSON as audit evidence via `## Cleanup Keep`.
- Cleanup preview/apply: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260802-dcc-controlled-browser-ux-optimization --mode preview` -> ready; keep task records, E2E script and result JSON; delete `<none>`; blocked `<none>`; warnings `<none>`.
- Cleanup apply: same script with `--mode apply` -> applied; deleted `<none>`.
- Remaining closeout blocker: no commit or push because current shared `int_main` worktree contains many unrelated modified/untracked files from other tasks; committing/pushing now risks mixing unrelated work.
- Latest Git status: `git status --short --branch --untracked-files=all` shows `## int_main...origin/int_main` without an ahead marker, plus many non-task modified/untracked files.
- Project experience consolidation: added `docs\e2e-rules.md#DCC 受控浏览当前有效版与权限隔离门禁` and routed matching keywords in `docs\experience-index.md`; no new long-term document created.
