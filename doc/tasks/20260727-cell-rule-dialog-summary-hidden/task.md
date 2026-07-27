# 单元格规则弹窗隐藏顶部汇总栏

## Task Goal

- 按截图要求隐藏“单元格规则”弹窗顶部红框内的汇总栏，仅保留只读表单预览和右侧规则编辑区域。

## Milestones

- [x] 建立任务目录，记录适用规则、既有脏工作区基线和 BDD 场景。
- [x] 补充最小 RED 静态合同，锁定顶部汇总栏不再渲染。
- [x] 在单元格规则弹窗中移除红框汇总栏显示，不影响预览、右侧编辑和保存动作。
- [x] 运行目标验证并记录 GREEN / REGRESSION 证据。
- [ ] 完成收尾记录、经验沉淀、提交与推送。

## Expected Verification

- `node tests/e2e/batch-record-cell-rule-summary-hidden-static.spec.js`
- `node tests/e2e/batch-record-cell-rule-default-fullscreen-static.spec.js`
- `node tests/e2e/edhr-cell-rule-type-background-colors-static.spec.js`
- `pnpm ts:check`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260727-cell-rule-dialog-summary-hidden/bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260727-cell-rule-dialog-summary-hidden/frontend-feature-evidence.md`

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接移除不需要展示的顶部汇总区域，不通过 CSS 隐藏或兼容分支绕过。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- `docs/experience-index.md` 已存在；本任务命中前端页面/样式和静态合同门禁，已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- 前端静态合同隔离门禁：本次只修改单元格规则弹窗局部显示，使用任务专用静态合同覆盖红框汇总栏隐藏。
- Windows 换行与脚本行为同步门禁：新增 `tests/e2e/*static.spec.js` 时需归一化 CRLF/LF 并运行目标静态合同。
- 脏工作区基线门禁：开始本任务前已保存既有脏改动基线 `ffc8e3c0` 和 `67aa7f22`；随后出现的附件配置任务文档为并行任务，不纳入本次选择性暂存。

## Cleanup Keep

- doc/tasks/20260727-cell-rule-dialog-summary-hidden/bug-regression-evidence.md
- doc/tasks/20260727-cell-rule-dialog-summary-hidden/frontend-feature-evidence.md
- doc/tasks/20260727-cell-rule-dialog-summary-hidden/verification-report.md
