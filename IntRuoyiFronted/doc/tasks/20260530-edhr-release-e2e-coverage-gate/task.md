# 20260530-edhr-release-e2e-coverage-gate

## Task Goal

为 eDHR 前端建立发布前 E2E 覆盖矩阵与统一 release gate，确保每个 eDHR 用户可见功能点、路由入口、关键 API 与真实 Playwright E2E 脚本有明确绑定。新增或调整 eDHR 页面/API 时，如果没有对应真实 E2E 覆盖，门禁必须 fail fast，不能靠静态合同、手工说明或间接假设放行。

本任务不修改业务页面行为，不创建测试专用 UI，不引入 mock、fallback、silent skip 或默认成功。统一 release gate 可以在缺少真实账号、真实测试数据或密码环境时失败，但必须说明缺失前置和影响；不得把 check-only 结果伪装成真实 E2E 全量通过。

## Scope

- `package.json`
- `scripts/edhr-release-e2e-coverage-contract.test.mjs`
- `scripts/edhr-release-e2e-coverage-gate.mjs`
- `tests/e2e/edhr-approval-tracking-real-flow.e2e.js`
- `tests/e2e/edhr-domain-trace-real-flow.e2e.js`
- `tests/e2e/edhr-field-audit-real-flow.e2e.js`
- `doc/tasks/20260530-edhr-release-e2e-coverage-gate/`

除非覆盖矩阵发现真实业务页面/API 没有可绑定 E2E，本任务不修改 `src/` 或既有 E2E 业务脚本；若必须修改，先补充 RED 证据并更新本文档。

## BDD Scenarios

- BDD: 功能点覆盖矩阵完整 -> Given eDHR 已有执行入口、执行详情、审批、归档、执行列表、追踪、签名、字段审计、主数据追溯、权限矩阵和归档健康功能 / When 发布前运行覆盖门禁 / Then 每个功能点必须绑定至少一个真实 Playwright E2E 脚本、对应 package 命令、关键路由/API token 和任务证据。
- BDD: 新增 eDHR 页面无 E2E 时阻塞 -> Given `src/views/mes/pro/edhr` 或 `src/api/mes/pro/edhr` 增加新的功能面 / When 覆盖矩阵没有登记对应真实 E2E / Then 门禁退出非零并报告未覆盖文件或 token，不能通过静态合同替代。
- BDD: 发布前统一 check gate -> Given 开发者运行 `pnpm e2e:edhr:release:check` / When 所有矩阵项的脚本、package scripts、BDD/FAIL/PASS 证据、no mock/fallback 约束和语法检查都满足 / Then check gate 通过并输出覆盖摘要。
- BDD: 发布前统一 real gate -> Given 测试租户真实账号、真实数据和所有 E2E 所需环境变量已准备 / When 开发者运行 `pnpm e2e:edhr:release` / Then gate 按矩阵顺序运行每个真实 E2E package script，任一失败立即退出非零并保留失败脚本名。
- BDD: 缺少真实前置即阻塞 -> Given 真实 E2E 所需账号、密码、fresh 工单/任务或受保护存储前置缺失 / When 运行统一 real gate / Then 子脚本必须 fail fast，统一 gate 不得吞错、跳过或降级为 check-only。
- BDD: 源码 token 不得误放行 -> Given eDHR 源码包含某个路由或 API token 但真实 E2E 脚本未触达 / When 运行 release coverage contract / Then `validateCoverageContract()` 必须失败并点名该 token 必须来自 E2E 脚本本身。
- BDD: 真实跨页动作可覆盖 -> Given 用户从审批、归档、字段审计或主数据追溯页面操作 / When 用户点击查看、我已审批、查看版本、定位执行记录或执行详情 / Then Playwright 脚本必须等待真实 API 或真实路由变化并断言同一业务记录。

## Strict TDD Plan

1. RED: 新增 `scripts/edhr-release-e2e-coverage-contract.test.mjs` 之前先运行该命令，预期失败为合同文件不存在。
2. RED: 子 agent 第一版 gate 将源码和 E2E token 合并检查，预期主审失败，原因是源码 token 可误放行真实覆盖。
3. GREEN: 新增 coverage gate 与 package scripts，使 `pnpm e2e:edhr:release:check` 可以验证覆盖矩阵、现有 E2E 语法、no mock/fallback 和 fail-fast 约束。
4. GREEN: 拆分 `apiTokens` 与 `e2eTokens`，`e2eTokens` 仅允许来自剥离注释后的 E2E 脚本本身；补真实用户路径覆盖审批详情、我已审批、归档版本、字段审计定位执行记录和主数据追溯执行详情。
5. BLOCKED/REAL: 不在缺少 fresh real E2E 环境变量时伪造 `pnpm e2e:edhr:release` 全量真实通过；如运行 full gate，必须接受其真实 fail-fast 结果。
6. REGRESSION: 运行现有 eDHR 静态合同子集、所有 eDHR E2E 语法检查、敏感密码扫描、`git diff --check`，并由独立 reviewer 子 agent 复审。

## Expected Verification

- `node --test scripts/edhr-release-e2e-coverage-contract.test.mjs`
- `node scripts/edhr-release-e2e-coverage-gate.mjs --check`
- `pnpm e2e:edhr:release:check`
- `node --check scripts/edhr-release-e2e-coverage-gate.mjs`
- `node --check tests/e2e/edhr-approval-tracking-real-flow.e2e.js`
- `node --check tests/e2e/edhr-domain-trace-real-flow.e2e.js`
- `node --check tests/e2e/edhr-field-audit-real-flow.e2e.js`
- `node --check tests/e2e/edhr-execution-list-real-flow.e2e.js`
- `node --check tests/e2e/edhr-tracking-signature-real-flow.e2e.js`
- `node --check tests/e2e/edhr-permission-tenant-matrix.e2e.js`
- `node --check tests/e2e/runtime-control-edhr-archive-health.e2e.js`
- `git diff --check`

## Current Status

- status: completed
- previous frontend task check: `20260529-edhr-tracking-signature-real-e2e-gate` is marked completed and committed.
- current gap evidence:
  - `package.json` has individual eDHR E2E scripts but no unified `e2e:edhr:release:check` or `e2e:edhr:release` gate.
  - Current route/API coverage is spread across multiple task documents and scripts, making it easy for a future eDHR page/API to miss real E2E without a machine gate.
  - This task turns the coverage expectation into a reusable release gate and does not claim full real E2E PASS unless every real E2E script actually runs successfully.
- review status:
  - REVIEW_FAIL: main reviewer rejected the first pass because the gate merged `sourceFiles` and `e2eFile` when checking route/API tokens, allowing source-only tokens such as approval detail and archive page contracts to count as real E2E coverage.
  - Fix applied: matrix now separates source/API `apiTokens` from real `e2eTokens`; `e2eTokens` are validated only against the E2E script itself, with JavaScript comments stripped from the E2E source before token matching.
  - Main reviewer re-check passed for the check gate and static contract; full real release E2E remains an explicit BLOCKED prerequisite, not a claimed PASS.
- completed work:
  - Added `scripts/edhr-release-e2e-coverage-gate.mjs` with a 10-item explicit eDHR release coverage matrix.
  - Added `scripts/edhr-release-e2e-coverage-contract.test.mjs` covering package scripts, matrix completeness, source/API/route/E2E/taskEvidence bindings, source-only token regression, check-only behavior, run-real order/fail-fast behavior, and plaintext default-password protection.
  - Added package scripts `e2e:edhr:release:check` and `e2e:edhr:release`.
  - Verified default no-arg gate mode equals `--check`.
  - Added a real approval detail E2E assertion inside `edhr-approval-tracking-real-flow.e2e.js`: the approval user opens the approval detail route from the real approval list, waits for `/mes/pro/batch-record-execution/approval-detail`, validates response fields, and asserts the page shows the same execution code.
  - Added real E2E user paths for done approval tab, archive version dialog, field-audit execution positioning, and domain-trace execution detail navigation.

## Final Verification Result

- RED: `node --test scripts\edhr-release-e2e-coverage-contract.test.mjs` -> FAIL before implementation, expected missing contract file.
- GREEN: `node --check scripts\edhr-release-e2e-coverage-gate.mjs` -> PASS.
- REVIEW_FAIL: 第一轮 worker gate 把源码 token 与 E2E token 合并，独立 reviewer 判定不能证明真实 Playwright 覆盖。
- GREEN: `node --test scripts\edhr-release-e2e-coverage-contract.test.mjs` -> PASS, 8 tests.
- GREEN: `node scripts\edhr-release-e2e-coverage-gate.mjs --check` -> PASS, features=10, checkScripts=7, syntaxFiles=7.
- GREEN: `pnpm e2e:edhr:release:check` -> PASS, features=10, checkScripts=7, syntaxFiles=7.
- GREEN: individual `node --check` for all seven existing eDHR real E2E files -> PASS.
- GREEN: `node scripts\edhr-release-e2e-coverage-gate.mjs` -> PASS, default mode equals `--check`.
- GREEN: source-only API regression -> PASS; `/approval-detail` present only in source cannot satisfy `e2eTokens`.
- GREEN: all matrix `e2eTokens` present in comment-stripped E2E script source -> PASS.
- GREEN: sensitive/default password and hidden skip scan on release E2E files -> PASS, no matches.
- GREEN: `git diff --check` -> PASS; Git reported only Windows LF/CRLF normalization warnings for touched files.
- GREEN: `doc/tasks/20260530-edhr-release-e2e-coverage-gate/verification-report.md` -> PASS, records requirement checklist, command evidence and the full real E2E blocker.
- BLOCKED: `task-closeout-cleanup --mode preview` -> BLOCKED for apply, because current linked worktree cannot fast-forward merge into `int_main` and current task code changes were still pending before commit; preview found no delete candidates and no cleanup apply/worktree removal was performed.
- REAL: `pnpm e2e:edhr:release` -> NOT RUN / BLOCKED in this worker pass because full real release E2E requires complete real account passwords, signature passwords, and fresh eDHR work order/task contexts. No password was requested, written, printed, or committed; check-only evidence is not claimed as full real E2E PASS.

## Cleanup Keep

- `doc/tasks/20260530-edhr-release-e2e-coverage-gate/task.md`
- `doc/tasks/20260530-edhr-release-e2e-coverage-gate/execution-log.md`
- `doc/tasks/20260530-edhr-release-e2e-coverage-gate/verification-report.md`

## Reviewer Gate

主 reviewer 只在以下条件同时满足时放行：

- 覆盖矩阵列出当前所有 eDHR 用户可见功能点，并绑定真实 Playwright E2E package script。
- 静态合同证明没有新增未覆盖 eDHR 页面/API，且统一 gate 不使用 mock、fallback、silent skip 或默认成功。
- `release:check` 通过；`release` full gate 的真实运行状态不得被伪装，如缺前置必须记录 BLOCKED/FAIL。
- 独立 reviewer 子 agent 判定 logic/usability/UI 均无阻塞问题。
- 当前任务直接相关文件通过验证并单独提交。
