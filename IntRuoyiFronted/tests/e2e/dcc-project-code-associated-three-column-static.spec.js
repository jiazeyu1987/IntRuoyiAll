const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const pageSource = readSource('src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue')
const projectCodeApiSource = readSource('src/api/dcc/controlledFile/projectCodes.ts')
const taxonomyStageUtility = readSource('src/views/dcc/controlled-file/shared/file-type-taxonomy-stage.ts')

assert.equal(
  packageJson.scripts['e2e:dcc:project-code-associated-three-column:static'],
  'node tests/e2e/dcc-project-code-associated-three-column-static.spec.js',
  'package.json must expose the DCC project-code associated document three-column static test'
)

for (const token of [
  'DCC_TECHNICAL_DOCUMENT_ROOT_NAME',
  'DCC_UNCLASSIFIED_TAXONOMY_STAGE',
  'buildDccFileTypeTaxonomyPathMap',
  'buildDccFileTypeTaxonomyStageNameMap',
  'buildDccFileTypeTaxonomyStageTypeNameMap',
  'buildDccFileTypeTaxonomyStageTypeOptionsMap',
  'getDccFileTypeTaxonomyStageRows',
  'toDccFileTypeTaxonomyStageOptions',
  'resolveDccFileTypeTaxonomyStageName',
  'resolveDccFileTypeTaxonomyStageTypeName'
]) {
  assert.ok(taxonomyStageUtility.includes(token), `shared taxonomy stage utility must expose ${token}`)
}

assert.ok(
  pageSource.includes("from '../../shared/file-type-taxonomy-stage'") &&
    pageSource.includes('toDccFileTypeTaxonomyStageOptions') &&
    pageSource.includes('buildDccFileTypeTaxonomyStageNameMap') &&
    pageSource.includes('buildDccFileTypeTaxonomyStageTypeNameMap') &&
    pageSource.includes('buildDccFileTypeTaxonomyStageTypeOptionsMap') &&
    pageSource.includes('resolveDccFileTypeTaxonomyStageName') &&
    pageSource.includes('resolveDccFileTypeTaxonomyStageTypeName') &&
    pageSource.includes('DCC_UNCLASSIFIED_TAXONOMY_STAGE'),
  'associated documents must reuse shared DCC file type taxonomy stage helpers'
)

assert.ok(
  pageSource.includes("from '@/api/dcc/controlledFile/fileTypeTaxonomies'") &&
    pageSource.includes('getFileTypeTaxonomyList') &&
    pageSource.includes('DccFileTypeTaxonomyVO') &&
    pageSource.includes('fileTypeTaxonomies'),
  'associated documents must load current DCC file type taxonomy data'
)

for (const token of [
  'DCC_PROJECT_CODE_STAGE_OPTIONS',
  '01 plan 策划',
  '02 input 输入',
  '03 output 输出',
  '04 verification 验证',
  '05 validation 确认',
  '06 transfer 转移'
]) {
  assert.ok(!pageSource.includes(token), `associated documents must not keep fixed legacy stage option: ${token}`)
}

for (const token of [
  'associatedStageGroups',
  'associatedNavigationFiles',
  'selectedAssociatedPagedFiles',
  'selectedAssociatedFilesTotal',
  'handleAssociatedFilePagination',
  'selectedAssociatedStageKey',
  'selectedAssociatedTypeKey',
  'selectedAssociatedStageGroup',
  'selectedAssociatedTypeGroup',
  'selectAssociatedStage',
  'selectAssociatedType',
  'resolveAssociatedStageKey',
  'resolveAssociatedTypeName'
]) {
  assert.ok(pageSource.includes(token), `associated documents must use three-column state: ${token}`)
}

for (const token of [
  'dcc-project-code-associated-layout',
  'dcc-project-code-associated-stage-list',
  'dcc-project-code-associated-type-list',
  'dcc-project-code-associated-file-table',
  'data-testid="dcc-project-code-associated-stage-list"',
  'data-testid="dcc-project-code-associated-type-list"',
  'data-testid="dcc-project-code-associated-file-table"'
]) {
  assert.ok(pageSource.includes(token), `associated documents must render three-column UI token: ${token}`)
}

assert.ok(
  pageSource.includes('size="96%"'),
  'associated document drawer must use a stable wide percent size'
)

assert.ok(
  pageSource.includes('DCC_PROJECT_CODE_ASSOCIATED_NAVIGATION_PAGE_SIZE = 200'),
  'associated document navigation request pageSize must not exceed backend max pageSize 200'
)

assert.ok(
  !pageSource.includes('DCC_PROJECT_CODE_ASSOCIATED_NAVIGATION_PAGE_SIZE = 10000'),
  'associated document navigation request must not use pageSize 10000'
)

assert.ok(
  !pageSource.includes('minmax(760px, 1fr)'),
  'associated document layout must not keep a fixed 760px right-column floor that can hide the drawer'
)

assert.match(
  pageSource,
  /\.dcc-project-code-associated-layout\s*{[\s\S]*?grid-template-columns: minmax\(150px, 0\.7fr\) minmax\(180px, 0\.8fr\) minmax\(0, 1\.8fr\);/,
  'associated document default layout must be shrinkable before media breakpoints apply'
)

assert.ok(
  pageSource.includes('.dcc-project-code-detail') &&
    pageSource.includes('overflow-x: hidden;') &&
    pageSource.includes('.dcc-project-code-associated-file-table') &&
    pageSource.includes('overflow-x: auto;'),
  'associated document drawer must prevent whole-drawer horizontal overflow and keep table overflow local'
)

assert.ok(
  pageSource.includes('@media (max-width: 1240px)') &&
    pageSource.includes('grid-template-columns: minmax(0, 1fr);') &&
    pageSource.includes('.dcc-project-code-associated-file-table') &&
    pageSource.includes('overflow-x: auto;'),
  'associated document layout must collapse and keep table overflow local on constrained drawer widths'
)

assert.match(
  pageSource,
  /<el-table-column label="文件名称" prop="fileName" min-width="360">/,
  'associated document file-name column must be wide enough for longer names'
)

assert.match(
  pageSource,
  /<el-table-column label="文件编号" prop="fileNumber" min-width="280" \/>/,
  'associated document file-number column must be wide enough for longer numbers'
)

assert.ok(
  pageSource.includes("file.fileTypeLevel2") && pageSource.includes("file.fileTypeLevel3"),
  'associated documents must keep fileTypeLevel2/fileTypeLevel3 only as legacy fallback metadata'
)

assert.ok(
  pageSource.includes('associatedTaxonomyStageOptions') &&
    pageSource.includes('associatedTaxonomyStageNames') &&
    pageSource.includes('stageMap.set(option.value') &&
    pageSource.includes('associatedTaxonomyStageNames.value.has(stage.key) || stage.count > 0'),
  'associated documents must build stage navigation from current taxonomy stage options'
)

assert.ok(
  pageSource.includes('associatedTaxonomyStageNames.value.has(stage)') &&
    pageSource.includes('resolveDccFileTypeTaxonomyStageName(file, associatedTaxonomyStageNameMap.value)'),
  'associated documents must prefer current fileTypeLevel2 values and derive stage from fileTypeTaxonomyId when needed'
)

assert.ok(
  pageSource.includes('associatedTaxonomyStageTypeOptionsMap') &&
    pageSource.includes('associatedTaxonomyStageTypeNameMap') &&
    pageSource.includes('associatedTaxonomyStageTypeOptionsMap.value.get(stageKey) || []') &&
    pageSource.includes('for (const option of associatedStageTypeOptions)') &&
    pageSource.includes('typeMap.set(option.value'),
  'associated documents must prebuild the file type column from DCC taxonomy direct children of the selected stage'
)

assert.ok(
  /resolveDccFileTypeTaxonomyStageTypeName\(\s*file,\s*associatedTaxonomyStageTypeNameMap\.value\s*\)/.test(pageSource) &&
    pageSource.includes('resolvedTaxonomyType?.typeName') &&
    pageSource.includes('normalizeAssociatedLevel(file.fileTypeLevel3)'),
  'associated files must be grouped by taxonomy path level 3 before falling back to legacy fileTypeLevel3'
)

assert.ok(
  pageSource.includes('未分类文件类型') && pageSource.includes('未分类'),
  'associated documents must keep uncategorized files separate without forcing them into lifecycle stages'
)

const associatedFilesPanel = pageSource.slice(
  pageSource.indexOf('data-testid="dcc-project-code-associated-files"'),
  pageSource.indexOf('<el-table v-else :data="[]"')
)
const fileTablePanel = pageSource.slice(
  pageSource.indexOf('data-testid="dcc-project-code-associated-file-table"'),
  pageSource.indexOf('</section>', pageSource.indexOf('data-testid="dcc-project-code-associated-file-table"'))
)

assert.ok(
  fileTablePanel.includes('<Pagination') &&
    fileTablePanel.includes('v-model:page="associatedFilePage.pageNo"') &&
    fileTablePanel.includes('v-model:limit="associatedFilePage.pageSize"') &&
    fileTablePanel.includes('@pagination="handleAssociatedFilePagination"'),
  'associated document pagination must render inside the third file-table panel'
)

assert.ok(
  !associatedFilesPanel.replace(fileTablePanel, '').includes('<Pagination'),
  'associated document pagination must not render below the whole three-column layout'
)

assert.ok(
  pageSource.includes('for (const file of associatedNavigationFiles.value)') &&
    !pageSource.includes('for (const file of associatedFiles.value)'),
  'associated stage/type groups must be based on stable navigation files instead of paged file rows'
)

assert.ok(
  pageSource.includes("openControlledFileViewer(router, route, row.id, 'project-code')"),
  'associated document file name must keep opening the controlled file viewer'
)

assert.ok(
  !pageSource.includes('resolveAssociatedFileTypePath') &&
    !pageSource.includes('group.pathLabel') &&
    !pageSource.includes('path.join(\' / \')'),
  'associated documents must no longer render one table per full category path'
)

for (const token of [
  'AI分类',
  'handleAiCategoryAssociatedFiles',
  'aiCategoryRunning',
  'aiCategoryProgressPercent',
  'resolveAiCategoryErrorMessage',
  'AI分类失败：已处理',
  '失败文件',
  '后端错误',
  'data-testid="dcc-project-code-ai-category"',
  'data-testid="dcc-project-code-ai-category-percent"',
  'for (const candidate of candidates)'
]) {
  assert.ok(pageSource.includes(token), `associated documents must expose AI category token: ${token}`)
}

assert.ok(
  pageSource.includes('catch (error)') &&
    pageSource.includes('await getAssociatedFiles()') &&
    pageSource.includes('aiCategoryRunning.value = false'),
  'AI category failure branch must display backend errors, refresh current list, and restore button state'
)

assert.ok(
  pageSource.includes('data-testid="dcc-project-code-ai-category-percent"'),
  'single project AI category progress must keep plain percent text in the detail drawer'
)

assert.ok(
  !pageSource.includes('while ('),
  'AI category execution must use a bounded candidate snapshot loop instead of while loops'
)

for (const token of [
  'DccProjectCodeAssociatedFileAiCategoryRespVO',
  'getProjectCodeAssociatedFileAiCategoryCandidates',
  'classifyProjectCodeAssociatedFileByAi',
  '/dcc/project-codes/${id}/associated-files/ai-category-candidates',
  '/dcc/project-codes/${id}/associated-files/${fileId}/ai-category'
]) {
  assert.ok(projectCodeApiSource.includes(token), `project code API must expose AI category token: ${token}`)
}

console.log('PASS: DCC project-code associated documents three-column static contract')
