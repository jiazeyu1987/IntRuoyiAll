# 任务：NAS 发布包流转前端

## 目标

配合运行控制台发布包流转，将前端操作从“直接发布服务器”调整为“构建发布包、部署到测试服、标记测试通过、上线正式服”。

## 里程碑

- [x] 更新运行控制台动作按钮与文案。
- [x] 请求体支持 ReleaseTag。
- [x] 静态测试覆盖发布包动作和禁止旧“提升正式服”文案。

## 预期验证

- `node tests/e2e/runtime-control-release-package-static.spec.js`

## 当前状态

completed

## Current Status

completed

## 验证结果

- `node tests\e2e\runtime-control-release-package-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。

## Cleanup Keep

- doc/tasks/20260528-nas-release-package-flow/task.md
- doc/tasks/20260528-nas-release-package-flow/execution-log.md
