const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(root, '..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readWorkspaceSource = (relativePath) =>
  fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const pageSource = readSource('src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue')
const controllerSource = readWorkspaceSource(
  'ruoyi-vue-pro/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/DccProjectCodeController.java'
)
const migrationSource = readWorkspaceSource('ruoyi-vue-pro/sql/mysql/20260707_dcc_ai_category_permission_menu.sql')

assert.equal(
  packageJson.scripts['e2e:dcc:project-code-ai-category-permission:static'],
  'node tests/e2e/dcc-project-code-ai-category-permission-static.spec.js',
  'package.json must expose the DCC project-code AI category permission static test'
)

for (const token of [
  "@ss.hasPermission('dcc:project-code:update')",
  "@ss.hasPermission('dcc:controlled-file:update')",
  'associated-files/ai-category-candidates',
  'associated-files/{fileId:\\\\d+}/ai-category'
]) {
  assert.ok(controllerSource.includes(token), `backend AI category endpoint must require permission token: ${token}`)
}

const aiButtonStart = pageSource.indexOf('data-testid="dcc-project-code-ai-category"')
assert.ok(aiButtonStart >= 0, 'AI category button must exist')
const aiButtonBlock = pageSource.slice(pageSource.lastIndexOf('<el-button', aiButtonStart), pageSource.indexOf('</el-button>', aiButtonStart))
const batchAiButtonStart = pageSource.indexOf('data-testid="dcc-project-code-batch-ai-category"')
assert.ok(batchAiButtonStart >= 0, 'Batch AI category button must exist')
const batchAiButtonBlock = pageSource.slice(
  pageSource.lastIndexOf('<el-button', batchAiButtonStart),
  pageSource.indexOf('</el-button>', batchAiButtonStart)
)

for (const token of [
  'v-if="canRunAiCategory"',
  'AI分类',
  ':loading="aiCategoryRunning"',
  '@click="handleAiCategoryAssociatedFiles"'
]) {
  assert.ok(aiButtonBlock.includes(token), `AI category button must include token: ${token}`)
}

for (const token of [
  "import { checkPermi, checkRole } from '@/utils/permission'",
  "checkPermi(['dcc:project-code:update'])",
  "checkPermi(['dcc:controlled-file:update'])",
  'const canRunAiCategory = computed('
]) {
  assert.ok(pageSource.includes(token), `frontend AI category permission must require both permissions: ${token}`)
}

for (const token of [
  'v-if="canRunBatchAiCategory"',
  '批量AI分类',
  ':loading="batchAiCategoryRunning"',
  '@click="handleBatchAiCategoryProjectCodes"'
]) {
  assert.ok(batchAiButtonBlock.includes(token), `batch AI category button must include token: ${token}`)
}

for (const token of [
  "checkRole(['doc_control'])",
  'const canRunBatchAiCategory = computed('
]) {
  assert.ok(pageSource.includes(token), `batch AI category permission must align with doc_control backend role: ${token}`)
}

const restoreStart = pageSource.indexOf('const restoreLatestBatchAiCategoryTask = async () => {')
assert.ok(restoreStart >= 0, 'restoreLatestBatchAiCategoryTask must exist')
const restoreBlock = pageSource.slice(restoreStart, pageSource.indexOf('const handleBatchAiCategoryProjectCodes', restoreStart))
assert.ok(
  restoreBlock.indexOf('if (!canRunBatchAiCategory.value)') >= 0 &&
    restoreBlock.indexOf('if (!canRunBatchAiCategory.value)') < restoreBlock.indexOf('getLatestControlledFileBatchRecognitionTask'),
  'latest batch AI task restore must check canRunBatchAiCategory before calling the backend'
)

assert.ok(
  !pageSource.includes(`v-hasPermi="['dcc:project-code:update', 'dcc:controlled-file:update']"`),
  'frontend must not use the any-match permission directive for backend endpoints that require both permissions'
)

assert.ok(
  migrationSource.includes('dcc:project-code:update') &&
    migrationSource.includes('dcc:controlled-file:update'),
  'AI category permission migration must seed both backend-required permissions'
)

assert.ok(
  migrationSource.includes("source_menu.`path` = 'controlled-file/categories'"),
  'AI category permission migration must mirror DCC role coverage from the category maintenance page'
)

console.log('PASS: DCC project-code AI category permission static contract')
