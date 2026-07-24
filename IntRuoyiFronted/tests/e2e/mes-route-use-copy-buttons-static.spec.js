const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const routeListPath = path.resolve(process.cwd(), 'src/views/mes/pro/route/index.vue')

assert(fs.existsSync(routeListPath), `required file missing: ${routeListPath}`)

const pageSource = fs.readFileSync(routeListPath, 'utf8')

assert.match(
  pageSource,
  /label="排产配置"[\s\S]*@click="openEditPage\(scope\.row\.id, 'schedule-config'\)"/,
  '工艺流程列表必须提供排产配置状态入口并打开排产配置页签。'
)
assert.match(
  pageSource,
  /label="批记录配置"[\s\S]*@click="openEditPage\(scope\.row\.id, 'batch-record-config'\)"/,
  '工艺流程列表必须提供批记录配置状态入口并打开批记录配置页签。'
)
assert.match(pageSource, /scheduleRouteEnabled/, '工艺流程列表必须显示排产配置启用状态。')
assert.match(pageSource, /batchRouteEnabled/, '工艺流程列表必须显示批记录配置启用状态。')

console.log('PASS: MES route flow config status entries static contract')
