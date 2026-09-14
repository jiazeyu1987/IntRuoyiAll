const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'),
  'utf8'
)

const pqcTabStart = panel.indexOf('<el-tab-pane label="PQC提交" name="pqcSubmissions">')
assert.ok(pqcTabStart > 0, '必须存在 PQC提交页签。')
const metaStart = panel.indexOf('<div class="team-leader-workbench__production-record-meta">', pqcTabStart)
assert.ok(metaStart > 0, '过程检验记录必须存在顶部汇总信息区。')
const metaEnd = panel.indexOf('<table', metaStart)
assert.ok(metaEnd > metaStart, '必须能定位过程检验记录顶部汇总信息区边界。')
const meta = panel.slice(metaStart, metaEnd)

assert.match(
  meta,
  /<span>批次数量<\/span>[\s\S]*pqcInspectionRecordSummary\.batchQuantityText/,
  '过程检验记录顶部汇总必须把原“检验设备”改为“批次数量”，并显示生产订单总数量。'
)

assert.doesNotMatch(
  meta,
  /<span>检验设备<\/span>/,
  '过程检验记录顶部汇总不得再显示“检验设备”。'
)

assert.match(
  panel,
  /const batchQuantity = formatTraceQuantity\(props\.detail\?\.workOrderQuantity\)[\s\S]*batchQuantityText:\s*batchQuantity === '-' \? '-' : `\$\{batchQuantity\} 件`/,
  '批次数量必须取当前生产订单总数量 workOrderQuantity，并显示为“数量 件”。'
)

console.log('PASS: active order PQC inspection record batch quantity summary static contract')
