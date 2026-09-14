const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../../../../..')
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8')

const sliceMethod = (source, signature) => {
  const startAt = source.indexOf(signature)
  assert.notEqual(startAt, -1, `missing method anchor: ${signature}`)
  const bodyStart = source.indexOf('{', startAt)
  assert.notEqual(bodyStart, -1, `missing method body: ${signature}`)
  let depth = 0
  for (let i = bodyStart; i < source.length; i += 1) {
    const char = source[i]
    if (char === '{') {
      depth += 1
    } else if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(startAt, i + 1)
      }
    }
  }
  assert.fail(`unterminated method body: ${signature}`)
}

const sliceBetween = (source, start, end) => {
  const startAt = source.indexOf(start)
  assert.notEqual(startAt, -1, `missing start anchor: ${start}`)
  const endAt = source.indexOf(end, startAt + start.length)
  assert.notEqual(endAt, -1, `missing end anchor: ${end}`)
  return source.slice(startAt, endAt)
}

const service = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java'
)
const saveVersion = sliceMethod(
  service,
  'public MesQaCommonRegulationSetRespVO.Version saveCommonRegulationSetVersion('
)
assert(
  saveVersion.includes('assertCommonRegulationSetVersionMutable(version)'),
  'EDHR-STATIC-001: save must reject non-draft common set versions before update/member replacement'
)
assert(
  saveVersion.indexOf('assertCommonRegulationSetVersionMutable(version)') <
    saveVersion.indexOf('commonRegulationSetVersionMapper.updateById'),
  'EDHR-STATIC-001: mutability check must happen before common set version update'
)
assert(
  saveVersion.indexOf('assertCommonRegulationSetVersionMutable(version)') <
    saveVersion.indexOf('replaceCommonRegulationSetVersionMembers'),
  'EDHR-STATIC-001: mutability check must happen before deleting/rebuilding members'
)

const mutable = sliceMethod(
  service,
  'private void assertCommonRegulationSetVersionMutable(MesQaCommonRegulationSetVersionDO version)'
)
assert(
  mutable.includes('STATUS_DRAFT') && mutable.includes('!Objects.equals'),
  'EDHR-STATIC-001: only DRAFT common set versions may be changed in place'
)

const deleteVersion = sliceMethod(service, 'public void deleteCommonRegulationSetVersion(Long setVersionId)')
assert(
  deleteVersion.includes('assertCommonRegulationSetVersionMutable(version)'),
  'EDHR-STATIC-001: delete must reject non-draft common set versions'
)
assert(
  deleteVersion.indexOf('assertCommonRegulationSetVersionMutable(version)') <
    deleteVersion.indexOf('commonRegulationSetVersionMemberMapper.deleteBySetVersionId'),
  'EDHR-STATIC-001: delete mutability check must happen before member physical delete'
)

const page = read('IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue')
const canEdit = sliceBetween(
  page,
  'const canEditCommonSetVersion = (version?: QaCommonRegulationSetVO',
  'const openSelectedCommonRegulationSetVersionDialog'
)
assert(
  canEdit.includes("version?.lifecycleStatus === 'DRAFT'"),
  'EDHR-STATIC-001: frontend editability must be limited to draft common set versions'
)
const versionTable = sliceBetween(
  page,
  'data-qa-common-versions',
  'data-qa-common-items'
)
assert(
  versionTable.includes(':disabled="!canEditCommonSetVersion(row)"'),
  'EDHR-STATIC-001: version table edit/delete buttons must be disabled outside draft status'
)
const documentMaintain = sliceBetween(
  page,
  'data-qa-common-set-document-maintain',
  '@click="openSelectedCommonRegulationSetVersionDialog"'
)
assert(
  documentMaintain.includes('!canEditCommonSetVersion(selectedCommonRegulationSetVersionPreview)'),
  'EDHR-STATIC-001: document composition maintain entry must also be disabled outside draft status'
)
const openDialog = sliceMethod(page, 'const openCommonRegulationSetVersionDialog = (')
assert(
  openDialog.includes('version && !canEditCommonSetVersion(version)') &&
    openDialog.includes('已发布通用规程套版本不可原地编辑'),
  'EDHR-STATIC-001: edit dialog handler must fail closed for published/non-draft versions'
)
const deleteHandler = sliceMethod(page, 'const deleteCommonRegulationSetVersion = async (')
assert(
  deleteHandler.includes('!canEditCommonSetVersion(version)') &&
    deleteHandler.includes('非草稿通用规程套版本不可删除'),
  'EDHR-STATIC-001: delete handler must fail closed for published/non-draft versions'
)

console.log('PASS: EDHR-STATIC-001 common set version immutability contract')
