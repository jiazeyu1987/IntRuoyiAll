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
  /const applyProductMasterSelection = \(product: DccControlledFileProductOptionVO \| undefined\) => \{[\s\S]*formData\.productMasterId = product\?\.id \?\? null[\s\S]*formData\.productCode = product\?\.dccProductCode \|\| ''/,
  'Product autofill must select the formal product master id and copy its DCC product code'
)

assert.match(
  uploadPage,
  /const resolveProjectProductAutofillKeywords = \(project: DccProjectCodeRespVO \| undefined\) =>[\s\S]*project\?\.projectName[\s\S]*project\?\.projectCode[\s\S]*project\?\.docControlNo/,
  'Product autofill must derive candidate search keywords from the selected DCC project'
)

assert.match(
  uploadPage,
  /const tryAutofillProductFromSelectedProject = async \(\) => \{[\s\S]*if \(!isProductRequiredForSelectedCategory\.value[\s\S]*return[\s\S]*const matchingProducts = uniqueProductOptionsById[\s\S]*if \(matchingProducts\.length === 1\) \{[\s\S]*applyProductMasterSelection\(matchingProducts\[0\]\)[\s\S]*return[\s\S]*message\.warning\('未能根据 DCC 项目唯一匹配产品主数据，请手动选择产品主数据'\)/,
  'DHF/DMR upload must auto-select only one unique formal product match and otherwise require manual selection'
)

assert.match(
  uploadPage,
  /const handleProjectCodeChange = async \(\) => \{[\s\S]*await tryAutofillProductFromSelectedProject\(\)/,
  'Changing DCC project must attempt product autofill'
)

assert.match(
  uploadPage,
  /const handleCategoryChange = async \(\) => \{[\s\S]*await tryAutofillProductFromSelectedProject\(\)/,
  'Changing to a DHF/DMR category must attempt product autofill'
)

assert.match(
  uploadPage,
  /const handleProductMasterChange = \(productId: number \| undefined\) => \{[\s\S]*applyProductMasterSelection\(product\)/,
  'Manual product selection must use the same formal product binding path as autofill'
)

assert(!uploadPage.includes('generateProductCode'), 'Upload page must not generate a temporary product code')

console.log('PASS: DCC upload product autofill static contract')
