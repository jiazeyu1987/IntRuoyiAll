const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const panel = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

const watchStart = panel.indexOf('watch(\n  visibleDeviceCards,')
const watchEnd = panel.indexOf('\nwatch(', watchStart + 1)
assert.ok(watchStart >= 0 && watchEnd > watchStart, 'visible device parameter initialization watcher must exist.')
const watchBlock = panel.slice(watchStart, watchEnd)

assert.match(
  watchBlock,
  /parameter\.defaultValue !== undefined\s*&&\s*parameter\.defaultValue !== null/,
  'frontline parameter initialization must reject both undefined and null defaults before numeric normalization.'
)

const normalizeStart = panel.indexOf('function normalizeProductionParameter')
const normalizeEnd = panel.indexOf('\n}', normalizeStart) + 2
assert.ok(normalizeStart >= 0 && normalizeEnd > normalizeStart, 'production parameter normalizer must exist.')
const normalizeBlock = panel.slice(normalizeStart, normalizeEnd)

assert.match(
  normalizeBlock,
  /value === undefined\s*\|\|\s*value === null\s*\|\|\s*String\(value\)\.trim\(\) === ''/,
  'production parameter normalization must preserve missing values instead of converting null to zero.'
)

console.log('PASS: frontline production device parameter midpoint default contract')
