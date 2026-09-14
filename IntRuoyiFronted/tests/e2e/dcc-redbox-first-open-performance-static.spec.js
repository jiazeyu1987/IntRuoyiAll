const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(repoRoot, '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const readWorkspaceSource = (relativePath) =>
  fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const menuSchema = readWorkspaceSource('IntRuoyiBackend/sql/mysql/20260513_dcc_base_schema.sql')
const categoriesPage = readSource('src/views/dcc/controlled-file/categories/index.vue')
const directoriesPage = readSource('src/views/dcc/controlled-file/directories/index.vue')
const routesPage = readSource('src/views/dcc/controlled-file/routes/index.vue')
const uploadPage = readSource('src/views/dcc/controlled-file/upload/index.vue')
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')

assert.equal(
  packageJson.scripts?.['e2e:dcc:redbox-first-open-performance:static'],
  'node tests/e2e/dcc-redbox-first-open-performance-static.spec.js',
  'package.json must expose the DCC red-box first-open performance contract'
)

for (const [title, component] of [
  ['文档目录', 'dcc/controlled-file/directories/index'],
  ['文控权限', 'dcc/controlled-file/categories/index'],
  ['上传审批', 'dcc/controlled-file/routes/index'],
  ['文件上传', 'dcc/controlled-file/upload/index'],
  ['受控浏览', 'dcc/controlled-file/browser/index']
]) {
  assert.ok(menuSchema.includes(`'${title}'`), `DCC menu seed must keep ${title}`)
  assert.ok(menuSchema.includes(`'${component}'`), `DCC menu seed must map ${title} to ${component}`)
}

for (const [source, component, importPath] of [
  [categoriesPage, 'CategoryReviewMatrixTable', './components/CategoryReviewMatrixTable.vue'],
  [categoriesPage, 'CategoryViewMatrixTable', './components/CategoryViewMatrixTable.vue'],
  [categoriesPage, 'DirectoryAuthorizationTabPanel', '../components/DirectoryAuthorizationTabPanel.vue'],
  [categoriesPage, 'CategoryForm', './components/CategoryForm.vue'],
  [categoriesPage, 'CategoryUploadSizePolicyDialog', './components/CategoryUploadSizePolicyDialog.vue'],
  [directoriesPage, 'DirectoryForm', './components/DirectoryForm.vue'],
  [uploadPage, 'ProtectedPdfViewer', '../view/index.vue'],
  [browserPage, 'ControlledFileMetadataDialog', '../shared/ControlledFileMetadataDialog.vue']
]) {
  assert.doesNotMatch(
    source,
    new RegExp(`import\\s+${component}\\s+from\\s+['"]${importPath.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}['"]`),
    `${component} must not be synchronously imported by a first-open page`
  )
  assert.match(
    source,
    new RegExp(`const\\s+${component}\\s*=\\s*defineAsyncComponent\\(\\s*\\(\\)\\s*=>\\s*import\\(['"]${importPath.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}['"]\\)\\s*\\)`),
    `${component} must be split behind defineAsyncComponent`
  )
}

for (const [label, name] of [
  ['审阅矩阵', 'review-matrix'],
  ['查看矩阵', 'view-matrix'],
  ['目录授权', 'directory-auth']
]) {
  assert.match(
    categoriesPage,
    new RegExp(`<el-tab-pane[\\s\\S]*label="${label}"[\\s\\S]*name="${name}"[\\s\\S]*\\slazy[\\s\\S]*>`),
    `文控权限 ${label} must be a lazy tab pane`
  )
}

for (const [source, component, guard] of [
  [categoriesPage, 'CategoryForm', 'categoryFormMounted'],
  [categoriesPage, 'CategoryUploadSizePolicyDialog', 'categoryUploadPolicyDialogMounted'],
  [directoriesPage, 'DirectoryForm', 'directoryFormMounted'],
  [browserPage, 'ControlledFileMetadataDialog', 'metadataDialogMounted']
]) {
  assert.match(
    source,
    new RegExp(`<${component}[\\s\\S]*v-if="${guard}"`),
    `${component} must not mount until the user opens it`
  )
}

assert.match(
  routesPage,
  /const loadInitialCategoryOptions = async \(\) => \{[\s\S]*?categories\.value = await getFileCategoryList\(\)/,
  '流程路线首开应只加载类别候选'
)
assert.doesNotMatch(
  routesPage,
  /onMounted\(async \(\) => \{[\s\S]*?getApprovalPositionList\(\)[\s\S]*?getSimpleUserList\(\)[\s\S]*?handleQuery\(\)/,
  '流程路线首开不得自动加载岗位、用户并立即查询预览'
)
assert.match(
  routesPage,
  /const loadRouteSubjectLookups = async \(\) => \{[\s\S]*getApprovalPositionList\(\)[\s\S]*getSimpleUserList\(\)/,
  '流程路线岗位和用户候选应延迟到预览需要时加载'
)

assert.doesNotMatch(
  `${categoriesPage}\n${directoriesPage}\n${routesPage}\n${uploadPage}\n${browserPage}`,
  /mock|placeholder data|降级|吞异常/i,
  'first-open performance optimization must not add mock data, degradation, or swallowed errors'
)

console.log('PASS: DCC red-box first-open performance static contract')
