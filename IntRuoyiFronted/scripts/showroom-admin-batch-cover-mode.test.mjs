import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('showroom-admin batch cover api contract exposes mode and skipped-existing summary', () => {
  const source = readText('src/api/showroom-admin/index.ts')

  assert.match(source, /ShowroomProductCoverGenerationMode/)
  assert.match(source, /coverGenerationMode\?: ShowroomProductCoverGenerationMode/)
  assert.match(source, /skippedExistingCount: number/)
})

test('showroom-admin batch cover flow lets publicity users choose regenerate-all or missing-only mode', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /ElMessageBox/)
  assert.match(source, /selectBatchCoverGenerationMode/)
  assert.match(source, /confirmButtonText: '重新生成所有'/)
  assert.match(source, /cancelButtonText: '只生成未上传的'/)
  assert.match(source, /distinguishCancelAndClose: true/)
  assert.match(source, /coverGenerationMode/)
  assert.match(source, /buildProductBatchGeneratePayload\(coverGenerationMode\)/)
  assert.match(source, /skippedExistingCount/)
  assert.match(source, /跳过已有封面/)
})
