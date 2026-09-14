const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const permissionCopyFiles = [
  'src/views/dcc/controlled-file/shared/handlingSummary.ts',
  'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixUserLookupDialog.vue',
  'src/views/dcc/controlled-file/categories/components/CategoryReviewMatrixUserLookupDialog.vue',
  'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixTable.vue',
  'src/views/dcc/controlled-file/detail/index.vue',
  'src/views/dcc/controlled-file/categories/components/CategoryMatrixDialog.vue'
]

for (const relativePath of permissionCopyFiles) {
  const source = readSource(relativePath)
  for (const forbiddenCopy of ['已发布预览', '待审预览', '待审原件预览', '待审预览边界', '可预览和下载现行版']) {
    assert.ok(
      !source.includes(forbiddenCopy),
      `${relativePath} must use 查看 wording instead of permission copy "${forbiddenCopy}"`
    )
  }
}

const viewMatrixLookup = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixUserLookupDialog.vue'
)
const reviewMatrixLookup = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryReviewMatrixUserLookupDialog.vue'
)
const viewMatrixTable = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixTable.vue'
)
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const handlingSummary = readSource('src/views/dcc/controlled-file/shared/handlingSummary.ts')

assert.ok(viewMatrixLookup.includes('label="已发布查看"'), 'view matrix lookup must show published view wording')
assert.ok(viewMatrixLookup.includes('label="待审查看"'), 'view matrix lookup must show pending view wording')
assert.ok(reviewMatrixLookup.includes('label="已发布查看"'), 'review matrix lookup must show published view wording')
assert.ok(reviewMatrixLookup.includes('label="待审查看"'), 'review matrix lookup must show pending view wording')
assert.ok(viewMatrixTable.includes('查看矩阵'), 'view matrix table must keep view-matrix wording')
assert.ok(detailPage.includes('detail-access-explanation__label">已发布查看</span>'), 'detail page must label access explanation as view')
assert.ok(handlingSummary.includes("nextStep: '可查看和下载现行版'"), 'controlled file status copy must use view wording')

console.log('PASS: DCC view/preview permission copy is unified')
