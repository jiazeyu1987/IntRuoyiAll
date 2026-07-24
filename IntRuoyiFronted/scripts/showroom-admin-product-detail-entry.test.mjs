import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('showroom product list exposes a detailed-information action for existing products', () => {
  const source = readText('src/views/showroom-admin/components/ProductListTable.vue')

  assert.match(source, /详细信息|编辑详情/)
  assert.match(source, /emit\('detail', row\.raw\)/)
  assert.match(source, /detail:\s*\[/)
})

test('showroom admin product page mounts the real ProductDetailDialog entry', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /ProductDetailDialog/)
  assert.match(source, /@detail="openProductDetail"/)
  assert.match(source, /productDetailDialogVisible/)
  assert.match(source, /activeProductDetailId/)
  assert.match(source, /const openProductDetail = async|const openProductDetail = \(/)
})
