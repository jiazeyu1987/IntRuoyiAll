# Bug Regression Evidence

## Bug Summary

eDHR 批次执行详情页右侧单据列表下方重新出现独立 `填写人 / 提交时间` 元信息块，用户截图标注要求删除该红框内容。

## Expected Behavior

右侧栏只保留当前工序单据卡片、卡片内填写人、阻断原因和操作入口；不得渲染独立 `edhr-batch-detail__primary-fill-meta` 红框块。

## Reproduction

- Path: eDHR 批次执行详情页右侧当前工序栏。
- Command: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js`

## Root Cause

待确认。初步定位为后续变更恢复了 `edhr-batch-detail__primary-fill-meta` 模板块及 `primaryFormFillMetaItems` 计算逻辑。

## Regression Test

- Updated: `IntRuoyiFronted/tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js`

## Evidence

- RED: pending。
- GREEN: pending。

## Risk And Scope

- Scope: `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue` 及相关静态契约。
- Risk: 不能删除单据卡片内填写人展示，否则会回退 20260724 的单据级填写人可见性。

## Blockers And Follow-up

- 当前工作区存在其他任务持续写入的非自有文档产物；本任务不纳入这些文件。
