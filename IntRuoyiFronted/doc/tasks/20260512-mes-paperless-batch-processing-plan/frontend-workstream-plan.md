# MES Paperless Batch Processing Frontend Workstream Plan

## Goal

Define the frontend work needed for paperless batch processing in `IntRuoyi`, with `MES` as the main entry and `BPM` or existing MES pages reused only where they fit.

## 1. Frontend Boundary

The frontend should not force every recognized document into one generic editor.

Two tracks are required:

- Track A: reuse existing `MES` and `BPM` pages for structured forms
- Track B: build a new `MES` electronic batch-record workspace for layout-preserving production records

The PP pressure pump production record sample belongs to Track B.

## 2. Reusable Frontend Surfaces

### Reuse directly

- `BPM` dynamic form designer and process start/detail pages for approval-oriented structured forms
- `MES` existing forms for SOP, SIP, process content, QC templates, IQC, IPQC, OQC, and RQC
- `MES` work order, task, card, and batch traceability pages as navigation entry points

### Do not reuse as the primary editor

- `BPM` dynamic form editor for multi-table batch production records
- generic current MES forms that only edit fixed business objects

These existing surfaces can host links into the new workspace, but they are not enough to represent a paper production record template and execution flow.

## 3. New Frontend Entry Points

Primary menu entry:

- `MES > Production > Electronic Batch Records`

Secondary links:

- production work order detail
- flow card detail
- production task detail
- batch traceability detail

## 4. Phase 1 Frontend Scope

Phase 1 covers only:

- the new menu entry
- template list page
- import button
- upload dialog
- parse preview modal
- template detail view
- template base-information update
- template delete

Phase 1 does not cover:

- execution list
- execution editor
- signoff UI
- export UI

## 5. Phase 1 Pages and Components

Create a page group under:

- `src/views/mes/pro/batchrecord/`

Minimum Phase 1 pages and components:

- `index.vue`: template management shell
- `TemplateList.vue`: template list, status, import action
- `ImportDialog.vue`: file chooser and parse trigger
- `ParsePreviewDialog.vue`: parsed-table preview and save selection
- `TemplateDetailDialog.vue`: read-only template structure preview
- `TemplateForm.vue`: base-information edit form

Later-phase pages:

- `ExecutionList.vue`
- `ExecutionEditor.vue`
- `SignoffPanel.vue`
- `ExportPanel.vue`

## 6. Phase 1 User Flow

The Phase 1 user flow should be:

1. open `Electronic Batch Records`
2. upload the pilot `.doc` sample
3. receive parse preview in a modal
4. choose which parsed tables to save
5. save selected parsed tables as template rows
6. return to template list
7. open one template detail view
8. update template base information
9. delete one template

This is the smallest frontend slice that matches the current Phase 1 backend scope.

## 7. UI Principles

The workspace should follow the current MES and IntPP production-order-list style:

- dense and operational, not marketing-like
- scanning-friendly tables and status bars
- restrained color with clear state signals
- no nested card-on-card layout
- stable dimensions for toolbars, dialogs, and navigation controls

## 8. Later Frontend Phases

### Phase 2: Execution shell

- execution list
- work-order and batch binding
- page navigator shell

### Phase 3: Execution editor

- field editing
- save and reopen flow

### Phase 4: Signoff and review

- operator or reviewer actions
- sign status
- review feedback

### Phase 5: Export and traceability

- export action/result
- traceability links
- evidence panel

## 9. Open Questions

- whether the future execution editor should be grid-first only, or support document-style preview from day one
- whether signoff UI should be embedded in the editor sidebar or opened as a dedicated drawer or modal
- whether export feedback should stay inside the workspace or also surface on batch traceability detail

## 10. Code Readiness Decision

For the current target feature, the answer is `YES`.

Ready-to-code frontend scope:

- menu entry
- upload dialog
- parse preview modal
- template list
- template detail preview
- template base-information update
- template delete

Not yet in frontend code-start scope:

- execution UI
- signoff UI
- export UI
