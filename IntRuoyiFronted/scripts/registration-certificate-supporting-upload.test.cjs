const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const root = path.resolve(__dirname, '..')
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8')

test('detail and workflow expose access actions without an independent upload path', () => {
  const detail = read('src/views/dcc/registration-certificate/detail/index.vue')
  const workflow = read('src/views/dcc/registration-certificate/workflow/ActionPanel.vue')
  const api = read('src/api/dcc/registrationCertificate/index.ts')
  assert.match(detail, /v-if="viewMode === 'access-request'"/)
  for (const source of [detail, workflow, api]) {
    assert.doesNotMatch(source, /uploadRegistrationCertificateSupportingDocument|canUploadSupporting|allowSupportingUpload|supporting-uploaded|supporting-documents\/upload/)
  }
  assert.doesNotMatch(workflow, /name="supporting"|type="file"|<el-upload/)
  assert.match(workflow, /submitRegistrationCertificateAccessRequest/)
  assert.match(detail, /@preview="openAttachmentPreview"/)
  assert.match(detail, /@download="downloadAttachment"/)
})

test('new, change and renewal retain their file inputs', () => {
  for (const flow of ['upload/UploadDialog.vue', 'change/ChangeDialog.vue', 'renewal/RenewalDialog.vue']) {
    assert.match(read('src/views/dcc/registration-certificate/' + flow), /<el-upload/)
  }
  const controller = path.join(root, '../IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/controller/admin/supportingdocument/DccRegistrationCertificateSupportingDocumentController.java')
  assert.equal(fs.existsSync(controller), false, 'independent supporting upload HTTP endpoint must be removed')
})
