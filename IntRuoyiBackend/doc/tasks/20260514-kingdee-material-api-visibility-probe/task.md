# Task: Probe Kingdee material API visibility

## Goal

Verify why the local Kingdee material sync does not return some PTCA-related material codes that are visible in the Kingdee UI, and identify whether the mismatch comes from API query conditions or account/data-visibility differences.

## Scope

- Reuse the current local Kingdee API account and query flow.
- Probe the real `BD_MATERIAL` API with targeted PTCA material codes.
- Compare API hits against the current local ERP/MES synced data.
- Record BDD and investigation evidence. Do not change sync behavior in this task unless a concrete fix is identified and requested.

## Milestones

- [x] M1: Previous backend task reviewed and confirmed completed before starting.
- [x] M2: Backend task directory and initial task document created before substantial work.
- [x] M3: Record investigation BDD and RED evidence for the mismatch.
- [x] M4: Run targeted Kingdee API probes and collect exact results.
- [x] M5: Update evidence and conclude the root-cause direction.

## Expected Verification

- Real Kingdee API probe results for representative PTCA material codes.
- Clear statement whether the API can see those codes under the current configured account.
- Clear statement whether the existing sync query shape is the cause.

## Current Status

Completed.
