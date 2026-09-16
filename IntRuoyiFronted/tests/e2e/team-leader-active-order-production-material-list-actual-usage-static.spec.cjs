const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'),
  'utf8'
)

const usageMapStart = panel.indexOf('const productionMaterialListActualUsageByCode')
const resolverStart = panel.indexOf('const resolveProductionMaterialListActualUsage')
assert.ok(usageMapStart >= 0, '生产用料清单实际用量必须建立当前生产输入物料聚合映射。')
assert.ok(resolverStart > usageMapStart, '实际用量 resolver 必须消费生产输入物料聚合映射。')

const usageMapBlock = panel.slice(usageMapStart, resolverStart)
assert.match(usageMapBlock, /computed\(\(\)\s*=>/, '生产输入物料实际用量映射必须是响应式 computed。')
assert.match(usageMapBlock, /props\.detail\?\.inputMaterialUsages/, '实际用量必须优先从当前活跃订单详情的订单级输入物料来源读取。')
assert.match(usageMapBlock, /props\.detail\?\.processes/, '实际用量必须兼容当前活跃订单详情的工序输入物料集合。')
assert.match(usageMapBlock, /process\.inputMaterials/, '实际用量必须读取生产输入物料集合。')
assert.match(usageMapBlock, /material\.materialCode/, '实际用量必须按物料编码匹配生产用料清单子项。')
assert.match(usageMapBlock, /material\.actualQuantity/, '实际用量必须使用生产输入物料 actualQuantity。')
assert.match(usageMapBlock, /existingQuantity[\s\S]*actualQuantity[\s\S]*>/, '重复输入物料必须只保留最大的实际数量。')

const resolverBlock = panel.slice(resolverStart, panel.indexOf('const resolveProductionMaterialListWarehouse', resolverStart))
assert.match(
  resolverBlock,
  /productionMaterialListActualUsageByCode\.value\.get\(\s*normalizeProductionMaterialCode\(row\.childMaterialCode\)\s*\)/,
  '生产用料清单实际用量必须用子项物料编码读取生产输入物料最大实际数量。'
)
assert.doesNotMatch(
  resolverBlock,
  /readProductionMaterialListText\(row,\s*\[['"]actualQuantity['"]/,
  '生产用料清单实际用量不得继续从生产用料清单行自身 actualQuantity 字段读取。'
)

console.log('PASS team-leader-active-order-production-material-list-actual-usage-static')
