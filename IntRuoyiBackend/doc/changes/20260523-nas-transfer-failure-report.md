# Change Request：NAS转移失败文件写入文档

## Request Summary And Source

- Request: NAS 转移时如果某个文件失败，直接跳过并继续下一个文件；同时记录失败文件位置并写成文档
- Source: user request on 2026-05-23 in current delivery thread

## Current Baseline Reviewed

- Current NAS transfer backend already uses per-file failure aggregation and continues subsequent files
- Current response returns `failures[{ nasPath, stage, reason }]`
- Current system does not persist a standalone failure document

## Classification

- Requirement change

## Impact

- Product impact:
  - failed NAS transfer items will be persisted to a markdown report on the server workspace
- API impact:
  - `POST /admin-api/dcc/controlled-files/nas-transfer` adds failure report metadata fields
- Test impact:
  - add transfer-service tests for partial success + failure report generation
  - add report-writer unit test for markdown output

## Decision

- Accept

## Required Approvals

- User request is explicit

## Downstream Skill Reruns

- `backend-api-delivery`

## Blockers And Next Action

- Blocker:
  - none
- Next action:
  - generate markdown failure report for failed transfer items
  - return absolute report path to caller
