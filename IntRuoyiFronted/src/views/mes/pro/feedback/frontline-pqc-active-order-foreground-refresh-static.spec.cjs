const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(path.join(__dirname, 'FrontlineFixedTemplatePanel.vue'), 'utf8')

assert.match(
  source,
  /const\s+refreshPqcActiveOrdersAndEnsureSelection\s*=\s*async\s*\(\)\s*=>/,
  'PQC page must expose a focused refresh helper for active orders.'
)
assert.match(
  source,
  /refreshPqcActiveOrdersAndEnsureSelection[\s\S]*loadFrontlinePqcActiveOrders\(deviceState\)[\s\S]*!deviceState\.selectedActiveOrder[\s\S]*handleSelectActiveOrder\(initialActiveOrder\)/,
  'PQC focused refresh must reload active orders and select the first order when nothing is selected.'
)
assert.match(
  source,
  /const\s+handlePqcForegroundRefresh\s*=\s*\(\)\s*=>[\s\S]*document\.visibilityState[\s\S]*refreshPqcActiveOrdersAndEnsureSelection\(\)/,
  'PQC page must refresh active orders when the page returns to the visible foreground.'
)
assert.match(
  source,
  /document\.addEventListener\('visibilitychange',\s*handlePqcForegroundRefresh\)/,
  'PQC page must listen for visibilitychange to recover from team-leader reset in another tab.'
)
assert.match(
  source,
  /window\.addEventListener\('focus',\s*handlePqcForegroundRefresh\)/,
  'PQC page must listen for window focus to recover stale active-order state.'
)
assert.match(
  source,
  /document\.removeEventListener\('visibilitychange',\s*handlePqcForegroundRefresh\)/,
  'PQC page must remove the visibilitychange listener on unmount.'
)
assert.match(
  source,
  /window\.removeEventListener\('focus',\s*handlePqcForegroundRefresh\)/,
  'PQC page must remove the focus listener on unmount.'
)

console.log('PASS: frontline PQC refreshes active orders when the page returns to foreground')
