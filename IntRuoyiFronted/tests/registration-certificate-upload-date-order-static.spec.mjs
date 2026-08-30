import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const appRoot = existsSync(join(root, 'src')) ? '' : 'IntRuoyiFronted'
const uploadDialogPath = join(appRoot, 'src/views/dcc/registration-certificate/upload/UploadDialog.vue')

assert.equal(existsSync(join(root, uploadDialogPath)), true, 'UploadDialog.vue must exist')

const source = readFileSync(join(root, uploadDialogPath), 'utf8')
const expectedMessage = '注册证日期顺序不正确：首次获证日期不能晚于生效日期，生效日期必须早于有效期至'
const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

assert.match(
  source,
  new RegExp(`const\\s+DATE_ORDER_MESSAGE\\s*=\\s*'${escapeRegExp(expectedMessage)}'`),
  'upload dialog should define a Chinese date order message'
)

assert.match(
  source,
  /function\s+isDateOrderValid\s*\(\)\s*\{[\s\S]*form\.firstObtainedDate\s*>\s*form\.effectiveDate[\s\S]*form\.effectiveDate\s*>=\s*form\.expiryDate/,
  'upload dialog should validate firstObtainedDate <= effectiveDate < expiryDate'
)

assert.match(
  source,
  /function\s+validateDateOrder\s*\([\s\S]*callback[\s\S]*new Error\(DATE_ORDER_MESSAGE\)/,
  'upload dialog should expose the date order rule as an Element Plus validator'
)

assert.match(
  source,
  /const\s+revalidateDateFields\s*=\s*\(\)\s*=>\s*\{[\s\S]*validateField\(\s*\['firstObtainedDate', 'effectiveDate', 'expiryDate'\]\)/,
  'date changes should revalidate all dependent date fields'
)

for (const label of ['首次获证日期', '生效日期', '有效期至']) {
  const itemStart = source.indexOf(`<el-form-item label="${label}"`)
  assert.notEqual(itemStart, -1, `${label} form item should exist`)
  const itemEnd = source.indexOf('</el-form-item>', itemStart)
  assert.notEqual(itemEnd, -1, `${label} form item should be closed`)
  const itemSource = source.slice(itemStart, itemEnd)
  assert.match(
    itemSource,
    /@change="revalidateDateFields"/,
    `${label} date picker should revalidate the cross-field date order`
  )
}

const rulesBlockStart = source.indexOf('const rules = reactive<FormRules>')
const rulesBlockEnd = source.indexOf('const resetForm')
assert.ok(rulesBlockStart > -1 && rulesBlockEnd > rulesBlockStart, 'form rules block should be present')
const rulesBlock = source.slice(rulesBlockStart, rulesBlockEnd)

for (const field of ['firstObtainedDate', 'effectiveDate', 'expiryDate']) {
  assert.match(
    rulesBlock,
    new RegExp(`${field}:\\s*\\[[\\s\\S]*validator:\\s*validateDateOrder`),
    `${field} should include date order validation`
  )
}

const submitStart = source.indexOf('const submit = async')
const submitEnd = source.indexOf('watch(', submitStart)
assert.ok(submitStart > -1 && submitEnd > submitStart, 'submit block should be present')
const submitBlock = source.slice(submitStart, submitEnd)
const invalidDateCheckIndex = submitBlock.indexOf('if (!isDateOrderValid())')
const formDataIndex = submitBlock.indexOf('const payload = new FormData()')
const uploadCallIndex = submitBlock.indexOf('submitRegistrationCertificateUpload')

assert.ok(invalidDateCheckIndex > -1, 'submit should check date order before upload')
assert.ok(formDataIndex > invalidDateCheckIndex, 'date order should be checked before FormData creation')
assert.ok(uploadCallIndex > invalidDateCheckIndex, 'date order should be checked before API submission')
assert.match(
  submitBlock.slice(invalidDateCheckIndex, formDataIndex),
  /message\.error\(DATE_ORDER_MESSAGE\)[\s\S]*return/,
  'invalid date order should show the Chinese message and stop submission'
)
