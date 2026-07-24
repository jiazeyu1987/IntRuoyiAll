const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const tagsViewStore = read('src/store/modules/tagsView.ts')

const singletonPaths = [
  'approval-center/manager/form-center/template',
  'mdm/form-center/template',
  'mes/pro/batch-record-form-list',
  'pro/batch-record-form-list',
  'mes/pro/batch-record-template',
  'pro/batch-record-template'
]

assert.match(
  tagsViewStore,
  /view\.meta\?\.tagsViewKeyMode\s*===\s*'path'[\s\S]*TAGS_VIEW_PATH_IDENTITY_PATHS\.has\(normalizedPath\)[\s\S]*return normalizedPath \? `\/\$\{normalizedPath\}` : view\.path/,
  'TagsView store must support path identity so query-only template jumps reuse one tab'
)

for (const protectedPath of singletonPaths) {
  assert.match(
    tagsViewStore,
    new RegExp(`['"]${protectedPath.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}['"]`),
    `${protectedPath} must use path identity so template tabs do not render duplicate (2)/(3) labels`
  )
}

assert.doesNotMatch(
  tagsViewStore,
  /catch\s*\([^)]*\)\s*\{\s*\}|fallback|placeholder|mock/i,
  'Template tab singleton behavior must not rely on fallback, placeholder, mock, or swallowed errors'
)

console.log('PASS: form and batch template tags view singleton static contract')
