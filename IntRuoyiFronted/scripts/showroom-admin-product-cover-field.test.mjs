import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('showroom product basic dialog exposes an optional cover-image upload field', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /UploadImg/)
  assert.match(source, /label="封面"/)
  assert.match(source, /productForm\.coverImage/)
  assert.match(source, /cover_image/)
  assert.doesNotMatch(source, /<el-form-item label="封面" required>/)
})

test('showroom admin api exposes product ai cover generation endpoint', () => {
  const source = readText('src/api/showroom-admin/index.ts')

  assert.match(source, /generateProductCoverImage/)
  assert.match(source, /url: '\/showroom\/product\/generate-cover-image'/)
})

test('showroom product basic dialog exposes ai cover button and approval gate message', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /AI生成/)
  assert.match(source, /handleGenerateProductCoverImage/)
  assert.match(source, /ShowroomAdminApi\.generateProductCoverImage/)
  assert.match(source, /需要产品基础信息经过审核之后才可以AI生成封面/)
  assert.match(source, /AI封面生成中，请稍候/)
  assert.match(source, /AI封面仍在生成中，请稍候/)
  assert.match(source, /requireGeneratedCoverImageUrl/)
  assert.match(source, /AI封面已生成，已回填表单，尚未保存草稿或发布/)
  assert.doesNotMatch(source, /syncGeneratedProductCoverBaseline\(productForm\.coverImage\)/)
})
