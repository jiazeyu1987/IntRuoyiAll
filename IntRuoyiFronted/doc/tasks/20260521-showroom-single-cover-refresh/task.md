# Task: 单个封面生成后刷新列表显示

## Goal

修复展厅产品单个 `AI生成` 成功后，当前列表页仍停留在“未上传”视觉状态的问题。

本次修复要求：

- 成功生成封面后，前端必须刷新产品列表；
- 详情弹窗中的基线 `cover_image` 也要同步到最新生成值，避免只因为封面字段被判定为未保存；
- 不改动其他字段的未保存变更判断。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- 需要时补充的 showroom 前端定向测试
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-single-cover-refresh\**`

## Non-Scope

- 不重写产品列表 UI。
- 不新增测试专用前端控件。
- 不处理无关在途改动。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-cover-real-data-verification\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 真实验收已完成并暴露“生成后列表未上传”的症状，本次针对该前端体验缺口收口。

## Milestones

1. 创建任务文档与回归证据骨架。
2. 先补 RED，锁定生成成功后必须刷新列表并同步封面 baseline。
3. 最小修复前端生成成功路径。
4. 跑通定向 GREEN 与真实页面复验。
5. 单独提交本任务范围改动。

## Current Status

- Status: Completed
- Completed work:
  - 已确认 `handleGenerateProductCoverImage()` 当前只回填 `productForm.coverImage`，没有刷新列表，也没有更新 baseline。
  - 已补齐成功路径刷新列表与封面 baseline 同步，并通过真实页面复验。
- Remaining blockers:
  - None.

## Milestone Status

### Milestone 1

- Status: Completed
- Completed work:
  - 已创建任务文档与执行日志。
  - 已把问题收敛到“成功生成后列表未刷新，baseline 未同步封面字段”。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-single-cover-refresh\task.md`
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-single-cover-refresh\execution-log.md`
- Remaining blockers:
  - 需要完成 RED/修复/GREEN。

### Milestone 2

- Status: Completed
- Completed work:
  - 已扩展前端源码断言，锁定成功路径必须刷新列表并同步封面 baseline。
  - 已执行 RED，确认旧实现缺少这两个动作。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-cover-field.test.mjs`
  - `node --test scripts/showroom-admin-product-cover-field.test.mjs`（RED）
- Remaining blockers:
  - 需要完成前端修复。

### Milestone 3

- Status: Completed
- Completed work:
  - 已在 `handleGenerateProductCoverImage()` 成功路径补齐 `syncGeneratedProductCoverBaseline(...)`。
  - 已补齐 `await loadProductRows()`。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- Remaining blockers:
  - 待完成真实复验与提交。

### Milestone 4

- Status: Completed
- Completed work:
  - 已通过前端源码回归测试。
  - 已通过真实页面复验，确认 `product_001` 列表行最终出现封面图片。
- Verification evidence:
  - `node --test scripts/showroom-admin-product-cover-field.test.mjs`
  - Playwright 会话 `showroom-single-cover-refresh-2` 真实 DOM 核对：`imgCount=1`
- Remaining blockers:
  - 待完成任务范围提交。

### Milestone 5

- Status: Completed
- Completed work:
  - 已将变更范围收敛到 `index.vue`、前端回归测试与当前任务目录。
  - 已创建本任务独立 commit `2f75620e`。
- Verification evidence:
  - `git commit -m "任务: 刷新单个封面生成列表"`
- Remaining blockers:
  - None.
