import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('showroom admin api exposes product narration script generation and narration read endpoints', () => {
  const source = readText('src/api/showroom-admin/index.ts')

  assert.match(source, /startBatchGenerateNarrationScriptTask/)
  assert.match(source, /url: '\/showroom\/product\/batch-generate-narration-script\/start'/)
  assert.match(source, /getBatchGenerateNarrationScriptTaskStatus/)
  assert.match(source, /url: '\/showroom\/product\/batch-generate-narration-script\/status'/)
  assert.match(source, /translateProductFieldsToEn/)
  assert.match(source, /url: '\/showroom\/product\/translate-fields-to-en'/)
  assert.match(source, /generateProductNarrationScript/)
  assert.match(source, /url: '\/showroom\/product\/generate-narration-script'/)
  assert.match(source, /generateProductNarrationAudio/)
  assert.match(source, /url: '\/showroom\/product\/generate-narration-audio'/)
  assert.match(source, /getNarration:/)
  assert.match(source, /url: '\/showroom\/narration\/get'/)
})

test('product basic dialog exposes bilingual narration editor and english generation actions', () => {
  const source = readText('src/views/showroom-admin/index.vue')
  const dialogSource =
    source.match(/<el-dialog[\s\S]*?v-model="productDialogVisible"[\s\S]*?<\/el-dialog>/)?.[0] || ''

  assert.match(source, /productNarrationScriptTaskStatus/)
  assert.match(source, /loadProductNarrationScriptTaskStatus/)
  assert.match(source, /handleStartBatchGenerateNarrationScriptTask/)
  assert.match(source, /ShowroomAdminApi\.startBatchGenerateNarrationScriptTask/)
  assert.match(source, /ShowroomAdminApi\.getBatchGenerateNarrationScriptTaskStatus/)
  assert.match(source, /中文讲解稿/)
  assert.match(source, /英文讲解稿/)
  assert.match(source, /AI Translate/)
  assert.match(source, /生成讲解稿/)
  assert.doesNotMatch(dialogSource, /Generate Audio|生成语音/)
  assert.match(source, /handleGenerateProductNarrationAudioFromRow/)
  assert.match(source, /ShowroomAdminApi\.translateProductFieldsToEn/)
  assert.match(source, /ShowroomAdminApi\.generateProductNarrationScript/)
  assert.match(source, /ShowroomAdminApi\.saveNarrationDraft/)
  assert.match(source, /ShowroomAdminApi\.generateProductNarrationAudio/)
  assert.match(source, /ShowroomAdminApi\.getNarration/)
})

test('product detail dialog no longer owns narration editing', () => {
  const source = readText('src/views/showroom-admin/product/ProductDetailDialog.vue')

  assert.doesNotMatch(source, /中文讲解稿/)
  assert.doesNotMatch(source, /英文讲解稿/)
  assert.doesNotMatch(source, /生成讲解稿/)
  assert.doesNotMatch(source, /生成语音/)
})
