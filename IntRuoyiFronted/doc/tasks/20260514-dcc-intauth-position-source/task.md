# Task: DCC 岗位列表切换到 IntAuth 来源

## Goal

Make the DCC controlled-file position assignment page load its position master list from IntAuth quality-system position assignments, while preserving the DCC-specific assignment maintenance flow.

## Scope

- Inspect the current DCC position page, API bindings, and downstream usage in routes/categories.
- Change the frontend behavior so the DCC position list reflects the IntAuth-backed position source exposed by the local DCC backend.
- Keep the current DCC assignment maintenance interaction working against the local DCC save API unless backend contract changes require otherwise.
- Add targeted frontend regression coverage and task evidence.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260514-dcc-controlled-file-unhandled-api-error/task.md`
- Status before this task: blocked due to user reprioritization
- Impact: the unrelated unhandled API error task remains paused while this DCC 岗位来源 task is implemented

## Milestones

- [x] M1: Review the latest frontend task and explicitly block it before switching scope.
- [x] M2: Create this frontend task document before production code changes.
- [ ] M3: Record BDD scenarios and RED evidence for the current DCC position source mismatch.
- [ ] M4: Implement the minimal frontend changes needed for the IntAuth-backed position source behavior.
- [ ] M5: Run targeted verification, update evidence, and prepare a scoped frontend commit.

## Expected Verification

- The DCC 岗位分配 page renders positions returned by the backend after it switches to the IntAuth-backed source.
- Existing DCC pages that consume approval positions continue to render and use the updated position list.
- Frontend regression coverage proves the old source assumption before the fix and the updated behavior after the fix.

## Current Status

Blocked before RED evidence and implementation because the user redirected the active frontend scope to the electronic batch-record DOC-to-report implementation requirement.

## Blocker And Impact

- Blocker: user priority changed to the electronic batch-record DOC import and report-generation feature before this DCC frontend task entered RED evidence and implementation.
- Impact: the DCC IntAuth-backed position-source frontend change remains unavailable until this task is resumed.

## Completion Status

Blocked pending user reprioritization.
