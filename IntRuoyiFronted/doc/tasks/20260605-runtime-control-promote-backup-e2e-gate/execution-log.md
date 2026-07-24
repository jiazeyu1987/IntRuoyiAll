# 执行日志：补齐备份服接管真实 E2E 门禁

## BDD

- BDD: 备份服接管真实 E2E 必须显式授权 -> Given `promote-backup` 会修改备份服务器应用版本并可能覆盖数据 / When 未设置 `RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_BACKUP=1` / Then 脚本必须在打开浏览器或提交动作前失败。
- BDD: 备份服接管必须绑定恢复集 -> Given 发布包需要作为接管单元 / When E2E 选择发布包 / Then 发布包候选必须暴露 `testedRecoverySetCandidateId`、`testedRecoverySetId` 和 `testedRecoverySetManifestHash`，缺失即阻塞。
- BDD: 接管后必须验证备份服与 DCC 读回 -> Given 备份服接管操作成功 / When E2E 收集结果 / Then 必须访问备份服 backend/frontend/Website/Showroom 健康 URL，并通过 DCC 下载或预览 URL 证明对象读回。

## TDD Evidence

- CHECK: 上一前端任务 -> PASS，`doc/tasks/20260604-dcc-nas-transfer-category-binding/task.md` 标记为 `completed`。

- RED: `node tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js` -> FAIL，缺少 `tests/e2e/runtime-control-promote-backup-real-flow.e2e.js`，证明备份服接管真实 E2E 门禁尚未建立。
- GREEN: `node tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js` -> PASS，真实 `promote-backup` E2E 门禁脚本、恢复集绑定字段、授权开关、备份健康 URL 与 DCC 读回 URL 合同已建立。
- GREEN: `node --check tests/e2e/runtime-control-promote-backup-real-flow.e2e.js` -> PASS。
- GREEN: `node tests/e2e/runtime-control-promote-backup-real-flow.e2e.js` 未设置 `RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_BACKUP=1` -> FAIL-FAST，错误要求显式设置授权变量，未进入 Playwright 浏览器或动作提交。
- GREEN: `node tests/e2e/runtime-control-foolproof-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/runtime-control-release-package-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/runtime-control-ops-static.spec.js` -> FAIL，旧静态合同仍查找较短的“恢复 MySQL / MinIO / 文件对象”，当前页面已使用更严格的“恢复同一恢复集的 MySQL / MinIO / 文件对象”。
- GREEN: `node tests/e2e/runtime-control-ops-static.spec.js` -> PASS，静态合同已对齐当前更严格的恢复集边界文案。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- tests/e2e/runtime-control-promote-backup-real-flow.e2e.js tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js tests/e2e/runtime-control-foolproof-static.spec.js tests/e2e/runtime-control-ops-static.spec.js src/api/infra/runtimeControl/index.ts doc/tasks/20260605-runtime-control-promote-backup-e2e-gate` -> PASS，仅 Git 行尾提示。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-runtime-control-promote-backup-e2e-gate --mode preview` -> READY，keep task/execution-log，delete `<none>`，blocked `<none>`，warnings `<none>`。
- BLOCKED: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-runtime-control-promote-backup-e2e-gate --mode apply` -> BLOCKED，清理工具未识别中文 `当前状态`，报告 `current status: unknown`；补充 `## Current Status` 镜像状态后重试。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-runtime-control-promote-backup-e2e-gate --mode apply` -> APPLIED，delete `<none>`，blocked `<none>`，warnings `<none>`。

## 当前阻塞

- 未执行真实 `promote-backup`。根据 `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md` 与根任务约束，远程测试服/备份服登录、E2E、发布、重启或接管必须获得当前任务明确授权。
