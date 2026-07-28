# Bug Regression Evidence

## Bug Summary

- Symptom: 登录后打开 `系统管理 > 测试管理 > 运行监控`，页面提示 `请求地址不存在:admin-api/system/codex-test-execution/monitor`。
- Expected: 监控接口应返回业务码 `0` 和运行任务数组；无运行任务时返回空数组，不应返回路由不存在。

## Reproduction

- RED: 使用本机默认登录来源获取 token 后请求 `GET http://127.0.0.1:48081/admin-api/system/codex-test-execution/monitor` -> HTTP `200`，业务码 `404`，消息 `请求地址不存在:admin-api/system/codex-test-execution/monitor`。

## Root Cause

- 旧的 `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar` 仍在 48081 运行，未包含新增的 `CodexTestExecutionController#getExecutionMonitor` 路由。
- 本地数据库 `system_codex_test_execution_case` 也缺少运行监控进度字段，加载新路由后会继续存在 schema 前置风险。

## Fix

- 在隔离 worktree `D:\IntRuoyiWorktree\codex-test-run-monitor-runtime` 中仅迁入 Codex 测试管理后端相关变更，执行 `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package` 生成新的 `yudao-server-exec.jar`。
- 执行幂等迁移 `20260726_system_codex_test_run_monitor_progress.sql`，补齐 `progress_phase`、`current_method_sort`、`current_checkpoint_sort`、`progress_message`。
- 停止已确认归属的旧 PID `34948`，用隔离构建的新 JAR 启动 48081 后端，当前 PID 为 `59524`。

## Verification

- GREEN: `jar tf yudao-module-system-2026.04-SNAPSHOT.jar` -> 包含 `CodexTestRunnerProgressReqVO.class`、`CodexTestExecutionController.class`、`CodexTestRunnerController.class`。
- GREEN: Docker MySQL `information_schema.COLUMNS` -> 四个运行监控字段均存在。
- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `UP`。
- GREEN: 登录态请求 `GET /admin-api/system/codex-test-execution/monitor` -> HTTP `200`，业务码 `0`，返回数组，当前运行任务数 `0`。
- GREEN: Playwright 真实页面路径登录后进入 `系统管理 > 测试管理` 并点击 `运行监控` -> 监控接口业务码 `0`，页面不再显示 `请求地址不存在`，摘要显示 `当前正在运行 0 个测试任务`。

## Risk And Scope

- Runtime fix only: 当前 48081 后端从隔离 worktree JAR 运行，未把主工作区广泛脏改动打入运行包。
- Commit/push closeout remains blocked by shared workspace unrelated dirty/ahead changes.
