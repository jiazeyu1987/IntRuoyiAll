const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const submitterSource = readSource('src/views/dcc/controlled-file/upload/submitter.ts')
const uploadPageSource = readSource('src/views/dcc/controlled-file/upload/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:product-category-rule:static'],
  'node tests/e2e/dcc-product-category-rule-static.spec.js',
  'package.json 必须提供 DCC 产品类别差异规则静态契约脚本'
)

assert.match(
  submitterSource,
  /PRODUCT_BOUND_CATEGORY_PREFIXES[\s\S]*DCC_FVM_DHF_[\s\S]*DCC_FVM_DMR_/,
  '上传提交器必须声明 DHF/DMR 类别前缀。'
)
assert.match(
  submitterSource,
  /isDccProductRequiredForCategoryCode/,
  '上传提交器必须能识别 DHF/DMR 类别需要产品编号。'
)
assert.match(
  submitterSource,
  /validateDccProjectProductCode[\s\S]*productRequired[\s\S]*请选择包含项目代码的 DCC 项目/,
  '产品校验必须在 DHF/DMR 类别缺 DCC 项目代码产品编号时阻止提交。'
)
assert.match(
  uploadPageSource,
  /isDccProductRequiredForCategoryCode\(selectedCategory\.value\?\.code\)/,
  '上传页必须按当前类别判断产品是否必选。'
)
assert.match(
  uploadPageSource,
  /placeholder="选择 DCC 项目后自动生成"/,
  '上传页产品编号必须提示由 DCC 项目自动生成。'
)
assert.match(
  uploadPageSource,
  /v-if="isProductRequiredForSelectedCategory"/,
  '上传页必须在 DHF/DMR 类别显示产品编号必选提示。'
)

console.log('PASS: DCC product category rule static contract')
