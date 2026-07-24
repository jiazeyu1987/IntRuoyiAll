const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const routePage = readSource('src/views/dcc/controlled-file/routes/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:routes-node-columns:static'],
  'node tests/e2e/dcc-controlled-file-routes-node-columns-static.spec.js',
  'package.json 必须提供流程路线节点列静态契约脚本'
)

for (const removedField of ['summary', 'remark']) {
  assert.doesNotMatch(
    routePage,
    new RegExp(`key:\\s*'${removedField}'`),
    `${removedField} 不应继续注册到流程路线主列表列配置。`
  )
  assert.doesNotMatch(
    routePage,
    new RegExp(`isRouteColumnVisible\\('${removedField}'\\)`),
    `${removedField} 不应继续作为主列表可见列渲染。`
  )
}

assert.doesNotMatch(routePage, /label="路线摘要"/, '主列表不应显示路线摘要列。')
assert.doesNotMatch(routePage, /label="备注"/, '主列表不应显示备注列。')
assert.doesNotMatch(routePage, /data-testid="dcc-route-summary"/, '主列表不应保留路线摘要单元格。')
assert.doesNotMatch(routePage, /row\.nodeSummary/, '主列表不应继续把所有节点合并成节点摘要显示。')
assert.doesNotMatch(routePage, /row\.remark \|\| '-'/, '主列表不应继续显示备注内容。')

for (const field of ['categoryName', 'node1', 'node2', 'node3', 'node4']) {
  assert.match(routePage, new RegExp(`key:\\s*'${field}'`), `${field} 必须注册到流程路线主列表列配置。`)
  assert.match(
    routePage,
    new RegExp(`isRouteColumnVisible\\('${field}'\\)`),
    `${field} 列必须受显示字段配置控制。`
  )
}

for (const stageNo of [1, 2, 3, 4]) {
  assert.match(routePage, new RegExp(`label="节点${stageNo}"`), `主列表必须显示节点${stageNo}列。`)
  assert.match(
    routePage,
    new RegExp(`formatRouteNodeAssignees\\(row, ${stageNo}\\)`),
    `节点${stageNo}列必须按 stageNo=${stageNo} 解析审批对象。`
  )
}

assert.match(
  routePage,
  /const formatRouteNodeAssignees = \(row: ControlledFileApprovalRouteVO, stageNo: number\) => \{/,
  '页面必须提供按节点阶段解析审批对象的函数。'
)
assert.match(
  routePage,
  /const formatRouteNodeSubject = \(node: ControlledFileApprovalRouteNodeVO\) => \{/,
  '页面必须提供单个节点审批对象解析函数。'
)
assert.match(routePage, /getApprovalPositionList\(\)/, '节点列需要加载 DCC 岗位名称。')
assert.match(routePage, /getSimpleUserList\(\)/, '节点列需要加载用户名称。')
assert.match(
  routePage,
  /await loadRouteSubjectLookups\(\)[\s\S]*await handleQuery\(\)/,
  '进入页面加载主列表前必须加载审批对象名称映射。'
)
assert.match(
  routePage,
  /row\.nodes\?\.filter\(\(node\) => node\.stageNo === stageNo\)/,
  '节点列必须来自后端真实 nodes 数组，不能从摘要文本拆分。'
)
assert.doesNotMatch(
  routePage,
  /nodeSummary\.split|split\(' \/ '\)/,
  '节点列不得通过拆分节点摘要文本得到。'
)

console.log('PASS: DCC controlled file routes node columns static contract')
