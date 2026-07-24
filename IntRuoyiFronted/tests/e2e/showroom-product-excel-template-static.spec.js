const fs = require('fs')
const path = require('path')

const expectedWorkbookName = '产品资料修改版-补充产品资料.xlsx'

const apiPath = path.resolve(__dirname, '../../src/api/showroom-admin/index.ts')
const apiSource = fs.readFileSync(apiPath, 'utf8')

if (!apiSource.includes("url: '/showroom/product/get-import-template'")) {
  throw new Error(`missing product import template endpoint in ${apiPath}`)
}

if (!apiSource.includes("url: '/showroom/product/export-excel'")) {
  throw new Error(`missing product export endpoint in ${apiPath}`)
}

const importFormPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/product/ShowroomProductImportForm.vue'
)
const importFormSource = fs.readFileSync(importFormPath, 'utf8')

if (!importFormSource.includes(expectedWorkbookName)) {
  throw new Error(`import template download name must match ${expectedWorkbookName}`)
}

for (const label of ['产品名-中文', '卖点文案', '产品图']) {
  if (!importFormSource.includes(label)) {
    throw new Error(`import form must expose reference workbook column: ${label}`)
  }
}

for (const label of ['导出文件可再次导入', '产品列表', '奖项', 'E 列必须放图片']) {
  if (!importFormSource.includes(label)) {
    throw new Error(`import form must explain award roundtrip template contract: ${label}`)
  }
}

if (importFormSource.includes('可读取“产品”“卖点文案”“产品图”列')) {
  throw new Error('import form must not advertise the removed 产品 column as an import field')
}

if (importFormSource.includes('展厅产品导入模板.xls')) {
  throw new Error('import template must not keep old .xls template filename')
}

const indexPath = path.resolve(__dirname, '../../src/views/showroom-admin/index.vue')
const indexSource = fs.readFileSync(indexPath, 'utf8')

if (!indexSource.includes(expectedWorkbookName)) {
  throw new Error(`export filename must match ${expectedWorkbookName}`)
}

if (indexSource.includes('展厅产品文字资料.xls')) {
  throw new Error('product export must not keep old .xls filename')
}

console.log('PASS: showroom product Excel frontend template contract matches reference workbook')
