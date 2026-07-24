# 执行日志：芋道源码 admin 验证运行控制台

- BDD: 芋道源码 admin 访问运行控制台 -> Given 本地后端 `48081` 已启动且前端入口可访问 / When 使用 `芋道源码/admin/admin123` 登录并进入 `/infra/monitors/runtime-control` / Then 页面显示 `运行控制台`，主要只读接口无 4xx/5xx 或业务错误。
- BDD: 只读验证不得触发运行控制写操作 -> Given 本次只做访问验证 / When Playwright 打开运行控制台 / Then 不得调用 `/infra/runtime-control` 下的非 GET 写接口。

- GREEN: task document created -> PASS。
- GREEN: credential source checked -> PASS，`docs/login-access.md` 记录正式环境/主租户凭据为 `芋道源码 / admin / admin123`。
- GREEN: backend health -> `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` returned `{"status":"UP"}`。
- RED: `RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> FAIL，expected reason: current `8081` process is not the main frontend and the runtime-control rollback candidate request returned business `500`。
- GREEN: direct backend readonly API check with `芋道源码/admin/admin123` -> PASS，tenant id `1`，`rollback-candidates`、`restore-candidates`、`backup-points` all returned `code=0` with one NAS backup-derived row each。
- GREEN: main frontend start on `8082` -> PASS，`pnpm dev -- --host 127.0.0.1 --port 8082 --strictPort` with `VITE_BASE_URL=http://127.0.0.1:48081` and `VITE_PROXY_TARGET=http://127.0.0.1:48081` served login page HTTP 200。
- GREEN: `RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8082 RUNTIME_CONTROL_E2E_VERIFY_TENANT=芋道源码 RUNTIME_CONTROL_E2E_VERIFY_USERNAME=admin RUNTIME_CONTROL_E2E_VERIFY_PASSWORD=admin123 node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> PASS，AC-01 alerts, AC-02 owner matrix, AC-03 wizard, AC-04 rollback candidates, AC-05 restore candidates, AC-06 inspection entry, AC-07 business health, AC-08 probes, AC-09 capacity, AC-10 backup points, AC-11 incidents all passed；`YUDAO_ADMIN_READONLY_PASS`；no runtime-control non-GET write request observed。
