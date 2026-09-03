const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const componentPath = path.join(
  root,
  'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue'
)
const source = fs.readFileSync(componentPath, 'utf8')

const defaultColumnsMatch = source.match(
  /const productCatalogDefaultColumns: UserTableColumnDefinition\[\] = \[([\s\S]*?)\]\r?\n\r?\nconst \{/
)
assert.ok(defaultColumnsMatch, '产品目录默认列池必须可定位。')
const defaultColumns = defaultColumnsMatch[1]

const defaultClassificationIndex = defaultColumns.indexOf("key: 'classification'")
const defaultRegistrationNameIndex = defaultColumns.indexOf("key: 'registrationCertificateName'")
assert.notEqual(defaultClassificationIndex, -1, '产品目录默认列池必须包含分类列。')
assert.notEqual(defaultRegistrationNameIndex, -1, '产品目录默认列池必须包含注册证名称列。')
assert.ok(
  defaultClassificationIndex < defaultRegistrationNameIndex,
  '产品目录默认列池中“分类”列必须位于“注册证名称”列前。'
)

const tableTemplateMatch = source.match(
  /<el-table[\s\S]*?data-user-table-key="dcc\.productCatalog\.main"[\s\S]*?<\/el-table>/
)
assert.ok(tableTemplateMatch, '产品目录右侧明细表格模板必须可定位。')
const tableTemplate = tableTemplateMatch[0]

const visibleClassificationIndex = tableTemplate.indexOf("isProductCatalogColumnVisible('classification')")
const visibleRegistrationNameIndex = tableTemplate.indexOf(
  "isProductCatalogColumnVisible('registrationCertificateName')"
)
assert.notEqual(visibleClassificationIndex, -1, '产品目录明细表格必须渲染分类列。')
assert.notEqual(visibleRegistrationNameIndex, -1, '产品目录明细表格必须渲染注册证名称列。')
assert.ok(
  visibleClassificationIndex < visibleRegistrationNameIndex,
  '产品目录明细表格中“分类”列必须显示在“注册证名称”列前。'
)

for (const preservedColumn of [
  'productCode',
  'projectCode',
  'registrationCertificateNumber',
  'effectiveDate',
  'expiryDate',
  'productStatus',
  'actions'
]) {
  assert.match(
    tableTemplate,
    new RegExp(`isProductCatalogColumnVisible\\('${preservedColumn}'\\)`),
    `产品目录明细表格必须保留 ${preservedColumn} 列。`
  )
}

console.log('PASS: DCC product catalog category column order static contract')
