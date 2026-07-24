const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const routePage = readSource('src/views/dcc/controlled-file/routes/index.vue')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const routeTable = extractBetween(
  routePage,
  'data-user-table-key="dcc.controlledFile.routes.main"',
  '</el-table>'
)

assert.strictEqual(
  packageJson.scripts['e2e:dcc:route-summary:static'],
  'node tests/e2e/dcc-route-summary-static.spec.js',
  'package.json 必须提供 e2e:dcc:route-summary:static 脚本'
)

assert.ok(routeTable.includes('label="文件类别"'), '审批路线主表必须显示文件类别列')
assert.ok(routeTable.includes('row.categoryName'), '审批路线主表必须使用真实类别名称')

for (const stageNo of [1, 2, 3, 4]) {
  assert.ok(routeTable.includes(`label="节点${stageNo}"`), `审批路线主表必须显示节点${stageNo}列`)
  assert.ok(
    routeTable.includes(`formatRouteNodeAssignees(row, ${stageNo})`),
    `审批路线主表节点${stageNo}必须显示该阶段审批对象`
  )
}

for (const removedHeader of ['路线摘要', '节点摘要', '备注', '版本号', '生效状态', '生效时间']) {
  assert.ok(
    !routeTable.includes(`label="${removedHeader}"`),
    `审批路线主表不应继续显示 ${removedHeader} 表头`
  )
}

for (const removedToken of ['data-testid="dcc-route-summary"', 'row.nodeSummary', "row.remark || '-'", 'formatRouteEffectiveDate']) {
  assert.ok(!routeTable.includes(removedToken), `审批路线主表不应继续使用旧摘要/备注 token：${removedToken}`)
}

for (const behaviorToken of [
  'getApprovalRoutePage(queryParams)',
  'routeTotal.value = pageResult.total ?? 0',
  'previewApprovalRoute({ categoryId: queryParams.categoryId })',
  'handlePreview',
  'resolvePositionNames(row.candidateSourceIds)',
  'resolveUserNames(row.resolvedUserIds)'
]) {
  assert.ok(routePage.includes(behaviorToken), `审批路线原有行为必须保留：${behaviorToken}`)
}

assert.ok(routePage.includes('const formatRouteNodeAssignees = '), '审批路线主表必须按节点聚合审批对象')
assert.ok(routePage.includes('const formatRouteNodeSubject = '), '审批路线主表必须解析单个节点审批对象')
assert.ok(routePage.includes('row.nodes?.filter((node) => node.stageNo === stageNo)'), '节点列必须来自真实 nodes 数组')

assert.ok(
  !routePage.includes('审批矩阵'),
  '审批路线页不得继续提供审阅矩阵维护入口，新增修改删除必须仅作用于审批路线版本'
)
assert.ok(
  !routePage.includes('CategoryMatrixDialog'),
  '审批路线页不得继续依赖矩阵维护弹窗'
)
assert.ok(routePage.includes('handleCreateRoute'), '审批路线页必须提供新增路线入口')
assert.ok(routePage.includes('handleEditRoute'), '审批路线页必须提供行级修改入口')
assert.ok(routePage.includes('handleDeleteRoute'), '审批路线页必须提供行级删除入口')

assert.ok(
  !/mock|placeholder data|fallback|降级|吞异常/.test(routeTable),
  '审批路线节点列不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC route node columns static contract')
