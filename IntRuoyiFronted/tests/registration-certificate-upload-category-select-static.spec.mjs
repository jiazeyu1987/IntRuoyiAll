import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const exists = (relativePath) => existsSync(join(root, relativePath))
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')
const appRoot = exists('src') ? '' : 'IntRuoyiFronted'
const uploadDialogPath = `${appRoot ? `${appRoot}/` : ''}src/views/dcc/registration-certificate/upload/UploadDialog.vue`

assert.equal(exists(uploadDialogPath), true, `${uploadDialogPath} must exist`)

const source = read(uploadDialogPath)
const categoryItemStart = source.indexOf('<el-form-item label="类别"')
const categoryItemEnd = source.indexOf('</el-form-item>', categoryItemStart)

assert.notEqual(categoryItemStart, -1, 'upload dialog must render the category form item')
assert.notEqual(categoryItemEnd, -1, 'category form item must be closed')

const categoryItem = source.slice(categoryItemStart, categoryItemEnd)

assert.match(
  source,
  /const\s+REGISTRATION_CERTIFICATE_CLASSIFICATION_OPTIONS\s*=\s*\[\s*'三类',\s*'二类',\s*'一类'\s*\]\s+as\s+const/,
  'upload dialog must define the exact category options in business order'
)
assert.match(
  categoryItem,
  /<el-select\b[\s\S]*v-model="form\.classification"/,
  'category must be selected through an Element Plus dropdown'
)
assert.match(
  categoryItem,
  /data-testid="registration-certificate-upload-classification"/,
  'category select must expose a stable E2E anchor'
)
assert.match(categoryItem, /placeholder="请选择类别"/, 'category placeholder must match dropdown behavior')
assert.match(
  categoryItem,
  /v-for="option in REGISTRATION_CERTIFICATE_CLASSIFICATION_OPTIONS"/,
  'category select must render options from the exact category option list'
)
assert.match(categoryItem, /:label="option"/, 'category option label must use the business value')
assert.match(categoryItem, /:value="option"/, 'category option value must submit the business value')
assert.doesNotMatch(
  categoryItem,
  /<el-input\b[\s\S]*v-model="form\.classification"/,
  'category must not remain a free text input'
)

const rulesBlock = source.slice(
  source.indexOf('const rules = reactive<FormRules>'),
  source.indexOf('const resetForm')
)
assert.match(
  rulesBlock,
  /classification:\s*\[\{\s*required:\s*true,\s*message:\s*'请选择类别',\s*trigger:\s*'change'\s*\}\]/,
  'category validation must be triggered by dropdown changes'
)

const submitBlock = source.slice(source.indexOf('const submit = async'), source.indexOf('watch('))
assert.match(
  submitBlock,
  /payload\.append\('classification',\s*form\.classification\.trim\(\)\)/,
  'submit must keep sending category through the existing classification field'
)
