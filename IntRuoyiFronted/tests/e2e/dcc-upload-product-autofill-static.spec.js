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
  /<el-form-item label="产品编号" prop="productCode">[\s\S]*<el-input[\s\S]*v-model="formData\.productCode"[\s\S]*readonly[\s\S]*placeholder="选择 DCC 项目后自动带出"/,
  'Upload product number must be a readonly field populated from the selected DCC project code'
)

assert.match(
  uploadPage,
  /const applyDccProjectCodeProductNumber = \(\) => \{[\s\S]*formData\.productMasterId = null[\s\S]*formData\.productCode = selectedProjectCode\.value\?\.projectCode\?\.trim\(\) \|\| ''[\s\S]*\}/,
  'Product number autofill must copy selectedProjectCode.projectCode and clear productMasterId'
)

assert.match(
  uploadPage,
  /validateDccProjectProductCode\(\s*formData\.productCode,\s*isProductRequiredForSelectedCategory\.value\s*\)/,
  'DHF/DMR upload validation must require the DCC project code product number, not a product master'
)

assert.match(
  uploadPage,
  /const handleProjectCodeChange = async \(\) => \{[\s\S]*applyDccProjectCodeProductNumber\(\)/,
  'Changing DCC project must refresh product number from the selected project code'
)

assert.match(
  uploadPage,
  /const handleCategoryChange = async \(\) => \{[\s\S]*applyDccProjectCodeProductNumber\(\)/,
  'Changing category must keep product number aligned to the selected DCC project code'
)

assert(
  !uploadPage.includes('getDccProductOptions') &&
    !uploadPage.includes('DCC_PRODUCT_STATUS_ENABLE') &&
    !uploadPage.includes('DccControlledFileProductOptionVO') &&
    !uploadPage.includes('tryAutofillProductFromSelectedProject') &&
    !uploadPage.includes('applyProductMasterSelection') &&
    !uploadPage.includes('handleProductMasterChange') &&
    !uploadPage.includes('产品主数据'),
  'Upload product number must not depend on product master options, matching, or product-master wording'
)

assert(!uploadPage.includes('generateProductCode'), 'Upload page must not generate a temporary product code')

console.log('PASS: DCC upload product autofill static contract')
