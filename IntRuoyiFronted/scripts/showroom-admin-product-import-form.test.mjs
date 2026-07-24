import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('Showroom admin product import form component exists', () => {
  const componentPath = path.join(
    root,
    'src/views/showroom-admin/product/ShowroomProductImportForm.vue'
  )

  assert.ok(
    fs.existsSync(componentPath),
    'Showroom product import form must exist because showroom-admin/index.vue imports it'
  )
})

test('Showroom admin index wires the product import form', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(
    source,
    /import ShowroomProductImportForm from '@\/views\/showroom-admin\/product\/ShowroomProductImportForm\.vue'/
  )
  assert.match(
    source,
    /<ShowroomProductImportForm ref="productImportFormRef" @success="handleProductImportSuccess" \/>/
  )
  assert.match(source, /const openProductImportForm = \(\) => \{/)
  assert.match(source, /productImportFormRef\.value\.open\('STANDARD'\)/)
  assert.match(source, /const openProductBaseWorkbookImportForm = \(\) => \{/)
  assert.match(source, /productImportFormRef\.value\.open\('BASE_WORKBOOK'\)/)
  assert.match(source, /const handleProductImportSuccess = async \(\) => \{/)
  assert.match(source, /await loadProductRows\(\)/)
})

test('Showroom product import form uses real import APIs and exposes explicit modes', () => {
  const source = readText('src/views/showroom-admin/product/ShowroomProductImportForm.vue')

  assert.match(source, /ShowroomAdminApi/)
  assert.match(source, /ShowroomAdminApi\.getProductImportTemplate\(/)
  assert.match(source, /ShowroomAdminApi\.importProductExcel\(/)
  assert.match(source, /ShowroomAdminApi\.importProductBaseWorkbook\(/)
  assert.match(source, /type ShowroomProductImportMode = 'STANDARD' \| 'BASE_WORKBOOK'|type ShowroomProductImportMode/)
  assert.match(source, /defineExpose\(\{ open \}\)/)
  assert.match(source, /defineEmits\(\['success'\]\)/)
  assert.match(source, /download\.excel\(/)
  assert.match(source, /FormData/)
  assert.match(source, /产品 Excel 导入/)
  assert.match(source, /产品更新底表导入/)
  assert.match(source, /展厅讲解软件产品资料更新底表\.xlsx/)
  assert.match(source, /下载模板/)
})

test('Showroom product import form sends explicit same product action', () => {
  const source = readText('src/views/showroom-admin/product/ShowroomProductImportForm.vue')

  assert.match(source, /相同产品处理/)
  assert.match(source, /sameProductAction/)
  assert.match(source, /'SKIP'/)
  assert.match(source, /'OVERWRITE'/)
  assert.match(source, /<el-radio-button\s+value="SKIP">跳过<\/el-radio-button>/)
  assert.match(source, /<el-radio-button\s+value="OVERWRITE">覆盖<\/el-radio-button>/)
  assert.match(source, /formData\.append\('sameProductAction', sameProductAction\.value\)/)
  assert.match(source, /:disabled="formLoading"/)
})

test('Showroom product import API uses product import request timeout', () => {
  const source = readText('src/api/showroom-admin/index.ts')

  assert.match(source, /const SHOWROOM_PRODUCT_IMPORT_REQUEST_TIMEOUT = 5 \* 60 \* 1000/)
  assert.match(
    source,
    /importProductExcel:[\s\S]*request\.upload\(\{\s*url: '\/showroom\/product\/import-excel',\s*data,\s*timeout: SHOWROOM_PRODUCT_IMPORT_REQUEST_TIMEOUT\s*\}\)/,
    'Product Excel import must not inherit the global 30000ms Axios timeout'
  )
  assert.match(
    source,
    /importProductBaseWorkbook:[\s\S]*request\.upload\(\{\s*url: '\/showroom\/product\/import-base-workbook',\s*data,\s*timeout: SHOWROOM_PRODUCT_IMPORT_REQUEST_TIMEOUT\s*\}\)/,
    'Base workbook import must use its dedicated backend endpoint and long timeout'
  )
})

test('Showroom product import result dialog renders line breaks without raw HTML tags', () => {
  const source = readText('src/views/showroom-admin/product/ShowroomProductImportForm.vue')

  assert.doesNotMatch(
    source,
    /join\(['"`]<br\/>['"`]\)/,
    'Import result dialog must not pass raw <br/> text into the plain alert message'
  )
  assert.match(
    source,
    /showroom-product-import-result__line/,
    'Import result dialog should render each result line as an explicit safe text node'
  )
})
