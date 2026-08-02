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

assert.ok(
  fs.existsSync(migrationPreflightPath),
  'M6 migration gate must provide a deterministic SQL preflight before full real E2E acceptance.'
)

const sql = fs.readFileSync(migrationPreflightPath, 'utf8')

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
  /FROM\s+`mes_pro_route_flow_process_batch_record`[\s\S]+`batch_record_report_id`\s+IS\s+NULL/i,
  'M6 migration preflight must fail on missing formal per-process batch record bindings.'
)

console.log('PASS role-matrix M6 migration preflight static contract')
