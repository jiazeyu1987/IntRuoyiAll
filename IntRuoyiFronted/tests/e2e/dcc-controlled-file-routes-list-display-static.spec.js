const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const routePage = readSource('src/views/dcc/controlled-file/routes/index.vue')
const routeApi = readSource('src/api/dcc/controlledFile/approvalRoutes.ts')

assert.equal(
  packageJson.scripts['e2e:dcc:routes-list-display:static'],
  'node tests/e2e/dcc-controlled-file-routes-list-display-static.spec.js',
  'package.json 必须提供流程路线列表化展示静态契约脚本'
)

assert.match(
  routeApi,
  /getApprovalRoutePage[\s\S]*request\.get\(\{ url: '\/dcc\/approval-routes\/page', params \}\)/,
  '流程路线前端 API 必须接入后端分页列表接口，不能继续只靠按类别单查。'
)

assert.match(routePage, /getApprovalRoutePage/, '流程路线页面必须调用分页列表接口加载当前列表。')
assert.match(
  routePage,
  /onMounted\(async \(\) => \{[\s\S]*await loadInitialCategoryOptions\(\)[\s\S]*await handleQuery\(\)[\s\S]*\}\)/,
  '进入流程路线页面必须默认加载列表数据。'
)
assert.doesNotMatch(
  routePage,
  /if \(!queryParams\.categoryId\) \{[\s\S]*routes\.value = \[\][\s\S]*return[\s\S]*\}/,
  '主列表不得因为未选择文件类别而清空；未筛选时应展示全部路线分页。'
)

for (const field of ['categoryName', 'node1', 'node2', 'node3', 'node4']) {
  assert.match(routePage, new RegExp(`key:\\s*'${field}'`), `${field} 必须注册到流程路线列表列配置。`)
  assert.match(
    routePage,
    new RegExp(`isRouteColumnVisible\\('${field}'\\)`),
    `${field} 列必须受显示字段配置控制。`
  )
}

assert.match(
  routeApi,
  /nodeSummary\?: string/,
  '流程路线列表接口类型必须包含后端整理好的节点摘要。'
)
assert.match(
  routeApi,
  /statusLabel\?: string/,
  '流程路线列表接口类型必须包含后端整理好的状态标签。'
)
assert.match(routePage, /label="文件类别"/, '流程路线列表必须显示文件类别列。')
assert.match(routePage, /row\.categoryName/, '文件类别列必须使用后端返回的真实类别名称。')
for (const stageNo of [1, 2, 3, 4]) {
  assert.match(routePage, new RegExp(`label="节点${stageNo}"`), `流程路线列表必须显示节点${stageNo}列。`)
  assert.match(
    routePage,
    new RegExp(`formatRouteNodeAssignees\\(row, ${stageNo}\\)`),
    `节点${stageNo}列必须按阶段显示审批对象。`
  )
}
assert.doesNotMatch(routePage, /label="路线摘要"/, '流程路线列表不再显示路线摘要列。')
assert.doesNotMatch(routePage, /label="备注"/, '流程路线列表不再显示备注列。')
assert.doesNotMatch(routePage, /row\.nodeSummary/, '流程路线列表不得继续显示合并后的节点摘要。')
assert.match(routePage, /:total="routeTotal"/, '流程路线列表分页必须绑定后端总数。')
assert.match(routePage, /routes\.value = pageResult\.list \?\? \[\]/, '流程路线列表必须使用后端分页结果 list。')
assert.match(routePage, /routeTotal\.value = pageResult\.total \?\? 0/, '流程路线列表必须使用后端分页结果 total。')
assert.doesNotMatch(
  routePage,
  /Promise\.all\([\s\S]*getApprovalRoutes/,
  '流程路线列表不得通过前端逐类别拼接接口来假分页。'
)
assert.doesNotMatch(
  routePage,
  /mock|placeholder data|fallback|降级|吞异常|默认成功/i,
  '流程路线列表化展示不得引入 mock、fallback、降级、吞异常或默认成功。'
)

console.log('PASS: DCC controlled file routes list display static contract')
