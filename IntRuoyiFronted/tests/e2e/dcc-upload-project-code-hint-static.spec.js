const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const uploadPage = readSource('src/views/dcc/controlled-file/upload/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:upload-project-code-hint:static'],
  'node tests/e2e/dcc-upload-project-code-hint-static.spec.js',
  'package.json must expose the DCC upload project-code hint static contract'
)

assert.match(
  uploadPage,
  /data-testid="dcc-upload-product-code-binding-hint"/,
  'DHF/DMR project-code helper must have a stable test id for UI regression coverage'
)

assert.match(
  uploadPage,
  /const isRequiredProjectCodeBound = computed\(\(\) => Boolean\(projectProduct\.value[\s\S]*projectProduct\.value\.source !== 'UNBOUND'[\s\S]*projectProduct\.value\.projectCodeId === String\(formData\.dccProjectCodeId\)\)\)/,
  'Upload readiness must bind the formal product projection to the exact selected project'
)

assert.match(
  uploadPage,
  /const productCodeBindingHintText = computed\(\(\) => \{[\s\S]*已解析编号：\$\{formData\.productCode\.trim\(\)\}[\s\S]*DHF\/DMR 类别必须选择已绑定正式产品的 DCC 项目[\s\S]*\}\)/,
  'Product-required helper distinguishes a formally bound product from UNBOUND'
)

assert.match(
  uploadPage,
  /const productCodeBindingHintClass = computed\(\(\) =>[\s\S]*isRequiredProjectCodeBound\.value[\s\S]*text-\[var\(--el-color-success\)\][\s\S]*text-\[var\(--el-color-danger\)\]/,
  'DHF/DMR helper must use success styling once the project code is bound and danger styling only while missing'
)

assert.doesNotMatch(
  uploadPage,
  /v-if="isProductRequiredForSelectedCategory"\s+class="[^"]*text-\[var\(--el-color-danger\)\]/,
  'DHF/DMR helper must not be hard-coded as red whenever the category requires a project code'
)

console.log('PASS: DCC upload project-code hint static contract')
