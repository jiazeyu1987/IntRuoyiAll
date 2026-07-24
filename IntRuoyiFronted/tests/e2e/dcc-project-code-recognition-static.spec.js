const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const workspaceRoot = path.resolve(root, '..')
const readWorkspaceSource = (relativePath) => fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const workflowApiSource = readSource('src/api/dcc/controlledFile/workflow.ts')
const projectCodeApiSource = readSource('src/api/dcc/controlledFile/projectCodes.ts')
const detailSource = readSource('src/views/dcc/controlled-file/detail/index.vue')
const basicInfoPanelSource = readSource('src/views/dcc/controlled-file/shared/ControlledFileBasicInfoPanel.vue')
const basicDataPageSource = readSource(
  'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'
)
const projectCodeServiceSource = readWorkspaceSource(
  'ruoyi-vue-pro/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/DccProjectCodeServiceImpl.java'
)
const recognitionServiceSource = readWorkspaceSource(
  'ruoyi-vue-pro/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileProjectCodeRecognitionServiceImpl.java'
)

const extractFrom = (source, startToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `未找到代码片段：${startToken}`)
  return source.slice(start, source.indexOf('\n}', start) + 2)
}

for (const forbidden of [
  '/recognize-product-name',
  'recognizeControlledFileProductName',
  'recognizeProductName',
  '识别产品名称'
]) {
  assert.ok(!workflowApiSource.includes(forbidden), `workflow API 不得继续使用旧识别接口：${forbidden}`)
  assert.ok(!detailSource.includes(forbidden), `文件详情不得继续调用旧识别逻辑：${forbidden}`)
  assert.ok(!basicInfoPanelSource.includes(forbidden), `基础信息面板不得继续暴露旧识别事件：${forbidden}`)
}

for (const token of [
  '/dcc/controlled-files/${id}/recognize-project-code',
  'recognizeControlledFileProjectCode',
  'ControlledFileProjectCodeRecognitionRespVO',
  'recognitionStatus',
  'NO_MATCH',
  'dccProjectCodeId',
  'matchType',
  'matchText'
]) {
  assert.ok(workflowApiSource.includes(token), `workflow API 必须声明新识别契约：${token}`)
}

for (const token of [
  '识别基础信息',
  '识别完成，未知基础数据，请人工确认',
  '识别完成，未识别项目名称，请人工确认',
  "result.recognitionStatus === 'UNKNOWN_DCC_BASIC_DATA'",
  "result.recognitionStatus === 'UNRECOGNIZED_PROJECT_NAME'",
  'projectCodeRecognitionLoading',
  "emit('recognizeProjectCode')",
  'DCC基础条目',
  'openDccProjectCode',
  '/mdm/project-code',
  'projectCodeId'
]) {
  assert.ok(basicInfoPanelSource.includes(token) || detailSource.includes(token),
    `文件详情必须支持基础条目识别和导航：${token}`)
}

for (const token of [
  '/dcc/project-codes/${id}',
  '/dcc/project-codes/${id}/controlled-files/page',
  'getProjectCode',
  'getProjectCodeControlledFilesPage'
]) {
  assert.ok(projectCodeApiSource.includes(token), `DCC 基础数据 API 必须声明关联文档契约：${token}`)
}

for (const token of [
  'fileTypeLevel1',
  'fileTypeLevel2',
  'fileTypeLevel3',
  'fileTypeLevel4',
  'fileTypeLevel5'
]) {
  assert.ok(workflowApiSource.includes(token), `受控文件响应类型必须声明识别分类层级字段：${token}`)
}

for (const token of [
  'useRoute()',
  'projectCodeId',
  'openProjectCodeDetail',
  'detailDrawerVisible',
  '关联文档',
  'associatedFiles',
  'getProjectCodeControlledFilesPage',
  'openControlledFileViewer'
]) {
  assert.ok(basicDataPageSource.includes(token), `DCC 基础数据页必须支持条目详情和关联文档：${token}`)
}
for (const token of [
  'associatedStageGroups',
  'selectedAssociatedStageGroup',
  'selectedAssociatedTypeGroup',
  'associatedTaxonomyStageOptions',
  'associatedTaxonomyStageNames',
  'associatedTaxonomyStageNameMap',
  'resolveDccFileTypeTaxonomyStageName',
  'fileTypeLevel2',
  'fileTypeLevel3',
  '未分类',
  'dcc-project-code-associated-layout'
]) {
  assert.ok(basicDataPageSource.includes(token), `DCC 基础数据关联文档必须按阶段/类型/文件三列展示：${token}`)
}

for (const forbidden of [
  'DccFileCategoryLifecycleStageEnum',
  'LIFECYCLE_STAGE_LABELS',
  'resolveLifecycleStageLabel',
  'lifecycleStageLabel'
]) {
  assert.ok(!projectCodeServiceSource.includes(forbidden), `项目代码关联文件分类不得使用旧阶段兜底：${forbidden}`)
  assert.ok(!recognitionServiceSource.includes(forbidden), `受控文件项目识别不得使用旧阶段兜底：${forbidden}`)
}

for (const token of [
  'category.getFileTypeTaxonomyId() != null',
  'resolveCategoryTaxonomyPath',
  'fileTypeTaxonomyAdminService.resolveActivePath(category.getFileTypeTaxonomyId())'
]) {
  assert.ok(projectCodeServiceSource.includes(token), `项目代码关联文件分类必须以默认文件分类为真源：${token}`)
}
assert.ok(
  recognitionServiceSource.includes('matchedCategory.getFileTypeTaxonomyId() != null') &&
    !recognitionServiceSource.includes('matchedCategory.getLifecycleStage()'),
  '受控文件项目识别必须只用绑定的文件分类路径，不得读取旧 lifecycleStage'
)
assert.ok(
  basicDataPageSource.includes("openControlledFileViewer(router, route, row.id, 'project-code')"),
  'DCC 基础数据关联文档必须打开受控预览页'
)
assert.ok(
  !basicDataPageSource.includes("name: 'DccControlledFileDetail'"),
  'DCC 基础数据关联文档不得继续跳普通文件详情页'
)

const newApiSegments = [
  extractFrom(workflowApiSource, 'export const recognizeControlledFileProjectCode'),
  extractFrom(projectCodeApiSource, 'export const getProjectCode ='),
  extractFrom(projectCodeApiSource, 'export const getProjectCodeControlledFilesPage')
].join('\n')

assert.ok(
  !/mock|placeholder data|fallback|降级|吞异常/.test(newApiSegments),
  'DCC 项目识别关联不得引入 mock、placeholder、fallback、降级或吞异常'
)

console.log('PASS: DCC project-code recognition static contract')
