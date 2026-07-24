# 20260608-runtime-console-test-root-cleanup

## 任务目标

在 IntRuoyi 运行控制台展示远程根分区剩余大小，提供目标环境选择、刷新按钮和清理按钮。支持测试服 `172.30.30.58`、正式服 `172.30.30.57`、备份服务器 `172.30.30.59`；正式服/备用服务器清理必须输入 `PROD`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。前端仅展示服务端返回的真实容量和清理证据，失败时直接暴露错误。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 显示远程根分区剩余空间 -> Given 运维人员选择 `test`、`prod` 或 `backup` / When 点击刷新 / Then 页面显示对应固定服务器 IP、根分区剩余量、使用率和采样时间。
- BDD: 清理远程临时目录 -> Given 运维人员点击清理按钮 / When 确认清理 / Then 页面调用后端受控清理接口并刷新根分区容量。
- BDD: 高危服务器清理需要 PROD 确认 -> Given 运维人员选择正式服或备用服务器 / When 打开清理确认 / Then 页面要求输入 `PROD`，否则不提交。

## 里程碑

- [x] M1：创建任务文档，写 RED 静态测试。
- [x] M2：前端 API 类型与请求方法。
- [x] M3：运行控制台 UI 和交互。
- [x] M4：GREEN 与回归验证。
- [x] M5：更新 execution-log，运行 task-closeout-cleanup 预览，只提交本任务相关改动。

## 预期验证

- `node tests/e2e/runtime-control-remote-root-cleanup-static.spec.js`
- `node tests/e2e/runtime-control-static.spec.js`
- `node tests/e2e/runtime-control-restore-target-static.spec.js`
- `node tests/e2e/runtime-control-rollback-target-static.spec.js`
- `node tests/e2e/dcc-backup-boundary-static.spec.js`
- `pnpm ts:check`
- 受影响 TypeScript/静态检查通过。

## 阻塞记录

- resolved: 2026-06-08 已恢复运行控制台根分区任务上下文，完成三目标查询/清理 UI 与验证；未混入登录页任务改动。

## 当前状态

completed
