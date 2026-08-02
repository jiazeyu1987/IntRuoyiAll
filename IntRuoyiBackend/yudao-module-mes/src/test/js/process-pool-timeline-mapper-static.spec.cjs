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
  'mes_pro_process_pool_pqc_record',
  'work_order.product_id AS productId',
  'product_item.code AS productCode',
  'product_item.name AS productName',
  'pqc_task.id AS pqcTaskId',
  'pqc_task.inspection_type AS inspectionType',
  'pqc_task.business_date AS pqcBusinessDate',
  'pqc_task.shift_code AS pqcShiftCode',
  'pqc_task.round_no AS roundNo',
  'JSON_EXTRACT(pool_event.raw_payload, \'$.pqcTaskId\')',
  'mes_pqc_inspection_task pqc_task',
  'latest_submission_review.review_status AS submissionReviewStatus'
]) {
  assert(source.includes(required), `时间轴 mapper 必须读取 F1 正式字段：${required}`)
}

for (const requiredFilter of [
  'reqVO.productId',
  'reqVO.productKeyword',
  'reqVO.inspectionType',
  'reqVO.roundNo',
  'reqVO.submissionReviewStatus'
]) {
  assert(source.includes(requiredFilter), `PQC 组长提交看板必须支持 AC-D32 筛选字段：${requiredFilter}`)
}

assert.match(
  source,
  /LEFT JOIN\s+mes_pqc_inspection_task\s+pqc_task[\s\S]*pqc_task\.id\s*=[\s\S]*JSON_EXTRACT\(pool_event\.raw_payload,\s*'\$\.pqcTaskId'\)/,
  'PQC task 必须通过提交 payload 中的 pqcTaskId 精确关联，不能只按工单/工序粗粒度一对多 JOIN。'
)
assert.doesNotMatch(
  source,
  /JOIN\s+mes_pqc_inspection_task\s+pqc_task[\s\S]{0,240}pqc_task\.work_order_id\s*=\s*pool_event\.work_order_id[\s\S]{0,240}pqc_task\.route_process_id\s*=\s*pool_event\.route_process_id/s,
  '提交看板不得用 workOrderId + routeProcessId + processId 直接 JOIN PQC task，避免重复行和分页 total 漂移。'
)

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
