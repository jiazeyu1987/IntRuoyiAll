const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const routeFlowGraphDesigner = read(
  'yudao-ui-admin-vue3/src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
)

assert.match(
  routeFlowGraphDesigner,
  /type\s+ConnectionTargetRouteProcessId\s*=\s*number\s*\|\s*typeof\s+PROCESS_END_NODE_ID/,
  '连接面板目标值类型必须支持工序结束边界节点。'
)

assert.match(
  routeFlowGraphDesigner,
  /const\s+createEndBoundaryConnectionOption\s*=\s*\(\):\s*ConnectionProcessOption\s*=>\s*\(\{\s*routeProcessId:\s*PROCESS_END_NODE_ID,\s*processName:\s*boundaryLabel\('END'\)/,
  '连接面板必须提供“工序结束”目标选项。'
)

assert.match(
  routeFlowGraphDesigner,
  /connectionTargetOptions[\s\S]{0,600}createEndBoundaryConnectionOption\(\)/,
  '目标工序下拉必须把“工序结束”加入可选项。'
)

assert.match(
  routeFlowGraphDesigner,
  /return\s+\[createEndBoundaryConnectionOption\(\),\s*\.\.\.processTargets\]/,
  '目标工序下拉应将“工序结束”置顶，避免工序较多时被挤到列表下方。'
)

assert.match(
  routeFlowGraphDesigner,
  /targetId\s*===\s*PROCESS_END_NODE_ID[\s\S]{0,120}addBoundaryEdge\('END',\s*sourceId\)/,
  '确认连接到工序结束时必须创建 END 边界关系。'
)

assert.match(
  routeFlowGraphDesigner,
  /<el-autocomplete[\s\S]{0,900}data-flow-field="connection-source"[\s\S]{0,900}:fetch-suggestions="queryConnectionSourceSuggestions"[\s\S]{0,900}@select="handleConnectionSourceSelect"/,
  '起始工序必须使用输入下拉选择控件，并通过 select 事件确认选择。'
)

assert.match(
  routeFlowGraphDesigner,
  /<el-autocomplete[\s\S]{0,900}data-flow-field="connection-target"[\s\S]{0,900}:fetch-suggestions="queryConnectionTargetSuggestions"[\s\S]{0,900}@select="handleConnectionTargetSelect"/,
  '目标工序必须使用输入下拉选择控件，并通过 select 事件确认选择。'
)

assert.match(
  routeFlowGraphDesigner,
  /const\s+queryConnectionSourceSuggestions\s*=/,
  '起始工序输入下拉必须提供建议项查询方法。'
)

assert.match(
  routeFlowGraphDesigner,
  /const\s+queryConnectionTargetSuggestions\s*=/,
  '目标工序输入下拉必须提供建议项查询方法。'
)

console.log('mes-route-flow-end-boundary-selector-static PASS')
