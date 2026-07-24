const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(frontendRoot, '..')

function readFromFrontend(relativePath) {
  const absolutePath = path.join(frontendRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required frontend file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

function readFromWorkspace(relativePath) {
  const absolutePath = path.join(workspaceRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required workspace file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const apiSource = readFromFrontend('src/api/dcc/controlledFile/fileTypeTaxonomies.ts')
const pageSource = readFromFrontend(
  'src/views/dcc/controlled-file/basic-data/file-type-taxonomy/index.vue'
)
const categoryFormSource = readFromFrontend(
  'src/views/dcc/controlled-file/categories/components/CategoryForm.vue'
)
const categoryListSource = readFromFrontend('src/views/dcc/controlled-file/categories/index.vue')
const metadataDialogSource = readFromFrontend(
  'src/views/dcc/controlled-file/shared/ControlledFileMetadataDialog.vue'
)
const workflowSource = readFromFrontend('src/api/dcc/controlledFile/workflow.ts')
const migrationSource = readFromWorkspace('ruoyi-vue-pro/sql/mysql/20260719_dcc_file_type_taxonomy.sql')

for (const marker of [
  '/dcc/file-type-taxonomies',
  'getFileTypeTaxonomyList',
  'createFileTypeTaxonomy',
  'updateFileTypeTaxonomy',
  'deleteFileTypeTaxonomy'
]) {
  assert.match(apiSource, new RegExp(marker), `file type taxonomy API must expose ${marker}.`)
}

for (const marker of [
  "defineOptions({ name: 'DccFileTypeTaxonomyBasicDataPage' })",
  'DCC文件分类',
  '五级分类',
  '新增一级',
  '新增下级',
  "v-hasPermi=\"['dcc:controlled-file:category:manage']\"",
  'getFileTypeTaxonomyList',
  'createFileTypeTaxonomy',
  'updateFileTypeTaxonomy',
  'deleteFileTypeTaxonomy'
]) {
  assert.match(pageSource, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `taxonomy page must include ${marker}.`)
}

assert.match(
  migrationSource,
  /component`?,?\s*`component_name`?[\s\S]*dcc\/controlled-file\/basic-data\/file-type-taxonomy\/index[\s\S]*DccFileTypeTaxonomyBasicDataPage/,
  'dynamic menu migration must place DCC文件分类 under the basic-data component path.'
)
assert.match(
  migrationSource,
  /dcc:controlled-file:category:manage/,
  'dynamic menu migration must reuse the DCC category management permission.'
)

assert.match(categoryFormSource, /fileTypeTaxonomyId/, 'file category form must persist the bound taxonomy id.')
assert.match(
  categoryFormSource,
  /默认文件分类[\s\S]*getFileTypeTaxonomyList/,
  'file category form must let users bind a default configured taxonomy path.'
)
assert.match(
  categoryListSource,
  /默认文件分类[\s\S]*taxonomyPathMap/,
  'file category list must show the bound configured taxonomy path.'
)

assert.match(workflowSource, /fileTypeTaxonomyId\?: number \| null/, 'workflow DTOs must carry fileTypeTaxonomyId.')
assert.match(
  metadataDialogSource,
  /label="文件分类"[\s\S]*fileTypeTaxonomyId[\s\S]*getFileTypeTaxonomyList/,
  'metadata dialog must choose configured taxonomy instead of free-text levels.'
)
assert.match(
  metadataDialogSource,
  /fileTypeTaxonomyId: metadataForm\.fileTypeTaxonomyId \|\| null/,
  'metadata update payload must submit fileTypeTaxonomyId.'
)
assert.doesNotMatch(
  metadataDialogSource,
  /label="文件类别 I"[\s\S]*el-input/,
  'metadata dialog must not keep free-text file category level inputs as the primary editing path.'
)

console.log('PASS: dcc file type taxonomy basic data static contract')
