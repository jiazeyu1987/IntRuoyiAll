# 任务：回滚候选显示 NAS 发布包来源

## 任务目标

配合后端“回滚版本候选读取 NAS 标准发布包”修复，更新运行控制台候选类型和显示文案：回滚候选显示“发布包”，恢复数据候选继续显示“备份”，避免用户把标准发布包误解为备份点。

## BDD 场景

- BDD: 回滚候选显示发布包 -> Given 后端返回回滚候选 `releaseTag` / When 用户打开“回滚版本”弹窗 / Then 候选元信息显示“发布包 <releaseTag>”和版本号。
- BDD: 恢复候选仍显示备份 -> Given 用户打开“恢复数据”弹窗 / When 候选来自备份点 / Then 候选元信息仍显示“备份 <backupId>”。

## 里程碑

- [x] M1：建立前端任务记录。
- [x] M2：补充回滚候选 `releaseTag` 类型。
- [x] M3：调整候选选择组件显示文案。
- [x] M4：运行前端相关静态/语法验证。

## 预期验证

- `node --check tests\e2e\runtime-control-rollback-app.e2e.js`
- `node --check tests\e2e\runtime-control-release-package-static.spec.js`
- `node tests\e2e\runtime-control-release-package-static.spec.js`

## Current Status

Completed.

## 最终验证

- `node --check tests\e2e\runtime-control-rollback-app.e2e.js; node --check tests\e2e\runtime-control-release-package-static.spec.js; node tests\e2e\runtime-control-release-package-static.spec.js` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260530-runtime-rollback-release-package-candidates --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
