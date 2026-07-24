# Task: 批记录识别结果对图修正

## Goal

将当前本地电子批记录识别出来的表格与用户提供的目标图片做对比，定位结构差异，尤其是合并单元格、多级表头和左侧纵向分区标题的差异，并修改识别链路，使本地识别结果更接近目标图。

## Scope

- 仅修改本地批记录模板识别链路，不改 DCC、不改无关 generated-report 脏改动。
- 优先修复本地 `sheetLayoutJson` 中的合并单元格跨度信息。
- 如需要，为前端预览提供最小额外元数据以支持更接近目标图的显示。

## Previous Task Check

- Previous backend task: `doc/tasks/20260516-local-batch-record-template-layout-constraints/task.md`
- Status before this task: completed.
- Impact: the local layout constraints are already in place, so this task focuses specifically on the remaining structure gap versus the target image.

## BDD Scenarios

- BDD: local parser should preserve merged table structure -> Given the source Word table contains merged header or section cells, When the local parser serializes `sheetLayoutJson`, Then the JSON should preserve the corresponding `rowSpan` and `colSpan`.
- BDD: local parser output should better match the target rough-wash image -> Given the rough-wash source document is parsed, When the preview renders the local layout JSON, Then major merged regions from the image are preserved instead of being flattened into repeated 1x1 cells.

## Milestones

1. [x] M1: Create task package and record the image-diff BDD scenarios.
2. [x] M2: Add RED parser assertions for merged structure preservation.
3. [x] M3: Implement local Word merge-span extraction and any minimal metadata needed by preview.
4. [x] M4: Run focused backend tests plus real frontend verification against the provided DOC.
5. [x] M5: Update evidence and commit only current task files.

## Expected Verification

- Local parser preserves merged spans from source Word tables.
- Real frontend preview after importing the provided DOC more closely matches the target image structure.

## Current Status

Completed for implementation and verification. The local Word parser now preserves real horizontal and vertical merged spans, which materially reduces the structure gap against the target rough-wash image, and the backend task files are ready for an isolated commit.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordWordParserTest,MesProBatchRecordTemplateServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
