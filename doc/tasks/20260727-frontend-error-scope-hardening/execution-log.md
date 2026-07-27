# Execution Log

## User Intent

- Continue optimizing the system after the static audit found additional frontend pages where local failures can pollute a page-level load error.

## BDD

- `BDD: deferred batch detail failure stays auxiliary -> Given the batch execution primary detail is visible, When deferred workbench or review data fails, Then the primary detail remains visible and the real error appears in an auxiliary section instead of the page load alert.`
- `BDD: directory child failure stays on the affected row -> Given the root directory tree is visible, When one lazy child request fails, Then the root tree remains valid and the affected row exposes the real child-load error without setting or clearing the page load error.`
- `BDD: field audit actions do not become list failures -> Given field-audit list or detail content is visible, When verify or export fails, Then the error appears as an action error and does not overwrite the primary load error.`
- `BDD: domain trace verification does not become detail-load failure -> Given domain trace detail is visible, When verification fails, Then the detail remains visible and the verification error is action-scoped.`
- `BDD: delivery and validation secondary failures stay local -> Given project/package primary lists are visible, When a selected-row panel or create/evaluate action fails, Then the real error appears in that panel or action scope and does not overwrite the primary list error.`

## Baseline Evidence

- `git status --short --branch` -> `int_main...origin/int_main [ahead 2]` with unrelated concurrent changes.
- `git diff -- IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue` -> one unrelated special-node filler display hunk; no overlap with the planned error-scope changes.

## TDD Evidence

- `RED: node tests/e2e/frontend-error-scope-hardening-static.spec.js -> FAIL, expected reason: loadBatchDetailSecondaryData still assigns the page-level loadError after primary detail can render.`

## Current Status

- in_progress

## Blockers

- None at task start.
