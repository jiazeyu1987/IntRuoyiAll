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

for (const file of [apiPath, listPath, renewalDialogPath, queryMapperPath, renewalCommandPath, renewalServicePath]) {
  assert.equal(exists(file), true, `${file} must exist`)
}

const api = read(apiPath)
assert.match(api, /rowVersion:\s*number/, 'current list item must expose rowVersion for renewal concurrency checks')
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
assert.match(list, />\s*延续\s*<\/el-button>/, 'row actions must show a visible 延续 button')
assert.match(list, /@saved="handleRenewalSaved"/, 'renewal success must refresh the current list')

const dialog = read(renewalDialogPath)
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

console.log('registration certificate renewal row approval static contract passed')
