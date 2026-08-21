const assert = require('node:assert/strict')
const { spawnSync } = require('node:child_process')
const fs = require('node:fs')
const path = require('node:path')

const orchestrator = path.resolve(
  __dirname,
  '../../../doc/tasks/20260814-frontline-active-order-submit-allocation-docs/fas_fixture_orchestrator.py'
)

assert.equal(fs.existsSync(orchestrator), true, 'P5 必须提供任务自有 fixture 外部编排器。')
const source = fs.readFileSync(orchestrator, 'utf8')

assert.match(source, /TASK_ID\s*=\s*["']20260814-frontline-active-order-submit-allocation-docs["']/)
assert.match(source, /DATA_PREFIX\s*=\s*["']FAS-20260814-["']/)
assert.match(source, /TENANT_ID\s*=\s*122/)
assert.match(source, /TENANT_NAME\s*=\s*["']测试租户["']/)

for (const action of ['prepare', 'verify', 'cleanup']) {
  assert.match(source, new RegExp(`\\b${action}\\b`), `外部编排器必须支持 ${action}。`)
}

for (const table of [
  'system_users',
  'system_role',
  'system_role_menu',
  'system_user_role',
  'system_login_log',
  'system_operate_log',
  'dcc_electronic_signature_authorization',
  'dcc_electronic_signature_authorization_audit',
  'mes_pro_route',
  'mes_pro_route_process',
  'mes_pro_route_version',
  'mes_pro_process_pool_team_employee_profile',
  'mes_pro_process_pool_team_employee_binding',
  'mes_pro_work_order',
  'mes_pro_process_pool_active_order',
  'mes_pro_process_pool_active_order_process_snapshot',
  'mes_pro_process_pool_submission_review',
  'mes_pro_process_pool_report_allocation',
  'mes_pro_process_pool_report_allocation_adjustment_audit',
  'mes_pro_process_pool_report_allocation_state',
  'mes_pro_process_pool_fifo_allocation_line',
  'mes_pro_process_pool_quantity_fragment',
  'mes_pro_process_pool_event',
  'mes_pro_process_pool',
  'mes_pro_feedback',
  'mes_pro_batch_record_execution_signature',
  'mes_md_auto_code_record'
]) {
  assert.match(source, new RegExp(table), `fixture/cleanup 合同必须覆盖 ${table}。`)
}

assert.match(source, /begin\(\)[\s\S]*commit\(\)/, 'fixture 写入必须在显式事务中提交。')
assert.match(source, /except[\s\S]*rollback\(\)/, 'fixture 任一断言失败必须整体回滚。')
assert.match(source, /FRONTLINE_MENU_IDS\s*=\s*\[[^\]]*5550[^\]]*5551[^\]]*5552[^\]]*900437[^\]]*\]/, '一线角色必须同时具备正式报工查询/创建权限和真实页面菜单。')
assert.match(source, /FRONTLINE_MENU_IDS\s*=\s*\[[^\]]*1221[^\]]*\]/, '一线账号必须具备全局审批待办徽标查询权限，真实页面不得产生权限错误。')
assert.match(source, /LEADER_MENU_IDS\s*=\s*\[[^\]]*1221[^\]]*\]/, '组长账号必须具备全局审批待办徽标查询权限，真实页面不得产生权限错误。')
assert.match(source, /username=%s[\s\S]*\(TENANT_ID,\s*["']admin["']\)/, '任务账号必须复制固定测试租户本机默认口令对应的 admin 哈希，不能复制其它账号哈希。')
assert.match(source, /review_id[\s\S]*is_nullable[\s\S]*YES/i, 'verify 必须明确核验正式迁移已使 report allocation.review_id 可空。')
assert.match(
  source,
  /mes_pro_process_pool_order_process_completion[\s\S]*last_review_id[\s\S]*is_nullable[\s\S]*YES/i,
  'verify 必须明确核验一线初始分配可在组长复核前保存 completion.last_review_id 空值。'
)
assert.match(
  source,
  /DELETE FROM `mes_pro_process_pool_order_process_completion` WHERE tenant_id=%s AND work_order_id IN/,
  'cleanup 必须按任务自有订单精确删除初始分配产生的订单工序完成状态。'
)
assert.match(
  source,
  /SELECT COUNT\(\*\) FROM mes_pro_process_pool_order_process_completion WHERE tenant_id=%s AND work_order_id IN/,
  'cleanup 的零残留核验必须覆盖订单工序完成状态。'
)
assert.match(source, /existingTaskDataCount/, 'prepare 必须在写入前检测并阻塞既有任务数据残留。')
assert.match(
  source,
  /["']configSnapshots["']:\s*\{["']flowGraph["']:\s*\{["']nodes["']:/,
  '路线版本必须把正式工序图写入 configSnapshots.flowGraph，供活跃订单页面读取。'
)
assert.match(
  source,
  /snapshot\.get\(["']configSnapshots["'],\s*\{\}\)\.get\(["']flowGraph["'],\s*\{\}\)\.get\(["']nodes["'],\s*\[\]\)/,
  'fixture verify 必须核验 configSnapshots.flowGraph.nodes 的正式工序身份。'
)
assert.match(
  source,
  /["']authorization_state["']:\s*["']ENABLED["']/,
  '任务账号电子签名授权必须写入正式 ENABLED 状态。'
)
assert.match(
  source,
  /grants\s*!=\s*\{[^}]+:\s*\(1,\s*["']ENABLED["']\)[^}]+:\s*\(1,\s*["']ENABLED["']\)/,
  'fixture verify 必须按正式 ENABLED 状态核验两个任务账号。'
)
assert.match(source, /review_signature_id/, 'cleanup 必须读取并清理组长复核签名精确 ID。')
assert.match(source, /RETAINED_EDHR_AUDIT_EVIDENCE_TABLES/, 'cleanup 必须显式声明保留 eDHR 追加型审计证据链。')
for (const table of [
  'mes_pro_batch_record_execution',
  'mes_pro_batch_record_execution_signature',
  'mes_pro_batch_record_execution_field_audit_batch',
  'mes_pro_batch_record_execution_field_audit_item'
]) {
  assert.match(source, new RegExp(table), `cleanup 必须识别 ${table} 为审计证据链的一部分。`)
}
assert.doesNotMatch(
  source,
  /DELETE FROM `mes_pro_batch_record_execution(?:_signature|_field_audit_(?:batch|item))?`/,
  'cleanup 禁止删除 eDHR execution/signature/field audit 追加型证据链。'
)
assert.doesNotMatch(
  source,
  /delete_ids\(cur,\s*["']mes_pro_batch_record_execution_signature["']/,
  'cleanup 禁止通过 ID 删除 eDHR 签名证据。'
)
assert.match(source, /retainedEdhrAuditEvidenceCount/, 'cleanup 必须单独报告保留的 eDHR 审计证据数量。')
assert.match(source, /remainingTaskDataCount/, 'cleanup 必须返回机器可读残留数量。')
assert.match(source, /cleanupVerified/, 'cleanup 必须返回机器可读核验状态。')
assert.doesNotMatch(source, /["'](?:admin123|password|secret)["']/i, '编排器不得硬编码口令。')
assert.doesNotMatch(source, /DELETE\s+FROM\s+\w+\s*(?:;|["'])/i, '清理 SQL 禁止无 WHERE 全表删除。')

const selfTest = spawnSync('python', ['-X', 'utf8', orchestrator, '--self-test'], {
  cwd: path.resolve(__dirname, '../../..'),
  encoding: 'utf8',
  windowsHide: true
})
assert.equal(selfTest.status, 0, `fixture 编排器自检失败：${selfTest.stderr || selfTest.stdout}`)

console.log('PASS: active-order submit allocation task-owned fixture contract')
