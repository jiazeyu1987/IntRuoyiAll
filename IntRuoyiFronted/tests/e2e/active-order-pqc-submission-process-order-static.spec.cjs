const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const api = fs.readFileSync(path.join(root, 'src/api/mes/pro/processpool/teamLeader.ts'), 'utf8')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'),
  'utf8'
)

assert.match(
  api,
  /export interface TeamLeaderActiveOrderPqcSubmissionDetailRespVO[\s\S]*qaProcessSort\?: number/,
  '前端详情类型必须接收后端一线 PQC 工序显示序号。'
)

const groupStart = panel.indexOf('const pqcProcessGroups = computed<ActiveOrderDetailPqcProcessGroup[]>')
const groupEnd = panel.indexOf('type ActiveOrderDetailPickListMaterialRow', groupStart)
assert.ok(groupStart > 0 && groupEnd > groupStart, '必须能定位 PQC 提交工序分组逻辑。')
const groupBlock = panel.slice(groupStart, groupEnd)

assert.match(
  panel,
  /interface ActiveOrderDetailPqcProcessGroup[\s\S]*qaProcessSort: number/,
  'PQC 提交工序分组必须持有一线 PQC 显示序号。'
)
assert.match(
  panel,
  /const compareActiveOrderPqcProcessGroups[\s\S]*left\.qaProcessSort - right\.qaProcessSort[\s\S]*left\.qaProcessId - right\.qaProcessId/,
  'PQC 提交工序分组必须按一线 PQC 显示序号排序。'
)
assert.match(
  groupBlock,
  /const qaProcessSort = Number\(submission\.qaProcessSort\)/,
  'PQC 提交分组必须读取后端透传的正式工序显示序号。'
)
assert.match(
  groupBlock,
  /PQC提交缺少正式检验工序排序/,
  '缺少 PQC 工序显示序号时必须失败暴露，不能用数组位置或原始返回顺序补齐。'
)
assert.match(
  groupBlock,
  /return Array\.from\(groupsByQaProcessId\.values\(\)\)\.sort\(compareActiveOrderPqcProcessGroups\)/,
  '模拟 PQC 提交页必须按一线 PQC 工序顺序渲染工序组。'
)

console.log('PASS: active order PQC submission process order static contract')
