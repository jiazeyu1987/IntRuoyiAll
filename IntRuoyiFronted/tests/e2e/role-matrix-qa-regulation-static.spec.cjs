const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.resolve(workspaceRoot, 'IntRuoyiFronted')
const backendRoot = path.resolve(workspaceRoot, 'IntRuoyiBackend')

const panelPath = path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const schemaPath = path.join(backendRoot, 'sql/mysql/20260512_mes_base_schema.sql')

const panelSource = fs.readFileSync(panelPath, 'utf8')
const schemaSource = fs.readFileSync(schemaPath, 'utf8')

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

console.log('PASS role-matrix QA regulation static contract')
