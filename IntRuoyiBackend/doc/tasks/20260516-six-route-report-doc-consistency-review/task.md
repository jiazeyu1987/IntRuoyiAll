# Task: Six-Route Report vs Doc Consistency Review

## Goal

Use six subagents to compare the fixed source document
`D:\ProjectPackage\Int\IntRuoyi\resource\批记录模板.doc`
against the generated report outputs for routes `A-F`, using visual page
rendering and screenshot evidence as the primary acceptance standard. Identify
image-display mismatches and apply scoped fixes until each route's generated
reports visually align with the source document as shown in the system.

## Scope

- Create this task package before any new production changes.
- Use six subagents, one per route `A-F`, and keep their findings isolated.
- Compare the source `.doc` with the generated system reports for each route.
- Use rendered source-document page images and actual report screenshots as the
  main evidence.
- Check image-display consistency first: layout, merge spans, title placement,
  row/column geometry, whitespace, overflow, truncation, and obvious visual
  compression artifacts.
- Treat count or textual equivalence as supporting evidence only, not the main
  acceptance gate.
- If mismatches are found, fix only the files directly responsible for the
  affected route or shared rendering path, then rerun verification.
- Do not touch unrelated dirty files already present in the repositories.

## Previous Task Check

- Previous backend task: `doc/tasks/20260516-report-management-six-route-recognition/task.md`
- Status before this task: completed.
- Impact: the six-route recognition delivery is available as the baseline input
  for this consistency-review task.

## Milestones

- [x] M1: Create the task package and confirm the previous backend task is complete.
- [x] M2: Capture BDD scenarios, collect baseline report outputs, and dispatch six subagents.
- [ ] M3: Review route-by-route comparison findings and implement any needed fixes.
- [ ] M4: Rerun route verification, record evidence, and mark the task completed.
- [ ] M5: Commit only the files produced by this task after verification passes.

## Expected Verification

- Source document page-image comparison evidence for routes `A-F`
- Focused backend tests for any changed routes
- Live route checks against `recognize-fixed?routeKey=A-F` as needed

## Current Status

Blocked and partially superseded for Route `B`. The second six-agent review pass
switched acceptance to real page-image comparison, and the remaining Route-B
generic page-title / first-screen layout work has now been moved into
`doc/tasks/20260518-batch-record-generic-page-layers-fix/` so it can proceed
under its own BDD/TDD evidence. The route status here is now:

- `A`: no Route-A recognizer defect found. The visible mismatch comes from the
  current screenshot/display chain still capturing JMReport designer chrome and
  extra blank canvas instead of a clean report view.
- `B`: same as `A`; no Route-B parser defect confirmed, and the visible gap is
  still attributed to the shared screenshot/display path.
- `C`: route-specific visual mismatch was found and patched. The recognizer now
  keeps the title/checklist header visible and compresses over-wide Route C
  grids before calibration. Focused Route C tests pass, but the latest code has
  not yet been live-rerun and re-screenshoted.
- `D`: route-specific visual mismatch is confirmed. The route now has focused
  tests for PDF-table extraction, but live regeneration is still blocked, first
  by encoding, then by PDF-table extraction timeout on the real sample.
- `E`: route-specific visual mismatch is confirmed. Even with structure-
  preserving rendering and multiple batching strategies, the route still fails
  live because the Codex image parser becomes unstable on certain real
  templates.
- `F`: no remaining Route-F recognizer visual defect was found in the image
  review after the earlier merge-preservation fix. Remaining visual noise still
  comes from the shared screenshot/display path rather than Route F logic.

The task remains open because:

- the shared screenshot/display chain still captures the designer chrome rather
  than a clean final report image for `A/B/F`
- `C` still needs one final live rerun plus screenshot confirmation after its latest
  visual fix outside the dedicated Route-B follow-up task
- `D` and `E` remain blocked by route-specific live-runtime failures
- the local backend runtime has not yet been stabilized after rebuilding the
  task's missing infrastructure endpoints (`23306` MySQL and `26379` Redis)

Latest repair state:

- Local backend auth and report-generation APIs are stable again in the rebuilt
  `23306` MySQL + `26379` Redis runtime.
- The shared screenshot/display blocker for `A/B/F` has been removed:
  `/jmreport/view` is rendering again, the screenshot helper now clips the
  active report canvas, and fresh clean-view screenshots have been recaptured
  for `A/B/C/D/F`.
- A dedicated DAO-level fix now prevents broken Redis token cache payloads from
  turning authenticated requests into `500`s when the database token record is
  still present.
- The remaining blocker is now concentrated in Route `E` only. Recent live
  outcomes in the repaired runtime were:
  - invalid CLI output shape caused by upstream `502` event-stream failures
  - low confidence (`0.39`) on a successful upstream run
  - timeout on a later retry even with a temporary local validation threshold
    of `0.35`

Latest generic-fix progress:

- Route `B` now has a shared title-resolution rule for standalone short
  template titles, so the recognizer no longer hard-codes page headers to only
  `产品信息` or `...工序生产记录`.
- The shared width budget for generic reports has been widened materially
  (`narrow/medium/dense`) so non-rough-wash process pages are allowed to use
  more of the browser page width instead of collapsing into the left half of
  the canvas.
- The shared process-page calibrator now treats “one long narrative cell plus a
  few short result/date cells” as a generic mixed row shape and gives the
  narrative cell extra width instead of squeezing it into near-vertical text.
- The route-B and layout test suites now isolate the missing external pilot DOC
  fixture by skipping only the sample-dependent assertions on machines where
  that file is absent, while still running the new generic rule regressions.

Latest source-contract change:

- The fixed-sample contract has now been explicitly changed by the user from
  the missing desktop `.doc` to
  `D:\ProjectPackage\Int\IntRuoyi\resource\批记录模板.doc`.
- The backend shared constants and fixed-route service path are being aligned
  to that new source location.
