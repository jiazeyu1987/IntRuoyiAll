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

const page = readSource('src/views/mes/pro/edhr-config/index.vue')
assert.ok(page.includes('<ReleaseDossierRequirements />'), 'MES page mounts fixed dossier requirements')
assert.ok(page.includes('v-if="canQuery"') && readSource('src/utils/businessConfigAccess.ts').includes(GOLDEN_FINGER_PERMISSION), 'MES page retains query permission')
assert.ok(!readSource('src/views/Profile/components/index.ts').includes('EdhrReleaseDossierRequirementSetting'), 'Profile barrel no longer owns dossier configuration')
const component = readSource('src/views/mes/pro/edhr-config/components/ReleaseDossierRequirements.vue')
for (const label of ['来料检报告', '灭菌报告', '成品检报告', '成品检记录']) {
  assert.ok(component.includes(label), 'fixed material remains visible: ' + label)
}
assert.ok(component.includes('固定必填'), 'fixed requirement is explicit')
assert.ok(!/el-switch|updateEdhrReleaseDossierRequirementSetting|ElMessageBox.confirm/.test(component), 'fixed materials have no editable control or write handler')
const lockedService = fs.readFileSync(path.resolve(repoRoot, '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseDossierRequirementSettingServiceImpl.java'), 'utf8')
assert.ok(lockedService.includes('PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_LOCKED'), 'server must retain locked PUT rejection')

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
