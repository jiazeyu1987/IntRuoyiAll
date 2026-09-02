import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const list = read('src/views/dcc/registration-certificate/index/index.vue')
const dialog = read('src/views/dcc/registration-certificate/change/ChangeDialog.vue')
const workflowPanel = read('src/views/dcc/registration-certificate/workflow/ActionPanel.vue')

assert.match(
  list,
  /<RegistrationCertificateChangeDialog[\s\S]*v-model="showChangeDialog"[\s\S]*:certificate="selectedChangeCertificate"[\s\S]*@saved="handleChangeSaved"/,
  'current registration-certificate list must mount the row-level change dialog and refresh after save'
)
assert.match(
  list,
  /@click="openChange\(row\)"[\s\S]*>\s*变更\s*</,
  'change button must open the dialog with the selected current row'
)
const openChange = /const openChange = \(row: DccRegistrationCertificatePageItemVO\) => \{([\s\S]*?)\n\}/.exec(list)?.[1] ?? ''
assert.match(openChange, /selectedChangeCertificate\.value = row/, 'change dialog must receive the selected row identity and row version')
assert.match(openChange, /showChangeDialog\.value = true/, 'change click must show the dialog')
assert.doesNotMatch(openChange, /router\.push|mode:\s*'change'/, 'change click must not route to the detail change mode')

assert.match(dialog, /data-testid="registration-certificate-change-dialog"/, 'change dialog must expose a stable UI anchor')
assert.match(dialog, /title="变更\/作废"/, 'change dialog title must match the business action')
for (const label of ['批准日期', '变更内容', '变更批件文件', '作废证书']) {
  assert.match(dialog, new RegExp(label), `change dialog must render ${label}`)
}
assert.match(dialog, /multiple[\s\S]*data-change-type-values="PRODUCT_NAME,MODEL_SPECIFICATION,STRUCTURE_COMPOSITION,INTENDED_USE,TECHNICAL_REQUIREMENTS,REGISTRANT_NAME,RESIDENCE_ADDRESS,PRODUCTION_ADDRESS,OTHER_CONTENT"/, 'change dialog must keep all nine multi-select change types')
assert.match(dialog, /placeholder: '变更后的产品名称'/, 'product-name change must request the changed value')
assert.match(dialog, /placeholder: '变更后的注册人名称'/, 'registrant-name change must request the changed value')
assert.match(
  dialog,
  /const selectedStructuredChangeTypes = computed\(\(\) => structuredChangeTypeOptions\.filter\(\(item\) => form\.changeTypes\.includes\(item\.value\)\)\)/,
  'every selected structured change type must render an editable field'
)
for (const mapping of [
  ['PRODUCT_NAME', 'detail.productName'],
  ['MODEL_SPECIFICATION', 'detail.modelSpecification'],
  ['STRUCTURE_COMPOSITION', 'detail.structureComposition'],
  ['INTENDED_USE', 'detail.intendedUse'],
  ['TECHNICAL_REQUIREMENTS', 'detail.technicalRequirements'],
  ['REGISTRANT_NAME', 'detail.registrantName'],
  ['RESIDENCE_ADDRESS', 'detail.residenceAddress'],
  ['PRODUCTION_ADDRESS', 'detail.productionAddress']
]) {
  assert.match(
    dialog,
    new RegExp(`${mapping[0]}['"]?:\\s*${mapping[1].replace('.', '\\.')}`),
    `${mapping[0]} must initialize from the current registration-certificate detail`
  )
}
assert.match(dialog, /getRegistrationCertificateDetail\(props\.certificate\.certificateId\)/, 'opening the dialog must load the formal current detail')
assert.match(dialog, /detail\.entrustedProduction[\s\S]*detail\.selfProduction[\s\S]*detail\.entrustedEnterprisesJson/, 'production relation must initialize from current detail')
assert.match(dialog, /detailLoadError[\s\S]*加载注册证当前信息失败/, 'detail loading failures must be visible')
assert.match(dialog, /:disabled="saving \|\| detailLoading \|\| Boolean\(detailLoadError\)"[\s\S]*@click="submit"/, 'confirmation must stay disabled until current detail is available')
assert.match(dialog, /委托生产和自行生产不可同时选择否。/, 'production relation validation must remain explicit')
assert.match(dialog, /@click="submit"[^>]*>确认</, 'dialog confirmation must submit the change for approval')
assert.match(dialog, /submitRegistrationCertificateChange\([\s\S]*props\.certificate\.certificateId[\s\S]*buildChangePayload\([\s\S]*DCC-REG-CERT-CHANGE/, 'confirmation must reuse the formal change approval API and idempotency key')
assert.match(dialog, /payload\.append\('expectedRowVersion', String\(currentDetail\.value\.rowVersion\)\)/, 'change dialog must submit the row version returned with current detail')
assert.match(dialog, /dialogVisible\.value = false[\s\S]*emit\('saved'\)/, 'success must close the dialog and notify the list to refresh')
assert.match(dialog, /message\.error\(resolveRegistrationCertificateUserMessage\(error, '提交变更申请失败'\)\)[\s\S]*throw error/, 'submit errors must remain visible and must not be swallowed')

assert.doesNotMatch(workflowPanel, /label="变更\/作废"/, 'detail workflow must no longer expose a duplicate change entry')
assert.doesNotMatch(workflowPanel, /handleSubmitChange/, 'detail workflow must not retain the moved change submit handler')

console.log('PASS: registration certificate change dialog static contract')
