# eDHR 批量作废工具栏清理

## Task Goal

删除批次执行列表页截图红框中的工具栏 item，并将蓝框按钮描述改为“批量作废”。

## Milestones

- [x] 创建任务目录并读取前端、E2E、PowerShell、收尾与经验门禁。
- [x] 更新批次执行列表页工具栏 UI。
- [x] 更新静态合同，覆盖红框入口删除与蓝框文案变更。
- [x] 运行相关静态验证并记录结果。
- [x] 完成收尾记录。

## Expected Verification

- `node tests/e2e/edhr-batch-execution-golden-finger-bulk-void-static.spec.js`
- `node tests/e2e/edhr-batch-local-state-sample-static.spec.js`（预期需同步改为删除入口后的合同）
- `pnpm ts:check`（如耗时或环境缺失则记录阻塞）

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接清理页面入口和对应静态合同。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 前端页面 / 表格 / 样式：保持现有 Element Plus 工具栏与权限控制模式，不引入无关设计体系。
- 静态合同与真实 E2E 同步门禁：修改 `tests/e2e/*static.spec.js` 时同步当前页面真实行为，窄范围修复不顺手改无关逻辑。
- PowerShell / Git 门禁：提交前检查 `git status --short --branch`、staged 列表；已有脏改动已按规则基线提交 `b727bb0c`。

## Cleanup Keep

- `doc/tasks/20260725-edhr-bulk-void-toolbar-cleanup/frontend-feature-evidence.md`
