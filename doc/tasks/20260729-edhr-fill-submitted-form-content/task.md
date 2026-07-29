# eDHR 主区域空表单与已提交内容显示

## Task Goal

红框主区域在当前工序没有已提交批记录内容时显示空表单；存在已提交内容时显示该表单对应单元格内容，且不得把草稿单元值误当成已提交内容。

## Milestones

1. 任务建档与门禁确认：completed
2. 定位批次详情主区域数据源和只读表单渲染链路：completed
3. 补充 RED 静态回归合同：completed
4. 实现最小前端修复：completed
5. 运行 GREEN/REGRESSION 验证并记录证据：completed
6. 收尾、经验沉淀、提交和推送：pending

## Expected Verification

- `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js`
- 新增或更新聚焦静态合同，覆盖“无已提交内容显示空表单且清空单元值”“有已提交内容显示 submitted formViewModel 单元值”。
- `pnpm ts:check`
- `node tests/e2e/edhr-batch-admin-preview-runtime-fix.e2e.js`

## Applicable Gates

### eDHR 管理员主区域已提交内容门禁

- Trigger: 主区域查看已提交批记录内容、`review-timeline.executionReviews.formViewModel`、`暂无已提交批记录内容`。
- Preflight check: 已提交内容只能来自 `review-timeline` 的 submitted execution `formViewModel`；若无 submitted execution，允许按本任务新需求用正式预览模板渲染空表单，但必须清空单元格值。
- Blocker: 只有草稿执行记录却展示草稿 `cell_values_json`、使用历史 execution 直连、API-only 或旧样本截图替代页面行为。
- Verification: 静态合同需证明 submitted 内容优先，空表单不携带草稿单元值；真实 E2E 若运行需记录批次、任务、execution/status、`review-timeline` 和写请求数。
- Forbidden action: 禁止把草稿有值解释为管理员应显示内容；禁止用 task preview 的单元值冒充已提交内容。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按主区域展示数据源边界区分 submitted 内容和空表单模板。
- `是否存在临时补丁或绕过`：否。

## Baseline

- Preexisting dirty baseline commit: `a6cfc066`
- Baseline files:
  - `IntRuoyiFronted/tests/e2e/edhr-fill-workspace-card-density-static.spec.js`
  - `doc/tasks/20260729-card-text-double/task.md`
  - `doc/tasks/20260729-card-text-double/execution-log.md`
  - `doc/tasks/20260729-card-text-double/frontend-feature-evidence.md`

## Cleanup Keep

- doc/tasks/20260729-edhr-fill-submitted-form-content/frontend-feature-evidence.md
- doc/tasks/20260729-edhr-fill-submitted-form-content/bug-regression-evidence.md
- doc/tasks/20260729-edhr-fill-submitted-form-content/admin-preview-e2e-output/admin-unstarted-form-preview.json
- doc/tasks/20260729-edhr-fill-submitted-form-content/admin-preview-e2e-output/admin-unstarted-form-preview.png
