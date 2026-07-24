# 前端任务：运行控制台真实数据 E2E 覆盖

## 任务目标

- 在前端 worktree 新增 `runtime-control-real-data-all-features.e2e.js`。
- 覆盖 AC-01 到 AC-11 每个功能点的真实用户路径。
- 测试租户执行功能，芋道源码/admin 只读复核。
- 不用静态合同、`node --check` 或 API shortcut 代替真实 E2E。

## 当前状态

- 状态：completed
- 当前阶段：真实数据 E2E 与芋道/admin 复核已通过；已融合进 `int_main` 并在主工作区服务上复验通过；新增 `芋道源码/admin` 专用只读 E2E 复验通过。
- 主控任务目录：后端 `doc/tasks/20260527-runtime-control-real-data-e2e`。
- 覆盖脚本：`tests/e2e/runtime-control-real-data-all-features.e2e.js`；`tests/e2e/runtime-control-yudao-admin-readonly.e2e.js`。
- 放行证据：全量脚本输出 `AC-01 PASS` 到 `AC-11 PASS`、`TEST_TENANT_PASS`、`YUDAO_ADMIN_VERIFY_PASS`；`int_main` 最终事故证据为 `E2E事故-1779871222493`。专用只读脚本输出 `AC-01 ADMIN_READONLY_PASS` 到 `AC-11 ADMIN_READONLY_PASS`、`YUDAO_ADMIN_READONLY_PASS`。

## 预期命令

```powershell
$env:RUNTIME_CONTROL_E2E_BASE_URL='http://127.0.0.1:8098'
$env:RUNTIME_CONTROL_E2E_ACTION_ORIGIN='http://127.0.0.1:48098'
$env:RUNTIME_CONTROL_E2E_TENANT='测试租户'
$env:RUNTIME_CONTROL_E2E_USERNAME='aoteman'
$env:RUNTIME_CONTROL_E2E_PASSWORD='admin123'
$env:RUNTIME_CONTROL_E2E_VERIFY_TENANT='芋道源码'
$env:RUNTIME_CONTROL_E2E_VERIFY_USERNAME='admin'
$env:RUNTIME_CONTROL_E2E_VERIFY_PASSWORD='admin123'
node tests\e2e\runtime-control-real-data-all-features.e2e.js
```

`芋道源码/admin` 专用只读复验：

```powershell
$env:RUNTIME_CONTROL_E2E_BASE_URL='http://127.0.0.1:8098'
node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js
```

## Cleanup Keep

- `doc/tasks/20260527-runtime-control-real-data-e2e/task.md`
- `doc/tasks/20260527-runtime-control-real-data-e2e/execution-log.md`
- `doc/tasks/20260527-runtime-control-real-data-e2e/task-state.json`
- `doc/tasks/20260527-runtime-control-real-data-e2e/verification-report.md`
