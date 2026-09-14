const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.join(process.cwd(), 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const personnelMarkerIndex = source.indexOf('data-pqc-leader-personnel-tab')
assert.notEqual(personnelMarkerIndex, -1, 'PQC personnel block marker must exist.')
const personnelStart = source.lastIndexOf('<ContentWrap', personnelMarkerIndex)
const personnelEnd = source.indexOf('</ContentWrap>', personnelMarkerIndex)
assert.notEqual(personnelStart, -1, 'PQC personnel ContentWrap start must exist.')
assert.notEqual(personnelEnd, -1, 'PQC personnel ContentWrap end must exist.')
const personnelBlock = source.slice(personnelStart, personnelEnd)

assert.doesNotMatch(
  personnelBlock,
  /v-model="pqcPersonnelQuery\.enabled"/,
  'PQC personnel must not render an enabled-status grouping control.'
)
assert.doesNotMatch(
  personnelBlock,
  /<el-option\s+label="已启用"[\s\S]*?<el-option\s+label="已禁用"/,
  'PQC personnel must not split enabled and disabled inspectors into filter groups.'
)

const queryStart = source.indexOf('const pqcPersonnelQuery = reactive({')
const queryEnd = source.indexOf('})', queryStart)
assert.notEqual(queryStart, -1, 'PQC personnel query state must exist.')
assert.notEqual(queryEnd, -1, 'PQC personnel query state end must exist.')
const queryBlock = source.slice(queryStart, queryEnd)
assert.doesNotMatch(
  queryBlock,
  /\benabled\s*:/,
  'PQC personnel query state must not default to enabled-only results.'
)

const refreshStart = source.indexOf('const refreshPqcPersonnel = async () => {')
const refreshEnd = source.indexOf('const handlePqcPersonnelPageChange', refreshStart)
assert.notEqual(refreshStart, -1, 'PQC personnel refresh handler must exist.')
assert.notEqual(refreshEnd, -1, 'PQC personnel refresh handler end must exist.')
const refreshBlock = source.slice(refreshStart, refreshEnd)
assert.match(
  refreshBlock,
  /getPqcPersonnelList\(\)/,
  'PQC personnel refresh must request the complete linked inspector list.'
)
assert.doesNotMatch(
  refreshBlock,
  /\benabled\s*:/,
  'PQC personnel refresh must not send an enabled filter.'
)

assert.match(
  personnelBlock,
  /class="team-leader-workbench__pqc-personnel-name"[\s\S]*:class="\{\s*'is-disabled':\s*row\.enabled\s*===\s*false\s*\}"/,
  'PQC inspector display names must expose an explicit disabled state class.'
)
assert.match(
  source,
  /\.team-leader-workbench__pqc-personnel-name\.is-disabled\s*\{[\s\S]*?color:\s*#f56c6c/,
  'Disabled PQC inspector names must use the requested red color.'
)
assert.match(
  personnelBlock,
  /\{\{\s*row\.enabled\s*===\s*false\s*\?\s*'已禁用'\s*:\s*'已启用'\s*\}\}/,
  'The status text must remain visible so color is not the only disabled-state indicator.'
)

console.log('PASS: PQC personnel unified status list static contract')
