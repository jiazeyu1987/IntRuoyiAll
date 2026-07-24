# 20260608-backup-incremental-manifest-short-loop

## 任务目标

在运行控制台接入后端备份点 manifest 字段，展示当前备份模式、保留策略、最近备份点、imageTag 和对象增量统计，服务于测试服 `172.30.30.58` 连续备份闭环验收。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。前端只展示后端返回字段，缺失时显示空态，不伪造策略或统计。
- 是否从根因和长期维护角度解决：是。字段来自备份 manifest 的正式 API VO。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 运行控制台展示备份策略 -> Given 后端返回最近备份点 / When 运维打开运行控制台 / Then 页面展示备份模式、保留策略、最近备份点和 imageTag。
- BDD: 运行控制台展示对象增量统计 -> Given 后端返回对象 added/modified/deleted/reused 统计 / When 运维查看备份策略表格 / Then 页面展示新增、修改、删除、复用数量。
- BDD: DCC 短闭环 E2E 脚本限定测试边界 -> Given 验收脚本会操作 DCC 新增、删除和恢复验证 / When 脚本启动 / Then 必须先硬断言不是 `172.30.30.57`、租户为 `测试租户`、账号不是 `admin`。

## 里程碑

- [x] M1：识别现有运行控制台备份策略展示位置。
- [x] M2：用 RED 静态测试锁定容量阈值字段接线。
- [x] M3：补 TypeScript 类型和保留策略显示。
- [x] M4：运行前端静态测试。
- [x] M5：DCC 上传、删除、恢复 E2E 脚本硬断言测试环境边界。

## 预期验证

- `node tests/e2e/runtime-control-backup-policy-static.spec.js`
- `node tests/e2e/dcc-backup-boundary-static.spec.js`
- `node tests/e2e/runtime-control-static.spec.js`
- `node tests/e2e/runtime-control-remote-root-cleanup-static.spec.js`

## 当前状态

completed

## Verification Result

- `node tests/e2e/runtime-control-backup-policy-static.spec.js` -> PASS。
- `node tests/e2e/dcc-backup-boundary-static.spec.js` -> PASS。
- `node tests/e2e/runtime-control-static.spec.js` -> PASS。
- `node tests/e2e/runtime-control-remote-root-cleanup-static.spec.js` -> PASS。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-backup-incremental-manifest-short-loop --mode preview` -> PASS，无删除项、无阻塞、无警告。
- `node tests/e2e/dcc-restore-verify.e2e.js` after B3/B4/B5 restore -> PASS；脚本输出 JSON 下载响应文本，避免把无权限 JSON 响应误判为原始文件。
