# Bug Regression Evidence

## Bug Summary

DCC controlled-file detail displays generic `系统异常` while previewing an active `.docx` controlled file.

## Expected Behavior

The preview path should return a valid viewer configuration for supported `.docx` files, or a precise preview-unavailable response. It must not hide the root cause behind a generic system exception.

## Reproduction

- Screenshot path: DCC `文控中心 > 受控浏览 > 受控文件详情`.
- Visible file: `STM-PM-002（A 0）微粒污染检测操作规程.docx`.
- Visible symptom: preview area shows `系统异常`.

## Root Cause

The controlled preview metadata path treated a missing selected preview artifact record as `CONTROLLED_FILE_ACCESS_DENIED` by calling `resolveBinaryFileRecord(...)` after preview eligibility had already passed. In the viewer path this prevents the page from receiving a typed preview-unavailable reason and surfaces as the generic outer viewer error.

## Regression Test

`DccControlledFileQueryServiceTest#getPreviewMetadata_activeOfficeFileWithMissingPublishedArtifactReturnsUnavailableReason` covers an active `.docx` controlled file whose `publishedFileId` points to a missing infra file record. Expected result: `previewKind=OFFICE`, filename preserved as `STM-PM-002（A 0）微粒污染检测操作规程.docx`, `contentType=application/octet-stream`, precise `previewUnavailableReason`, and no OnlyOffice token issuance.

## RED Evidence

RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#getPreviewMetadata_activeOfficeFileWithMissingPublishedArtifactReturnsUnavailableReason" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BLOCKED, main workspace DCC target cleanup/compile could not reach Surefire because of Windows file IO state.

Blocked before behavior assertion. The focused Maven command could not reach the test phase because `yudao-module-dcc\target` entered a Windows file IO failure state:

- Reactor RED command: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#getPreviewMetadata_activeOfficeFileWithMissingPublishedArtifactReturnsUnavailableReason" "-Dsurefire.failIfNoSpecifiedTests=false" test`.
- DCC-only rerun failed at compile with `NoSuchFileException` under `yudao-module-dcc\target\classes`.
- Reactor rerun with `-Dmaven.compiler.useIncrementalCompilation=false` also failed at DCC compile with the same target-class missing pattern.
- Scoped `mvn -pl yudao-module-dcc clean` stalled in `WinNTFileSystem.delete0` and was stopped.

## GREEN Evidence

GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#getPreviewMetadata_activeOfficeFileWithMissingPublishedArtifactReturnsUnavailableReason" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS.

- Focused GREEN command in detached verification worktree: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#getPreviewMetadata_activeOfficeFileWithMissingPublishedArtifactReturnsUnavailableReason" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test`.
- Result: PASS / `BUILD SUCCESS`; tests run: 1, failures: 0, errors: 0.
- Adjacent preview metadata command: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#getPreviewMetadata_officeFileReturnsOnlyOfficeLink+getPreviewMetadata_officeFileWithoutOnlyOfficeConfig_returnsUnavailableReason+getPreviewMetadata_activeOfficeFileWithMissingPublishedArtifactReturnsUnavailableReason" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test`.
- Result: PASS / `BUILD SUCCESS`; tests run: 3, failures: 0, errors: 0.

## Verification

- Focused missing-artifact `.docx` preview metadata regression passed in the detached verification worktree.
- Adjacent preview metadata methods for configured OnlyOffice and missing OnlyOffice config also passed.
- Full `DccControlledFileQueryServiceTest` was attempted and failed on unrelated existing non-preview fixture/contract issues, so the focused preview tests are the task gate.

## Blockers

- Main workspace `IntRuoyiBackend\yudao-module-dcc\target` remains unsafe for cleanup while unrelated DCC Maven jobs may own it.
- The blocker was resolved for this task by using a task-owned detached verification worktree with an isolated `target`, then removing that worktree after verification.

## Risk And Scope

- Scope is limited to DCC controlled-file preview behavior and the closest affected test(s).
- No fallback, mock success, or swallowed exception is allowed.
- Runtime behavior is intentionally fail-fast/explicit: missing artifact records produce a preview-unavailable reason and do not switch to another artifact or issue a fake OnlyOffice document URL.

## Follow-Up

Main workspace `IntRuoyiBackend\yudao-module-dcc\target` still needs safe cleanup outside this task's verification path if future main-workspace Maven runs continue to stall. Do not stop unrelated DCC Maven jobs or delete shared target output while another task owns it.
