const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)

const watcherStart = panel.search(/watch\(\s*visibleDeviceCards,/)
assert.notEqual(watcherStart, -1, 'visibleDeviceCards watcher must exist.')
const watcherEnd = panel.search(/\r?\n\)\r?\n\r?\nwatch\(\r?\n\s*configuredDefectReasons/)
assert.notEqual(watcherEnd, -1, 'visibleDeviceCards watcher block must be extractable.')
const watcher = panel.slice(watcherStart, watcherEnd)

assert.match(
  watcher,
  /if \(!devices\.length\)[\s\S]*selectedProductionDeviceKey\.value = undefined[\s\S]*selectedProductionDeviceKeys\.value = \[\]/,
  'no-device state must clear active and selected device keys.'
)

assert.match(
  watcher,
  /ensureProductionDefaultDeviceSelection\(devices\)/,
  'visible device watcher must call the shared default-selection normalizer.'
)

assert.match(
  panel,
  /const ensureProductionDefaultDeviceSelection = \(devices: ProductionDeviceCard\[\]\) => \{[\s\S]*const firstVisibleDeviceKey = devices\[0\]\.key[\s\S]*selectedProductionDeviceKeys\.value = \[firstVisibleDeviceKey\]/,
  'shared normalizer must select the first visible device when no selected device remains.'
)

assert.match(
  panel,
  /if \(!selectedProductionDeviceKeys\.value\.includes\(selectedProductionDeviceKey\.value \|\| ''\)\)[\s\S]*selectedProductionDeviceKey\.value = selectedProductionDeviceKeys\.value\[0\]/,
  'active device key must follow the selected device keys without overriding an existing valid selection.'
)

assert.match(
  panel,
  /\.frontline-production-device-card\.active \.device-tab-selection::after[\s\S]*content: '✓'/,
  'active device selection indicator must render a visible check mark.'
)

const restoreStart = panel.indexOf('const restoreProductionMaterialDraft =')
assert.notEqual(restoreStart, -1, 'restoreProductionMaterialDraft must exist.')
const restoreEndMatch = /\r?\n\r?\nconst clearProductionMaterialDrafts =/.exec(panel.slice(restoreStart))
const restoreEnd = restoreEndMatch ? restoreStart + restoreEndMatch.index : -1
assert.notEqual(restoreEnd, -1, 'restoreProductionMaterialDraft block must be extractable.')
const restoreBlock = panel.slice(restoreStart, restoreEnd)
assert.match(
  restoreBlock,
  /selectedProductionDeviceKeys\.value = \(materialDraft\.selectedDeviceKeys \|\| \[\]\)[\s\S]*ensureProductionDefaultDeviceSelection\(visibleDeviceCards\.value\)/,
  'restoring a material draft with empty device keys must re-apply default device selection.'
)

console.log('PASS: frontline production defaults to one selected device when devices exist')
