# 单元格规则弹窗默认全屏

## Task Goal

- 将批记录表单列表中的“单元格规则”弹窗调整为显示时默认全屏，减少用户再次点击右上角全屏按钮的操作。

## Milestones

- [x] 创建任务目录并记录适用项目规则、基线提交和 BDD 场景。
- [x] 定位现有单元格规则弹窗组件与 Dialog 使用契约。
- [x] 先补最小静态合同 RED，再实现默认全屏。
- [x] 运行目标验证并记录 GREEN / REGRESSION 证据。
- [ ] 完成收尾记录、经验沉淀、提交与推送。

## Expected Verification

- 静态合同能证明 `BatchRecordCellRulesConfirmDialog.vue` 打开时传入默认全屏配置。
- 前端类型检查或最小静态合同通过。
- 不引入 fallback、降级、吞异常、无关重构或视觉重设计。

## Current Status

- ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接调整弹窗默认显示状态，不新增兼容分支。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- Frontend feature gate: 先确认页面入口、组件契约和最小静态合同；不修改无关 API、路由或设计体系。
- Frontend static contract isolation gate: 若全量前端检查受无关历史问题阻塞，使用任务专用最小静态合同记录 RED/GREEN，并明确全量回归剩余阻塞。
- PowerShell / UTF-8 gate: 中文任务文档使用 UTF-8 路径读写，PowerShell 不使用 `&&`。
- Dirty workspace baseline gate: 开始实现前发现已有脏改动，已独立提交基线 `1a564046` 与 `d9a17b39`，避免混入本次实现提交。

## Cleanup Keep

- doc/tasks/20260727-cell-rule-dialog-default-fullscreen/frontend-feature-evidence.md
