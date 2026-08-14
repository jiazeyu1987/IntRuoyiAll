const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

assert.match(
  page,
  /data-team-leader-allocation-clear[\s\S]*:disabled="row\.editable === false"[\s\S]*@click="clearAllocationQuantity\(row\)"[\s\S]*清除/,
  'allocation rows must expose a row-level 清除 button wired to the clear handler.'
)

assert.match(
  page,
  /const\s+clearAllocationQuantity\s*=\s*\(line:\s*TeamLeaderReportAllocationDraftLine\)\s*=>\s*\{[\s\S]*line\.editable === false[\s\S]*return[\s\S]*line\.allocatedQuantity\s*=\s*0[\s\S]*markManualAllocation\(\)/,
  'clear handler must set only the clicked editable row allocatedQuantity to 0 and mark manual allocation.'
)

assert.match(
  page,
  /const\s+buildAllocationSubmitLines\s*=\s*\(\):\s*TeamLeaderReportAllocationLine\[\]\s*=>\s*\{[\s\S]*normalizeAllocationSubmitQuantity\(line\.allocatedQuantity,\s*'分配数量必须为0或正整数'\)[\s\S]*allocatedQuantity === 0[\s\S]*return \[\][\s\S]*activeOrderId:\s*requirePositiveNumber\(line\.activeOrderId,\s*'活跃订单不能为空'\)[\s\S]*allocatedQuantity/,
  'submit builder must normalize cleared 0 quantities and omit them from formal allocation payloads.'
)
