const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)

assert.match(
  panel,
  /const readFrontlineFormalSubmitContext[\s\S]*selectedActiveOrder[\s\S]*workOrderId:\s*selectedActiveOrder\?\.workOrderId/,
  '生产正式提交必须从用户选中的活跃订单读取 workOrderId'
)
assert.match(
  panel,
  /assertFrontlineFormalSubmitContext[\s\S]*活跃订单[\s\S]*订单与工序上下文不一致/,
  '提交前必须校验已选订单以及订单与工序路线一致'
)
assert.match(
  panel,
  /feedbackPayload:[\s\S]*workOrderId:\s*formalContext\.workOrderId[\s\S]*processPoolContext:[\s\S]*workOrderId:\s*formalContext\.workOrderId/,
  '报工记录和工序池事件必须写入同一个选中订单'
)
assert.doesNotMatch(
  panel,
  /outputQuantity\s*>\s*selectedActiveOrder\.quantity|selectedActiveOrder\.quantity\s*<\s*productionDraft\.outputQuantity/,
  '员工端不得用订单数量上限阻断超报'
)

console.log('PASS: frontline production selected-order submit attribution static contract')
