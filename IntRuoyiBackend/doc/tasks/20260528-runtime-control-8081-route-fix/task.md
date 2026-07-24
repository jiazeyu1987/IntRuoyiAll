# 任务：修复 8081 运行控制台访问目标

## 任务目标

- 将本地 `8081` 恢复为主前端入口。
- 确保 `8081` 访问运行控制台时连接当前已修复的后端 `48081`。
- 使用 `芋道源码 / admin / admin123` 验证运行控制台不再访问旧 `backupPointsRoot` 逻辑。

## BDD 场景

- BDD: 8081 访问运行控制台使用当前后端 -> Given 本地 `8081` 被其他 worktree 前端占用并连接旧后端 / When 停止旧 `8081` 前端并启动主前端指向 `48081` / Then `8081` 运行控制台只读 E2E 通过，不再出现 `backupPointsRoot 目录不存在或不可读`。
- BDD: 切换入口不得停止后端或修改业务数据 -> Given `48081` 当前后端已健康 / When 修复 `8081` 前端入口 / Then 不停止 `48081`，不修改数据库，仅调整本地前端进程。

## 里程碑

- [x] M1：确认 `8081` 当前连接错误 worktree / 旧后端。
- [x] M2：停止占用 `8081` 的旧前端进程。
- [x] M3：启动主前端到 `8081` 并指向 `48081`。
- [x] M4：用 `芋道源码/admin` 执行运行控制台只读验证。
- [x] M5：记录结果并收尾。

## 预期验证

- GREEN: `http://127.0.0.1:8081/login?redirect=/infra/monitors/runtime-control` 返回 HTTP 200。
- GREEN: `RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> PASS。

## 当前状态

completed

## 当前发现

- `8081` 由 `worktrees\20260527-edhr-prod-doc-code-subagent-review\yudao-ui-admin-vue3` 前端进程占用。
- 当前 `48081` 后端健康检查为 `UP`。
- `48098` 也有一个后端进程存活，用户在 `8081` 看到旧错误与该旧链路一致。
- 已停止 `8081` 上的旧 worktree 前端监听进程，并启动主前端 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 到 `8081`。
- 主前端 `8081` 以 `VITE_BASE_URL=http://127.0.0.1:48081`、`VITE_PROXY_TARGET=http://127.0.0.1:48081` 启动。
- 展厅 `5188` 端口仍返回 HTTP 200；本次未停止后端 `48081`。

## 验证结果

- GREEN: stop old `8081` frontend listener -> PASS，`PORT_8081_FREE`。
- GREEN: start main frontend on `8081` with backend target `48081` -> PASS，login route returned HTTP 200。
- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS，`{"status":"UP"}`。
- GREEN: `RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8081 RUNTIME_CONTROL_E2E_VERIFY_TENANT=芋道源码 RUNTIME_CONTROL_E2E_VERIFY_USERNAME=admin RUNTIME_CONTROL_E2E_VERIFY_PASSWORD=admin123 node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> PASS，AC-01 至 AC-11 全部通过，`YUDAO_ADMIN_READONLY_PASS`。
