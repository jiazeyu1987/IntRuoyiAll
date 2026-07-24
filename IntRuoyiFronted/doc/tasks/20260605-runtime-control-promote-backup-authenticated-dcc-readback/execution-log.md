# 执行日志：补齐备份服接管 DCC 认证读回校验

## BDD

- BDD: DCC 读回必须使用已登录认证头 -> Given 运行控制台已登录测试租户 / When `promote-backup` E2E 校验 DCC 读回 URL / Then 请求必须携带 `Authorization`、`tenant-id`，并在有 `visitTenantId` 时携带 `visit-tenant-id`。
- BDD: 缺少浏览器登录态时必须阻塞 -> Given 浏览器缓存中没有 `ACCESS_TOKEN` 或 `tenantId` / When E2E 尝试校验 DCC 读回 / Then 脚本必须 fail-fast，不得把匿名请求结果当成成功证据。
- BDD: DCC 读回必须证明是真实文件而不是失败 envelope -> Given DCC 读回请求已发送 / When 返回响应 / Then 响应必须是 2xx/3xx、字节数达到阈值，且 `content-type` 不能是 `application/json`。

## TDD Evidence

- CHECK: 上一前端任务 -> PASS，`doc/tasks/20260605-runtime-control-promote-backup-e2e-gate/task.md` 标记为 `completed`。
- RED: `node tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js` -> FAIL，缺少从浏览器缓存读取 `ACCESS_TOKEN`/`tenantId` 的 DCC 认证读回逻辑。
- GREEN: `node tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js` -> PASS，`promote-backup` real-flow 已要求从浏览器缓存读取 `ACCESS_TOKEN`、`tenantId`、`visitTenantId` 并附带认证请求头执行 DCC 读回。
- GREEN: `node --check tests/e2e/runtime-control-promote-backup-real-flow.e2e.js` -> PASS。
- GREEN: `node tests/e2e/runtime-control-foolproof-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- tests/e2e/runtime-control-promote-backup-real-flow.e2e.js tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js tests/e2e/runtime-control-foolproof-static.spec.js doc/tasks/20260605-runtime-control-promote-backup-authenticated-dcc-readback` -> PASS，仅 Git 行尾提示。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-runtime-control-promote-backup-authenticated-dcc-readback --mode preview` -> READY，keep task/execution-log，delete `<none>`，blocked `<none>`，warnings `<none>`。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-runtime-control-promote-backup-authenticated-dcc-readback --mode apply` -> APPLIED，delete `<none>`，blocked `<none>`，warnings `<none>`。

- RED: 
ode tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js -> FAIL，缺少从浏览器缓存读取 ACCESS_TOKEN/	enantId 的 DCC 认证读回逻辑。
