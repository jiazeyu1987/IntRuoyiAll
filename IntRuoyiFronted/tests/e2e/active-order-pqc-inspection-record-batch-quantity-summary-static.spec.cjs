const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'),
  'utf8'
)

const pqcTabStart = panel.search(/<el-tab-pane\b[^>]*label="过程检表单"[^>]*name="pqcSubmissions"[^>]*>/)
assert.ok(pqcTabStart > 0, '必须存在 PQC提交页签。')
const materialTabStart = panel.indexOf('data-team-leader-active-order-detail-material-tab', pqcTabStart)
assert.ok(materialTabStart > pqcTabStart, '必须能定位 PQC提交页签边界。')
const pqcTab = panel.slice(pqcTabStart, materialTabStart)

assert.match(
  pqcTab,
  /<span>批次数量<\/span>[\s\S]*pqcInspectionRecordSummary\.batchQuantityText/,
  '过程检验记录顶部汇总必须把原“检验设备”改为“批次数量”，并显示生产订单总数量。'
)

assert.doesNotMatch(
  pqcTab,
  /<span>检验设备<\/span>/,
  '过程检验记录顶部汇总不得再显示“检验设备”。'
)

assert.match(
  panel,
  /const batchQuantity = formatTraceQuantity\(props\.detail\?\.workOrderQuantity\)[\s\S]*batchQuantityText:\s*batchQuantity === '-' \? '-' : `\$\{batchQuantity\} 件`/,
  '批次数量必须取当前生产订单总数量 workOrderQuantity，并显示为“数量 件”。'
)

console.log('PASS: active order PQC inspection record batch quantity summary static contract')
