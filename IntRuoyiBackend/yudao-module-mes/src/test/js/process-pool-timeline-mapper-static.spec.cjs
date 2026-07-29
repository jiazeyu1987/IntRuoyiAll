const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../../../../..')
const mapperPath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml'
)

assert(fs.existsSync(mapperPath), '工序池时间轴 mapper 必须存在。')

const source = fs.readFileSync(mapperPath, 'utf8')

for (const required of [
  'mes_pro_process_pool_event',
  'pool_event.server_submit_time AS submittedAt',
  'pool_event.raw_payload',
  'pool_event.device_account_id AS loginUserId',
  'pool_event.actual_employee_id AS actualEmployeeUserId',
  'pool_event.signature_user_id AS signatureEmployeeUserId',
  'pool_event.signature_id AS electronicSignatureId',
  'pool_event.feedback_source_id AS sourceFeedbackId',
  'pool_event.recordbook_source_id AS sourceRecordbookEventId',
  'mes_pro_process_pool_pqc_record'
]) {
  assert(source.includes(required), `时间轴 mapper 必须读取 F1 正式字段：${required}`)
}

for (const forbidden of [
  'pool_event.submitted_at',
  'pool_event.login_user_name',
  'pool_event.actual_employee_user_id',
  'pool_event.signature_employee_user_id',
  'pool_event.electronic_signature_id',
  'pool_event.source_feedback_id',
  'pool_event.original_payload_json',
  'feedback_surplus_pool',
  'surplusPool'
]) {
  assert(!source.includes(forbidden), `时间轴 mapper 不得引用非 F1 工序池字段或余量池：${forbidden}`)
}

console.log('PASS process-pool-timeline-mapper-static')
