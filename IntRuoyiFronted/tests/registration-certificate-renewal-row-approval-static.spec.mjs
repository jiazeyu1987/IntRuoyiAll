import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')
const exists = (relativePath) => existsSync(join(root, relativePath))

const apiPath = 'IntRuoyiFronted/src/api/dcc/registrationCertificate/index.ts'
const listPath = 'IntRuoyiFronted/src/views/dcc/registration-certificate/index/index.vue'
const renewalDialogPath =
  'IntRuoyiFronted/src/views/dcc/registration-certificate/renewal/RenewalDialog.vue'
const queryMapperPath =
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/dal/mysql/DccRegistrationCertificateQueryMapper.java'
const renewalCommandPath =
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/renewal/DccRegistrationCertificateRenewalCommand.java'
const renewalServicePath =
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/renewal/DccRegistrationCertificateRenewalService.java'
const errorCodePath =
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/enums/ErrorCodeConstants.java'

for (const file of [apiPath, listPath, renewalDialogPath, queryMapperPath, renewalCommandPath, renewalServicePath, errorCodePath]) {
  assert.equal(exists(file), true, `${file} must exist`)
}

const api = read(apiPath)
assert.match(api, /rowVersion:\s*number/, 'current list item must expose rowVersion for renewal concurrency checks')
assert.match(api, /hasPendingRenewal:\s*boolean/,
  'current list item must expose the formal pending-renewal guard for the row action')
assert.match(api, /export\s+const\s+submitRegistrationCertificateRenewal\b/,
  'renewal API must be a dedicated row-level upload submission')
assert.match(api, /url:\s*`\/dcc\/registration-certificates\/\$\{certificateId\}\/renewals`/,
  'renewal API must target the row certificate renewal endpoint')
assert.match(api, /request\.upload\(/, 'renewal submission must use multipart upload')
assert.match(api, /['"]Idempotency-Key['"]/, 'renewal submission must send Idempotency-Key')
assert.doesNotMatch(api, /DccRegistrationCertificateRenewalUploadReqVO[\s\S]*businessFileId/,
  'renewal upload payload must not expose a pre-staged businessFileId')
assert.match(api, /DccRegistrationCertificateRenewalUploadReqVO[\s\S]*categoryChanged:\s*boolean/,
  'row-level renewal must expose the required category-changed switch')
assert.match(api, /DccRegistrationCertificateRenewalUploadReqVO[\s\S]*certificateNo\??:\s*string/,
  'row-level renewal must allow changing certificate number when category changes')
assert.match(api, /DccRegistrationCertificateRenewalUploadReqVO[\s\S]*classification\??:\s*string/,
  'row-level renewal must allow changing category when category changes')

const list = read(listPath)
assert.match(list, /RegistrationCertificateRenewalDialog/, 'list page must mount the renewal dialog')
assert.match(list, /openRenewalDialog\(row\)/, 'each current-list row must open renewal with that row')
assert.match(list, /v-hasPermi="\['dcc:registration-certificate:renewal:upload'\]"/,
  'row-level renewal button must use the formal renewal upload permission')
const renewalButton = /<el-button[\s\S]*?v-hasPermi="\['dcc:registration-certificate:renewal:upload'\]"[\s\S]*?>\s*延续\s*<\/el-button>/.exec(list)?.[0] ?? ''
assert.match(renewalButton, /v-if="row\.status === 'CURRENT'"/,
  'only the current effective version may expose the renewal action')
assert.match(list, />\s*延续\s*<\/el-button>/, 'row actions must show a visible 延续 button')
assert.match(list, /@saved="handleRenewalSaved"/, 'renewal success must refresh the current list')
const renewalPendingMessage = '该注册证已有待审批或待生效的延续，请勿重复提交'
const openRenewalDialogBlock = /const\s+openRenewalDialog\s*=\s*\(row:[\s\S]*?\n\}/.exec(list)?.[0] ?? ''
assert.match(openRenewalDialogBlock, /if\s*\(row\.hasPendingRenewal\)/,
  'clicking renewal must consult the server-provided pending-renewal guard before opening the form')
assert.match(openRenewalDialogBlock, new RegExp(`ElMessage\\.warning\\('${renewalPendingMessage}'\\)`),
  'clicking renewal with an open request or pending-effective candidate must show the formal duplicate message')
const renewalPendingGuardIndex = openRenewalDialogBlock.indexOf('if (row.hasPendingRenewal)')
const renewalDialogOpenIndex = openRenewalDialogBlock.indexOf('showRenewalDialog.value = true')
assert.ok(renewalPendingGuardIndex >= 0 && renewalDialogOpenIndex > renewalPendingGuardIndex,
  'the pending-renewal guard must run before the dialog can open')
assert.match(openRenewalDialogBlock.slice(renewalPendingGuardIndex, renewalDialogOpenIndex), /return/,
  'the pending-renewal guard must stop the click handler instead of opening a duplicate submission form')

const dialog = read(renewalDialogPath)
const dateOrderMessage = '注册证日期顺序不正确：批准日期不能晚于生效日期，生效日期必须早于有效期至'
const approvalDateMessage = '批准日期不能晚于当前日期'
assert.match(dialog, /data-testid="registration-certificate-renewal-dialog"/,
  'renewal dialog must expose a stable anchor')
assert.match(dialog, /title="延续注册证"/, 'renewal dialog title must match the business action')
assert.match(dialog, /width="8[4-9]0px"/, 'renewal dialog must be wide enough for complete date inputs')
assert.match(dialog, /label-width="12[4-9]px"/, 'renewal form labels must reserve readable label width')
assert.match(dialog, /<el-row\s+:gutter="2[4-9]"/, 'renewal form columns must keep horizontal breathing room')
for (const token of ['批准日期', '生效日期', '有效期至', '类别否变更', '注册证号', '类别', '延续注册证文件']) {
  assert.match(dialog, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `renewal dialog must contain ${token}`)
}
for (const token of ['批准日期', '生效日期', '有效期至']) {
  assert.match(
    dialog,
    new RegExp(`<el-col\\s+:span="12"[^>]*>[\\s\\S]*?<el-form-item label="${token}"`),
    `renewal date field ${token} must use a half-width row column instead of cramped thirds`
  )
}
assert.match(dialog, new RegExp(`const\\s+RENEWAL_DATE_ORDER_MESSAGE\\s*=\\s*'${dateOrderMessage}'`),
  'renewal dialog should define a Chinese renewal date order message')
assert.match(dialog, new RegExp(`const\\s+RENEWAL_APPROVAL_DATE_MESSAGE\\s*=\\s*'${approvalDateMessage}'`),
  'renewal dialog should define a Chinese future approval date message')
assert.match(dialog,
  /function\s+isRenewalDateOrderValid\s*\(\)\s*\{[\s\S]*form\.approvalDate\s*>\s*form\.effectiveDate[\s\S]*form\.effectiveDate\s*>=\s*form\.expiryDate/,
  'renewal dialog should validate approvalDate <= effectiveDate < expiryDate')
assert.match(dialog,
  /function\s+validateRenewalDateOrder\s*\([\s\S]*callback[\s\S]*new Error\(RENEWAL_DATE_ORDER_MESSAGE\)/,
  'renewal dialog should expose renewal date order validation as an Element Plus validator')
assert.match(dialog,
  /function\s+validateRenewalApprovalDate\s*\([\s\S]*callback[\s\S]*new Error\(RENEWAL_APPROVAL_DATE_MESSAGE\)/,
  'renewal dialog should expose future approval date validation as an Element Plus validator')
assert.match(dialog,
  /const\s+revalidateRenewalDateFields\s*=\s*\(\)\s*=>\s*\{[\s\S]*validateField\(\s*\['approvalDate', 'effectiveDate', 'expiryDate'\]\)/,
  'renewal date changes should revalidate all dependent renewal date fields')
for (const token of ['批准日期', '生效日期', '有效期至']) {
  const itemStart = dialog.indexOf(`<el-form-item label="${token}"`)
  assert.notEqual(itemStart, -1, `${token} form item should exist`)
  const itemEnd = dialog.indexOf('</el-form-item>', itemStart)
  assert.notEqual(itemEnd, -1, `${token} form item should be closed`)
  const itemSource = dialog.slice(itemStart, itemEnd)
  assert.match(itemSource, /@change="revalidateRenewalDateFields"/,
    `${token} date picker should revalidate renewal cross-field dates`)
}
const renewalRulesStart = dialog.indexOf('const rules = reactive<FormRules>')
const renewalRulesEnd = dialog.indexOf('const resetForm')
assert.ok(renewalRulesStart > -1 && renewalRulesEnd > renewalRulesStart,
  'renewal form rules block should be present')
const renewalRules = dialog.slice(renewalRulesStart, renewalRulesEnd)
for (const field of ['approvalDate', 'effectiveDate', 'expiryDate']) {
  assert.match(renewalRules, new RegExp(`${field}:\\s*\\[[\\s\\S]*validator:\\s*validateRenewalDateOrder`),
    `${field} should include renewal date order validation`)
}
assert.match(renewalRules, /approvalDate:\s*\[[\s\S]*validator:\s*validateRenewalApprovalDate/,
  'approvalDate should include future-date validation')
const renewalSubmitStart = dialog.indexOf('const submit = async')
const renewalSubmitEnd = dialog.indexOf('</script>', renewalSubmitStart)
assert.ok(renewalSubmitStart > -1 && renewalSubmitEnd > renewalSubmitStart,
  'renewal submit block should be present')
const renewalSubmitBlock = dialog.slice(renewalSubmitStart, renewalSubmitEnd)
const invalidRenewalDateIndex = renewalSubmitBlock.indexOf('if (!isRenewalDateOrderValid())')
const futureApprovalIndex = renewalSubmitBlock.indexOf('if (isRenewalApprovalDateInFuture())')
const renewalFormDataIndex = renewalSubmitBlock.indexOf('const payload = new FormData()')
assert.ok(invalidRenewalDateIndex > -1, 'renewal submit should check date order before upload')
assert.ok(futureApprovalIndex > invalidRenewalDateIndex,
  'renewal submit should check future approval date after date order')
assert.ok(renewalFormDataIndex > futureApprovalIndex,
  'renewal date checks should run before FormData creation')
assert.match(renewalSubmitBlock.slice(invalidRenewalDateIndex, futureApprovalIndex),
  /message\.error\(RENEWAL_DATE_ORDER_MESSAGE\)[\s\S]*return/,
  'invalid renewal date order should show the Chinese message and stop submission')
assert.match(renewalSubmitBlock.slice(futureApprovalIndex, renewalFormDataIndex),
  /message\.error\(RENEWAL_APPROVAL_DATE_MESSAGE\)[\s\S]*return/,
  'future renewal approval date should show the Chinese message and stop submission')
for (const token of ['产品名称', '注册人名称', '型号规格', '结构组成', '适用范围']) {
  assert.doesNotMatch(dialog, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `renewal dialog must not expose editable non-renewal field ${token}`)
}
const styleStart = dialog.indexOf('<style')
const styleEnd = dialog.indexOf('</style>')
assert.notEqual(styleStart, -1, 'renewal dialog must keep scoped layout styles')
assert.notEqual(styleEnd, -1, 'renewal dialog must close the style block')
const style = dialog.slice(styleStart, styleEnd)
const labelRule = style.match(/\.el-form-item__label\s*\{[\s\S]*?\}/)?.[0] || ''
const mobileRule = style.match(/@media\s*\(max-width:\s*720px\)\s*\{[\s\S]*$/)?.[0] || ''
assert.match(labelRule, /padding-right:\s*12px\s*;/, 'renewal labels must keep a fixed gap before controls')
assert.match(labelRule, /line-height:\s*32px\s*;/, 'renewal labels must align with standard input height')
assert.match(labelRule, /white-space:\s*nowrap\s*;/, 'renewal labels must not wrap into date controls')
assert.doesNotMatch(labelRule, /padding:\s*0\s*;/, 'renewal labels must not reset all padding to zero')
assert.match(style, /\.el-row\s*\{[\s\S]*?row-gap:\s*4px\s*;/, 'renewal form rows must keep vertical breathing room')
assert.match(style, /\.el-form-item\s*\{[\s\S]*?margin-bottom:\s*20px\s*;/,
  'renewal form items must keep a comfortable bottom gap')
assert.match(style, /\.el-input,\s*\n\s*\.el-select,\s*\n\s*\.el-date-editor\s*\{[\s\S]*?width:\s*100%\s*;/,
  'renewal controls must fill their column width')
assert.match(mobileRule, /\.el-form-item\s*\{[\s\S]*?display:\s*block\s*;/,
  'narrow screens must switch renewal form items to stacked layout')
assert.match(mobileRule, /\.el-form-item__label\s*\{[\s\S]*?width:\s*100%\s*!important\s*;/,
  'narrow screens must place renewal labels above inputs')
assert.match(dialog, /submitRegistrationCertificateRenewal/, 'dialog must submit through the dedicated renewal API')
assert.match(dialog, /payload\.append\('expectedRowVersion', String\(props\.certificate\.rowVersion\)\)/,
  'dialog must submit the rowVersion from the selected row')
assert.match(dialog, /payload\.append\('currentVersionId', String\(props\.certificate\.versionId\)\)/,
  'dialog must submit the current row version identity')
assert.match(dialog, /payload\.append\('categoryChanged', String\(form\.categoryChanged\)\)/,
  'dialog must submit the category-changed switch')
assert.match(dialog, /payload\.append\('certificateNo', form\.certificateNo\.trim\(\)\)/,
  'dialog must submit the changed certificate number only when category changes')
assert.match(dialog, /payload\.append\('classification', form\.classification\.trim\(\)\)/,
  'dialog must submit the changed classification only when category changes')
assert.match(dialog, /payload\.append\('file', selectedFile\.value\)/,
  'dialog must require and submit the renewal certificate file')

const queryMapper = read(queryMapperPath)
assert.match(queryMapper, /v\.id\s*=\s*COALESCE\(c\.pending_version_id,\s*c\.current_version_id\)/,
  'current list must prefer the approved pending renewal version over the old current version')
assert.match(queryMapper, /v\.status\s+IN\s+\('CURRENT',\s*'PENDING_EFFECTIVE'\)/,
  'current list must expose only the single active display version')
assert.match(queryMapper, /AS\s+has_pending_renewal/,
  'current list query must return the formal pending-renewal guard to the frontend')
assert.match(queryMapper, /c\.pending_version_id\s+IS\s+NOT\s+NULL[\s\S]*dcc_registration_certificate_access_request[\s\S]*status\s+IN\s+\('SUBMITTED',\s*'BPM_BOUND'\)/,
  'pending-renewal guard must cover both pending-effective candidates and BPM-bound approval requests')
const currentWhereBlock = /private static String currentWhere\(\)[\s\S]*?private static String filters\(\)/.exec(queryMapper)?.[0] ?? ''
assert.doesNotMatch(currentWhereBlock, /v\.status\s*!=\s*'OLD'/,
  'current list must not join every non-old version and duplicate certificates')

const renewalCommand = read(renewalCommandPath)
assert.match(renewalCommand, /\bBoolean\s+categoryChanged\b/,
  'renewal command must carry the category-changed switch')
assert.match(renewalCommand, /\bString\s+certificateNo\b/,
  'renewal command must carry the changed certificate number')
assert.match(renewalCommand, /\bString\s+classification\b/,
  'renewal command must carry the changed classification')

const renewalService = read(renewalServicePath)
assert.match(renewalService, /REGISTRATION_CERTIFICATE_RENEWAL_CATEGORY_CHANGE_REQUIRED/,
  'renewal service must fail fast when category change details are incomplete')
assert.match(renewalService, /resolveRenewalCertificateNo/,
  'renewal service must resolve certificate number from command only when category changes')
assert.match(renewalService, /resolveRenewalClassification/,
  'renewal service must resolve classification from command only when category changes')
assert.match(renewalService, /\.categoryChanged\(categoryChanged\)/,
  'renewal version must persist whether the renewal changed category')

const errorCodes = read(errorCodePath)
assert.match(errorCodes,
  /REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT\s*=\s*new ErrorCode\([\s\S]*?"该注册证已有待审批或待生效的延续，请勿重复提交"\)/,
  'renewal pending conflict must explain the existing renewal state in Chinese')

console.log('registration certificate renewal row approval static contract passed')
