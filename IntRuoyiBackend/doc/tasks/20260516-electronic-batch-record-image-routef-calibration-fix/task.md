# Task: Electronic Batch Record Image Route F Calibration Fix

## Goal

Repair the backend table-restoration path so the provided electronic batch-record screenshot can land as a report table whose merged cells and section layout stay close to the source form, with emphasis on the "精洗工序生产记录" process page.

## Scope

- Review the current electronic batch-record image/table tasks before changing code.
- Create this task package before new production edits.
- Record BDD scenarios and strict-TDD evidence for the structure defect.
- Change only the batch-record parsing/calibration code and focused regression tests needed for this screenshot table.
- Do not add fallback OCR, alternate parsers, or unrelated frontend/report-engine changes.

## Previous Task Check

- Related tasks:
  - `doc/tasks/20260516-electronic-batch-record-image-table-structure-fix/task.md`
  - `doc/tasks/20260516-electronic-batch-record-image-performance-optimization/task.md`
- Status before this task:
  - structure-fix task completed in its Route E scope;
  - performance-optimization task remains blocked by unrelated broader verification issues.
- Impact:
  - The remaining gap is now in downstream table calibration fidelity for the screenshot-derived process template layout.

## Milestones

- [x] M1: Review related task state and create this task package.
- [x] M2: Record BDD scenarios and RED expectation for the process-page structure defect.
- [x] M3: Implement the minimal calibration/test changes for the screenshot table layout.
- [x] M4: Run focused verification and update task evidence.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportLayoutCalibratorTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed for the focused calibrator slice. The screenshot process page now has regression coverage around preserved tall section labels, retained operation-grid hierarchy, and the fixed 92-row calibrated report layout.

## Blocker And Impact

- Blocker: none in the focused calibrator scope.
- Impact: focused backend verification passed; broader end-to-end screenshot fidelity beyond the calibrator still depends on upstream recognizer quality and should be validated separately if more visual parity is required.
