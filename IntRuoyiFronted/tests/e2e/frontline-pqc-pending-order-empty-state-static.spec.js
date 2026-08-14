const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const contextSource = fs
  .readFileSync(path.join(root, 'src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts'), 'utf8')
  .replace(/\r\n/g, '\n')
const panelSource = fs
  .readFileSync(path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

assert.match(
  contextSource,
  /export const FRONTLINE_PQC_NO_PENDING_ORDER_TEXT\s*=\s*['"]当前暂无待执行 PQC 检验任务['"]/,
  'PQC context must define the formal no-pending-task empty-state text.'
)
assert.match(
  contextSource,
  /state\.activeOrderOptions\s*=\s*activeOrders[\s\S]*clearFrontlinePqcSelectionIfUnavailable/,
  'PQC active-order reload must clear a selected order that is no longer returned by the pending-task read model.'
)
assert.match(
  contextSource,
  /const clearFrontlinePqcSelectionIfUnavailable[\s\S]*selectedActiveOrder\s*=\s*undefined[\s\S]*selectedProcess\s*=\s*undefined[\s\S]*processOptions\s*=\s*\[\]/,
  'Clearing an unavailable PQC order must also clear its process context.'
)
assert.match(
  panelSource,
  /data-pqc-order-empty-state[\s\S]*activeOrderPickerEmptyText/,
  'PQC order picker must expose a dedicated empty-state region.'
)
assert.match(
  panelSource,
  /const activeOrderPickerEmptyText[\s\S]*FRONTLINE_PQC_NO_PENDING_ORDER_TEXT[\s\S]*未找到匹配的待检工单/,
  'PQC order picker must distinguish no pending tasks from a keyword with no matches.'
)
assert.match(
  panelSource,
  /isPqcMode\.value && !deviceState\.selectedActiveOrder[\s\S]*FRONTLINE_PQC_NO_PENDING_ORDER_TEXT/,
  'PQC status text must explain that there are no executable inspection tasks when the order list is empty.'
)
assert.doesNotMatch(
  panelSource,
  /未找到匹配的活跃订单/,
  'PQC empty state must not use the old active-order wording.'
)

console.log('PASS: PQC pending-order filtering and empty-state contract')
