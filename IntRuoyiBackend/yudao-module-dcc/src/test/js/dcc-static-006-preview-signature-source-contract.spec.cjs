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
  /if\s*\(accessType == DccAccessTypeEnum\.PREVIEW\)\s*\{/,
  'preview binary resolution must have an explicit preview branch'
)
assert.doesNotMatch(
  binaryResolver,
  /isPendingPreviewStatus\(file\.getStatus\(\)\)[\s\S]{0,120}return file\.getOriginalFileId\(\);/,
  'pending approval preview must not directly return originalFileId because checked-in minor versions preserve the old historical original'
)
assert.match(
  binaryResolver,
  /isPendingPreviewStatus\(status\)[\s\S]{0,240}file\.getSourceFileId\(\) == null \? file\.getOriginalFileId\(\) : file\.getSourceFileId\(\)/,
  'pending approval preview must prefer current sourceFileId and use originalFileId only when legacy data has no sourceFileId'
)

assert.match(
  signatureEvidenceService,
  /String sourceFileHash = digestFile\(revision\.getSourceFileId\(\)\);/,
  'DCC signature evidence hashes the same current sourceFileId identity used by pending approval preview'
)
assert.match(
  queryServiceTest,
  /readPreviewFile_pendingCheckinRevisionReadsSourceBinaryWhenOriginalDiffers/,
  'JUnit regression must cover A/2 sourceFileId differing from A/1 originalFileId'
)

console.log('DCC-STATIC-006 preview/signature source contract passed')
