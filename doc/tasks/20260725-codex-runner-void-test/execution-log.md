# Execution Log

## User Intent

- 用户要求使用当前电脑的 Codex 环境配置当前系统 Codex。
- 配置完成后运行测试管理中的“作废测试”项。

## Scope Boundary

- Current task records: `doc/tasks/20260725-codex-runner-void-test/`
- Candidate frontend: `IntRuoyiFronted`
- Candidate backend: `IntRuoyiBackend`
- Candidate runner: `IntRuoyiFronted/scripts/codex-test-runner.mjs`
- Existing unrelated dirty/ahead worktree state is outside this task boundary.

## BDD / TDD

- BDD: Runner 本机配置 -> Given 本机已有 Codex CLI 和 IntRuoyi 本机前后端 / When 配置系统测试管理 Runner / Then Runner 能注册、领取测试任务、调用 Codex 执行并把结果回写系统。
- BDD: 作废测试执行 -> Given 测试管理存在“作废测试”测试项 / When 用户在真实测试管理页面触发该项执行 / Then 系统创建只包含该项的执行批次并记录检查点结果。
- BDD: Runner 无登录审计字段 -> Given Runner 协议端点为 `@PermitAll` 且没有登录用户 / When Runner 注册会话或上传 artifact / Then 写库记录必须带明确的系统操作者 `codex-runner`，不能让 `creator/updater` 为 null。
- RED: `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestRunnerServiceImplTest,CodexTestArtifactServiceImplTest" test` -> FAIL, expected reason: 无登录 Runner 写入的 `creator/updater` 为 `null`。
- GREEN: `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestRunnerServiceImplTest,CodexTestArtifactServiceImplTest" test` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestRunnerControllerTest,CodexTestRunnerServiceImplTest,CodexTestArtifactServiceImplTest" test` -> PASS。
- GREEN: `node tests/e2e/system-codex-test-management-static.spec.js` -> PASS。

## Command Log

- 读取 `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/e2e-rules.md`、`docs/local-runtime.md`、`docs/login-access.md`、`docs/worktree-restrictions.md`、`docs/database-rules.md`：通过。
- 当前 Git 状态：`int_main...origin/int_main [ahead 1]`，且存在非本任务未跟踪 `doc/tasks/20260725-edhr-bulk-void-toolbar-cleanup/task.md`；本任务不触碰该并发任务文件。
- 读取 `bug-regression-fix-loop`、`backend-api-delivery`、`playwright`、`project-experience-consolidation` 技能：通过。
- 本机前置：前端 `http://127.0.0.1:8081/login?redirect=/index` 返回 200；后端 `http://127.0.0.1:48081/actuator/health` 为 `UP`；`codex.cmd --version` 为 `codex-cli 0.145.0`；Node `v24.12.0`；`npx`、Playwright 可用。
- IMPLEMENTED: `IntRuoyiFronted/scripts/codex-test-runner.mjs` 要求 `CODEX_TEST_TENANT_ID`，所有 Runner JSON/artifact 请求发送 `tenant-id` 与 `X-Codex-Runner-Token`。
- IMPLEMENTED: `CodexTestRunnerController` 对 Runner 端点读取 `tenant-id` 头，并在服务调用期间绑定管理租户上下文；缺少管理租户 fail fast。
- RED: Runner 注册真实探针曾返回 500，日志显示 `TenantContextHolder 不存在租户编号`，根因是 `@TenantIgnore` Runner 协议未绑定管理租户。
- RED: 修复租户后 Runner 注册真实探针仍返回 500，日志显示 `Column 'creator' cannot be null`，根因是 `@PermitAll` Runner 无登录用户，MyBatis 审计字段填充不到 `creator/updater`。
- IMPLEMENTED: 新增 `CodexTestRunnerAuditSupport`，对 Runner 会话与 artifact 写入显式标记 `creator/updater=codex-runner`，不改全局审计填充逻辑。
- GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS，重新生成 `yudao-server-exec.jar`。
- GREEN: 通过任务专用脚本重启本机后端并注入临时 Runner token；后端健康检查为 `UP`。
- GREEN: Runner 注册探针 POST `/admin-api/system/codex-test-runner/register`，带 `tenant-id=1` 和任务临时 token -> `code=0`，返回 `runnerSessionId=1`。
- BLOCKER: `node doc/tasks/20260725-codex-runner-void-test/run-void-test-from-ui.mjs` 按真实页面搜索“作废测试” -> `searchTotal=0`，未创建 executionId。
- BLOCKER: 同脚本按“作废”搜索 -> `searchTotal=0`。
- BLOCKER: 只读页面会话 API 列表核对 -> 当前测试管理总数 `1`，唯一名称为 `排产工单手动重排 881MO093613/881MO093615`。
- BLOCKER: 只读 DB 核对 `system_codex_test_case` schema 与数据 -> 不存在名称、测试方法或测试数据包含“作废”的测试项；未执行任何写入 SQL。
- EXPERIENCE: 已更新 `docs/e2e-rules.md#codex-runner-目标测试项存在性门禁` 与 `docs/experience-index.md`，沉淀“目标测试项不存在时不得自动创建、改跑其它项或把空领取当成功”的门禁。
