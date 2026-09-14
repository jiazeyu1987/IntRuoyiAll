import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')
const exists = (relativePath) => existsSync(join(root, relativePath))
const appRoot = exists('src') ? '' : 'IntRuoyiFronted'
const backendRoot = exists('IntRuoyiBackend') ? 'IntRuoyiBackend' : '../IntRuoyiBackend'

const apiPath = `${appRoot ? `${appRoot}/` : ''}src/api/dcc/registrationCertificate/index.ts`
const listPath = `${appRoot ? `${appRoot}/` : ''}src/views/dcc/registration-certificate/index/index.vue`
const uploadDialogPath = `${appRoot ? `${appRoot}/` : ''}src/views/dcc/registration-certificate/upload/UploadDialog.vue`
const uploadControllerPath = `${backendRoot}/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/controller/admin/upload/DccRegistrationCertificateUploadController.java`
const menuSqlPath = `${backendRoot}/sql/mysql/20260816_dcc_registration_certificate_menu.sql`

for (const file of [apiPath, listPath, uploadDialogPath, uploadControllerPath, menuSqlPath]) {
  assert.equal(exists(file), true, `${file} must exist`)
}

const api = read(apiPath)
assert.match(api, /export\s+const\s+submitRegistrationCertificateUpload\b/, 'upload API must be exported')
assert.match(
  api,
  /url:\s*['"]\/dcc\/registration-certificates\/uploads['"]/,
  'upload API must target the upload submission endpoint'
)
assert.match(api, /request\.upload\(/, 'upload API must use multipart request upload')
assert.match(api, /['"]Idempotency-Key['"]/, 'upload API must send the explicit Idempotency-Key header')
assert.match(api, /REGISTRATION_CERTIFICATE_UPLOAD_REQUEST_ID_HEADER\s*=\s*['"]X-DCC-Request-Id['"]/,
  'upload API must define the formal DCC audit request id header')
assert.match(api, /\[REGISTRATION_CERTIFICATE_UPLOAD_REQUEST_ID_HEADER\]:\s*idempotencyKey/,
  'upload API must send the same bounded upload id as the DCC audit request id')
assert.match(api, /export\s+const\s+getUploadEntrustedEnterprises\b/,
  'upload API must expose entrusted enterprise candidates')
assert.match(api, /url:\s*['"]\/dcc\/registration-certificates\/uploads\/entrusted-enterprises['"]/,
  'upload entrusted enterprise API must use the upload-owned candidate endpoint')
assert.match(api, /export\s+const\s+getUploadOwnerCompanies\b/,
  'upload API must expose current-user owner company candidates')
assert.match(api, /url:\s*['"]\/dcc\/registration-certificates\/uploads\/owner-companies['"]/,
  'upload owner company API must use the upload-owned company candidate endpoint')
for (const field of [
  'companyId',
  'productName',
  'projectCodeId',
  'certificateNo',
  'firstObtainedDate',
  'effectiveDate',
  'expiryDate',
  'classification',
  'entrustedProduction',
  'selfProduction',
  'entrustedEnterpriseIds',
  'remark'
]) {
  assert.match(api, new RegExp(field.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `upload API must carry ${field}`)
}

const list = read(listPath)
assert.match(list, /上传注册证/, 'list page must expose the upload action')
assert.match(list, /openUploadDialog/, 'list page must open the upload dialog')
assert.match(list, /registration-certificate-upload-dialog/, 'list page must mount the upload dialog')
assert.match(list, /approval-center\/todo\?viewType=TODO/, 'upload save must route to the approval center todo page')
assert.doesNotMatch(list, /approval-center\?moduleCode=DCC&viewType=TODO/,
  'upload save must not force the DCC controlled-file filter for native BPM registration-certificate approval tasks')
for (const legacyToken of ['RegistrationCertificateActionPanel', 'openCreateDraft', 'showCreateDraft', '新增注册证']) {
  assert.doesNotMatch(list, new RegExp(legacyToken.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `list page must not keep ${legacyToken}`)
}

const uploadDialog = read(uploadDialogPath)
assert.match(uploadDialog, /data-testid="registration-certificate-upload-dialog"/,
  'upload dialog must expose a stable anchor')
for (const token of [
  'DCC 项目代码',
  '公司名称',
  '项目代码',
  '产品名称',
  '注册证号',
  '首次获证日期',
  '生效日期',
  '有效期至',
  '类别',
  '是否委托生产',
  '是否自行生产',
  '受托企业',
  '受托企业：',
  '备注',
  '注册证文件'
]) {
  assert.match(uploadDialog, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `upload dialog must contain ${token}`)
}
for (const token of [
  '批准日期',
  '注册人名称',
  '型号规格',
  '结构组成',
  '适用范围',
  '技术要求',
  '注册人住所',
  '生产地址',
  '受托企业 ID',
  '访问申请',
  '正式化',
  '延续',
  '变更/作废'
]) {
  assert.doesNotMatch(uploadDialog, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `upload dialog must not contain ${token}`)
}
assert.match(uploadDialog, /getProjectCodePage/, 'upload dialog must load DCC project codes')
assert.doesNotMatch(uploadDialog, /requireDccProductCode:\s*true/,
  'upload dialog must not require selected project codes to already bind a DCC product code')
assert.doesNotMatch(uploadDialog, /getProduct/,
  'upload dialog must not resolve or overwrite the manually entered product name from the selected project code')
assert.match(uploadDialog, /getUploadOwnerCompanies/,
  'upload dialog must load current-user owner companies for company-name selection')
assert.doesNotMatch(uploadDialog, /<el-input\s+v-model="form\.companyName"/,
  'upload dialog must not keep company name as arbitrary free text')
assert.match(uploadDialog, /v-model="form\.companyId"/,
  'upload dialog must submit selected owner company id rather than matching by company name')
assert.match(uploadDialog, /payload\.append\('companyId',\s*String\(form\.companyId\)\)/,
  'upload dialog must post the selected owner company id')
assert.doesNotMatch(uploadDialog, /payload\.append\('companyName'/,
  'upload dialog must not post owner company name text for authorization matching')
assert.match(uploadDialog, /getUploadEntrustedEnterprises/,
  'upload dialog must load enabled entrusted enterprises when entrusted production is selected')
assert.match(uploadDialog, /是否委托生产和是否自行生产不能同时为否/,
  'upload dialog must reject both production modes set to no')
assert.match(uploadDialog, /请选择受托企业/,
  'upload dialog must require entrusted enterprise selection when entrusted production is yes')
assert.match(uploadDialog, /payload\.append\('entrustedProduction', String\(form\.entrustedProduction\)\)/,
  'upload dialog must submit entrusted production')
assert.match(uploadDialog, /payload\.append\('selfProduction', String\(form\.selfProduction\)\)/,
  'upload dialog must submit self production')
assert.match(uploadDialog, /entrustedEnterpriseIds\.forEach\(\(enterpriseId\) => payload\.append\('entrustedEnterpriseIds', String\(enterpriseId\)\)\)/,
  'upload dialog must submit entrusted enterprise ids')
assert.match(uploadDialog, /submitRegistrationCertificateUpload/, 'upload dialog must submit through the upload API')
assert.match(uploadDialog, /FormData/, 'upload dialog must post multipart form data')

const controller = read(uploadControllerPath)
assert.match(controller, /@RequestMapping\("\/dcc\/registration-certificates\/uploads"\)/,
  'upload controller must expose the upload route root')
assert.match(controller, /dcc:registration-certificate:upload:create/,
  'upload controller must require the upload create permission')
assert.match(controller, /Idempotency-Key/, 'upload controller must require the idempotency header')
assert.match(controller, /DccRequestAuditContext/,
  'upload controller must use the DCC request audit context for request trace id')
assert.match(
  controller,
  /DccRequestAuditContext\.from\(\s*request\s*,\s*TracerUtils\.getTraceId\(\)\s*\)\.requestId\(\)/,
  'upload controller must generate a DCC request trace id when SkyWalking trace id is absent'
)
assert.doesNotMatch(
  controller,
  /submitUploadForApproval\(\s*tenantId\s*,\s*actorId\s*,\s*idempotencyKey\s*,\s*TracerUtils\.getTraceId\(\)/,
  'upload controller must not pass a blank SkyWalking trace id directly to the upload service'
)

const menuSql = read(menuSqlPath)
for (const permission of [
  'dcc:registration-certificate:upload:create',
  'dcc:registration-certificate:upload:approve'
]) {
  assert.match(menuSql, new RegExp(permission.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `menu SQL must grant ${permission}`)
}
