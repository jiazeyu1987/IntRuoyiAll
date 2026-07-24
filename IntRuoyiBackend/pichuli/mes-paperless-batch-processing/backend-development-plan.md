# MES Paperless Batch Processing Development Plan

> For agentic workers: use `subagent-driven-development` or `executing-plans` task-by-task.

**Goal:** build paperless batch-processing capability inside `IntRuoyi` with `MES` as the business entry, reusing existing `MES/BPM/Infra` where possible and adding a dedicated `MES` batch-record subdomain where necessary.

**Architecture:** split the solution into two tracks. Track A reuses existing structured forms in `MES` and `BPM`. Track B adds a new `MES` electronic batch-record subdomain for layout-preserving production records. Phase 1 is only the template-import slice. Execution, signoff, review, and export are later phases.

**Tech Stack:** `yudao-module-mes`, `yudao-module-bpm`, `yudao-module-infra`, Vue 3 admin frontend, MyBatis data layer, and a controlled `.doc/.docx` parser adapter.

---

## 1. Problem Statement

The current `IntRuoyi` system already supports:

- BPM dynamic form definition and variable submission
- structured MES business forms with create, update, save, and submit patterns
- work order, task, card, process, QC, and batch-trace master data
- file upload and storage

The current system does not provide:

- a layout-preserving batch-record template model
- a parser that turns one Word batch record into multiple reusable form templates
- an execution workspace for page-by-page batch-record filling
- record-level signoff, review, deviation, release, and export packaging

## 2. Pilot Sample

The Phase 1 pilot sample is:

`C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`

Observed structure:

- total top-level tables: `10`
- table 1: product information
- tables 2 to 10: operation-specific production record tables
- recurring fields include batch number, product specification, production basis, pre-check, requirement, result, operator/date, and reviewer/date

Conclusion:

- this sample is not a generic BPM form
- this sample must be treated as a batch-record template source
- Phase 1 must support direct parsing of this `.doc` file without requiring the operator to manually convert it first

## 3. Reuse Matrix

### Reusable as-is

- MES work orders, routes, tasks, cards, process content, calendars, and batch traceability
- MES QC templates and QC forms for structured quality data
- BPM dynamic forms and process variables for approval-oriented structured documents
- Infra file upload and file storage support

### Reusable with mapping

- product information blocks -> work order, batch, item, or process references
- SOP or SIP-like sections -> existing process content or product instruction modules
- standard inspection items -> QC templates or QC execution objects
- approval-type fields -> BPM variables

### New capability required

- `.doc/.docx` batch-record parsing
- one-file-to-multiple-template split rules
- batch-record template persistence
- layout JSON generation and preview
- later-phase execution, signoff, review, and export chain

## 4. Target Architecture

### 4.1 Entry model

Primary menu entry:

- `MES > Production > Electronic Batch Records`

Secondary links:

- production work order detail
- flow card detail
- production task detail
- batch traceability detail

### 4.2 Backend module boundary

New backend package root under `yudao-module-mes`:

- `controller.admin.pro.batchrecord`
- `service.pro.batchrecord`
- `dal.dataobject.pro.batchrecord`
- `dal.mysql.pro.batchrecord`

### 4.3 Phase structure

- Phase 1: template import and template CRUD
- Phase 2: execution workspace
- Phase 3: signoff and review
- Phase 4: deviation, release, export, and traceability

## 5. Phase 1 Scope

Phase 1 covers only:

- one button to choose a batch-processing document
- upload and parse one `.doc` file from the pilot sample
- keep `.docx` on the same parser contract
- split one source file into multiple batch-record form templates
- preview the parsed template list
- save selected templates
- update saved template base information
- delete saved templates

Phase 1 does not cover:

- execution instances
- signoff
- review
- deviation
- release
- export bundle
- OCR for scanned PDFs

## 6. Phase 1 Reuse Versus New Development

### Reusable by extending the current system

- MES menu, route, and permission registration
- upload button, file chooser, modal, and feedback interaction
- standard template list page and delete flow
- standard backend CRUD structure in `controller/service/mapper`
- existing MES process and product reference binding
- Infra file storage and file metadata

### Must be newly implemented

- `.doc/.docx` parser adapter
- `.doc` conversion/parse chain that runs inside the system import flow
- file-to-multiple-template split rules
- template layout JSON generation
- parse preview payload
- batch-record template data model

## 7. Phase 1 Data Model

Two tables are sufficient for the first slice.

### 7.1 `mes_pro_batch_record_template_import`

Purpose:

- record one import session
- store parse status
- store raw parse output for preview and troubleshooting

Suggested fields:

- `id`
- `import_no`
- `source_file_name`
- `source_extension`
- `source_file_id`
- `parse_status`
- `table_count`
- `selected_count`
- `raw_parse_json`
- `error_message`
- `remark`
- standard `creator/create_time/updater/update_time/deleted`

### 7.2 `mes_pro_batch_record_template`

Purpose:

- store one parsed table as one reusable batch-record template

Suggested fields:

- `id`
- `template_code`
- `template_name`
- `import_id`
- `sort`
- `status`
- `process_id`
- `product_name`
- `source_table_index`
- `table_title`
- `sheet_layout_json`
- `meta_json`
- `remark`
- standard `creator/create_time/updater/update_time/deleted`

Phase 1 hard rule:

- the pilot `.doc` must parse successfully without requiring operator-side pre-conversion
- if the parser internally converts `.doc`, that conversion is part of system behavior, not a manual prerequisite

## 8. Phase 1 API Surface

Minimum API set:

- `POST /mes/pro/batch-record-template/import/parse`
- `POST /mes/pro/batch-record-template/import/commit`
- `GET /mes/pro/batch-record-template/page`
- `GET /mes/pro/batch-record-template/get`
- `PUT /mes/pro/batch-record-template/update`
- `DELETE /mes/pro/batch-record-template/delete`

Design rules:

- `parse` returns preview data and does not create official template rows
- `commit` persists selected parsed tables as templates
- `update` in Phase 1 changes base information only; it does not introduce a full layout editor

## 9. Phase 1 Backend Class Inventory

Recommended package root:

- `cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord`
- `cn.iocoder.yudao.module.mes.service.pro.batchrecord`
- `cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord`
- `cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord`

Recommended classes:

### Controllers

- `MesProBatchRecordTemplateController`
- `MesProBatchRecordTemplateImportController`

### Request and response objects

- `MesProBatchRecordTemplatePageReqVO`
- `MesProBatchRecordTemplateSaveReqVO`
- `MesProBatchRecordTemplateRespVO`
- `MesProBatchRecordTemplateImportCommitReqVO`
- `MesProBatchRecordTemplateImportParseRespVO`
- `MesProBatchRecordTemplateImportRespVO`

### Data objects

- `MesProBatchRecordTemplateDO`
- `MesProBatchRecordTemplateImportDO`

### Mappers

- `MesProBatchRecordTemplateMapper`
- `MesProBatchRecordTemplateImportMapper`

### Services

- `MesProBatchRecordTemplateService`
- `MesProBatchRecordTemplateServiceImpl`
- `MesProBatchRecordTemplateImportService`
- `MesProBatchRecordTemplateImportServiceImpl`
- `MesProBatchRecordTemplateParser`
- `MesProBatchRecordTemplateCodeGenerator`
- optional helper classes for parser command, parser result, and process runner

## 10. Parser Architecture

Phase 1 parser principles:

- parser contract must be identical for `.doc` and `.docx`
- parser should focus only on file-to-structure conversion
- business persistence and template code generation stay outside the parser

Recommended parser responsibilities:

- validate source file availability
- identify extension
- handle `.doc` through an internal conversion/parse chain
- read top-level tables
- emit normalized table, row, and cell structure
- output raw JSON for preview and troubleshooting

Recommended service responsibilities:

- save file
- create import record
- call parser
- validate parse result
- update import status
- build preview response
- commit selected templates

## 11. Phase 1 Implementation Order

Recommended implementation order:

1. import and template tables
2. DO and mapper layer
3. template CRUD service and controller
4. import service skeleton
5. parser adapter and `.doc` chain
6. parse preview endpoint
7. commit endpoint
8. targeted tests with the pilot sample

This order keeps the highest-risk parsing work isolated while allowing the management shell to come up early.

## 12. Code Readiness Decision

For the current target feature, the answer is `YES`.

Ready-to-code scope:

- import button
- `.doc` upload
- parse preview
- save selected templates
- template list
- template base-information update
- template delete

Not yet in code-start scope:

- execution instances
- signoff and review
- release
- export bundle

Main remaining risk:

- `.doc` conversion and parse-chain complexity remains the main technical risk, but the Phase 1 slice is now specific enough to begin coding.
