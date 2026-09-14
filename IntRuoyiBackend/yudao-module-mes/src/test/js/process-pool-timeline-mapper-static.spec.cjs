const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../../../../..')
const mapperPath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml'
)
const performanceMigrationPath = path.join(
  repoRoot,
  'IntRuoyiBackend/sql/mysql/20260804_mes_process_pool_timeline_performance_indexes.sql'
)

assert(fs.existsSync(mapperPath), '工序池时间轴 mapper 必须存在。')
assert(fs.existsSync(performanceMigrationPath), 'AC-D32 PQC 提交看板必须有 M6 时间轴性能索引迁移。')

const source = fs.readFileSync(mapperPath, 'utf8')
const performanceMigration = fs.readFileSync(performanceMigrationPath, 'utf8')

for (const required of [
  'mes_pro_process_pool_event',
  'pool_event.server_submit_time AS submittedAt',
  'pool_event.raw_payload',
  'pool_event.device_account_id AS loginUserId',
  'pool_event.actual_employee_id AS actualEmployeeUserId',
  "COALESCE(NULLIF(actual_employee.nickname, ''), NULLIF(actual_employee_profile_by_user.display_name, ''), NULLIF(actual_employee_profile_by_user.employee_name, ''), NULLIF(actual_employee_profile_by_id.display_name, ''), NULLIF(actual_employee_profile_by_id.employee_name, ''), NULLIF(actual_employee.username, '')) AS actualEmployeeUserName",
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
  'pqc.process_inspection_aggregation_status AS processInspectionAggregationStatus',
  'pqc.process_inspection_review_id AS processInspectionReviewId',
  'pqc.process_inspection_aggregated_at AS processInspectionAggregatedAt',
  'pool_event.pqc_task_id',
  'mes_pqc_inspection_task pqc_task',
  'latest_submission_review.review_status AS submissionReviewStatus',
  'review_leader.nickname AS submissionReviewLeaderUserName'
]) {
  assert(source.includes(required), `时间轴 mapper 必须读取 F1 正式字段：${required}`)
}

assert.doesNotMatch(
  source,
  /NULL\s+AS\s+actualEmployeeUserName/,
  '生产组长报工列表员工列必须从正式用户表读取姓名，不能返回 NULL 后让前端退回显示员工编号。'
)
assert.match(
  source,
  /LEFT JOIN\s+system_users\s+actual_employee[\s\S]{0,240}actual_employee\.id\s*=\s*pool_event\.actual_employee_id[\s\S]{0,240}actual_employee\.tenant_id\s*=\s*pool_event\.tenant_id[\s\S]{0,240}actual_employee\.deleted\s*=\s*0/,
  '时间轴 mapper 必须按租户和删除标记关联 system_users actual_employee，解析正式员工身份。'
)
assert.match(
  source,
  /LEFT JOIN\s+mes_pro_process_pool_team_employee_profile\s+actual_employee_profile_by_id[\s\S]{0,260}actual_employee_profile_by_id\.id\s*=\s*pool_event\.actual_employee_id[\s\S]{0,260}actual_employee_profile_by_id\.tenant_id\s*=\s*pool_event\.tenant_id[\s\S]{0,260}actual_employee_profile_by_id\.deleted\s*=\s*0/,
  '时间轴 mapper 必须按档案 ID 关联临时员工档案，读取档案显示名。'
)
assert.match(
  source,
  /LEFT JOIN\s*\(\s*SELECT[\s\S]{0,500}ROW_NUMBER\(\)\s+OVER\s*\([\s\S]{0,260}PARTITION BY\s+profile\.tenant_id,\s*profile\.system_user_id[\s\S]{0,500}actual_employee_profile_by_user[\s\S]{0,260}actual_employee_profile_by_user\.system_user_id\s*=\s*pool_event\.actual_employee_id/,
  '时间轴 mapper 必须以不产生重复事件的方式按系统用户 ID 关联正式员工档案。'
)
assert.match(
  source,
  /LEFT JOIN\s+system_users\s+review_leader[\s\S]{0,260}review_leader\.id\s*=\s*latest_submission_review\.leader_user_id[\s\S]{0,260}review_leader\.tenant_id\s*=\s*pool_event\.tenant_id[\s\S]{0,260}review_leader\.deleted\s*=\s*0/,
  '报工历史必须按租户和删除标记关联 system_users review_leader，使用 nickname 作为审核通过人姓名。'
)

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
  /<if test="reqVO\.submittedAtStart != null and reqVO\.submittedAtEnd != null">[\s\S]*pool_event\.server_submit_time\s*<!\[CDATA\[>=\]\]>\s*#\{reqVO\.submittedAtStart\}[\s\S]*pool_event\.server_submit_time\s*<!\[CDATA\[<\]\]>\s*#\{reqVO\.submittedAtEnd\}[\s\S]*<\/if>/,
  '提交日期未选择时不得应用隐藏时间窗口；显式日期必须继续按闭开区间过滤。'
)
assert.match(
  source,
  /ORDER BY\s+pool_event\.server_submit_time\s+DESC,\s*pool_event\.id\s+DESC\s+LIMIT\s+#\{offset\},\s*#\{reqVO\.pageSize\}/,
  '报工管理分页必须按服务端提交时间倒序返回，确保最近提交的报工记录在第一页最前面；时间相同按事件 ID 倒序稳定排列。'
)
assert.doesNotMatch(
  source,
  /ORDER BY\s+pool_event\.server_submit_time\s+ASC,\s*pool_event\.id\s+ASC/,
  '报工管理分页不得按提交时间升序返回。'
)

assert.match(
  source,
  /LEFT JOIN\s+mes_pqc_inspection_task\s+pqc_task[\s\S]*pqc_task\.id\s*=\s*pool_event\.pqc_task_id/,
  'PQC task 必须通过持久化 pqc_task_id 精确关联，不能在分页查询中逐行 JSON_EXTRACT 或按工单/工序粗粒度 JOIN。'
)
assert.doesNotMatch(
  source,
  /JOIN\s+mes_pqc_inspection_task\s+pqc_task[\s\S]{0,240}pqc_task\.work_order_id\s*=\s*pool_event\.work_order_id[\s\S]{0,240}pqc_task\.route_process_id\s*=\s*pool_event\.route_process_id/s,
  '提交看板不得用 workOrderId + routeProcessId + processId 直接 JOIN PQC task，避免重复行和分页 total 漂移。'
)
assert.doesNotMatch(
  source,
  /JSON_EXTRACT\(pool_event\.raw_payload,\s*'\$\.pqcTaskId'\)/,
  'AC-D32 分页查询不得在 JOIN 条件中逐行 JSON_EXTRACT pqcTaskId，必须使用迁移生成的可索引 pqc_task_id。'
)

for (const requiredMigrationClause of [
  '-- release-migration: allowedEnvironments=test,backup,prod;',
  '20260804_mes_process_pool_timeline_performance_indexes',
  '`pqc_task_id` bigint GENERATED ALWAYS AS',
  'JSON_EXTRACT(`raw_payload`, \'\'$.pqcTaskId\'\')',
  'idx_mes_pp_event_timeline_acd32',
  'idx_mes_pqc_task_timeline_acd32',
  'idx_mes_pp_review_latest_event'
]) {
  assert(
    performanceMigration.includes(requiredMigrationClause),
    `AC-D32 性能迁移必须包含正式索引/生成列合同：${requiredMigrationClause}`
  )
}
assert.match(
  performanceMigration,
  /KEY `idx_mes_pp_event_timeline_acd32` \(`tenant_id`, `deleted`, `template_type`, `actual_employee_id`, `process_id`, `work_order_id`, `server_submit_time`, `id`\)/,
  '工序池事件必须具备 AC-D32 同条件分页索引，覆盖租户、删除标记、PQC 模板、人员、工序、工单、提交时间和稳定排序 id。'
)
assert.match(
  performanceMigration,
  /KEY `idx_mes_pqc_task_timeline_acd32` \(`tenant_id`, `deleted`, `inspection_type`, `round_no`, `id`\)/,
  'PQC task 必须具备 AC-D32 检验类型、轮次和精确 task id 关联索引。'
)
assert.match(
  performanceMigration,
  /KEY `idx_mes_pp_review_latest_event` \(`tenant_id`, `deleted`, `event_id`, `reviewed_at`, `id`\)/,
  '最新复核状态子查询必须具备按事件取最新复核的索引，避免提交看板分页出现全量 review 扫描。'
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
