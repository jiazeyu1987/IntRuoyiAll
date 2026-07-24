# 执行日志：eDHR 发布前 E2E 覆盖矩阵门禁

BDD: 功能点覆盖矩阵完整 -> Given eDHR 已有执行入口、执行详情、审批、归档、执行列表、追踪、签名、字段审计、主数据追溯、权限矩阵和归档健康功能 / When 发布前运行覆盖门禁 / Then 每个功能点必须绑定至少一个真实 Playwright E2E 脚本、对应 package 命令、关键路由/API token 和任务证据。

BDD: 新增 eDHR 页面无 E2E 时阻塞 -> Given `src/views/mes/pro/edhr` 或 `src/api/mes/pro/edhr` 增加新的功能面 / When 覆盖矩阵没有登记对应真实 E2E / Then 门禁退出非零并报告未覆盖文件或 token，不能通过静态合同替代。

BDD: 发布前统一 check gate -> Given 开发者运行 `pnpm e2e:edhr:release:check` / When 所有矩阵项的脚本、package scripts、BDD/FAIL/PASS 证据、no mock/fallback 约束和语法检查都满足 / Then check gate 通过并输出覆盖摘要。

BDD: 发布前统一 real gate -> Given 测试租户真实账号、真实数据和所有 E2E 所需环境变量已准备 / When 开发者运行 `pnpm e2e:edhr:release` / Then gate 按矩阵顺序运行每个真实 E2E package script，任一失败立即退出非零并保留失败脚本名。

BDD: 缺少真实前置即阻塞 -> Given 真实 E2E 所需账号、密码、fresh 工单/任务或受保护存储前置缺失 / When 运行统一 real gate / Then 子脚本必须 fail fast，统一 gate 不得吞错、跳过或降级为 check-only。

RED: `node --test scripts/edhr-release-e2e-coverage-contract.test.mjs` -> EXPECTED FAIL, before implementation the release E2E coverage contract and unified gate do not exist.

RED: `node --test scripts/edhr-release-e2e-coverage-contract.test.mjs` -> FAIL, `Could not find 'scripts\edhr-release-e2e-coverage-contract.test.mjs'`; confirms the unified release E2E coverage gate is absent before implementation.

RED: `node --test scripts\edhr-release-e2e-coverage-contract.test.mjs` -> FAIL, `Could not find 'scripts\edhr-release-e2e-coverage-contract.test.mjs'`; rerun before implementation in this worker pass.

GREEN: `node --check scripts\edhr-release-e2e-coverage-gate.mjs` -> PASS.

GREEN: `node --test scripts\edhr-release-e2e-coverage-contract.test.mjs` -> PASS, 7 tests; covered package scripts, matrix feature set, source/API/route/E2E/taskEvidence bindings, no mock interception, no hidden skip, check-only behavior, run-real order/fail-fast behavior, and plaintext default-password protection.

GREEN: `node scripts\edhr-release-e2e-coverage-gate.mjs --check` -> PASS, `PASS: eDHR release E2E coverage check completed; features=10, checkScripts=7, syntaxFiles=7`.

GREEN: `pnpm e2e:edhr:release:check` -> PASS, package script runs `node scripts/edhr-release-e2e-coverage-gate.mjs --check` and reports features=10, checkScripts=7, syntaxFiles=7.

GREEN: `node --check tests\e2e\edhr-approval-tracking-real-flow.e2e.js` -> PASS.

GREEN: `node --check tests\e2e\edhr-domain-trace-real-flow.e2e.js` -> PASS.

GREEN: `node --check tests\e2e\edhr-field-audit-real-flow.e2e.js` -> PASS.

GREEN: `node --check tests\e2e\edhr-execution-list-real-flow.e2e.js` -> PASS.

GREEN: `node --check tests\e2e\edhr-tracking-signature-real-flow.e2e.js` -> PASS.

GREEN: `node --check tests\e2e\edhr-permission-tenant-matrix.e2e.js` -> PASS.

GREEN: `node --check tests\e2e\runtime-control-edhr-archive-health.e2e.js` -> PASS.

GREEN: `node scripts\edhr-release-e2e-coverage-gate.mjs` -> PASS, default no-argument mode equals `--check`.

GREEN: `git diff --check` -> PASS; Git emitted only the Windows LF/CRLF normalization warning for `package.json`.

REAL: `pnpm e2e:edhr:release` -> NOT RUN / BLOCKED, because full real release E2E requires complete real account passwords, electronic signature passwords, and fresh eDHR work order/task contexts. This worker did not request, print, write, or commit any password and does not claim check-only evidence as full real E2E PASS.

REVIEW_FAIL: 第一轮主审与独立 reviewer -> FAIL, `scripts/edhr-release-e2e-coverage-gate.mjs` 将源码 token 与 E2E token 合并检查，导致 `/approval-detail`、`approval-done-page`、归档版本分页等源码/静态 token 可能被误判为真实 Playwright 覆盖。

RED: source-only token regression -> FAIL expected, 构造矩阵中源码含 `/mes/pro/batch-record-execution/approval-detail` 但 E2E 脚本不含该 token，`validateCoverageContract()` 必须返回失败。

GREEN: source-only token regression -> PASS, `node --test scripts\edhr-release-e2e-coverage-contract.test.mjs` 覆盖源码 token 不得替代真实 E2E token。

GREEN: real E2E token source -> PASS, release matrix `e2eTokens` 均出现在剥离注释后的 E2E 脚本源码中，不再从 `src/` 或 task evidence 计入。

GREEN: approval detail done tab archive versions -> PASS, `node --check tests\e2e\edhr-approval-tracking-real-flow.e2e.js` 覆盖审批详情真实 API、我已审批分页和归档版本分页脚本语法。

GREEN: field audit and domain trace cross-page -> PASS, `node --check tests\e2e\edhr-field-audit-real-flow.e2e.js` 与 `node --check tests\e2e\edhr-domain-trace-real-flow.e2e.js` 覆盖定位执行记录与执行详情跨页脚本语法。

GREEN: `node --test scripts\edhr-release-e2e-coverage-contract.test.mjs` -> PASS, 8 tests.

GREEN: `node scripts\edhr-release-e2e-coverage-gate.mjs --check` -> PASS, `PASS: eDHR release E2E coverage check completed; features=10, checkScripts=7, syntaxFiles=7`.

GREEN: `pnpm e2e:edhr:release:check` -> PASS, package script runs `node scripts/edhr-release-e2e-coverage-gate.mjs --check` and reports features=10, checkScripts=7, syntaxFiles=7.

GREEN: `node scripts\edhr-release-e2e-coverage-gate.mjs` -> PASS, default no-argument mode equals `--check`.

GREEN: default password / hidden skip / mock interception scans -> PASS, no matches in release E2E files.

GREEN: `git diff --check` -> PASS; Git emitted only Windows LF/CRLF normalization warnings for touched files.

BLOCKED: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260530-edhr-release-e2e-coverage-gate --mode preview` -> BLOCKED, cleanup preview kept task docs and found no delete candidates, but apply is unsafe because this linked worktree cannot fast-forward merge into `int_main` and still has current task code changes pending before commit. No cleanup apply or worktree removal was performed.
