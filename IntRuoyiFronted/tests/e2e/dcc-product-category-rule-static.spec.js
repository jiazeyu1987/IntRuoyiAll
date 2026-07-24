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
  '上传提交器必须能识别 DHF/DMR 类别需要产品主数据。'
)
assert.match(
  submitterSource,
  /validateProductMasterSelection[\s\S]*productRequired[\s\S]*请选择产品主数据/,
  '产品校验必须在产品相关类别缺产品时阻止提交。'
)
assert.match(
  uploadPageSource,
  /isDccProductRequiredForCategoryCode\(selectedCategory\.value\?\.code\)/,
  '上传页必须按当前类别判断产品是否必选。'
)
assert.match(
  uploadPageSource,
  /:placeholder="isProductRequiredForSelectedCategory \? '请选择产品主数据' : '可不选择产品主数据'"/,
  '上传页必须按当前类别切换产品选择提示。'
)
assert.match(
  uploadPageSource,
  /v-if="isProductRequiredForSelectedCategory"/,
  '上传页必须在 DHF/DMR 类别显示产品必选提示。'
)

console.log('PASS: DCC product category rule static contract')
