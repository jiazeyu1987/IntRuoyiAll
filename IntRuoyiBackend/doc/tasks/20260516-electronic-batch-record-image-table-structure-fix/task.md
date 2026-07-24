# Task: Electronic Batch Record Image Table Structure Fix

## Goal

Adjust the existing image-to-report recognition path so the provided electronic batch-record screenshot is restored into a system report table with structure closer to the source image, especially for merged cells, section layout, and large product-information blocks.

## Scope

- Check the latest electronic batch-record report task status before changing code.
- Create this task package before production code changes.
- Record BDD scenarios and RED expectation for the current structure mismatch.
- Change only the image-recognition prompt and Route E image rendering/batching behavior needed for better table reconstruction.
- Add or update focused regression coverage for the changed structure contract.
- Do not add fallback OCR engines, alternate parsers, or frontend changes.

## Previous Task Check

- Previous backend task: `doc/tasks/20260516-electronic-batch-record-image-performance-optimization/task.md`
- Status before this task: in progress but blocked by unrelated compile issues for its verification/commit step.
- Impact: the prior optimization findings remain usable context, and this task is limited to improving the screenshot-to-table structure quality.

## Milestones

- [x] M1: Review the latest related task state and create this task package.
- [x] M2: Record BDD scenarios and RED expectation for the current structure mismatch.
- [x] M3: Implement the minimal Route E / image-parser changes for better table reconstruction.
- [x] M4: Run focused verification and update task evidence.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteERecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed for the focused backend slice. Route E now batches `5` rendered templates per recognition image again, keeps anti-aliased batch rendering, and uses a stronger structure-first prompt so the screenshot path can preserve large-table boundaries and merged-cell intent more faithfully.

## Blocker And Impact

- Blocker: no blocker in the focused Route E test scope.
- Impact: focused Maven verification passed, but live screenshot quality should still be judged against the real backend import path if further precision tuning is needed.
