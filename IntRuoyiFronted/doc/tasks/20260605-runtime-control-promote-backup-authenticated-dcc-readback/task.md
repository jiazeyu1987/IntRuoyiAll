# 任务：补齐备份服接管 DCC 认证读回校验

## 任务目标

在 `tests/e2e/runtime-control-promote-backup-real-flow.e2e.js` 中，把备份服接管后的 DCC 读回验证改为基于已登录浏览器会话的真实认证请求。脚本不得依赖匿名可访问 URL；必须从浏览器缓存读取 `ACCESS_TOKEN`、`tenantId`、`visitTenantId`，附带 `Authorization`、`tenant-id`、`visit-tenant-id` 请求头访问 DCC 读回 URL，并把“有登录态认证头 + 非 JSON 文件响应”一起作为接管证据。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260605-runtime-control-promote-backup-e2e-gate/task.md`
- 状态：`completed`
- 处理：上一任务已完成并提交 `4b99727f1`；本任务只继续强化 `promote-backup` 真实 E2E 的证据质量，不执行远程接管。

## BDD 场景

- BDD: DCC 读回必须使用已登录认证头 -> Given 运行控制台已登录测试租户 / When `promote-backup` E2E 校验 DCC 读回 URL / Then 请求必须携带 `Authorization`、`tenant-id`，并在有 `visitTenantId` 时携带 `visit-tenant-id`。
- BDD: 缺少浏览器登录态时必须阻塞 -> Given 浏览器缓存中没有 `ACCESS_TOKEN` 或 `tenantId` / When E2E 尝试校验 DCC 读回 / Then 脚本必须 fail-fast，不得把匿名请求结果当成成功证据。
- BDD: DCC 读回必须证明是真实文件而不是失败 envelope -> Given DCC 读回请求已发送 / When 返回响应 / Then 响应必须是 2xx/3xx、字节数达到阈值，且 `content-type` 不能是 `application/json`。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务 completed。
- [x] M2：新增 RED 静态合同，要求 `promote-backup` real-flow 使用认证读回。
- [x] M3：实现浏览器会话认证读回 helper 和验证逻辑。
- [x] M4：运行静态合同、语法检查和 fail-fast 验证。
- [x] M5：更新任务证据、cleanup 预览并提交。

## Expected Verification

- RED/GREEN：`node tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js`
- GREEN：`node --check tests/e2e/runtime-control-promote-backup-real-flow.e2e.js`
- GREEN：`node tests/e2e/runtime-control-foolproof-static.spec.js`
- GREEN：`pnpm ts:check`
- GREEN：`git diff --check -- tests/e2e/runtime-control-promote-backup-real-flow.e2e.js tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js tests/e2e/runtime-control-foolproof-static.spec.js doc/tasks/20260605-runtime-control-promote-backup-authenticated-dcc-readback`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺认证缓存、缺 tenant 上下文或返回 JSON 失败 envelope 时直接阻塞。
- `是否从根因和长期维护角度解决`：是。把 DCC 读回改成和真实前端登录态一致的请求头模型，而不是临时 public URL 假设。
- `是否存在临时补丁或绕过`：否。不绕过登录、不改成匿名访问、不写死 token。

## 当前状态

completed

## Current Status

completed

## 完成内容

- 收紧 `tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js`，要求 `promote-backup` real-flow 明确读取浏览器缓存中的 `ACCESS_TOKEN`、`tenantId`、`visitTenantId`，并用认证头做 DCC 读回。
- 将 `tests/e2e/runtime-control-promote-backup-real-flow.e2e.js` 的 DCC 校验从匿名 `fetch` 改为 `page.evaluate` 中的登录态 `window.fetch`。
- DCC 读回现在会在浏览器上下文中 fail-fast：缺 `ACCESS_TOKEN` 或 `tenantId` 直接失败，不再把匿名 URL 访问结果当成接管成功证据。
- DCC 读回现在会同时校验认证上下文、HTTP 状态、返回字节数和非 `application/json` 内容类型，避免把失败 envelope 当成文件读回成功。

## 验证结果

- RED：`node tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js` -> FAIL，缺少从浏览器缓存读取 `ACCESS_TOKEN`/`tenantId` 的 DCC 认证读回逻辑。
- GREEN：`node tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js` -> PASS。
- GREEN：`node --check tests/e2e/runtime-control-promote-backup-real-flow.e2e.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-foolproof-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：`git diff --check -- tests/e2e/runtime-control-promote-backup-real-flow.e2e.js tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js tests/e2e/runtime-control-foolproof-static.spec.js doc/tasks/20260605-runtime-control-promote-backup-authenticated-dcc-readback` -> PASS，仅 Git 行尾提示。
- GREEN：`task_closeout.py --task-id 20260605-runtime-control-promote-backup-authenticated-dcc-readback --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。
- GREEN：`task_closeout.py --task-id 20260605-runtime-control-promote-backup-authenticated-dcc-readback --mode apply` -> APPLIED，delete `<none>`，blocked `<none>`，warnings `<none>`。

## 阻塞

- 本任务未执行真实备份服接管；它只把 DCC 读回证据改成认证模型。真实 `promote-backup` 和真实 `rollback-app` 仍需当前任务明确授权后执行。

## Cleanup Keep

- `doc/tasks/20260605-runtime-control-promote-backup-authenticated-dcc-readback/task.md`
- `doc/tasks/20260605-runtime-control-promote-backup-authenticated-dcc-readback/execution-log.md`
