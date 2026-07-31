const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const ENDPOINT = '/mes/pro/edhr-release-setting/dossier-requirements'
const GOLDEN_FINGER_PERMISSION = 'mes:pro-batch-record-execution:golden-finger'
const fields = [
  'incomingInspectionReportRequired',
  'sterilizationReportRequired',
  'finishedProductInspectionReportRequired',
  'finishedProductInspectionRecordRequired'
]

const api = readSource('src/api/mes/pro/edhr/releaseDossierRequirementSetting.ts')
assert.ok(api.includes(ENDPOINT), 'API wrapper must target release dossier requirement setting endpoint.')
assert.ok(api.includes('getEdhrReleaseDossierRequirementSetting'), 'API wrapper must expose GET helper.')
assert.ok(api.includes('updateEdhrReleaseDossierRequirementSetting'), 'API wrapper must expose PUT helper.')
for (const field of fields) {
  assert.ok(api.includes(`${field}: boolean`), `API wrapper must type ${field}.`)
}

const profile = readSource('src/views/Profile/Index.vue')
assert.ok(profile.includes('EdhrReleaseDossierRequirementSetting'), 'Profile config tab must render dossier setting component.')
assert.ok(profile.includes(GOLDEN_FINGER_PERMISSION), 'Profile config tab must keep golden-finger permission gate.')
assert.ok(profile.includes('v-if="hasGoldenFingerPermission"'), 'Config tab must be hidden from ordinary users.')

const exportsSource = readSource('src/views/Profile/components/index.ts')
assert.ok(exportsSource.includes('EdhrReleaseDossierRequirementSetting'), 'Profile component index must export dossier setting.')

const component = readSource('src/views/Profile/components/EdhrReleaseDossierRequirementSetting.vue')
for (const label of ['来料检报告', '灭菌报告', '成品检报告', '成品检记录限制']) {
  assert.ok(component.includes(label), `Dossier setting component must show switch label: ${label}`)
}
for (const field of fields) {
  assert.ok(component.includes(field), `Dossier setting component must bind ${field}.`)
}
assert.ok(component.includes('el-switch'), 'Dossier setting component must use switches.')
assert.ok(component.includes('ElMessageBox.confirm'), 'Each switch save must confirm before PUT.')
assert.ok(component.includes('updateEdhrReleaseDossierRequirementSetting'), 'Component must call update API.')
assert.ok(component.includes('getEdhrReleaseDossierRequirementSetting'), 'Component must load current setting.')
assert.ok(
  /const\s+previousSetting\s*=/.test(component) && component.includes('setting.value = previousSetting'),
  'Component must rollback to the previous full setting on cancel or API failure.'
)
assert.ok(component.includes('接口保存失败'), 'Component must surface backend save errors without silent downgrade.')
for (const field of fields) {
  assert.ok(
    new RegExp(`${field}:\\s*setting\\.value\\.${field}`).test(component),
    `PUT payload must include the full boolean object field: ${field}`
  )
}

const presentation = readSource('src/views/mes/pro/edhr/shared/releaseCheckPresentation.ts')
for (const [code, label] of [
  ['DOSSIER_INCOMING_INSPECTION_REPORT', '来料检报告资料限制'],
  ['DOSSIER_STERILIZATION_REPORT', '灭菌报告资料限制'],
  ['DOSSIER_FINISHED_PRODUCT_INSPECTION_REPORT', '成品检报告资料限制'],
  ['DOSSIER_FINISHED_PRODUCT_INSPECTION_RECORD', '成品检记录限制']
]) {
  assert.ok(presentation.includes(code), `Release check presentation must map ${code}.`)
  assert.ok(presentation.includes(label), `Release check presentation must show ${label}.`)
}
assert.ok(presentation.includes("DOSSIER: '放行资料限制'"), 'Release check categories must include DOSSIER.')
assert.ok(
  presentation.includes("SPECIAL_NODE_ATTACHMENT: '特殊节点附件'"),
  'Release check source object type must include special node attachment.'
)

console.log('PASS: eDHR release dossier requirement setting frontend static contract')
