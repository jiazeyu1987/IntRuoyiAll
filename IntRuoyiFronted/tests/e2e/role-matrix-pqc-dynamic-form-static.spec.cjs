const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.resolve(workspaceRoot, 'IntRuoyiFronted')
const backendRoot = path.resolve(workspaceRoot, 'IntRuoyiBackend')

const panelPath = path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const contextServicePath = path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
)

const panelSource = fs.readFileSync(panelPath, 'utf8')
const contextSource = fs.readFileSync(contextServicePath, 'utf8')

assert.match(
  contextSource,
  /regulationVersionId|inspectionType|businessDate|shiftCode|roundNo/,
  'PQC context must expose task identity from a regulation snapshot.'
)
assert.match(
  panelSource,
  /pqcInspectionItems|inspectionItems|regulationVersionId/,
  'PQC page must render dynamic inspection items from task/regulation data.'
)
assert.doesNotMatch(
  panelSource,
  /type\s+PqcInspectionItemKey\s*=\s*'length'\s*\|\s*'appearance'\s*\|\s*'seal'\s*\|\s*'pressure'/,
  'PQC item keys must not be a hardcoded length/appearance/seal/pressure union.'
)
assert.doesNotMatch(
  panelSource,
  /PQC_INSPECTION_ITEMS\s*=\s*\{|length:\s*\{|appearance:\s*\{|seal:\s*\{|pressure:\s*\{/,
  'PQC item definitions must not be hardcoded in the frontend.'
)

console.log('PASS role-matrix PQC dynamic form static contract')
