const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelSource = fs
  .readFileSync(path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

const sliceBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `${label} missing end marker`)
  return source.slice(start, end)
}

const mountedBlock = sliceBetween(
  panelSource,
  'onMounted(async () => {',
  'onUnmounted(() => {',
  'mounted initialization'
)

const mountedPqcBlock = sliceBetween(
  mountedBlock,
  'if (isPqcMode.value) {',
  'Object.assign(draft.fieldValues, buildProductionFieldValues())',
  'PQC mounted initialization'
)

assert.match(
  mountedPqcBlock,
  /const activeOrders = await loadFrontlinePqcActiveOrders\(deviceState\)/,
  'PQC mounted initialization must load the formal pending active-order list.'
)

assert.doesNotMatch(
  mountedPqcBlock,
  /requestedActiveOrder|context\.workOrderId|order\.workOrderId\s*===/,
  'PQC mounted initialization must not match or branch on a URL workOrderId.'
)

assert.match(
  mountedPqcBlock,
  /const initialActiveOrder = activeOrders\[0\]/,
  'PQC mounted initialization must select the current first pending active order from the realtime list.'
)

assert.match(
  mountedPqcBlock,
  /if \(initialActiveOrder\) \{[\s\S]*await handleSelectActiveOrder\(initialActiveOrder\)[\s\S]*\}/,
  'PQC mounted initialization must select the resolved initial active order.'
)

assert.ok(
  mountedPqcBlock.indexOf('await handleSelectActiveOrder(initialActiveOrder)') <
    mountedPqcBlock.indexOf('Object.assign(draft.fieldValues, buildPqcFieldValues())'),
  'PQC draft values must be built after the default active order has initialized its process context.'
)

console.log('PASS: frontline PQC defaults to the first pending active order on entry')
