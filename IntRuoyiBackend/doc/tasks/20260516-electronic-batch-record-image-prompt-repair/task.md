# Task: Electronic Batch Record Image Prompt Repair

## Goal

Repair the Codex CLI image-recognition prompt used by the MES electronic
batch-record parser so the backend explicitly instructs the model to convert
the screenshot into a structured system report table instead of sending a
garbled prompt payload.

## Scope

- Check the latest related image-recognition task status before changing code.
- Create this task package before production code changes.
- Record BDD scenarios and RED verification for the prompt contract.
- Change only the backend prompt wiring and the focused regression coverage
  needed for this repair.
- Do not add fallback OCR engines, alternate parsers, or frontend changes.

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260516-electronic-batch-record-image-table-structure-fix/task.md`
- Status before this task: completed.
- Impact: the prior structure/layout work remains valid, and this task is
  limited to repairing the backend prompt contract used by Codex CLI.

## Milestones

- [x] M1: Review the latest related task state and create this task package.
- [x] M2: Record BDD scenarios and RED expectation for the prompt repair.
- [x] M3: Implement the prompt repair with focused regression coverage.
- [x] M4: Run focused verification, update evidence, and mark the task completed.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordCodexCliImageParserTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed for the focused backend slice. `MesProBatchRecordCodexCliImageParser`
now sends the repaired Chinese structure-first prompt that explicitly asks
Codex CLI to convert the screenshot into a system report table and return only
schema-valid JSON. Focused regression coverage now protects that prompt
contract.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordCodexCliImageParserTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
