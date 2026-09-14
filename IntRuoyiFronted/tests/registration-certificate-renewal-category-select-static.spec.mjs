import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const exists = (relativePath) => existsSync(join(root, relativePath))
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')
const appRoot = exists('src') ? '' : 'IntRuoyiFronted'
const renewalDialogPath = `${appRoot ? `${appRoot}/` : ''}src/views/dcc/registration-certificate/renewal/RenewalDialog.vue`
const renewalLifecycleE2ePath = `${appRoot ? `${appRoot}/` : ''}tests/e2e/registration-certificate-renewal-lifecycle-real.spec.js`
const renewalRowDialogE2ePath = `${appRoot ? `${appRoot}/` : ''}tests/e2e/registration-certificate-renewal-row-dialog-real.spec.js`
const registrationCertificateRealFlowPath = `${appRoot ? `${appRoot}/` : ''}tests/e2e/registration-certificate-real-flow.spec.js`

for (const file of [
  renewalDialogPath,
  renewalLifecycleE2ePath,
  renewalRowDialogE2ePath,
  registrationCertificateRealFlowPath
]) {
  assert.equal(exists(file), true, `${file} must exist`)
}

const source = read(renewalDialogPath)
const categoryItemStart = source.indexOf('<el-form-item label="类别" prop="classification">')
const categoryItemEnd = source.indexOf('</el-form-item>', categoryItemStart)

assert.notEqual(categoryItemStart, -1, 'renewal dialog must render the category form item')
assert.notEqual(categoryItemEnd, -1, 'renewal category form item must be closed')

const categoryItem = source.slice(categoryItemStart, categoryItemEnd)

assert.match(
  source,
  /const\s+REGISTRATION_CERTIFICATE_RENEWAL_CLASSIFICATION_OPTIONS\s*=\s*\[\s*'三类',\s*'二类',\s*'一类'\s*\]\s+as\s+const/,
  'renewal dialog must define the exact category options in business order'
)
assert.match(
  categoryItem,
  /<el-select\b[\s\S]*v-model="form\.classification"/,
  'renewal category must be selected through an Element Plus dropdown'
)
assert.match(
  categoryItem,
  /data-testid="registration-certificate-renewal-classification"/,
  'renewal category select must expose a stable E2E anchor'
)
assert.match(
  categoryItem,
  /placeholder="请选择变更后的类别"/,
  'renewal category placeholder must match dropdown behavior'
)
assert.match(
  categoryItem,
  /v-for="option in REGISTRATION_CERTIFICATE_RENEWAL_CLASSIFICATION_OPTIONS"/,
  'renewal category select must render options from the exact category option list'
)
assert.match(categoryItem, /:label="option"/, 'renewal category option label must use the business value')
assert.match(categoryItem, /:value="option"/, 'renewal category option value must submit the business value')
assert.doesNotMatch(
  categoryItem,
  /<el-input\b[\s\S]*v-model="form\.classification"/,
  'renewal category must not remain a free text input'
)

const rulesBlock = source.slice(
  source.indexOf('const rules = reactive<FormRules>'),
  source.indexOf('const resetForm')
)
assert.match(
  rulesBlock,
  /classification:\s*\[[\s\S]*new Error\('请选择变更后的类别'\)[\s\S]*trigger:\s*'change'/,
  'renewal category validation must be triggered by dropdown changes'
)

const submitBlock = source.slice(source.indexOf('const submit = async'), source.indexOf('</script>'))
assert.match(
  submitBlock,
  /payload\.append\('classification',\s*form\.classification\.trim\(\)\)/,
  'renewal submit must keep sending changed category through the existing classification field'
)

const renewalLifecycleE2e = read(renewalLifecycleE2ePath)
assert.match(
  renewalLifecycleE2e,
  /registration-certificate-upload-classification[\s\S]*'二类'/,
  'renewal lifecycle E2E must choose the initial upload category through the upload dropdown'
)
assert.match(
  renewalLifecycleE2e,
  /registration-certificate-renewal-classification[\s\S]*'三类'/,
  'renewal lifecycle E2E must choose the changed renewal category through the renewal dropdown anchor'
)
assert.doesNotMatch(
  renewalLifecycleE2e,
  /input\[placeholder="请输入变更后的类别"\]\)\.fill/,
  'renewal lifecycle E2E must not fill the old category input placeholder'
)
assert.match(
  renewalLifecycleE2e,
  /classification:\s*'三类'/,
  'renewal lifecycle E2E evidence must expect the selected dropdown category'
)
assert.match(
  renewalLifecycleE2e,
  /expect\(renewalDetail\.classification\)\.toBe\('三类'\)/,
  'renewal lifecycle E2E must verify the selected dropdown category'
)

for (const file of [renewalRowDialogE2ePath, registrationCertificateRealFlowPath]) {
  const e2e = read(file)
  assert.doesNotMatch(
    e2e,
    /input\[placeholder="请输入变更后的类别"\]/,
    `${file} must not locate the removed renewal category input placeholder`
  )
  assert.match(
    e2e,
    /registration-certificate-renewal-classification/,
    `${file} must locate the renewal category dropdown by its stable anchor`
  )
}
