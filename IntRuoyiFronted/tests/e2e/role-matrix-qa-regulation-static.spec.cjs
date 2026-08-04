const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.resolve(workspaceRoot, 'IntRuoyiFronted')
const backendRoot = path.resolve(workspaceRoot, 'IntRuoyiBackend')

const panelPath = path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const qcTemplatePagePath = path.join(frontendRoot, 'src/views/mes/qc/template/index.vue')
const qcTemplateApiPath = path.join(frontendRoot, 'src/api/mes/qc/template/index.ts')
const qaControllerPath = path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/MesQaInspectionRegulationController.java'
)
const qaServicePath = path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java'
)
const qaPqcFormalFixturePath = path.join(
  workspaceRoot,
  'doc/tasks/20260801-role-requirement-matrix-implementation/m6-local-runtime-qa-pqc-formal-fixture.sql'
)
const schemaPaths = [
  path.join(backendRoot, 'sql/mysql/20260512_mes_base_schema.sql'),
  path.join(backendRoot, 'sql/mysql/20260802_mes_qa_inspection_regulation.sql'),
  path.join(backendRoot, 'sql/mysql/20260802_mes_pqc_inspection_task.sql')
]

const panelSource = fs.readFileSync(panelPath, 'utf8')
const qcTemplatePageSource = fs.readFileSync(qcTemplatePagePath, 'utf8')
const qcTemplateApiSource = fs.readFileSync(qcTemplateApiPath, 'utf8')
const qaControllerSource = fs.existsSync(qaControllerPath) ? fs.readFileSync(qaControllerPath, 'utf8') : ''
const qaServiceSource = fs.existsSync(qaServicePath) ? fs.readFileSync(qaServicePath, 'utf8') : ''
const qaPqcFormalFixtureSource = fs.existsSync(qaPqcFormalFixturePath)
  ? fs.readFileSync(qaPqcFormalFixturePath, 'utf8')
  : ''
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
assert.doesNotMatch(
  panelSource,
  /for\s*\(const\s*\[stateKey,\s*stateValues\]\s*of\s*Object\.entries\(pqcPieceValues\)\)[\s\S]*values\[stateKey\]/,
  'PQC submit payload must only send formal itemCode keys; composite UI state keys would duplicate raw pqcPieceValues and can create repeated piece-detail inserts on rerun.'
)
assert.match(
  panelSource,
  /buildPqcPieceValuesPayload[\s\S]*for\s*\(const itemKey of pqcInspectionItemKeys\.value\)[\s\S]*values\[itemKey\]\s*=\s*getPqcStoredPieceValues\(itemKey\)\.slice\(0,\s*pqcInspectionQuantity\.value\)[\s\S]*return values/,
  'PQC submit payload must rebuild pqcPieceValues from the current formal QA inspection itemCode list.'
)

assert.match(
  qcTemplateApiSource,
  /getPublishedQaRegulationVersion[\s\S]*\/mes\/qa\/inspection-regulation\/published-version/,
  'QA regulation page must call the formal QA inspection regulation API instead of relying on the old QC template list.'
)
assert.match(
  qcTemplatePageSource,
  /loadPublishedQaRegulation[\s\S]*getPublishedQaRegulationVersion/,
  'QA regulation page must load formal published-version evidence from the formal API.'
)
assert.match(
  qcTemplatePageSource,
  /data-qa-regulation-published-version/,
  'QA regulation page must render formal published-version evidence selectors.'
)
for (const selector of [
  'data-qa-regulation-route-version',
  'data-qa-regulation-route-process',
  'data-qa-regulation-first-inspection-rule',
  'data-qa-regulation-patrol-inspection-rule',
  'data-qa-regulation-final-inspection-rule',
  'data-qa-regulation-batch-record-binding',
  'data-qa-regulation-version-immutable'
]) {
  assert.ok(qcTemplatePageSource.includes(selector), `QA regulation page missing formal selector ${selector}`)
}
assert.doesNotMatch(
  qcTemplatePageSource,
  /formBindings[\s\S]*data-qa-regulation-batch-record-binding|data-qa-regulation-batch-record-binding[\s\S]*formBindings/,
  'QA regulation batch-record evidence must not be derived from formBindings/form slots.'
)
assert.match(
  qaControllerSource,
  /@RequestMapping\("\/mes\/qa\/inspection-regulation"\)[\s\S]*published-version[\s\S]*mes:qc-template:query/s,
  'Backend must expose a permission-protected formal QA inspection regulation published-version endpoint.'
)
assert.match(
  qaServiceSource,
  /MesQaInspectionRegulationVersionMapper[\s\S]*MesQaInspectionRegulationItemMapper[\s\S]*getSnapshotJson/s,
  'Backend service must assemble published-version evidence from regulation version, items, and immutable snapshot JSON.'
)
assert.match(
  qaPqcFormalFixtureSource,
  /JSON_OBJECT[\s\S]*batchRecordReports[\s\S]*JSON_ARRAY[\s\S]*batchRecordReportId[\s\S]*batchRecordReportName/s,
  'M6 QA/PQC formal fixture must freeze the formal per-process batch-record binding inside immutable snapshot JSON.'
)
assert.match(
  qaPqcFormalFixtureSource,
  /mes_pro_route_process[\s\S]*batch_record_report_id[\s\S]*mes_pro_batch_record_report/s,
  'M6 QA/PQC formal fixture must derive batch-record evidence from route process formal binding metadata, not formBindings.'
)
assert.doesNotMatch(
  qaPqcFormalFixtureSource,
  /formBindings[\s\S]*batchRecordReports|batchRecordReports[\s\S]*formBindings/,
  'M6 QA/PQC formal fixture must not use formBindings or form slots as the QA batch-record source.'
)
assert.match(
  qaPqcFormalFixtureSource,
  /CREATE TEMPORARY TABLE tmp_rrm_reset_pqc_task[\s\S]*mes_pqc_inspection_task[\s\S]*@rrm_active_order_id/,
  'M6 QA/PQC formal fixture must freeze the task-owned PQC task ids before resetting them to PENDING.'
)
assert.match(
  qaPqcFormalFixtureSource,
  /DELETE detail[\s\S]*FROM mes_pqc_inspection_piece_detail detail[\s\S]*JOIN tmp_rrm_reset_pqc_task/,
  'M6 QA/PQC formal fixture must clear old piece-detail rows before reusing task-owned PENDING PQC task identities.'
)
assert.match(
  qaPqcFormalFixtureSource,
  /SELECT COUNT\(\*\) = 0[\s\S]*mes_pqc_inspection_task task[\s\S]*mes_pqc_inspection_piece_detail detail[\s\S]*task_status = 'PENDING'/,
  'M6 QA/PQC formal fixture must assert no PENDING task retains old piece-detail rows after reset.'
)
for (const inspectionType of ['FIRST', 'PATROL', 'FINAL']) {
  assert.match(
    qaPqcFormalFixtureSource,
    new RegExp(
      `SELECT COUNT\\(DISTINCT route_process_id\\) = 14[\\s\\S]*WHERE inspection_type = '${inspectionType}'`
    ),
    `Every RRM formal QA regulation version must have ${inspectionType} rule evidence for the QA page.`
  )
}

console.log('PASS role-matrix QA regulation static contract')
