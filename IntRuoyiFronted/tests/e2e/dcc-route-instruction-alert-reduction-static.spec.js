const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const routePage = readSource('src/views/dcc/controlled-file/routes/index.vue')

assert.doesNotMatch(
  routePage,
  /审批路线页统一承载审核功能/,
  'route page must remove the redundant instructional info alert above the table'
)
assert.doesNotMatch(
  routePage,
  /若要调整会签岗位、批准岗位或生效时间/,
  'route page must not keep toolbar instructions as a page-level alert'
)
assert.doesNotMatch(
  routePage,
  /<el-alert[\s\S]*?type="info"[\s\S]*?审批路线页统一承载审核功能/,
  'route page must not render the old info alert'
)

for (const token of [
  'data-testid="dcc-route-summary"',
  'dcc.controlledFile.routes.preview',
  'routePreviewError',
  'handlePreview'
]) {
  assert.match(routePage, new RegExp(token), `route page must keep ${token}`)
}

assert.doesNotMatch(
  routePage,
  /审批矩阵|CategoryMatrixDialog|openMatrixDialog/,
  'route page must no longer expose matrix maintenance entrypoints'
)

const mainRouteTableIndex = routePage.indexOf('data-user-table-key="dcc.controlledFile.routes.main"')
assert.ok(mainRouteTableIndex > 0, 'route page must keep the main route table')
const beforeRouteTable = routePage.slice(0, mainRouteTableIndex)
assert.doesNotMatch(
  beforeRouteTable,
  /<el-alert[\s\S]*?type="info"/,
  'route page must not place an info alert between toolbar and the main route table'
)

assert.doesNotMatch(
  routePage,
  /mock|placeholder data|降级|吞异常|默认成功/i,
  'route instruction cleanup must not introduce mock, downgrade, swallowed errors, or default success'
)

console.log('PASS: DCC route instruction alert reduction static contract')
