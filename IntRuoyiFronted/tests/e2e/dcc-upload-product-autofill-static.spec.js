const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const uploadPagePath = path.join(repoRoot, 'src/views/dcc/controlled-file/upload/index.vue')
const packageJsonPath = path.join(repoRoot, 'package.json')

const uploadPage = fs.readFileSync(uploadPagePath, 'utf8')
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'))

assert.equal(
  packageJson.scripts['e2e:dcc:upload-product-autofill:static'],
  'node tests/e2e/dcc-upload-product-autofill-static.spec.js',
  'package.json must expose the DCC upload product autofill static contract'
)

assert.match(
  uploadPage,
  /<el-form-item label="产品编号" prop="productCode">[\s\S]*<el-input[\s\S]*v-model="formData\.productCode"[\s\S]*readonly[\s\S]*placeholder="选择 DCC 项目后读取正式产品编码"/,
  'Upload product number must be readonly and read from the formal server projection'
)

assert.match(
  uploadPage,
  /const applyDccProjectCodeProductNumber = async \(\) => \{[\s\S]*previewControlledFileProjectProduct\(projectCodeId\)[\s\S]*readProjectProductIdentity\(result, projectCodeId\)[\s\S]*formData\.productCode = product\.productCode \|\| ''/,
  'Product number must consume the validated formal preview, never a displayed project code'
)

assert.match(
  uploadPage,
  /validateDccProjectProductCode\(\s*formData\.productCode,\s*isProductRequiredForSelectedCategory\.value\s*\)/,
  'Product-required categories still validate the formally projected product code'
)

assert.match(
  uploadPage,
  /const handleProjectCodeChange = async \(\) => \{[\s\S]*applyDccProjectCodeProductNumber\(\)/,
  'Changing DCC project must refresh its formal product preview'
)

assert.match(
  uploadPage,
  /const handleCategoryChange = async \(\) => \{[\s\S]*applyDccProjectCodeProductNumber\(\)/,
  'Changing category must keep product aligned to the selected project formal preview'
)

assert(
  !uploadPage.includes('getDccProductOptions') &&
    !uploadPage.includes('DCC_PRODUCT_STATUS_ENABLE') &&
    !uploadPage.includes('DccControlledFileProductOptionVO') &&
    !uploadPage.includes('tryAutofillProductFromSelectedProject') &&
    !uploadPage.includes('applyProductMasterSelection') &&
    !uploadPage.includes('handleProductMasterChange'),
  'Upload must not guess product identity by an independent option list or name match'
)
assert.doesNotMatch(uploadPage, /formData\.productCode = selectedProjectCode\.value\?\.projectCode/,
  'Project code is not a fallback product identity')
assert.match(uploadPage, /projectProductRequestSequence\+\+/, 'Unmount must invalidate the formal preview')

assert(!uploadPage.includes('generateProductCode'), 'Upload page must not generate a temporary product code')

console.log('PASS: DCC upload formal product preview static contract')
