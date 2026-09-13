# DCC-STATIC-021 Bug Regression Evidence

## Bug

WORKING and pending approval previews for engineering drawings selected the CAD/SolidWorks `sourceFileId` instead of the paired PDF, causing protected preview to fail for files that cannot be rendered online.

## Expected

Drawing current-version preview must choose the current version `drawingPdfFileId`; signature evidence must keep the source-file association; replacing a drawing source without a current paired PDF must fail fast instead of using the CAD source or reusing an old PDF.

## Reproduction

- `node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-021-drawing-preview-pdf-contract.spec.cjs`
- `mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryServiceTest#readPreviewFile_workingDrawingRequesterReadsCurrentDrawingPdfBinary+getPreviewMetadata_workingDrawingWithoutCurrentPdfRejectsBeforeSourcePreview+checkinDrawingSourceWithoutCurrentPdfRejectsBeforeCopyingOldPdf" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

The binary resolver returned `sourceFileId` directly for WORKING and pending approval previews, and the check-in copy path copied the previous `drawingPdfFileId` without deriving a current-version drawing PDF binding.

## RED

- RED: `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-021-drawing-preview-pdf-contract.spec.cjs` -> FAIL, expected reason: missing `resolveCurrentRevisionPreviewFileId` and direct WORKING `sourceFileId` return.
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryServiceTest#readPreviewFile_workingDrawingRequesterReadsCurrentDrawingPdfBinary+getPreviewMetadata_workingDrawingWithoutCurrentPdfRejectsBeforeSourcePreview+checkinDrawingSourceWithoutCurrentPdfRejectsBeforeCopyingOldPdf" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: targeted regressions reproduced CAD source preview and old PDF reuse risk.

## GREEN

- GREEN: `node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-021-drawing-preview-pdf-contract.spec.cjs` -> PASS.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryServiceTest#readPreviewFile_workingDrawingRequesterReadsCurrentDrawingPdfBinary+getPreviewMetadata_workingDrawingWithoutCurrentPdfRejectsBeforeSourcePreview+checkinDrawingSourceWithoutCurrentPdfRejectsBeforeCopyingOldPdf" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3/3 targeted tests passed.

## Verification

Static and targeted backend tests now prove current drawing previews use `drawingPdfFileId`, missing current PDFs are rejected, and DCC signature evidence still hashes `sourceFileId`.

## Blockers

None for the requested verification scope.
