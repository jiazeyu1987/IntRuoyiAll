const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const flowConfigApi = read('src/api/mes/pro/route/flowconfig.ts')
const designer = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

assert.match(
  flowConfigApi,
  /export interface ProRouteFlowFormBindingSaveVO \{[\s\S]*formSlotType\?: ProRouteFlowFormSlotType \| null/,
  '动态表单保存类型必须携带 formSlotType，避免损耗单等槽位被默认归为 MAIN。'
)

assert.match(
  flowConfigApi,
  /export interface ProRouteFlowProcessConfigSaveVO \{[\s\S]*batchRecordReports\?: ProRouteFlowBatchRecordVO\[\]/,
  '批记录工序配置保存类型必须包含 legacy batchRecordReports。'
)

assert.match(
  designer,
  /batchRecordReports:\s*buildLegacyBatchRecordSaveRows\(draft\.legacyBatchRecords\)/,
  '保存工序批记录配置时必须把 legacy batchRecordReports 一起提交。'
)

assert.match(
  designer,
  /formSlotType:\s*normalizeRecordBindingSlotType\(binding\.formSlotType,\s*binding\.formBindingKey\)/,
  '动态表单保存行必须显式提交 formSlotType。'
)

assert.match(
  designer,
  /batchRecordReports:\s*processConfig\.batchRecordReports/,
  '最终 saveBatchRecordConfig payload 不得丢弃 batchRecordReports。'
)

assert.match(
  designer,
  /legacyBatchRecords:\s*_legacyBatchRecords[\s\S]*\.\.\.scheduleSnapshot/,
  '排产属性变更判断必须排除 legacy batchRecordReports，避免仅批记录绑定变化触发排产保存。'
)

assert.match(
  designer,
  /const baselineRecordBindingSnapshot = \{[\s\S]*legacyBatchRecords:\s*baselineSnapshot\.legacyBatchRecords \|\| \[\][\s\S]*recordBindings:\s*baselineSnapshot\.recordBindings \|\| \[\]/,
  '批记录绑定变更判断必须同时比较 legacy batchRecordReports 和动态 formBindings。'
)

console.log('PASS: route batch record save contract preserves legacy reports and form slots')
