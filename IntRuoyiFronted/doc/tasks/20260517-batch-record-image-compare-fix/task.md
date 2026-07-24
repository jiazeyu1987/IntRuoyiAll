# Task: 批记录识别结果对图修正

## Goal

把当前本地电子批记录识别结果和用户提供的目标图片做比对，定位差异，给出修改建议，并把前端预览修正到更接近目标图。

## Scope

- 对比真实导入后的本地预览与用户图片。
- 记录结构差异和修正建议。
- 配合后端 richer layout JSON，修正前端预览的合并结构和纵向分区显示。
- 不改 DCC 页面，不做无关 UI 改造。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260516-batch-record-single-page-layout-constraints/task.md`
- Status before this task: completed.
- Impact: compact single-page constraints are already green, so this task can focus on structural fidelity against the target image.

## BDD Scenarios

- BDD: local preview should preserve merged structure from the recognized table -> Given the parser returns merged `rowSpan` and `colSpan`, When the local preview renders the rough-wash form, Then major header and section merges remain visible instead of being flattened.
- BDD: local preview should display left-side section labels closer to the target image -> Given the recognized layout includes tall narrow section cells, When the preview renders those cells, Then the left-side section labels are displayed in a vertical-friendly way that better matches the source image.

## Milestones

1. [x] M1: Create task package and record comparison BDD scenarios.
2. [x] M2: Capture the current recognized preview and summarize image-vs-preview differences.
3. [x] M3: Implement preview adjustments for merged structure and vertical section labels.
4. [x] M4: Re-run real DOC verification and compare the updated preview against the target image.
5. [x] M5: Update evidence and commit only current task files.

## Expected Verification

- Updated preview retains merged structure from the parser output.
- Left-side section labels look closer to the target image.
- Real DOC import still succeeds end-to-end.

## Current Status

Completed for implementation and verification. The local preview now respects parser merge spans and renders tall first-column section labels in a vertical-friendly way, which makes the rough-wash preview materially closer to the target image, and the frontend task files are ready for an isolated commit.

## Final Verification Result

- `pnpm exec eslint src/views/mes/pro/batchrecordtemplate/TemplateLayoutPreview.vue` -> PASS
