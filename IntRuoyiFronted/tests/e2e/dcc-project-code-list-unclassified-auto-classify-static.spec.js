const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const pageSource = readSource('src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:project-code-list-unclassified-auto-classify:static'],
  'node tests/e2e/dcc-project-code-list-unclassified-auto-classify-static.spec.js',
  'package.json must expose the DCC project-code list unclassified auto-classify static contract'
)

const exportButtonIndex = pageSource.indexOf('@click="handleExport"')
const importButtonIndex = pageSource.indexOf('@click="openImportDialog"')
const listAutoClassifyButtonIndex = pageSource.indexOf(
  'data-testid="dcc-project-code-list-auto-classify-unclassified"'
)
assert.ok(importButtonIndex >= 0, 'project-code toolbar must keep import button')
assert.ok(exportButtonIndex >= 0, 'project-code toolbar must keep export button')
assert.ok(
  listAutoClassifyButtonIndex >= 0 && listAutoClassifyButtonIndex < importButtonIndex,
  'list unclassified auto-classify button must render in the red-box toolbar position before import'
)

for (const token of [
  'data-testid="dcc-project-code-list-auto-classify-unclassified"',
  '按文件名归类未分类',
  'listUnclassifiedAutoClassifyRunning',
  'handleListAutoClassifyUnclassifiedProjectCodes',
  'listUnclassifiedAutoClassifyProgressVisible',
  'data-testid="dcc-project-code-list-auto-classify-progress"',
  'listUnclassifiedAutoClassifyProcessedProjects',
  'listUnclassifiedAutoClassifyTotalProjects',
  'listUnclassifiedAutoClassifyProcessedFiles'
]) {
  assert.ok(pageSource.includes(token), `list auto-classify UI must expose token: ${token}`)
}

for (const token of [
  'DCC_PROJECT_CODE_LIST_AUTO_CLASSIFY_PAGE_SIZE',
  'fetchAllFilteredProjectCodes',
  'getProjectCodePage({',
  '...queryParams',
  'pageNo',
  'pageCount',
  '包括未加载分页',
  '当前筛选条件',
  '全部项目代码'
]) {
  assert.ok(pageSource.includes(token), `list auto-classify must fetch all filtered pages: ${token}`)
}

for (const token of [
  'fetchProjectCodeAssociatedFiles',
  'getProjectCodeControlledFilesPage',
  'DCC_PROJECT_CODE_ASSOCIATED_NAVIGATION_PAGE_SIZE',
  'autoClassifyUnclassifiedFilesForProjectCode',
  'associatedAutoClassifyTargetOptions',
  'isAssociatedFileUnclassified',
  'resolveBestAssociatedAutoClassifyTarget',
  'buildDccAssociatedFileAutoClassifyPayload(file, target, projectCode)',
  'await updateControlledFileMetadata(file.id, payload)'
]) {
  assert.ok(pageSource.includes(token), `list auto-classify must reuse formal detail logic: ${token}`)
}

assert.ok(
  pageSource.includes('targetOptions.length === 0') &&
    pageSource.includes('没有可用于归类的正式文件类型') &&
    pageSource.includes('await loadFileTypeTaxonomies()'),
  'list auto-classify must fail fast when formal taxonomy candidates are missing'
)

assert.ok(
  pageSource.includes('await message.confirm') &&
    pageSource.includes('将按当前筛选条件处理') &&
    pageSource.includes('不会只处理当前页'),
  'list auto-classify must ask for explicit confirmation before all-page metadata writes'
)

assert.ok(
  pageSource.includes('列表批量按文件名归类失败：') &&
    pageSource.includes('resolveAiCategoryErrorMessage(error)') &&
    pageSource.includes('throw error'),
  'list auto-classify failures must surface backend errors instead of swallowing them'
)

assert.ok(
  pageSource.includes('await getList()') &&
    pageSource.includes('detailDrawerVisible.value') &&
    pageSource.includes('await getAssociatedFiles()'),
  'list auto-classify must refresh the visible list and open detail navigation after writes'
)

assert.ok(
  pageSource.includes('batchAiCategoryRunning ||') &&
    pageSource.includes('listUnclassifiedAutoClassifyRunning'),
  'list auto-classify must be mutually exclusive with existing batch/detail classification actions'
)

assert.ok(
  !pageSource.includes('Math.random') &&
    !pageSource.includes('targetOptions[0] ||') &&
    !pageSource.includes('catch (error) {\n    return'),
  'list auto-classify must not use random targets, fallback targets, or swallowed errors'
)

console.log('PASS: DCC project-code list unclassified auto-classify static contract')
