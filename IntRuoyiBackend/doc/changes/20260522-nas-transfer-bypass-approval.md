# Change Request：NAS转移不走审批

## Request Summary And Source

- Request: `NAS管理` 中发起的 NAS 目录转移可以不走审批
- Source: user request on 2026-05-22 in current delivery thread

## Current Baseline Reviewed

- Active backend task:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-dcc-nas-transfer-controlled-files-backend\task.md`
- Current implementation baseline:
  - NAS transfer reuses `DccControlledFileWorkflowService.submitControlledFile(...)`
  - Real runtime failure has advanced to approval-route prerequisite:
    - `Approval position runtime mapping failed: 编制人直接主管 requires the submitter to have a direct manager in IntAuth`
- Normal manual upload path still enters BPM approval.

## Classification

- Requirement change
- Scope change inside the active NAS transfer task

## Impact

- Product impact:
  - NAS transfer semantics change from “submit for approval” to “directly create active/training-gated controlled revision”
- Design impact:
  - NAS transfer must stop depending on BPM route resolution and IntAuth submitter-manager mapping
  - Manual upload path remains unchanged
- Data impact:
  - Imported controlled files must still create normal DCC master/revision records
  - Distribution / training / stamped published artifacts must still follow existing finalization rules
- API impact:
  - `POST /admin-api/dcc/controlled-files/nas-transfer` response shape stays unchanged
  - No frontend contract change required
- Test impact:
  - Add backend tests for no-approval workflow path and direct finalization path
  - Re-run NAS transfer service tests and workflow/finalization regression tests
- Release / operations impact:
  - Removes current IntAuth approval-route runtime blocker for NAS transfer
  - OnlyOffice runtime blocker remains unrelated

## Decision

- Accept

## Required Approvals

- User approval already provided via direct request: `可以不走审批`

## Downstream Skill Reruns

- `backend-api-delivery`

## Blockers And Next Action

- Blocker:
  - local OnlyOffice service is still unavailable for browser-side Office preview verification
- Next action:
  - accepted change has been implemented
  - NAS transfer now bypasses approval and skips post-release distribution/training governance
  - real transfer on `1. QMS documents/PD可编辑` verified with `createdFileCount=4` and `failedFileCount=0`
