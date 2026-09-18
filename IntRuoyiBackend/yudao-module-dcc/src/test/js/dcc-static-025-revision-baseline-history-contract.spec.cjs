const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..', '..', '..')
const servicePath = path.join(
  repoRoot,
  'yudao-module-dcc',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'dcc',
  'service',
  'file',
  'DccControlledFileQueryServiceImpl.java'
)
const historyVoPath = path.join(
  repoRoot,
  'yudao-module-dcc',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'dcc',
  'controller',
  'admin',
  'file',
  'vo',
  'DccControlledFileVersionHistoryRespVO.java'
)
const detailPagePath = path.join(
  repoRoot,
  '..',
  'IntRuoyiFronted',
  'src',
  'views',
  'dcc',
  'controlled-file',
  'detail',
  'index.vue'
)

const service = fs.readFileSync(servicePath, 'utf8')
const historyVo = fs.readFileSync(historyVoPath, 'utf8')
const detailPage = fs.readFileSync(detailPagePath, 'utf8')

function extractBetween(source, start, end) {
  const startIndex = source.indexOf(start)
  assert.notStrictEqual(startIndex, -1, `missing start anchor: ${start}`)
  const endIndex = source.indexOf(end, startIndex)
  assert.notStrictEqual(endIndex, -1, `missing end anchor: ${end}`)
  return source.slice(startIndex, endIndex)
}

const copyForCheckin = extractBetween(
  service,
  'private DccControlledFileDO copyForCheckin',
  'private void cleanupPreparedSourceIfNeeded'
)
assert(
  copyForCheckin.includes('.predecessorControlledFileId(file.getId())'),
  'check-in copy must keep direct predecessor identity'
)
assert(
  copyForCheckin.includes('.revisionBaseActiveControlledFileId(file.getRevisionBaseActiveControlledFileId())'),
  'check-in copy must inherit the immutable major-revision formal baseline from the source version'
)

const buildVersionHistory = extractBetween(
  service,
  'private List<DccControlledFileVersionHistoryRespVO> buildVersionHistory(Long userId, DccControlledFileDO file',
  'private List<DccControlledFileDistributionStatusRespVO> buildDistributionStatuses'
)
assert(
  buildVersionHistory.includes('respVO.setPredecessorControlledFileId(history.getPredecessorControlledFileId())'),
  'history response must project direct predecessor identity'
)
assert(
  buildVersionHistory.includes('respVO.setRevisionBaseActiveControlledFileId(history.getRevisionBaseActiveControlledFileId())'),
  'history response must project the stored revision baseline for each historical row'
)
assert(
  !/setRevisionBaseActiveControlledFileId\([^)]*currentActive/i.test(buildVersionHistory),
  'history response must not infer historical revision baseline from current ACTIVE state'
)

assert(
  historyVo.includes('private Long revisionBaseActiveControlledFileId;'),
  'history VO must expose revisionBaseActiveControlledFileId'
)
assert(
  detailPage.includes('version.revisionBaseActiveControlledFileId') &&
    detailPage.includes('创建时正式基线'),
  'frontend history trace must consume revisionBaseActiveControlledFileId when backend returns it'
)

console.log('DCC-STATIC-025 revision baseline history contract passed')
