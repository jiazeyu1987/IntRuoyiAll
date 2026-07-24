import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('ProductDetailDialog uses version center as the primary history entry', () => {
  const source = readText('src/views/showroom-admin/product/ProductDetailDialog.vue')

  assert.match(source, /进入版本中心/)
  assert.match(source, /open-version-center/)
  assert.match(source, /emit\('open-version-center'/)
  assert.match(source, /ShowroomAdminApi\.getProduct\(/)
  assert.doesNotMatch(source, /selectedRevisionId/)
  assert.doesNotMatch(source, /versionHistoryRows/)
  assert.doesNotMatch(source, /ShowroomAdminApi\.getProductHistory/)
  assert.doesNotMatch(source, /handleRevisionChange/)
  assert.doesNotMatch(source, /查看版本|浏览版本/)
})

test('showroom admin product page routes both list and detail entries into the version center route', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /@version-center="openProductVersionCenter"/)
  assert.match(source, /@open-version-center="handleOpenProductVersionCenterFromDetail"/)
  assert.match(source, /ShowroomAdminProductVersionCenter/)
  assert.match(source, /closeProductDetailDialog\(\)/)
  assert.match(source, /revisionId/)
})
