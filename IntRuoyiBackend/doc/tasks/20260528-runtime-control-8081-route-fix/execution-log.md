# 执行日志：修复 8081 运行控制台访问目标

- BDD: 8081 访问运行控制台使用当前后端 -> Given 本地 `8081` 被其他 worktree 前端占用并连接旧后端 / When 停止旧 `8081` 前端并启动主前端指向 `48081` / Then `8081` 运行控制台只读 E2E 通过，不再出现 `backupPointsRoot 目录不存在或不可读`。
- BDD: 切换入口不得停止后端或修改业务数据 -> Given `48081` 当前后端已健康 / When 修复 `8081` 前端入口 / Then 不停止 `48081`，不修改数据库，仅调整本地前端进程。

- GREEN: preflight -> `48081` health returned `{"status":"UP"}`。
- RED: user report -> `http://127.0.0.1:8081` 访问运行控制台提示 `backupPointsRoot 目录不存在或不可读：\mnt\nas\备份`，expected reason: current `8081` frontend is connected to an old backend/worktree route rather than current `48081`。
- GREEN: stopped old `8081` listener -> PASS，listener PID belonged to `worktrees\20260527-edhr-prod-doc-code-subagent-review\yudao-ui-admin-vue3` Vite process。
- GREEN: started main frontend on `8081` -> PASS，explicit env `VITE_BASE_URL=http://127.0.0.1:48081` and `VITE_PROXY_TARGET=http://127.0.0.1:48081`，login route HTTP 200。
- GREEN: showroom `5188` check -> PASS，HTTP 200 after `8081` route fix。
- GREEN: backend health after frontend switch -> PASS，`{"status":"UP"}`。
- GREEN: `RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8081 RUNTIME_CONTROL_E2E_VERIFY_TENANT=芋道源码 RUNTIME_CONTROL_E2E_VERIFY_USERNAME=admin RUNTIME_CONTROL_E2E_VERIFY_PASSWORD=admin123 node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> PASS，AC-01 alerts, AC-02 owner matrix, AC-03 wizard, AC-04 rollback candidates, AC-05 restore candidates, AC-06 inspection entry, AC-07 business health, AC-08 probes, AC-09 capacity, AC-10 backup points, AC-11 incidents all passed；`YUDAO_ADMIN_READONLY_PASS`；no runtime-control non-GET write request observed。
