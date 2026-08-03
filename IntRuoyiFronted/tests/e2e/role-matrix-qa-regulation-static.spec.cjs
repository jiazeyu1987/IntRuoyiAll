const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.resolve(workspaceRoot, 'IntRuoyiFronted')
const backendRoot = path.resolve(workspaceRoot, 'IntRuoyiBackend')

const panelPath = path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const schemaPaths = [
  path.join(backendRoot, 'sql/mysql/20260512_mes_base_schema.sql'),
  path.join(backendRoot, 'sql/mysql/20260802_mes_qa_inspection_regulation.sql'),
  path.join(backendRoot, 'sql/mysql/20260802_mes_pqc_inspection_task.sql')
]

const panelSource = fs.readFileSync(panelPath, 'utf8')
const schemaSource = schemaPaths.map((schemaPath) => fs.readFileSync(schemaPath, 'utf8')).join('\\n')

assert.match(
  schemaSource,
  /mes_qa_inspection_regulation|mes_qc_inspection_regulation|qms_.*regulation/i,
  'QA regulation must have a formal owned schema before M3 can proceed.'
)
assert.match(
  schemaSource,
  /regulation_version|qa.*version|inspection.*version/i,
  'QA regulation must have an immutable published version model.'
)
assert.doesNotMatch(
  panelSource,
  /data-pqc-inspection-entry="(?:length|appearance|seal|pressure)"/,
  'PQC inspection entries must be rendered from published regulation items, not hardcoded demo items.'
)
assert.doesNotMatch(
  panelSource,
  /inspectionType:\s*'PATROL'|inspectionQuantity:\s*30/,
  'PQC defaults must come from a task/regulation snapshot, not PATROL/30 literals.'
)
assert.match(
  panelSource,
  /data-pqc-inspection-meta/,
  'PQC page must visibly render method/standard/result metadata for each published regulation item.'
)
assert.match(
  panelSource,
  /formatPqcInspectionMeta/,
  'PQC regulation item metadata must be formatted from the formal QA regulation snapshot.'
)
assert.match(
  panelSource,
  /inspectionMethod[\s\S]*standardText[\s\S]*resultType|resultType[\s\S]*inspectionMethod[\s\S]*standardText/,
  'PQC inspection item model must retain inspection method, standard text, and result type from the formal regulation item.'
)

console.log('PASS role-matrix QA regulation static contract')
