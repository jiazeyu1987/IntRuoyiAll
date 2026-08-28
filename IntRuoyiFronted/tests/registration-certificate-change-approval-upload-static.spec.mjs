import assert from 'node:assert/strict'
import fs from 'node:fs'

const apiPath = 'IntRuoyiFronted/src/api/dcc/registrationCertificate/index.ts'
const panelPath = 'IntRuoyiFronted/src/views/dcc/registration-certificate/workflow/ActionPanel.vue'
const backendControllerPath =
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/controller/admin/change/DccRegistrationCertificateChangeController.java'
const backendReqPath =
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/controller/admin/change/vo/DccRegistrationCertificateChangeApplyReqVO.java'

for (const path of [apiPath, panelPath, backendControllerPath, backendReqPath]) {
  assert.ok(fs.existsSync(path), `${path} must exist`)
}

const api = fs.readFileSync(apiPath, 'utf8')
const panel = fs.readFileSync(panelPath, 'utf8')
const backendController = fs.readFileSync(backendControllerPath, 'utf8')
const backendReq = fs.readFileSync(backendReqPath, 'utf8')

const functionBlock = (source, marker) => {
  const start = source.indexOf(marker)
  assert.ok(start >= 0, `${marker} must exist`)
  const next = source.indexOf('\nexport const ', start + marker.length)
  return source.slice(start, next > start ? next : source.length)
}

const changeSubmitApi = functionBlock(api, 'export const submitRegistrationCertificateChange')

assert.match(changeSubmitApi, /data:\s*FormData/, 'change approval API must submit FormData')
assert.match(changeSubmitApi, /request\.upload/, 'change approval API must use multipart upload')
assert.doesNotMatch(
  api,
  /DccRegistrationCertificateChangeApplyReqVO[\s\S]{0,500}businessFileId/,
  'change approval request type must not expose manually typed businessFileId'
)
assert.match(backendController, /@ModelAttribute\s+DccRegistrationCertificateChangeApplyReqVO/, 'backend change submit must bind multipart model')
assert.match(backendReq, /MultipartFile\s+file/, 'backend change request must require uploaded file')

const changePanelStart = panel.indexOf('data-testid="registration-certificate-change-form"')
const changePanelEnd = panel.indexOf('data-testid="registration-certificate-supporting-document-action"', changePanelStart)
assert.ok(changePanelStart >= 0 && changePanelEnd > changePanelStart, 'change panel block must be locatable')
const changePanel = panel.slice(changePanelStart, changePanelEnd)

assert.match(changePanel, /v-model="changeForm\.changeTypes"[\s\S]{0,180}multiple/, 'change content must support multiple selection')
assert.match(changePanel, /变更批件文件/, 'change panel must ask for the actual change approval file')
assert.match(changePanel, /data-testid="registration-certificate-change-approval-file"/, 'change file upload must have a stable test id')
for (const field of ['PRODUCT_NAME', 'REGISTRANT_NAME', 'PRODUCTION_ADDRESS', 'OTHER_CONTENT']) {
  assert.match(changePanel, new RegExp(field), `${field} option must be present`)
}
assert.match(changePanel, /是否委托生产/, 'production address change must expose entrusted production')
assert.match(changePanel, /是否自行生产/, 'production address change must expose self production')
assert.doesNotMatch(changePanel, /变更批件业务文件 ID/, 'users must not manually type business file ids')

assert.match(panel, /selectedChangeFile\.value/, 'change submit must read the selected upload file')
assert.match(panel, /payload\.append\('file',\s*selectedChangeFile\.value\)/, 'change submit must append uploaded file')
assert.match(panel, /changeForm\.changeTypes\.includes\('OTHER_CONTENT'\)/, 'other content must be handled as one selected item')
assert.match(panel, /changeForm\.changeTypes\.includes\('PRODUCTION_ADDRESS'\)/, 'production address must trigger production relation validation')

console.log('registration certificate change approval upload static contract passed')
