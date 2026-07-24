# 任务：芋道源码 admin 验证运行控制台

## 任务目标

- 使用真实前端路径访问本地运行控制台。
- 使用租户 `芋道源码`、用户 `admin`、密码 `admin123` 登录。
- 确认运行控制台页面可见，主要只读接口返回正常，不触发写操作。

## BDD 场景

- BDD: 芋道源码 admin 访问运行控制台 -> Given 本地后端 `48081` 已启动且前端入口可访问 / When 使用 `芋道源码/admin/admin123` 登录并进入 `/infra/monitors/runtime-control` / Then 页面显示 `运行控制台`，主要只读接口无 4xx/5xx 或业务错误。
- BDD: 只读验证不得触发运行控制写操作 -> Given 本次只做访问验证 / When Playwright 打开运行控制台 / Then 不得调用 `/infra/runtime-control` 下的非 GET 写接口。

## 里程碑

- [x] M1：创建任务记录并确认登录凭据来源。
- [x] M2：确认本地前后端入口状态。
- [x] M3：执行 Playwright 真实路径验证。
- [x] M4：记录验证结果并完成收尾。

## 预期验证

- GREEN: `RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> PASS。
- GREEN: `http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`。

## 当前状态

completed

## 当前发现

- 本地后端 `48081` 当前健康检查为 `UP`。
- 本地 `8081` 当前可访问，但运行进程来自 `worktrees\20260527-edhr-prod-doc-code-subagent-review\yudao-ui-admin-vue3`，不是主前端目录。
- 主前端目录 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 已在 `8082` 启动，显式指向当前后端 `48081`。
- 首次基于 `8081` 的 E2E 失败于 `rollback-candidates business code 500`，原因是该端口并非主前端且 `.env.local` 默认目标为 `48098`，不能作为本次当前后端验证依据。
- 基于主前端 `8082` 的 E2E 已通过，覆盖运行控制台 AC-01 至 AC-11。

## 验证结果

- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS，`{"status":"UP"}`。
- RED: `RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> FAIL，expected reason: `8081` 当前为其他 worktree 前端，不是主前端与当前 `48081` 后端组合。
- GREEN: direct backend readonly API check with `芋道源码/admin/admin123` -> PASS，`rollback-candidates`、`restore-candidates`、`backup-points` 均返回 `code=0`。
- GREEN: main frontend on `8082` started with `VITE_BASE_URL=http://127.0.0.1:48081` and `VITE_PROXY_TARGET=http://127.0.0.1:48081` -> PASS。
- GREEN: `RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8082 node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> PASS，AC-01 至 AC-11 全部通过，且 `YUDAO_ADMIN_READONLY_PASS`。

## Cleanup Keep

- doc/tasks/20260528-runtime-control-yudao-admin-verify/verification-report.md
