# 批记录填写配置同版本导航

## Task Goal

调整批记录表单“填写配置”弹窗布局：将底部 `关闭 / 重新读取 / 保存填写配置` 移到顶部右侧，在顶部中间新增 `上一张 / 下一张`，并只允许在同一产品、同一版本的批记录表单集合内切换。

## Milestones

1. `completed`：读取前端、E2E、任务收尾、经验索引、PowerShell/Git 与编码规则，完成既有脏工作区基线提交。
2. `in_progress`：补充任务专用 RED 静态合同，锁定顶部工具栏、导航事件、分页候选范围和 footer 移除。
3. `pending`：修改 `BatchRecordCellRulesConfirmDialog.vue` 与批记录表单列表页，完成顶部三段式工具栏、脏数据确认和同产品同版本候选导航。
4. `pending`：运行定向静态合同、相邻回归和 `pnpm ts:check`。
5. `pending`：更新验证报告、经验沉淀、cleanup、提交并推送。

## Expected Verification

- `node tests/e2e/batch-record-cell-rule-navigation-static.spec.js`
- `node tests/e2e/batch-record-cell-rule-default-fullscreen-static.spec.js`
- `node tests/e2e/batch-record-cell-rule-dialog-size-static.spec.js`
- `node tests/e2e/edhr-visual-fill-config-static.spec.js`
- `pnpm ts:check`
- 真实 Playwright 验证仅在本机前后端、登录态和目标数据可用时执行；不可用则记录明确 blocker。

## Applicable Gates

- 前端静态契约隔离门禁：当前行为用专用静态合同 RED/GREEN 证明，不用无关宽合同替代。
- 前端延迟辅助加载错误归属门禁：候选导航加载失败必须显示真实错误并禁用导航，不污染列表首屏状态、不吞异常。
- 批记录/表单槽位边界：本任务只处理批记录表单目录，不使用 `formBindings`、表单槽位或动态表单链路推导导航集合。
- PowerShell/Git 脏工作区基线门禁：任务开始前既有 dirty 文件和未跟踪任务目录已拆成独立基线提交。
- PowerShell 分号串联测试退出码门禁：验证命令逐条执行并记录结果，避免后续 PASS 掩盖前序失败。

## Baseline Commits

- `f410a338`：`IntRuoyiFronted/tests/e2e/edhr-assist-fill-mode-static.spec.js` 既有改动基线。
- `3a556a26`：`doc/tasks/20260729-edhr-fill-workspace-redbox-hide/` 既有未跟踪任务文档基线。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接调整填写配置弹窗布局和正式批记录表单候选导航状态。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress
