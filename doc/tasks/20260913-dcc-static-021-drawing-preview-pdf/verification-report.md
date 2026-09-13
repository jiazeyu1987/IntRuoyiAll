# DCC-STATIC-021 Verification Report

## Summary

- Result: PASS for static code logic and targeted backend regression scope.
- Task: DCC-STATIC-021 drawing current-version preview must select the current paired `drawingPdfFileId`, while signature evidence keeps the source-file association.
- Scope: `DccControlledFileQueryServiceImpl`, `DccControlledFileQueryServiceTest`, and a DCC-STATIC-021 static contract.
- Local int_main fusion: fast-forwarded from `cd376a2a2` to `b80fd2655` after 2026-09-14 user authorization.

## Bug

Engineering drawing WORKING and pending approval previews selected the CAD/SolidWorks `sourceFileId` instead of the current paired PDF, so the protected preview path could not display the uploaded drawing even though the upload flow requires a paired PDF.

## Expected

- WORKING and pending approval drawing previews return the current version `drawingPdfFileId`.
- Signature evidence continues hashing and associating `sourceFileId`.
- Replacing a drawing source without a current paired PDF is rejected before a new version can copy or reuse an old PDF.

## Reproduction

- `node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-021-drawing-preview-pdf-contract.spec.cjs`
- `mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryServiceTest#readPreviewFile_workingDrawingRequesterReadsCurrentDrawingPdfBinary+getPreviewMetadata_workingDrawingWithoutCurrentPdfRejectsBeforeSourcePreview+checkinDrawingSourceWithoutCurrentPdfRejectsBeforeCopyingOldPdf" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

`resolveBinaryFileId` treated WORKING, rejected, and pending approval previews as source-file previews and returned `sourceFileId` directly. The check-in copy path also carried the previous row's `drawingPdfFileId` into the next version, so a replaced drawing source could have retained an old paired PDF.

## RED

- RED: `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-021-drawing-preview-pdf-contract.spec.cjs` -> FAIL, expected reason: static contract could not find `resolveCurrentRevisionPreviewFileId`; old `resolveBinaryFileId` still returned `sourceFileId` directly for WORKING preview.
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryServiceTest#readPreviewFile_workingDrawingRequesterReadsCurrentDrawingPdfBinary+getPreviewMetadata_workingDrawingWithoutCurrentPdfRejectsBeforeSourcePreview+checkinDrawingSourceWithoutCurrentPdfRejectsBeforeCopyingOldPdf" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 3 targeted regressions exposed CAD source selection, missing PDF not rejected, and old check-in behavior.

## GREEN

- GREEN: `node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-021-drawing-preview-pdf-contract.spec.cjs` -> PASS, `DCC-STATIC-021 drawing preview PDF contract passed`.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryServiceTest#readPreviewFile_workingDrawingRequesterReadsCurrentDrawingPdfBinary+getPreviewMetadata_workingDrawingWithoutCurrentPdfRejectsBeforeSourcePreview+checkinDrawingSourceWithoutCurrentPdfRejectsBeforeCopyingOldPdf" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`; reactor `BUILD SUCCESS`.
- GREEN: post-merge local `int_main` static contract -> PASS, `DCC-STATIC-021 drawing preview PDF contract passed`.
- GREEN: post-merge local `int_main` bug regression evidence validator -> PASS, `Bug regression evidence is valid`.
- GREEN: post-merge local `int_main` targeted Maven regression -> PASS, `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`; reactor `BUILD SUCCESS`.
- CLEANUP_APPLY: `task_closeout.py --mode apply --worktree-closeout off` -> PASS, delete `<none>`.

## Verification

- Static contract verifies WORKING and pending preview paths route through the current revision preview resolver.
- Unit tests verify WORKING drawing preview reads the paired PDF binary, missing current PDF rejects before source preview, and drawing source check-in without current PDF rejects before copying old PDF.
- Maven target compiled the affected DCC module and required upstream modules as part of the test reactor.
- Shared bug file now marks only DCC-STATIC-021 as `FIXED_STATIC_VERIFIED` for this task; DCC-STATIC-017至020及024至027 remain outside this task's implementation scope unless already separately fixed by other tasks.
- Local `int_main` post-merge branch runtime port guard passed for `int_main/int_main`, frontend `8081`, backend `48081`.
- Not run by request: E2E, service start/restart, database writes, remote operations.

## Blockers

- None for static and targeted backend verification.
- None for local `int_main` fusion.
- Remote push remains out of scope until explicitly authorized.
