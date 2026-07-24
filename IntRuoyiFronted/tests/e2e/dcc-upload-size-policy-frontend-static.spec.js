const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

const categoryPage = read('src/views/dcc/controlled-file/categories/index.vue')
const dialog = read('src/views/dcc/controlled-file/categories/components/CategoryUploadSizePolicyDialog.vue')
const api = read('src/api/dcc/controlledFile/uploadSizePolicies.ts')

assert(
  categoryPage.includes('CategoryUploadSizePolicyDialog') && categoryPage.includes('上传策略'),
  'DCC category page must expose the row-level upload policy dialog'
)
assert(
  categoryPage.includes("v-hasPermi=\"['dcc:controlled-file:category:manage']\""),
  'row-level upload policy entry must use DCC category management permission'
)
assert(
  dialog.includes('CATEGORY_PURPOSE') && dialog.includes('SOURCE') && dialog.includes('maxBytes'),
  'dialog must support category-purpose source upload size policy fields'
)
assert(
  dialog.includes('createUploadSizePolicy') && dialog.includes('updateUploadSizePolicy'),
  'dialog must support create and update operations'
)
assert(
  api.includes('/dcc/protection/upload-size-policies') &&
    api.includes('/dcc/protection/upload-size-policies/effective'),
  'API wrapper must use the backend upload size policy management endpoints'
)

console.log('PASS: DCC upload size policy frontend static contract')
