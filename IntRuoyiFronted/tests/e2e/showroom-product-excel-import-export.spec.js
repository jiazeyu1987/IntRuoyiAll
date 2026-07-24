const fs = require('fs')
const path = require('path')

const listPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/components/ProductListTable.vue'
)
const listSource = fs.readFileSync(listPath, 'utf8')

if (!listSource.includes('导出')) {
  throw new Error(`missing export excel action in ${listPath}`)
}

if (!listSource.includes('导入')) {
  throw new Error(`missing import excel action in ${listPath}`)
}

if (!listSource.includes("'export-excel': []")) {
  throw new Error(`missing export excel emit contract in ${listPath}`)
}

if (!listSource.includes("'import-excel': []")) {
  throw new Error(`missing import excel emit contract in ${listPath}`)
}

if (!listSource.includes("'import-base-workbook': []")) {
  throw new Error(`missing base workbook import emit contract in ${listPath}`)
}

const apiPath = path.resolve(__dirname, '../../src/api/showroom-admin/index.ts')
const apiSource = fs.readFileSync(apiPath, 'utf8')

if (!apiSource.includes('exportProductExcel')) {
  throw new Error(`missing exportProductExcel api in ${apiPath}`)
}

if (!apiSource.includes("url: '/showroom/product/export-excel'")) {
  throw new Error(`missing showroom product export-excel endpoint in ${apiPath}`)
}

if (!apiSource.includes('getProductImportTemplate')) {
  throw new Error(`missing getProductImportTemplate api in ${apiPath}`)
}

if (!apiSource.includes("url: '/showroom/product/get-import-template'")) {
  throw new Error(`missing showroom product import template endpoint in ${apiPath}`)
}

if (!apiSource.includes('importProductExcel')) {
  throw new Error(`missing importProductExcel api in ${apiPath}`)
}

if (!apiSource.includes("url: '/showroom/product/import-excel'")) {
  throw new Error(`missing showroom product import-excel endpoint in ${apiPath}`)
}

if (!apiSource.includes('importProductBaseWorkbook')) {
  throw new Error(`missing importProductBaseWorkbook api in ${apiPath}`)
}

if (!apiSource.includes("url: '/showroom/product/import-base-workbook'")) {
  throw new Error(`missing showroom product import-base-workbook endpoint in ${apiPath}`)
}

const indexPath = path.resolve(__dirname, '../../src/views/showroom-admin/index.vue')
const indexSource = fs.readFileSync(indexPath, 'utf8')

if (!indexSource.includes('@export-excel="handleExportProductExcel"')) {
  throw new Error(`missing product excel export binding in ${indexPath}`)
}

if (!indexSource.includes('@import-excel="openProductImportForm"')) {
  throw new Error(`missing product excel import binding in ${indexPath}`)
}

if (!indexSource.includes('@import-base-workbook="openProductBaseWorkbookImportForm"')) {
  throw new Error(`missing base workbook import binding in ${indexPath}`)
}

if (!indexSource.includes('<ShowroomProductImportForm')) {
  throw new Error(`missing showroom product import form mount in ${indexPath}`)
}

if (!indexSource.includes('const handleExportProductExcel = async () =>')) {
  throw new Error(`missing handleExportProductExcel action in ${indexPath}`)
}

if (!indexSource.includes('const openProductImportForm = () =>')) {
  throw new Error(`missing openProductImportForm action in ${indexPath}`)
}

if (!indexSource.includes('const openProductBaseWorkbookImportForm = () =>')) {
  throw new Error(`missing openProductBaseWorkbookImportForm action in ${indexPath}`)
}

const importFormPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/product/ShowroomProductImportForm.vue'
)

if (!fs.existsSync(importFormPath)) {
  throw new Error(`missing showroom product import form component: ${importFormPath}`)
}

const importFormSource = fs.readFileSync(importFormPath, 'utf8')

if (!importFormSource.includes('下载模板')) {
  throw new Error(`missing import template download entry in ${importFormPath}`)
}

if (!importFormSource.includes('产品更新底表导入')) {
  throw new Error(`missing base workbook import title in ${importFormPath}`)
}

if (!importFormSource.includes('展厅讲解软件产品资料更新底表.xlsx')) {
  throw new Error(`missing base workbook import guidance in ${importFormPath}`)
}

if (!importFormSource.includes('accept=".xlsx, .xls"')) {
  throw new Error(`missing xls/xlsx accept restriction in ${importFormPath}`)
}

if (!importFormSource.includes('产品资料修改版-补充产品资料.xlsx')) {
  throw new Error(`missing reference workbook download name in ${importFormPath}`)
}

for (const label of ['产品名-中文', '卖点文案', '产品图']) {
  if (!importFormSource.includes(label)) {
    throw new Error(`missing reference workbook column ${label} in ${importFormPath}`)
  }
}

for (const label of ['导出文件可再次导入', '产品列表', '奖项', 'E 列必须放图片']) {
  if (!importFormSource.includes(label)) {
    throw new Error(`missing award roundtrip import guidance "${label}" in ${importFormPath}`)
  }
}

if (importFormSource.includes('可读取“产品”“卖点文案”“产品图”列')) {
  throw new Error(`import form still advertises removed 产品 column in ${importFormPath}`)
}

const downloadPath = path.resolve(__dirname, '../../src/utils/download.ts')
const downloadSource = fs.readFileSync(downloadPath, 'utf8')

if (!downloadSource.includes('document.body.appendChild(downA)')) {
  throw new Error(`download link must be attached to the document before clicking in ${downloadPath}`)
}

if (!/setTimeout\(\s*\(\)\s*=>[\s\S]*URL\.revokeObjectURL\(href\)/.test(downloadSource)) {
  throw new Error(`blob URL cleanup must be deferred until after the browser starts downloading in ${downloadPath}`)
}

console.log('PASS: showroom product excel import/export flow is wired in frontend source')
