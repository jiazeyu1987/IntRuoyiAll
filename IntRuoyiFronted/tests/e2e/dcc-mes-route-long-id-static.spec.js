const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')

const readSource = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const assertIncludes = (source, expected, message) => {
  assert.ok(source.includes(expected), `${message}\nMissing: ${expected}`)
}

const assertNotIncludes = (source, forbidden, message) => {
  assert.ok(!source.includes(forbidden), `${message}\nForbidden: ${forbidden}`)
}

const categoriesPage = readSource('src/views/dcc/controlled-file/categories/index.vue')
const directoryAuthPanel = readSource(
  'src/views/dcc/controlled-file/components/DirectoryAuthorizationTabPanel.vue'
)
const dccBrowserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const dccBrowserCache = readSource('src/views/dcc/controlled-file/browser/state-cache.ts')
const dccWorkflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')
const routeEditPage = readSource('src/views/mes/pro/route/RouteEditPage.vue')
const proRouteApi = readSource('src/api/mes/pro/route/index.ts')
const batchCellLinkPage = readSource('src/views/mes/pro/batchrecordcelllink/index.vue')
const batchCellLinkApi = readSource('src/api/mes/pro/batchrecordcelllink/index.ts')
const edhrSimulateStatic = readSource('tests/e2e/edhr-batch-template-simulate-static.spec.js')
const edhrRecordChangeStatic = readSource('tests/e2e/edhr-record-change-toolbar-advanced-static.spec.js')

assertIncludes(
  categoriesPage,
  "import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'",
  'DCC 分类页必须复用 routeQueryId 字符串 ID 解析工具。'
)
assertNotIncludes(
  categoriesPage,
  'Number(route.query.directoryId)',
  'DCC 分类页不得把 directoryId route query 转成 Number。'
)

assertIncludes(
  directoryAuthPanel,
  'initialDirectoryId?: string',
  'DCC 目录授权面板 initialDirectoryId 必须接收字符串 ID。'
)
assertNotIncludes(
  directoryAuthPanel,
  'Number(props.initialDirectoryId)',
  'DCC 目录授权面板不得数字化 initialDirectoryId。'
)

for (const token of [
  'parsePositiveNumber(route.query.directoryId)',
  'parsePositiveNumber(route.query.categoryId)',
  'parsePositiveNumber(route.query.batchRecognitionTaskId)'
]) {
  assertNotIncludes(dccBrowserPage, token, 'DCC 受控浏览业务 ID route query 不得数字化。')
}
assertIncludes(
  dccBrowserPage,
  "import { parsePositiveRouteQueryId, sameRouteQueryId } from '@/utils/routeQueryId'",
  'DCC 受控浏览必须使用字符串 ID 解析和比较工具。'
)

for (const token of [
  'directoryId?: string',
  'lastOpenedDirectoryId?: string',
  'categoryId?: string',
  'batchRecognitionTaskId?: string',
  'normalizePositiveIdText'
]) {
  assertIncludes(dccBrowserCache, token, 'DCC 受控浏览缓存状态必须按字符串保存业务 ID。')
}

for (const token of [
  'categoryId?: DccRouteId',
  'directoryId?: DccRouteId',
  'batchRecognitionTaskId?: DccRouteId'
]) {
  assertIncludes(dccWorkflowApi, token, 'DCC 受控浏览查询 API 入参必须允许字符串业务 ID。')
}

for (const token of [
  'Number(route.params.id || route.query.id)',
  'Number(route.query.routeProcessId)',
  'Number(normalizeRouteQueryText(route.query.routeVersionId))'
]) {
  assertNotIncludes(routeEditPage, token, 'MES 工艺路线编辑页不得数字化 route ID。')
}
assertIncludes(
  routeEditPage,
  "import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'",
  'MES 工艺路线编辑页必须复用 routeQueryId 字符串 ID 解析工具。'
)

for (const token of [
  'getRoute: async (id: MesRouteId)',
  'getRouteVersionList: async (routeId: MesRouteId)',
  'getRouteVersion: async (id: MesRouteId)'
]) {
  assertIncludes(proRouteApi, token, 'MES 工艺路线 API 必须允许字符串业务 ID。')
}

for (const token of [
  'routeId: parseNumber(route.query.routeId)',
  'definitionId: parseNumber(route.query.definitionId)',
  'versionId: parseNumber(route.query.versionId)'
]) {
  assertNotIncludes(batchCellLinkPage, token, 'MES 批记录单元格链接页不得数字化 route query ID。')
}
assertIncludes(
  batchCellLinkPage,
  "import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'",
  'MES 批记录单元格链接页必须复用 routeQueryId 字符串 ID 解析工具。'
)
for (const token of ['routeId?: EdhrRouteId', 'definitionId?: EdhrRouteId', 'versionId?: EdhrRouteId']) {
  assertIncludes(batchCellLinkApi, token, 'MES 批记录单元格链接 API 入参必须允许字符串业务 ID。')
}

assertNotIncludes(
  edhrSimulateStatic,
  'Number(route.query.id)',
  'eDHR 模拟填写静态契约不得继续断言旧 Number 写法。'
)
assertNotIncludes(
  edhrRecordChangeStatic,
  'parsePositiveNumber(route.query.batchExecutionId)',
  'eDHR 变更记录静态契约不得继续断言旧 parsePositiveNumber 写法。'
)

console.log('PASS: DCC/MES route Long ID static contract')
