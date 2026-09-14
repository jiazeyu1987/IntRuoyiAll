const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const repairPath = path.join(
  workspaceRoot,
  'doc/tasks/20260809-pqc-formal-standard-method-source/local-cleaning-formal-source-repair.sql'
)
const rollbackPath = path.join(
  workspaceRoot,
  'doc/tasks/20260809-pqc-formal-standard-method-source/local-cleaning-formal-source-rollback.sql'
)

assert.ok(fs.existsSync(repairPath), 'The scoped local data repair SQL must exist.')
assert.ok(fs.existsSync(rollbackPath), 'The scoped local rollback SQL must exist.')

const repair = fs.readFileSync(repairPath, 'utf8')
const rollback = fs.readFileSync(rollbackPath, 'utf8')

assert.match(repair, /@target_tenant_id\s*:=\s*1/)
assert.match(repair, /@target_active_order_id\s*:=\s*49/)
assert.match(repair, /@target_work_order_id\s*:=\s*923889/)
assert.match(repair, /@target_route_version_id\s*:=\s*627/)
assert.match(repair, /@target_route_process_id\s*:=\s*980647/)
assert.match(repair, /@retired_fixture_regulation_id\s*:=\s*41/)
assert.match(
  repair,
  /SELECT COUNT\(\*\) INTO v_count[\s\S]*FROM mes_pqc_inspection_task[\s\S]*task_status = 'CANCELLED'[\s\S]*actual_inspection_quantity = 0[\s\S]*IF v_count <> 4/,
  'Repair must be blocked unless the parallel task left exactly four cancelled, untouched fixture tasks.'
)
assert.match(
  repair,
  /SELECT COUNT\(\*\) INTO v_count[\s\S]*FROM mes_pqc_inspection_piece_detail[\s\S]*JOIN mes_pqc_inspection_task[\s\S]*IF v_count <> 0/,
  'Repair must be blocked when the target tasks have piece-detail data.'
)
assert.match(repair, /version_no[\s\S]*'G\/0'/)
assert.match(
  repair,
  /UPDATE mes_qa_inspection_regulation[\s\S]*SET deleted = b'1'[\s\S]*id = @retired_fixture_regulation_id/,
  'Authorized repair must soft-delete only the exact retired fixture regulation to release the route-process unique key.'
)
assert.match(
  repair,
  /INSERT INTO mes_qa_inspection_regulation[\s\S]*'MES_QA'/,
  'Repair must create a new formally owned MES_QA regulation instead of reactivating the fixture.'
)
assert.match(repair, /item_code[\s\S]*'ID-001-WASH-APP'/)
assert.match(
  repair,
  /弹簧、胶塞、套筒、手柄、齿条、芯杆、螺盖清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。/
)
assert.match(
  repair,
  /正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。/
)
for (const inspectionType of ['FIRST', 'PATROL', 'FINAL']) {
  assert.match(repair, new RegExp(`'${inspectionType}'`))
}
assert.match(
  repair,
  /INSERT INTO mes_pqc_inspection_task[\s\S]*@new_version_id[\s\S]*'PENDING'/,
  'Repair must create new task-owned pending tasks without reactivating or rewriting cancelled fixture tasks.'
)
assert.match(
  rollback,
  /DELETE FROM mes_pqc_inspection_task[\s\S]*creator = 'codex-pqc-formal-standard'/,
  'Rollback must delete only the new task-owned pending tasks.'
)
assert.match(rollback, /DELETE FROM mes_qa_inspection_regulation_item/)
assert.match(rollback, /DELETE FROM mes_qa_inspection_regulation_version/)
assert.match(rollback, /DELETE FROM mes_qa_inspection_regulation/)
assert.match(
  rollback,
  /UPDATE mes_qa_inspection_regulation[\s\S]*SET deleted = b'0'[\s\S]*id = @retired_fixture_regulation_id/,
  'Rollback must restore the retired fixture regulation unique-key row exactly.'
)

console.log('PASS pqc-cleaning-formal-source-repair-static')
