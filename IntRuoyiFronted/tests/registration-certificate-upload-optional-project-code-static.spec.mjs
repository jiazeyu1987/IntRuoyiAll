import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const appRoot = existsSync(join(root, 'src')) ? '' : 'IntRuoyiFronted'
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')

const apiPath = `${appRoot ? `${appRoot}/` : ''}src/api/dcc/registrationCertificate/index.ts`
const uploadDialogPath = `${appRoot ? `${appRoot}/` : ''}src/views/dcc/registration-certificate/upload/UploadDialog.vue`

for (const file of [apiPath, uploadDialogPath]) {
  assert.equal(existsSync(join(root, file)), true, `${file} must exist`)
}

const api = read(apiPath)
assert.match(
  api,
  /productName:\s*string/,
  'upload API contract must submit product name as a formal required field'
)
assert.match(
  api,
  /projectCodeId\?:\s*number\s*\|\s*string/,
  'upload API contract must make DCC project code optional'
)
assert.match(
  api,
  /export\s+const\s+getUploadOwnerCompanies\b/,
  'upload API must expose current-user owner company candidates'
)
assert.match(
  api,
  /url:\s*['"]\/dcc\/registration-certificates\/uploads\/owner-companies['"]/,
  'owner company candidates must come from the upload-owned company endpoint'
)

const uploadDialog = read(uploadDialogPath)
assert.equal(
  (uploadDialog.match(/<el-form-item label="项目代码"/g) || []).length,
  0,
  'upload dialog must not render a second project-code form item'
)

const companyNameItem = uploadDialog.slice(
  uploadDialog.indexOf('<el-form-item label="公司名称"'),
  uploadDialog.indexOf('</el-form-item>', uploadDialog.indexOf('<el-form-item label="公司名称"'))
)
assert.match(companyNameItem, /<el-select\b/, 'company name must be selected from authorized companies')
assert.match(companyNameItem, /remote/, 'company name selection must search authorized companies remotely')
assert.match(
  companyNameItem,
  /data-testid="registration-certificate-upload-owner-company"/,
  'company name select must expose a stable E2E anchor'
)
assert.doesNotMatch(
  companyNameItem,
  /<el-input\b/,
  'company name must not remain a free-text input that can submit unauthorized companies'
)
assert.match(
  uploadDialog,
  /getUploadOwnerCompanies/,
  'upload dialog must load owner company candidates for the current account'
)
assert.match(
  uploadDialog,
  /ownerCompanyOptions/,
  'upload dialog must keep owner company options separate from entrusted enterprises'
)

const productNameItem = uploadDialog.slice(
  uploadDialog.indexOf('<el-form-item label="产品名称"'),
  uploadDialog.indexOf('</el-form-item>', uploadDialog.indexOf('<el-form-item label="产品名称"'))
)
assert.match(productNameItem, /prop="productName"/, 'product name must participate in form validation')
assert.doesNotMatch(productNameItem, /\breadonly\b/, 'product name must be editable when no project code is selected')

const rulesBlock = uploadDialog.slice(
  uploadDialog.indexOf('const rules = reactive<FormRules>'),
  uploadDialog.indexOf('const resetForm')
)
assert.doesNotMatch(
  rulesBlock,
  /projectCodeId:\s*\[\{\s*required:\s*true/,
  'DCC project code must not be required by the upload form'
)
assert.match(
  rulesBlock,
  /companyName:\s*\[\{\s*required:\s*true,\s*message:\s*'请选择公司名称',\s*trigger:\s*'change'/,
  'company name validation must match the authorized company selector'
)
assert.match(
  rulesBlock,
  /productName:\s*\[\{\s*required:\s*true,\s*message:\s*'请输入产品名称'/,
  'product name must remain required for upload'
)

const loadProjectCodesBlock = uploadDialog.slice(
  uploadDialog.indexOf('const loadProjectCodes'),
  uploadDialog.indexOf('const searchProjectCodes')
)
assert.doesNotMatch(
  loadProjectCodesBlock,
  /requireDccProductCode:\s*true/,
  'project-code choices must include codes that have not bound a DCC product code'
)

const clearProjectCodeBlock = uploadDialog.slice(
  uploadDialog.indexOf('if (!projectCodeId)'),
  uploadDialog.indexOf('let projectCode', uploadDialog.indexOf('if (!projectCodeId)'))
)
assert.doesNotMatch(
  clearProjectCodeBlock,
  /form\.productName\s*=\s*''/,
  'clearing an optional project code must not erase a manually typed product name'
)

const submitBlock = uploadDialog.slice(
  uploadDialog.indexOf('const submit = async'),
  uploadDialog.indexOf('watch(')
)
assert.doesNotMatch(
  submitBlock,
  /请选择DCC项目代码/,
  'submit must not block when DCC project code is blank'
)
assert.match(
  submitBlock,
  /payload\.append\('productName',\s*form\.productName\.trim\(\)\)/,
  'submit must send the required product name'
)
assert.match(
  submitBlock,
  /if\s*\(form\.projectCodeId\)\s*\{[\s\S]*payload\.append\('projectCodeId',\s*String\(form\.projectCodeId\)\)[\s\S]*\}/,
  'submit must only send projectCodeId when the user selected one'
)
