const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

assert.ok(
  panelSource.includes('data-pqc-active-inspection-panel'),
  'PQC page must render a single active inspection detail panel instead of expanding every item.'
)
assert.ok(
  panelSource.includes('data-pqc-inspection-tabs'),
  'PQC page must expose an inspection tab strip for switching items.'
)
assert.match(
  panelSource,
  /v-for="item in pqcInspectionItems"[\s\S]*data-pqc-inspection-tab/,
  'PQC inspection items must render as tabs from the formal inspection item list.'
)
assert.match(
  panelSource,
  /class="pqc-item-tab"[\s\S]*activePqcTabKey === item\.key/,
  'PQC tabs must visibly mark the active inspection item.'
)
assert.match(
  panelSource,
  /grid-template-columns:\s*repeat\(5,\s*minmax\(0,\s*1fr\)\)/,
  'PQC tab strip must use a 5-column grid so 10 tabs render as two complete rows.'
)
assert.match(
  panelSource,
  /data-pqc-tab-requirement[\s\S]*formatPqcTabRequirement/,
  'Each PQC tab must show the requirement field independently.'
)
assert.match(
  panelSource,
  /data-pqc-tab-progress[\s\S]*getPqcProgressText/,
  'Each PQC tab must show progress independently without relying on truncated meta text.'
)
assert.ok(
  panelSource.includes('pqc-select-card') &&
    panelSource.includes('data-pqc-equipment-card') &&
    panelSource.includes('data-pqc-equipment-number-card'),
  'Equipment and equipment number controls must render as touch-style cards.'
)
assert.match(
  panelSource,
  /class="pqc-select-native"[\s\S]*data-pqc-equipment-select/,
  'The formal equipment select must remain available inside the styled card for real selection.'
)
assert.match(
  panelSource,
  /class="pqc-select-native"[\s\S]*data-pqc-equipment-number-select/,
  'The formal equipment number select must remain available inside the styled card for real selection.'
)
assert.match(
  panelSource,
  /\.pqc-select-native[\s\S]*opacity:\s*0/,
  'Native select controls must not visually appear as raw browser selects in the PQC red-box area.'
)
assert.ok(
  !/<h3>检验内容<\/h3>/.test(panelSource) && !/<h3>填检验<\/h3>/.test(panelSource),
  'Prototype-approved large section titles must not be shown in the compact PQC operator layout.'
)
assert.ok(
  !/<template v-for="item in pqcInspectionItems" :key="item.key">/.test(panelSource),
  'PQC page must not keep the old vertically expanded item list.'
)

console.log('PASS: PQC inspection tabs layout static contract')
