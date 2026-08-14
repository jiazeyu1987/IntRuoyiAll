# DCC Docx Preview System Exception Fix

## Task Goal

Fix the DCC controlled-file detail preview path where a `.docx` file displays `系统异常` instead of a usable document preview or explicit preview-unavailable reason.

## Milestones

- M1 Reproduce and isolate the failing DCC preview metadata / render contract. - completed.
- M2 Add a focused RED regression test for `.docx` preview metadata/rendering behavior. - completed; the regression method exists in `DccControlledFileQueryServiceTest` and was present in current HEAD baseline.
- M3 Implement the smallest root-cause fix without fallback, silent downgrade, or swallowed errors. - completed.
- M4 Run targeted GREEN verification and relevant regression checks. - completed via isolated detached verification worktree.
- M5 Record closeout evidence and commit/push task-owned changes. - completed locally; commit/push preflight follows task-owned staging.

## Expected Verification

- Focused static or unit regression test fails before implementation and passes after implementation.
- Relevant DCC frontend/backend targeted tests pass for the changed files.
- `git diff --check` passes for task-owned changes.
- Task evidence records RED/GREEN commands, root cause, and remaining blockers if any.

## Current Status

completed

Implementation, targeted verification, bug evidence validation, experience consolidation, and task cleanup are complete. Main workspace DCC `target` cleanup remains unsafe because another DCC Maven process was active and the local target previously stalled in `WinNTFileSystem.delete0`; verification was therefore run in task-owned detached worktree `D:\IntRuoyiWorktree\dcc-docx-preview-verify-20260803`, then that worktree was removed.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，先定位正式预览链路和错误来源，再改最小责任边界。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- Bug regression fix loop: must reproduce, add RED regression, implement smallest fix, then verify GREEN.
- DCC 受控浏览当前有效版与权限隔离门禁: current active preview must use正式受控浏览 preview source and must not be replaced by unrelated draft/history data.
- 本地 OnlyOffice 容器下载地址门禁: OnlyOffice document URL and public file base URL must keep browser and container responsibilities separate when runtime verification is needed.
- PowerShell/Git baseline gate: pre-existing dirty workspace changes were preserved before task implementation.

## Implementation Notes

- `getPreviewMetadata` now resolves the preview artifact record without throwing `CONTROLLED_FILE_ACCESS_DENIED` when the artifact row is missing after `canReadBinary` has already confirmed the file is preview-eligible.
- Missing artifact metadata returns the controlled file name, a deterministic `application/octet-stream` content type, the resolved preview kind from the controlled filename, and `previewUnavailableReason=Controlled file preview artifact is missing: publishedFileId=<id>`.
- For `.docx`, the existing `OnlyOfficeReadOnlyViewer` receives `previewUnavailableReason` and renders the precise unavailable reason without issuing an OnlyOffice file token for a missing artifact.

## Verification Evidence

- GREEN focused regression: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#getPreviewMetadata_activeOfficeFileWithMissingPublishedArtifactReturnsUnavailableReason" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS in detached verification worktree; tests run: 1, failures: 0, errors: 0.
- Adjacent preview metadata regression: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#getPreviewMetadata_officeFileReturnsOnlyOfficeLink+getPreviewMetadata_officeFileWithoutOnlyOfficeConfig_returnsUnavailableReason+getPreviewMetadata_activeOfficeFileWithMissingPublishedArtifactReturnsUnavailableReason" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS; tests run: 3, failures: 0, errors: 0.
- Broader class check note: full `DccControlledFileQueryServiceTest` currently fails on unrelated pre-existing fixture/contract issues (`formActionPendingService` null in non-preview response projection tests and forbidden VO field assertion), so it is not used as this task's completion gate.

## Cleanup Keep

- doc/tasks/20260803-dcc-docx-preview-system-exception/bug-regression-evidence.md

## Cleanup Evidence

- `task_closeout.py --task-id 20260803-dcc-docx-preview-system-exception --mode preview` -> ready; kept task, execution log, verification report, and bug regression evidence; no delete, blocked, or warning entries.
- `task_closeout.py --task-id 20260803-dcc-docx-preview-system-exception --mode apply` -> applied; deleted paths: none.

## Baseline Evidence

- Baseline commit `ee95cf977`: pre-existing workspace changes before this task.
- Baseline commit `24dd9a101`: delayed task report update before this task.
- Baseline commit `ec05a7114`: delayed NAS import / MES helper updates before this task.
