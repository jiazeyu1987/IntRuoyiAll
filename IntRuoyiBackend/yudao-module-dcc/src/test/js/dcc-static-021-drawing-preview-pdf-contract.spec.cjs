const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')

const extract = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker`)
  return source.slice(start, end)
}

const queryService = read('src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java')
const signatureEvidenceService = read('src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileSignatureEvidenceServiceImpl.java')
const queryServiceTest = read('src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java')

const binaryResolver = extract(
  queryService,
  'private Long resolveBinaryFileId',
  'private FileDO resolveBinaryFileRecord',
  'controlled file binary resolver'
)
assert.match(
  binaryResolver,
  /resolveCurrentRevisionPreviewFileId\(file,\s*file\.getSourceFileId\(\)\)/,
  'WORKING preview must route through the current revision preview resolver'
)
assert.match(
  binaryResolver,
  /resolveCurrentRevisionPreviewFileId\(file,\s*[\r\n\s]*file\.getSourceFileId\(\) == null \? file\.getOriginalFileId\(\) : file\.getSourceFileId\(\)\)/,
  'pending/rejected preview must route through the current revision preview resolver with legacy source fallback'
)
assert.doesNotMatch(
  binaryResolver,
  /DccControlledFileStatusEnum\.WORKING[\s\S]{0,160}return file\.getSourceFileId\(\);/,
  'WORKING drawing preview must not directly return CAD sourceFileId'
)

const currentPreviewResolver = extract(
  queryService,
  'private Long resolveCurrentRevisionPreviewFileId',
  'private FileDO resolveBinaryFileRecord',
  'current revision preview resolver'
)
assert.match(
  currentPreviewResolver,
  /fileMapper\.selectById\(currentSourceFileId\)/,
  'current preview resolver must inspect the current source file record before choosing preview artifact'
)
assert.match(
  currentPreviewResolver,
  /DccControlledFileUploadTypePolicy\.isDrawingSourceName\(sourceFile\.getName\(\)\)[\s\S]{0,240}file\.getDrawingPdfFileId\(\) == null[\s\S]{0,160}CONTROLLED_FILE_DRAWING_PDF_REQUIRED/,
  'drawing source preview must explicitly reject a missing current paired PDF'
)
assert.match(
  currentPreviewResolver,
  /DccControlledFileUploadTypePolicy\.isDrawingSourceName\(sourceFile\.getName\(\)\)[\s\S]{0,320}return file\.getDrawingPdfFileId\(\);/,
  'drawing source preview must return the current version drawingPdfFileId'
)

const checkinFlow = extract(
  queryService,
  'DccWindchillVersionNumber nextVersion',
  'private DccControlledFileDO copyForCheckin',
  'checkin flow'
)
assert.match(
  checkinFlow,
  /Long checkinDrawingPdfFileId = resolveCheckinDrawingPdfFileId\(file,\s*preparedSource\.sourceFileId\(\),\s*hasUpload,\s*sourceUpload\);/,
  'check-in must derive the next drawing PDF binding before copying the next version'
)

const checkinCopy = extract(
  queryService,
  'private DccControlledFileDO copyForCheckin',
  'private void cleanupPreparedSourceIfNeeded',
  'checkin copy'
)
assert.match(
  checkinCopy,
  /\.drawingPdfFileId\(drawingPdfFileId\)/,
  'check-in must copy only the derived current drawing PDF binding'
)
assert.doesNotMatch(
  checkinCopy,
  /\.drawingPdfFileId\(file\.getDrawingPdfFileId\(\)\)/,
  'check-in must not blindly copy an old drawing PDF onto a replaced drawing source'
)

assert.match(
  signatureEvidenceService,
  /String sourceFileHash = digestFile\(revision\.getSourceFileId\(\)\);/,
  'signature evidence must continue hashing the sourceFileId, not the drawing preview PDF'
)
assert.match(
  queryServiceTest,
  /readPreviewFile_workingDrawingRequesterReadsCurrentDrawingPdfBinary/,
  'JUnit regression must cover WORKING drawing preview reading the paired PDF'
)
assert.match(
  queryServiceTest,
  /checkinDrawingSourceWithoutCurrentPdfRejectsBeforeCopyingOldPdf/,
  'JUnit regression must cover replacement drawing source without current PDF rejection'
)

console.log('DCC-STATIC-021 drawing preview PDF contract passed')
