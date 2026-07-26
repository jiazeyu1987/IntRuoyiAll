# Execution Log

## Intent

- 用户反馈：点击右侧“新增表单”后，节点右上角数量仍为 `1`，期望立即变为 `2`。
- 初步根因：上一轮已将新增空绑定默认槽位改为非 `MAIN`，但 `getRouteNodeAdditionalFormCount()` 仍额外要求 `formTemplateId > 0`，因此新建空行未被计数。

## BDD

- BDD: 点击新增表单立即更新节点数量 -> Given 用户选中“表单槽位”且当前工序已有 1 个非 `MAIN` 动态表单，When 用户点击右侧“新增表单”产生第二个非 `MAIN` 动态槽位行，Then 节点右上角数量徽标立即显示 `2`，不必等待模板选择完成。

## Verification Evidence

- RED: pending
- GREEN: pending

## Blockers

- 当前分支 `int_main` 已存在非本任务 ahead 提交与少量脏改动；本任务需选择性提交或记录推送阻塞，避免混入并行工作。

