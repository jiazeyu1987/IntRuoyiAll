import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('dcc positions query uses an applied query state instead of empty click handler', () => {
  const source = readText('src/views/dcc/controlled-file/positions/index.vue')
  assert.match(source, /const appliedQueryParams = reactive/)
  assert.match(source, /appliedQueryParams\.code/)
  assert.match(source, /appliedQueryParams\.name/)
  assert.match(source, /appliedQueryParams\.active/)
  assert.doesNotMatch(source, /const handleQuery = \(\) => \{\s*\}/)
})

test('dcc directories query uses an applied query state instead of empty click handler', () => {
  const source = readText('src/views/dcc/controlled-file/directories/index.vue')
  assert.match(source, /const appliedQueryParams = reactive/)
  assert.match(source, /appliedQueryParams\.name/)
  assert.match(source, /appliedQueryParams\.active/)
  assert.doesNotMatch(source, /const handleQuery = \(\) => \{\s*\}/)
})

test('printer label button opens real barcode detail flow', () => {
  const source = readText('src/views/mes/wm/barcode/components/PrinterLabel.vue')
  assert.match(source, /<BarcodeDetail ref="barcodeDetailRef"/)
  assert.match(source, /barcodeDetailRef\.value\?\.openByBusiness/)
  assert.match(source, /BarcodeBizTypeEnum/)
  assert.doesNotMatch(source, /标签打印功能暂未实现/)
})

test('sales notice finish action hands off to product sales creation flow', () => {
  const source = readText('src/views/mes/wm/salesnotice/SalesNoticeForm.vue')
  assert.match(source, /<ProductSalesForm ref="productSalesFormRef"/)
  assert.match(source, /productSalesFormRef\.value\?\.openByNotice/)
  assert.match(source, /生成销售出库单|销售出库单流程/)
  assert.doesNotMatch(source, /执行出库功能暂时不支持/)
})

test('product sales form exposes openByNotice prefill entrypoint', () => {
  const source = readText('src/views/mes/wm/productsales/ProductSalesForm.vue')
  assert.match(source, /const openByNotice = async/)
  assert.match(source, /noticeId/)
  assert.match(source, /generateCode/)
  assert.match(source, /defineExpose\(\{ open, openByNotice \}\)/)
})

test('barcode config template button uses a real selector dialog', () => {
  const source = readText('src/views/mes/wm/barcode/config/BarcodeConfigForm.vue')
  assert.match(source, /<BarcodeTemplateSelectDialog ref="templateDialogRef"/)
  assert.match(source, /templateDialogRef\.value\?\.open/)
  assert.match(source, /handleTemplateSelected/)
  assert.doesNotMatch(source, /打印模板选择功能暂未实现/)
})

test('barcode template selector provides report-list browsing and path normalization helpers', () => {
  const source = readText('src/views/mes/wm/barcode/config/components/BarcodeTemplateSelectDialog.vue')
  assert.match(source, /\/jmreport\/list\?token=/)
  assert.match(source, /normalizeTemplateValue/)
  assert.match(source, /IFrame|iframe/i)
  assert.match(source, /defaultDesignerSrc/)
})
