const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const fixturePath = path.join(
  workspaceRoot,
  'doc/tasks/20260801-role-requirement-matrix-implementation/m6-local-runtime-qa-pqc-formal-fixture.sql'
)

assert.ok(
  fs.existsSync(fixturePath),
  'M6 local formal QA/PQC fixture must exist before repeatable real E2E can run.'
)

const source = fs.readFileSync(fixturePath, 'utf8')

for (const token of [
  'mes_pqc_inspection_task',
  'mes_pqc_inspection_piece_detail',
  'mes_qa_inspection_regulation',
  'mes_qa_inspection_regulation_item',
  'active_order_id',
  'route_process_id',
  'regulation_version_id',
  'planned_inspection_quantity',
  'task_status',
  'PENDING',
  'SUBMITTED',
  'ACTIVE',
  'REMOVED'
]) {
  assert.match(source, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `fixture SQL must include ${token}.`)
}

assert.match(
  source,
  /active_status\s+IN\s*\([\s\S]*'ACTIVE'[\s\S]*'REMOVED'[\s\S]*\)/,
  'Repeatable M6 fixture must accept the task-owned active order after final cleanup leaves it REMOVED.'
)
assert.match(
  source,
  /tmp_rrm_pqc_task_slot[\s\S]*slot_offset/,
  'Repeatable M6 fixture must allocate future formal PENDING task slots instead of reusing a consumed identity.'
)
assert.match(
  source,
  /INSERT\s+IGNORE\s+INTO\s+mes_pqc_inspection_task[\s\S]*DATE_ADD\(\s*@rrm_business_date\s*,\s*INTERVAL\s+slot\.slot_offset\s+DAY\s*\)/,
  'Repeatable M6 fixture must top up formal future-dated PENDING tasks without mutating submitted history.'
)
assert.match(
  source,
  /DELETE\s+detail[\s\S]*WHERE[\s\S]*task\.task_status\s*=\s*'PENDING'/s,
  'Repeatable M6 fixture may clear piece details only for still-PENDING local tasks, never submitted history.'
)
assert.doesNotMatch(
  source,
  /ON\s+DUPLICATE\s+KEY\s+UPDATE[\s\S]*task_status\s*=\s*'PENDING'/i,
  'Repeatable M6 fixture must not reset SUBMITTED PQC tasks back to PENDING.'
)
assert.doesNotMatch(
  source,
  /DELETE\s+detail[\s\S]*JOIN\s+tmp_rrm_reset_pqc_task[\s\S]*WHERE\s+detail\.tenant_id/i,
  'Repeatable M6 fixture must not delete all historical piece details for the active order task identity.'
)
assert.doesNotMatch(
  source,
  /task_status\s*=\s*'PENDING'[\s\S]*updater\s*=\s*@rrm_actor/i,
  'Repeatable M6 fixture must not repair repeatability by overwriting existing task status.'
)

console.log('PASS role-matrix-pqc-rerun fixture static contract')
