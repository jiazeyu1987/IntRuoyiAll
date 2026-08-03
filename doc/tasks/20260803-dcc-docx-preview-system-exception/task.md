# DCC Docx Preview System Exception Fix

## Task Goal

Fix the DCC controlled-file detail preview path where a `.docx` file displays `系统异常` instead of a usable document preview or explicit preview-unavailable reason.

## Milestones

- M1 Reproduce and isolate the failing DCC preview metadata / render contract.
- M2 Add a focused RED regression test for `.docx` preview metadata/rendering behavior.
- M3 Implement the smallest root-cause fix without fallback, silent downgrade, or swallowed errors.
- M4 Run targeted GREEN verification and relevant regression checks.
- M5 Record closeout evidence and commit/push task-owned changes.

## Expected Verification

- Focused static or unit regression test fails before implementation and passes after implementation.
- Relevant DCC frontend/backend targeted tests pass for the changed files.
- `git diff --check` passes for task-owned changes.
- Task evidence records RED/GREEN commands, root cause, and remaining blockers if any.

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，先定位正式预览链路和错误来源，再改最小责任边界。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- Bug regression fix loop: must reproduce, add RED regression, implement smallest fix, then verify GREEN.
- DCC 受控浏览当前有效版与权限隔离门禁: current active preview must use正式受控浏览 preview source and must not be replaced by unrelated draft/history data.
- 本地 OnlyOffice 容器下载地址门禁: OnlyOffice document URL and public file base URL must keep browser and container responsibilities separate when runtime verification is needed.
- PowerShell/Git baseline gate: pre-existing dirty workspace changes were preserved before task implementation.

## Baseline Evidence

- Baseline commit `ee95cf977`: pre-existing workspace changes before this task.
- Baseline commit `24dd9a101`: delayed task report update before this task.
- Baseline commit `ec05a7114`: delayed NAS import / MES helper updates before this task.
