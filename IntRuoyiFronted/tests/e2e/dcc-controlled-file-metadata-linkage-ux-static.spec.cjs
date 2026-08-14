const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const packageJson = JSON.parse(readSource('package.json'))
const metadataDialog = readSource('src/views/dcc/controlled-file/shared/ControlledFileMetadataDialog.vue')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const projectCodePanel = readSource('src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:metadata-linkage-ux:static'],
  'node tests/e2e/dcc-controlled-file-metadata-linkage-ux-static.spec.cjs',
  'package.json must expose DCC metadata linkage UX static contract'
)

for (const token of [
  'data-testid="dcc-metadata-impact-preview"',
  '变更影响预览',
  '当前 DCC 项目',
  '目标 DCC 项目',
  '当前分类路径',
  '目标分类路径',
  '受控浏览目录落位',
  'targetProjectCodeImpactText',
  'currentProjectCodeImpactText',
  'targetTaxonomyImpactText',
  'currentTaxonomyImpactText',
  'targetDirectoryImpactText'
]) {
  assert.match(metadataDialog, new RegExp(escapeRegExp(token)), `metadata dialog must show impact preview token: ${token}`)
}

for (const token of [
  'resolveMetadataPermissionErrorMessage',
  'Only doc control can update controlled file metadata',
  'doc_control',
  'user_role_ids',
  '权限缓存',
  '重新登录',
  '文控角色',
  'resolveReadSideErrorMessage(error'
]) {
  assert.match(metadataDialog, new RegExp(escapeRegExp(token)), `metadata save must expose diagnostic token: ${token}`)
}

for (const token of [
  'data-testid="dcc-detail-project-code-linkage"',
  'DCC 项目代码联动',
  '当前 DCC 项目',
  '当前文件分类',
  '关联文档入口',
  '修正追溯入口',
  'openDccProjectCodeTrace',
  'openDccProjectCode',
  "associatedFocus: '1'",
  'associatedFileId: String(fileDetail.value.id)',
  'fileTypeTaxonomyId:',
  'fileDetail.value.fileTypeTaxonomyId',
  "logType: 'PROJECT_CODE_CHANGE'",
  'controlledFileId: String(fileDetail.value.id)'
]) {
  assert.match(detailPage, new RegExp(escapeRegExp(token)), `detail page must expose DCC project-code linkage token: ${token}`)
}

for (const token of [
  'resolveQueryAssociatedFileId',
  'resolveQueryAssociatedTaxonomyId',
  'applyAssociatedRouteFocus',
  'focusedAssociatedFileId',
  'is-associated-route-focus',
  'data-testid="dcc-project-code-associated-route-focus"',
  'associatedFocus',
  'route.query.associatedFileId',
  'route.query.fileTypeTaxonomyId',
  'selectAssociatedStage',
  'selectAssociatedType'
]) {
  assert.match(projectCodePanel, new RegExp(escapeRegExp(token)), `project-code associated document drawer must support route focus token: ${token}`)
}

assert.doesNotMatch(
  metadataDialog + detailPage + projectCodePanel,
  /mock|placeholder data|默认成功|吞异常|fallback|降级/i,
  'metadata linkage UX must not introduce mock, fallback, default success, swallowed errors, or downgrade'
)

console.log('PASS: DCC controlled-file metadata linkage UX static contract')
