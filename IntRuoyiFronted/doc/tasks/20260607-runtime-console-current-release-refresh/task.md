# 任务：运行控制台标记测试通过刷新当前发布包

## Goal

修复运行控制台点击“标记测试通过”时当前测试服发布包显示为“无”的问题，并修复“恢复数据到测试服”错误要求 `PROD` 的目标环境门禁问题，确保可以按 UI 从构建发布包重新完成测试服部署、标记测试通过、上线备份服务器、测试服备份和测试服恢复。

## Scope

- 前端运行控制台 `openOperation` 打开依赖当前测试服发布包的动作时刷新概览。
- 前端运行控制台 `restore-data` / `backup-now` / `rollback-app` 的 `PROD` 确认按目标环境判断，测试服目标不要求 `PROD`，备份/正式目标仍要求。
- 保持后端 fail-fast 校验不变，不在前端伪造 releaseTag、恢复集候选或成功状态。
- 配合当前总目标继续从“构建发布包”重新验证。

## Non-Scope

- 不修改正式服务器程序和数据。
- 不绕过恢复集候选、责任人或 PROD 门禁。
- 不使用 CLI 或接口结果替代最终 UI 验证。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；概览刷新失败仍按现有错误提示暴露，不伪造当前发布包。
- `是否从根因和长期维护角度解决`：是；修复弹窗读取 stale overview 的根因。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 标记测试通过读取最新测试服发布包 -> Given 测试服已部署发布包 A / When 操作员打开“标记测试通过”弹窗 / Then 页面先刷新运行概览并展示当前测试服 `releaseTag`，允许在恢复集候选可用时提交。
- BDD: 禁止伪造当前测试服发布包 -> Given 概览刷新失败或测试服未返回 `currentReleaseTag` / When 操作员打开或提交“标记测试通过” / Then 页面阻止提交并暴露缺失前置条件。
- BDD: 恢复数据到测试服不要求 PROD -> Given 操作员打开“恢复数据”弹窗且目标为测试服 / When 选择测试服恢复集候选 / Then 页面不显示 `PROD` 确认；切换备份服务器目标时才显示 `PROD` 确认。

## Milestones

- [x] M1：确认上一前端任务已完成并建立本任务文档。
- [x] M2：补充 RED 静态契约测试。
- [x] M3：最小修复前端打开弹窗刷新概览。
- [x] M4：运行前端静态回归测试。
- [x] M5：配合总目标完成真实 UI 复测和回归验证。
- [x] M6：收尾清理预览。

## Expected Verification

- `node tests/e2e/runtime-control-release-package-static.spec.js`
- `node tests/e2e/runtime-control-restore-data.e2e.js`
- `node tests/e2e/runtime-control-ops-static.spec.js`
- `node tests/e2e/runtime-control-restore-target-static.spec.js`
- `node --check doc/tasks/20260607-runtime-console-current-release-refresh/scripts/runtime-console-full-goal.e2e.js`
- `node --check doc/tasks/20260607-runtime-console-current-release-refresh/scripts/diagnose-mark-release-tested.e2e.js`
- 真实运行控制台 UI 打开“标记测试通过”时不再显示当前测试服发布包为“无”。
- 真实运行控制台 UI 从构建发布包重新完成完整目标链路，发布包 A 为 `20260607_ui_code_only_onlyoffice_A_043314`。

## Current Status

completed

## Final Verification

- ReleaseTag A: `20260607_ui_code_only_onlyoffice_A_043314`
- NAS release path: `Backup/ReleasePackage/20260607_ui_code_only_onlyoffice_A_043314`
- Full UI artifact: `doc/tasks/20260607-runtime-console-current-release-refresh/artifacts/runtime-console-full-goal-result.json`
- Full UI stdout: `doc/tasks/20260607-runtime-console-current-release-refresh/artifacts/runtime-console-full-goal-20260607_ui_code_only_onlyoffice_A_043314.stdout.log`
- Full UI stderr: `doc/tasks/20260607-runtime-console-current-release-refresh/artifacts/runtime-console-full-goal-20260607_ui_code_only_onlyoffice_A_043314.stderr.log`，长度 0。
- Operation IDs:
  - `build-release`: `f45a3095-a28c-423c-94cf-e2257e2120f5`
  - `publish-test`: `c035f04c-f8db-4555-b9fc-c6c0b3307056`
  - `mark-release-tested`: `404bea05-1954-40c1-b7a1-0fdadd8e9e30`
  - `promote-backup`: `41455001-0c1e-44e0-8ef1-9dc4d7ab6cf6`
  - `backup-now` 测试服: `599de693-0aec-414d-9362-cc3b37a1971f`
  - `restore-data` 测试服: `21336b5c-a5b2-4bd0-8e65-f61536e37f41`
- Production boundary: 本次 UI 驱动记录 `No promote-prod action and no targetEnvironment=prod were submitted by this run.`；未对正式服务器 `172.30.30.57` 提交发布、重启、写入或恢复动作。
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260607-runtime-console-current-release-refresh --mode preview` -> `status: ready`，`blocked: <none>`。
