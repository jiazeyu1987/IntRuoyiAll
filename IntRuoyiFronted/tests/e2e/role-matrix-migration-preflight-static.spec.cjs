const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const backendRoot = path.join(workspaceRoot, 'IntRuoyiBackend')
const migrationPreflightPath = path.join(
  backendRoot,
  'sql/mysql/20260802_role_requirement_matrix_m6_migration_preflight.sql'
)
const pqcPieceDetailReconcilePath = path.join(
  backendRoot,
  'sql/mysql/20260804_mes_pqc_piece_detail_legacy_equipment_nullable.sql'
)

assert.ok(
  fs.existsSync(migrationPreflightPath),
  'M6 migration gate must provide a deterministic SQL preflight before full real E2E acceptance.'
)
assert.ok(
  fs.existsSync(pqcPieceDetailReconcilePath),
  'M6 migration gate must reconcile legacy PQC piece-detail equipment columns before full real E2E acceptance.'
)

const sql = fs.readFileSync(migrationPreflightPath, 'utf8')
const pieceDetailReconcileSql = fs.readFileSync(pqcPieceDetailReconcilePath, 'utf8')

for (const token of [
  'release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_pqc_inspection_task,20260802_mes_process_pool_active_order_transfer_trace,20260802_mes_process_pool_team_leader_scope_extended; type=config; riskLevel=medium',
  'assert_rrm_m6_active_order_conflicts',
  'assert_rrm_m6_open_order_authority',
  'assert_rrm_m6_pqc_task_authority',
  'assert_rrm_m6_batch_record_binding_authority',
  'SIGNAL SQLSTATE',
  '双活跃来源冲突',
  '开放订单缺路线版本或系数',
  '开放PQC缺任务身份或规程版本',
  '正式批记录绑定缺失或冲突'
]) {
  assert.match(sql, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `M6 migration preflight must include ${token}.`)
}

assert.match(
  sql,
  /FROM\s+`mes_pro_process_pool_active_order`[\s\S]+GROUP BY[\s\S]+`work_order_id`[\s\S]+HAVING\s+COUNT\(\*\)\s*>\s*1/i,
  'M6 migration preflight must detect duplicate active orders by formal order/route/version identity.'
)
assert.match(
  sql,
  /FROM\s+`mes_pro_process_pool_active_order`[\s\S]+`route_version_id`\s+IS\s+NULL[\s\S]+`erp_fixed_quantity_snapshot`\s+IS\s+NULL/i,
  'M6 migration preflight must fail on open active orders missing route version or ERP fixed quantity.'
)
assert.match(
  sql,
  /FROM\s+`mes_pqc_inspection_task`[\s\S]+`active_order_id`\s+IS\s+NULL[\s\S]+`regulation_version_id`\s+IS\s+NULL/i,
  'M6 migration preflight must fail on open PQC tasks missing task identity or regulation version.'
)
assert.match(
  sql,
  /FROM\s+`mes_pro_route_flow_process_batch_record`[\s\S]+`form_slot_type`\s*=\s*'MAIN'[\s\S]+`record_category`\s*=\s*'BATCH_RECORD'[\s\S]+`batch_record_report_id`\s+IS\s+NULL/i,
  'M6 migration preflight must fail only on missing formal MAIN/BATCH_RECORD per-process batch record bindings.'
)
assert.doesNotMatch(
  sql,
  /WHERE\s+`deleted`\s*=\s*b'0'\s+AND\s+\(\s*`batch_record_report_id`\s+IS\s+NULL/i,
  'M6 migration preflight must not treat INTERNAL_RECORD form slots as missing formal batch record bindings.'
)

for (const token of [
  'release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_pqc_inspection_task; type=schema; riskLevel=medium',
  'selected_equipment_id',
  'selected_equipment_code',
  'selected_equipment_name',
  'selected_equipment_number',
  'MODIFY COLUMN `selected_equipment_id` bigint NULL',
  'MODIFY COLUMN `selected_equipment_code` varchar(64) NULL',
  'MODIFY COLUMN `selected_equipment_name` varchar(128) NULL',
  'MODIFY COLUMN `selected_equipment_number` varchar(64) NULL'
]) {
  assert.match(
    pieceDetailReconcileSql,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `PQC piece-detail legacy equipment schema reconcile must include ${token}.`
  )
}

console.log('PASS role-matrix M6 migration preflight static contract')
