const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const routeCandidateEntry = read('src/views/mes/pro/route/routeCandidateEntry.ts')
const proRouteApi = read('src/api/mes/pro/route/index.ts')

assert.match(
  routeEditPage,
  /import\s+\{\s*parsePositiveRouteQueryId\s*\}\s+from\s+['"]@\/utils\/routeQueryId['"]/,
  '工艺路线编辑页必须复用字符串 route query ID 解析工具，避免长 ID 被 Number 截断。'
)

assert.doesNotMatch(
  routeEditPage,
  /Number\(route\.params\.id\s*\|\|\s*route\.query\.id\)/,
  '工艺路线编辑页不得把列表传入的路线 ID 数字化。'
)

assert.doesNotMatch(
  routeEditPage,
  /Number\(normalizeRouteQueryText\(route\.query\.routeVersionId\)\)/,
  '工艺路线编辑页不得把候选版本 ID 数字化。'
)

assert.match(
  routeEditPage,
  /const\s+routeId\s*=\s*computed\(\(\)\s*=>\s*parsePositiveRouteQueryId\(route\.params\.id\s*\|\|\s*route\.query\.id\)\)/,
  '工艺路线编辑页必须从 params.id 或 query.id 解析出字符串路线 ID。'
)

assert.match(
  routeEditPage,
  /isCurrentRouteEditPage\.value\s*&&\s*!routeId\.value/,
  '无效入口校验必须基于字符串 ID 解析结果判断。'
)

assert.match(
  routeEditPage,
  /const\s+routeVersionId\s*=\s*parsePositiveRouteQueryId\(route\.query\.routeVersionId\)/,
  '候选版本上下文必须按字符串 ID 解析。'
)

assert.match(
  routeCandidateEntry,
  /import\s+\{\s*parsePositiveRouteQueryId\s*\}\s+from\s+['"]@\/utils\/routeQueryId['"]/,
  '候选版本入口必须复用字符串 ID 解析工具。'
)

assert.doesNotMatch(
  routeCandidateEntry,
  /Number\.isFinite\(routeId\)/,
  '候选版本入口不得用 Number.isFinite 校验路线 ID。'
)

for (const expected of [
  'export type MesRouteId = number | string',
  'getRoute: async (id: MesRouteId)',
  'getRouteVersionList: async (routeId: MesRouteId)',
  'getRouteVersion: async (id: MesRouteId)'
]) {
  assert.ok(proRouteApi.includes(expected), `MES 工艺路线 API 必须允许字符串业务 ID：${expected}`)
}

console.log('PASS: process-route-edit-valid-route-query-static')
