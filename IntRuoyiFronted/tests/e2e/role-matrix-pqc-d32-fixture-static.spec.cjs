const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const fixturePath = path.join(
  workspaceRoot,
  'doc/tasks/20260801-role-requirement-matrix-implementation/m6-pqc-d32-same-filter-local-seed.sql'
)

assert.ok(
  fs.existsSync(fixturePath),
  'M6 D32 same-filter PQC fixture SQL must exist before AC-D32 real pagination can clear.'
)

const source = fs.readFileSync(fixturePath, 'utf8')

for (const token of [
  'mes_pqc_inspection_task',
  'mes_pro_process_pool_submission_review',
  'active_order_id',
  'work_order_id',
  'route_process_id',
  'process_id',
  'regulation_version_id',
  'inspection_type',
  'business_date',
  'shift_code',
  'round_no',
  'planned_inspection_quantity',
  'task_status',
  'PENDING',
  'PATROL',
  'CURDATE()',
  'v_pending_same_filter_event_count',
  'v_pending_same_filter_task_count',
  'RRM M6 D32 same-filter local E2E fixture'
]) {
  assert.match(source, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `fixture SQL must include ${token}.`)
}

assert.match(
  source,
  /SET\s+v_route_process_id\s*=\s*928609\b/,
  'D32 fixture must target the already-submitted first pressure-pump route process 928609.'
)
assert.match(
  source,
  /SET\s+v_process_id\s*=\s*922985\b/,
  'D32 fixture must target processId 922985 so the existing process filter can reach page 2.'
)
assert.match(
  source,
  /SIGNAL\s+SQLSTATE\s+'45000'/,
  'D32 fixture must fail fast when formal prerequisites are missing.'
)
assert.match(
  source,
  /COALESCE\(\s*latest_submission_review\.review_status[\s\S]*CONVERT\('PENDING'[\s\S]*=\s*CONVERT\('PENDING'/,
  'D32 fixture must count only same-filter pending review events; approved or rejected events cannot satisfy pending pagination.'
)
assert.match(
  source,
  /WHILE\s+v_pending_same_filter_event_count\s*\+\s*v_pending_same_filter_task_count\s+<\s+2\s+DO[\s\S]*INSERT\s+INTO\s+mes_pqc_inspection_task[\s\S]*SET\s+v_pending_same_filter_task_count\s*=\s*v_pending_same_filter_task_count\s*\+\s*1/,
  'D32 fixture must prepare enough formal pending PQC tasks so real page submissions can reach two same-filter pending events.'
)
assert.doesNotMatch(
  source,
  /INSERT\s+INTO\s+mes_pro_process_pool_event/i,
  'D32 fixture must not fake submitted process-pool events; the real page must create the second event.'
)
assert.doesNotMatch(
  source,
  /UPDATE\s+mes_pqc_inspection_task[\s\S]*task_status\s*=\s*'SUBMITTED'/i,
  'D32 fixture must not mark the task submitted; the real PQC page submit path must do that.'
)

console.log('PASS role-matrix-pqc-d32 fixture static contract')
