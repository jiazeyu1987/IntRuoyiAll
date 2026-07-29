# Execution Log

## Intent

- User request: 将批次执行详情页主区域的辅助模式改成与“辅助表单预览”一致。
- Target evidence: 配置页按责任主体显示固定 `12 × 9` 辅助表格；详情页当前仅显示 `87 项`扁平字段列表。

## BDD

- BDD: 批次详情按配置网格预览辅助表单 -> Given 当前批记录执行快照包含按责任主体配置的辅助表格 rowKey 和映射字段, When 用户在批次执行详情页切换到辅助模式, Then 主区域应按责任主体显示与配置预览一致的行列网格, And 已映射字段位于配置坐标, And 未映射格子仍占据原配置位置。
- BDD: 批次详情辅助模式保持只读 -> Given 用户正在查看批次详情辅助网格, When 页面渲染字段和当前值, Then 页面不得提供保存、提交、签名或上传动作。
- BDD: 非辅助网格配置明确阻塞 -> Given 执行快照包含无法解析为正式辅助表格坐标的辅助行, When 用户切换辅助模式, Then 页面不得把这些行伪装成与配置预览一致的网格。

## Initial State

- Date: 2026-07-29.
- Branch: `int_main`.
- Existing workspace state: 当前仓库存在并行任务改动；本任务不回滚、不覆盖，并在提交时使用选择性暂存。
- Root cause: 批次详情模板遍历 `selectedPreviewAssistFields` 渲染卡片列表；字段构建逻辑未解析辅助格 rowKey、未按责任主体分组，也未生成空单元格。

## TDD

- RED: `node tests/e2e/edhr-batch-detail-assist-grid-parity-static.spec.js` -> FAIL，首个断言证明详情页缺少当前 `USERS/ROLE` 与旧版 `U` 辅助格 rowKey 解析，仍无法按配置坐标构建网格。
- GREEN: pending.

## Milestone Updates

- Task setup: completed.
- Focused regression contract RED: completed.
