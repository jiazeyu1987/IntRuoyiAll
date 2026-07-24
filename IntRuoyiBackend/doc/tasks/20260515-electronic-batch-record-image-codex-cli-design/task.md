# Task: Electronic Batch Record Image Import With Codex CLI Correction Design

## Goal

Produce an implementation-ready design for converting a production-record image into an electronic batch-record template, using OCR or layout analysis to create a draft and `Codex CLI` to correct the structure before the template is committed or published as a report surface.

## Scope

- Check the latest backend task document status before starting this design task.
- Create the task document and execution log before writing design artifacts.
- Document the recommended frontend entry, backend APIs, data model, `Codex CLI` correction contract, configuration, security, and failure behavior.
- Keep the design fail-fast. Do not introduce fallback, silent downgrade, or hidden manual-review branches.
- Do not modify production code in this task.

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-workorder-erp-billno-code/task.md`
- Status before this task: completed.
- Impact: no unfinished backend task blocks this design work.

## Milestones

- [x] M1: Confirm the previous backend task is completed and create this task document.
- [x] M2: Record BDD scenarios and design-only execution notes.
- [x] M3: Inspect current electronic batch-record, report, parser, and CLI boundaries.
- [x] M4: Produce frontend, backend/API, data-model, and config/security/deployment design docs.
- [x] M5: Verify the design artifacts exist with the required section structure and record the final conclusion.

## Expected Verification

- `Get-ChildItem doc/tasks/20260515-electronic-batch-record-image-codex-cli-design/*.md`
- `rg -n "Purpose and Scope|Evidence Reviewed|Open Questions|Design Blockers" doc/tasks/20260515-electronic-batch-record-image-codex-cli-design`

## Current Status

Completed. The task now contains scoped design artifacts for a fail-fast image-import pipeline that uses OCR or layout analysis to create a draft, `Codex CLI` to correct the layout JSON, and the existing MES batch-record template model to persist the final template candidates.

## Blocker And Impact

- Blocker 1: the current report-designer list page is an iframe that loads Jimu's `/jmreport/list`, so the red-box toolbar area is not owned by local Vue code.
- Impact 1: the first implementation should not try to inject a new button into Jimu's internal toolbar. The controlled entry should remain in the local MES page first.

- Blocker 2: the current frontend page `yudao-ui-admin-vue3/src/views/mes/pro/batchrecordtemplate/index.vue` uses `batchrecordreport` APIs, while the backend already contains a separate `batchrecord` template-import model and endpoints.
- Impact 2: the image-import implementation must first align the page with the `batchrecord` template-import contract instead of stacking another feature onto the report-only API.

- Blocker 3: the repository currently has no selected OCR or table-layout engine dependency for image sources.
- Impact 3: implementation cannot start safely until the OCR/layout analyzer contract is explicitly chosen.

- Blocker 4: the existing `CodexCliChatModel` is a plain-text chat wrapper and not yet a strict image-correction adapter with schema enforcement.
- Impact 4: implementation needs a dedicated `Codex CLI` correction service that validates structured JSON output before accepting it.

## Final Verification Result

- Confirmed the previous backend task is completed.
- Confirmed the design task directory contains:
  - `task.md`
  - `execution-log.md`
  - `frontend-design.md`
  - `backend-api-design.md`
  - `data-model.md`
  - `config-security-deployment.md`
- Confirmed each design artifact contains the required section structure.

## Completion Status

Completed. No production code changes were made in this task.
