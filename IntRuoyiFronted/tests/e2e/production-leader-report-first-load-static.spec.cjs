const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const sourcePath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
)
const source = fs.readFileSync(sourcePath, 'utf8')

const mountedStart = source.indexOf('onMounted(() => {')
assert(mountedStart >= 0, '生产组长工作台必须保留 onMounted 入口。')
const mountedSource = source.slice(mountedStart, source.indexOf('</script>', mountedStart))

assert.match(
  mountedSource,
  /activeProductionModuleTab\.value === 'report'[\s\S]*getSubmissionList\(\)/,
  '报工管理首屏必须加载报工列表。'
)
for (const forbidden of [
  'refreshProductionPersonnel()',
  'loadResponsibleRoutes()',
  'loadTeamDeviceOptions()',
  'loadActiveOrders()',
  'loadProcessConfigRows()'
]) {
  assert(
    !mountedSource.includes(forbidden),
    '报工管理首屏不得无条件加载非当前页签数据：' + forbidden
  )
}

assert.match(
  source,
  /watch\(activeProductionModuleTab[\s\S]*tab === 'activeOrder'[\s\S]*loadActiveOrders\(\)/,
  '活跃订单必须在首次进入活跃订单页签时加载。'
)
assert.match(
  source,
  /watch\(activeProductionModuleTab[\s\S]*tab === 'processConfig'[\s\S]*loadProcessConfigRows\(\)/,
  '工序配置必须在首次进入工序配置页签时加载。'
)
assert.match(
  source,
  /watch\(activeProductionModuleTab[\s\S]*tab === 'personnel'[\s\S]*refreshProductionPersonnel\(\)/,
  '人员管理必须在首次进入人员页签时加载。'
)

console.log('PASS production-leader-report-first-load-static')
