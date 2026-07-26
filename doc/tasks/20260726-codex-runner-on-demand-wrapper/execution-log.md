# Execution Log

## User Intent

- 用户要求做一个按需 Runner 包装层：点击执行时拉起 Runner，Runner 受控调用 Codex CLI，并把测试方法项、目标项和失败原因结构化回写；前后端同步修改、设计、开发、验证。

## BDD Scenarios

- BDD: 按需执行可拉起 Runner -> Given 测试管理存在可执行测试项且本机配置了 Runner 启动器 When 用户点击该测试项执行 Then 后端创建执行任务并触发本机 Runner 注册领取任务 And 前端不再因为没有常驻 Runner 直接失败。
- BDD: 缺少 Runner 启动前置时 fail-fast -> Given 本机未配置 Runner 启动脚本或 Codex CLI When 用户点击执行 Then 后端拒绝启动并返回明确缺失前置 And 前端展示该原因。
- BDD: 运行监控展示步骤进度 -> Given Runner 正在执行测试方法第 N 项 When 监控页签刷新 Then N 之前的方法为绿色、第 N 项为黄色、后续保持待执行。
- BDD: 目标验证失败可查看原因 -> Given Runner 验证目标项失败并回写失败原因 When 用户在监控页签点击红色目标 Then 页面展示对应失败原因。

## RED / GREEN Evidence

- RED: `mvn -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest,CodexTestRunnerServiceImplTest,CodexTestRunnerBootstrapServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, missing `CodexTestRunnerBootstrapService`, `CodexTestRunnerBootstrapServiceImpl`, `CODEX_TEST_RUNNER_STARTER_MISSING`, `CODEX_TEST_RUNNER_START_FAILED`.
- GREEN: `mvn -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest,CodexTestRunnerServiceImplTest,CodexTestRunnerBootstrapServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests, 0 failures.
- RED: `node -e "... git show HEAD:IntRuoyiFronted/src/views/system/codex-test-management/index.vue ..."` -> FAIL, baseline still blocks on Runner status or exposes heartbeat diagnostics.
- GREEN: `node tests\e2e\system-codex-test-management-static.spec.js` -> PASS.
- GREEN: `node --check scripts\codex-test-runner.mjs` -> PASS.
- GREEN: PowerShell parser accepted `IntRuoyiFronted/scripts/start-codex-test-runner.ps1`.
- REGRESSION BLOCKED: `pnpm ts:check` -> FAIL unrelated `src/views/mes/pro/route/RouteEditPage.vue(429,5): Cannot find name 'suppressRouteVersionSubmitAfterSaveOnce'.`

## Command Log

- PASS: skill-read -> `backend-api-delivery`、`frontend-feature-delivery`、`bug-regression-fix-loop`、`behavior-driven-development`
- PASS: project-rules-read -> `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`
- GREEN: experience-preflight -> PASS，命中 `docs/e2e-rules.md#codex-runner-自动测试门禁` 与 `docs/e2e-rules.md#codex-runner-目标测试项存在性门禁`
- PASS: implemented backend on-demand wrapper service using configured `.ps1` starter and real Runner heartbeat/capability confirmation.
- PASS: changed frontend execute actions to rely on backend authoritative startup and switch to monitor tab after execution creation.
- PASS: project-experience-consolidation -> merged on-demand Runner wrapper gate into `docs/e2e-rules.md#codex-runner-自动测试门禁` and `docs/experience-index.md`.
- PASS: removed heartbeat/常驻在线 wording from test management primary status strip.

## Blockers

- Full `pnpm ts:check` is blocked by unrelated `RouteEditPage.vue` symbol error.
- Current local runtime was not restarted; source changes require rebuild/restart before browser verification.
