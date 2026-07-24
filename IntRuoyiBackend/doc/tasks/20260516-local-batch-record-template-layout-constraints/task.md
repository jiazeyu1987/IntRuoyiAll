# Task: 本地批记录模板布局约束

## Goal

给本地电子批记录模板识别链路输出的 `sheetLayoutJson` 增加紧凑单页显示约束和空单元格文字占位符，让识别后的表单在浏览器预览中尽量单页显示，同时明确哪些位置可填写。

## Scope

- 仅修改 `yudao-module-mes` 本地批记录模板识别链路。
- 在本地 `sheetLayoutJson` 中增加显示约束元数据和空单元格占位字段。
- 不改 DCC、不改其他仓库、不处理无关的 `batchrecordreport` 脏改动。

## Previous Task Check

- Previous backend task: `doc/tasks/20260516-batch-record-single-page-layout-constraints/task.md`
- Status before this task: completed for the JimuReport generated-report chain.
- Impact: the generated-report single-page rules are already complete, and this task only extends similar constraints to the separate local batch-record template recognition chain.

## BDD Scenarios

- BDD: local template parser should emit compact single-page constraints -> Given a Word table is recognized into local `sheetLayoutJson`, When the parser serializes the layout, Then the JSON carries constrained row height, effective width, and font size metadata for compact browser rendering.
- BDD: local template parser should mark blank cells with a visible placeholder -> Given a recognized local template cell is empty, When the parser serializes `sheetLayoutJson`, Then the cell remains text-empty but carries visible placeholder metadata indicating the field is fillable.

## Milestones

1. [x] M1: Create task package and record BDD scenarios.
2. [x] M2: Add RED parser assertions for display constraints and blank-cell placeholders.
3. [x] M3: Implement local parser layout rules and richer `sheetLayoutJson`.
4. [x] M4: Run focused backend verification or record exact blocker.
5. [x] M5: Update evidence and commit only current task files.

## Expected Verification

- Parser output contains `displayConstraints`.
- Blank cells contain placeholder metadata.
- Main MES/backend code compiles.
- Live frontend import path can consume the new layout JSON successfully.

## Current Status

Completed for implementation and verification. The local batch-record parser now emits compact display constraints and blank-cell placeholder metadata, the local template service regressions remain green, the live frontend import path successfully consumed the richer layout JSON, and the backend task files are ready for an isolated commit.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordWordParserTest,MesProBatchRecordTemplateServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS
