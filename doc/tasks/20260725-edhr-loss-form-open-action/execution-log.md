# Execution Log

## User Intent

用户反馈：选择 eDHR 批次详情右侧“损耗单”时提示“必填路线表单不允许跳过”。期望关闭前都可以修改，损耗单应可继续打开填写。

## BDD

- `BDD: required loss form opens instead of skip -> Given` 批次详情右侧存在必填动态表单“损耗单”，`When` 用户点击“打开填写”，`Then` 前端必须执行打开填写路径，不得调用跳过表单路径。
- `BDD: optional route form skip remains constrained -> Given` 路线表单是可选且满足跳过条件，`When` 用户点击跳过入口，`Then` 仅可选表单允许调用跳过接口，必填表单仍被阻止。

## Milestone Updates

- in_progress: 创建任务记录，准备读取经验门禁并定位源码。

## TDD Evidence

- RED: pending
- GREEN: pending

## Verification Evidence

- pending

## Blockers

- pending
