# 填写配置弹窗最大化按钮可见修复

## Task Goal

修复截图中“填写配置”弹窗右上角缺少最大化/恢复按钮的问题；真实显示的批记录填写配置弹窗也必须和用户要求一致，默认最大化打开，并在右上角显示最大化/恢复按钮。

## Milestones

- [x] 启动门禁：读取 bug 回归、前端交付、前端开发、任务收尾、PowerShell 编码和编排规则。
- [x] 脏工作区隔离记录：当前工作区存在大量并行脏改，本任务只修改填写配置弹窗和相关静态合同/证据，不提交、不回滚并行改动。
- [x] RED：复跑批记录填写配置最大化静态合同，证明当前真实弹窗未启用最大化按钮。
- [x] GREEN：修改真实批记录填写配置弹窗，启用右上角最大化/恢复按钮并默认最大化。
- [x] REGRESSION：运行批记录填写配置、eDHR visual fill config、表单中心填写配置合同和类型/组件检查。
- [x] CLOSEOUT：更新证据，按当前状态记录是否可收尾。

## Expected Verification

- `node tests/e2e/batch-record-cell-rule-default-fullscreen-static.spec.js`
- `node tests/e2e/edhr-visual-fill-config-static.spec.js`
- `node tests/e2e/form-template-fill-config-static.spec.js`
- `pnpm exec eslint --ext .vue src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`
- `pnpm ts:check`

## Applicable Gates

- 前端静态契约隔离门禁：用最小静态合同锁定本次 UI 行为，不用无关大合同绕过。
- 表单模板三按钮领域边界门禁：本次只补批记录填写配置弹窗按钮显示，不改变表单中心模板自身数据归属或批记录数据链路。
- PowerShell 分号串联测试退出码门禁：验证命令逐条执行并记录结果。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，修正真实显示弹窗的 Dialog 配置，并同步静态合同。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260728-fill-config-maximize-button-visible/bug-regression-evidence.md`

## Verification Evidence

- `node tests/e2e/batch-record-cell-rule-default-fullscreen-static.spec.js`：RED 先失败于缺少默认全屏配置；修复后 PASS。
- `node tests/e2e/edhr-visual-fill-config-static.spec.js`：PASS。
- `node tests/e2e/form-template-fill-config-static.spec.js`：PASS。
- `node tests/e2e/form-center-static.spec.js`：PASS。
- `node tests/e2e/form-template-button-interaction-parity-static.spec.js`：PASS。
- `node tests/e2e/form-template-independent-button-actions-static.spec.js`：PASS。
- `pnpm exec eslint --ext .vue src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`：PASS。
- `pnpm ts:check`：PASS。

## Current Status

ready_for_closeout
