import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const source = fs.readFileSync(
  path.join(root, 'src/views/showroom-admin/index.vue'),
  'utf8'
)

test('single product field publish does not require narration readiness before publishing fields', () => {
  assert.match(source, /const buildDirectPublishPayload = \(productDetail: ShowroomProductDetail\) =>/)
  assert.match(source, /sourceRevisionId: productDetail\.revisionId/)
  assert.match(source, /materialBlockers/)
  assert.doesNotMatch(source, /const narrationPair = await loadCurrentRevisionProductNarrationPair\(productDetail\)/)
  assert.doesNotMatch(source, /当前中文讲解稿不属于当前待发布版本/)
  assert.doesNotMatch(source, /当前英文讲解稿不属于当前待发布版本/)
})
