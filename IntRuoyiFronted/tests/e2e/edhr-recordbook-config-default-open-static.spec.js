const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(process.cwd())
const read = (relativePath) =>
  fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const routeDesigner = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

assert.doesNotMatch(
  routeDesigner,
  /data-route-process-setting-field="recordbook-enabled"/,
  '路线配置右侧动态表单列表不得再显示“记录本”开关。'
)

assert.doesNotMatch(
  routeDesigner,
  /handleRecordBindingRecordbookEnabledChange/,
  '记录本开关隐藏后不得保留可切换记录本启用状态的前端处理器。'
)

assert.match(
  routeDesigner,
  /const createEmptyRecordBinding = \(\): RouteFlowRecordBinding => \(\{[\s\S]*recordbookEnabled:\s*true/,
  '新增表单绑定必须默认开启记录本。'
)

assert.doesNotMatch(
  routeDesigner,
  /recordbookEnabled:\s*(report|binding)\.recordbookEnabled\s*!==\s*false/,
  '记录本默认开启后，前端不得继续从历史配置或隐藏字段保存关闭状态。'
)

const defaultOpenWrites = routeDesigner.match(/recordbookEnabled:\s*true/g) || []
assert.ok(
  defaultOpenWrites.length >= 3,
  '读取、草稿快照和保存 payload 都必须明确写入 recordbookEnabled: true。'
)

console.log('PASS: eDHR recordbook config default-open static contract')
