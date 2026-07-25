# Execution Log

## User Intent

用户反馈：选择 eDHR 批次详情右侧“损耗单”时提示“必填路线表单不允许跳过”。期望关闭前都可以修改，损耗单应可继续打开填写。

## BDD

- `BDD: required loss form opens instead of skip -> Given` 批次详情右侧存在必填动态表单“损耗单”，`When` 用户点击“打开填写”，`Then` 前端必须执行打开填写路径，不得调用跳过表单路径。
- `BDD: optional route form skip remains constrained -> Given` 路线表单是可选且满足跳过条件，`When` 用户点击跳过入口，`Then` 仅可选表单允许调用跳过接口，必填表单仍被阻止。

## Milestone Updates

- in_progress: 创建任务记录，准备读取经验门禁并定位源码。
- completed: 读取 `docs/experience-index.md` 命中 eDHR 动态表单、损耗单和静态合同门禁，并补入 `task.md`。
- completed: 定位到前端 `isOptionalTask` 把 `!isRequiredBatchRecordTask(row)` 作为可选/可跳过口径，和后端 `requiredPolicy == OPTIONAL` 的跳过规则不一致。

## TDD Evidence

- RED: `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> FAIL, expected reason: 当前 `isOptionalTask` 未包含 `row.requiredPolicy === 'OPTIONAL'`，会把非 OPTIONAL 的路线表单误纳入可跳过判断。
- GREEN: pending

## Verification Evidence

- pending

## Blockers

- pending
