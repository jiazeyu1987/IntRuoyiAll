const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const readUtf8 = (absolutePath) => fs.readFileSync(absolutePath, 'utf8').replace(/\r\n/g, '\n')

const page = readUtf8(path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
))
const timelineApi = readUtf8(path.join(
  frontendRoot,
  'src/api/mes/pro/processpool/index.ts'
))
const timelineMapper = readUtf8(path.join(
  workspaceRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml'
))
const timelineReadDO = readUtf8(path.join(
  workspaceRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/ProcessPoolTimelineEventReadDO.java'
))
const timelineRespVO = readUtf8(path.join(
  workspaceRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/ProcessPoolTimelineEventRespVO.java'
))
const timelineService = readUtf8(path.join(
  workspaceRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/ProcessPoolTimelineServiceImpl.java'
))

const managementTabCount = (page.match(/data-pqc-leader-module-tab-management\b/g) || []).length
const historyTabCount = (page.match(/data-pqc-leader-module-tab-history\b/g) || []).length
assert.ok(managementTabCount > 0, 'PQC management tab must exist.')
assert.equal(
  historyTabCount,
  managementTabCount,
  '每一组 PQC 模块页签都必须在 PQC管理 同级暴露历史表单页签。'
)

assert.match(
  page,
  /<el-tab-pane\s+label="历史表单"\s+name="history"\s+data-pqc-leader-module-tab-history\s*\/>/,
  '历史表单必须是独立 PQC module tab，不能复用 PQC管理 文案。'
)
assert.match(
  page,
  /const activePqcModuleTab = ref<'personnel' \| 'management' \| 'equipment' \| 'detail' \| 'history'>\s*\(\s*'management'\s*\)/,
  'activePqcModuleTab 类型必须包含 history。'
)
assert.match(
  page,
  /const showPqcFormHistoryModule = computed\([\s\S]*activePqcModuleTab\.value === 'history'[\s\S]*\)/,
  '历史表单必须有独立显示状态，不能只靠 PQC管理 条件。'
)
assert.match(
  page,
  /showPqcFormHistoryModule\.value[\s\S]*activePqcModuleTab\.value === 'management'/,
  'PQC管理和历史表单必须复用正式 PQC 表格区域。'
)

assert.match(
  page,
  /const PQC_FORM_HISTORY_TABLE_KEY = `\$\{PQC_SUBMISSION_TABLE_KEY\}\.history`/,
  '历史表单必须使用独立列配置 tableKey，避免污染 PQC管理列设置。'
)
assert.match(
  page,
  /const pqcFormHistoryDefaultColumns[\s\S]*key: 'approvedBy'[\s\S]*label: '审核通过人'[\s\S]*key: 'approvedAt'[\s\S]*label: '审核通过时间'/,
  '历史表单默认列必须增加审核通过人和审核通过时间。'
)
assert.match(
  page,
  /activeLeaderTab\.value === 'PQC'\s*\?[\s\S]*isPqcFormHistoryTab\.value\s*\?\s*pqcFormHistoryColumnControl\s*:\s*pqcSubmissionColumnControl/,
  'PQC管理与历史表单必须选择不同列池。'
)

assert.match(
  page,
  /const isPqcFormHistoryTab = computed\(\s*\(\)\s*=>\s*activeLeaderTab\.value === 'PQC' && activePqcModuleTab\.value === 'history'\s*\)/,
  '历史表单必须有稳定 computed 状态供查询与操作边界复用。'
)
assert.match(
  page,
  /submissionReviewStatus:\s*isPqcFormHistoryTab\.value\s*\?\s*'APPROVED'\s*:\s*queryParams\.submissionReviewStatus \|\| undefined/,
  '历史表单查询必须强制提交 submissionReviewStatus=APPROVED。'
)
assert.match(
  page,
  /if \(isPqcFormHistoryTab\.value\) \{[\s\S]*queryParams\.submissionReviewStatus = 'APPROVED'[\s\S]*return[\s\S]*\}/,
  '历史表单切换或重置时必须把查询状态保持为 APPROVED。'
)

assert.match(
  page,
  /label="审核通过人"[\s\S]*data-pqc-leader-history-approved-by[\s\S]*submissionReviewLeaderUserName/,
  '历史表单表格必须显示审核通过人姓名。'
)
assert.match(
  page,
  /label="审核通过时间"[\s\S]*data-pqc-leader-history-approved-at[\s\S]*formatDateTime\(row\.submissionReviewedAt\)/,
  '历史表单表格必须显示审核通过时间。'
)
assert.match(
  page,
  /const canReviewSubmission = \(row: ProcessPoolTimelineEventVO\) =>\s*!\(isProductionReportHistoryTab\.value \|\| isPqcFormHistoryTab\.value\)[\s\S]*!row\.released[\s\S]*Boolean\(row\.id\)/,
  '历史表单行不得出现复核入口。'
)
assert.match(
  page,
  /const canCorrectSubmission = \(row: ProcessPoolTimelineEventVO\) =>\s*!\(isProductionReportHistoryTab\.value \|\| isPqcFormHistoryTab\.value\)[\s\S]*\(isProductionLeader\.value \|\| !row\.released\)[\s\S]*Boolean\(row\.id\)/,
  '历史表单行不得出现修改入口。'
)

assert.match(
  timelineApi,
  /submissionReviewLeaderUserName\?:\s*string/,
  '前端时间轴 VO 必须暴露审核通过人姓名字段。'
)
assert.match(
  timelineMapper,
  /review_leader\.nickname AS submissionReviewLeaderUserName/,
  '后端时间轴 mapper 必须读取审核通过人姓名，不能只返回 leader_user_id。'
)
assert.match(
  timelineMapper,
  /LEFT JOIN\s+system_users\s+review_leader[\s\S]*review_leader\.id\s*=\s*latest_submission_review\.leader_user_id[\s\S]*review_leader\.tenant_id\s*=\s*pool_event\.tenant_id[\s\S]*review_leader\.deleted\s*=\s*0/,
  '审核通过人姓名必须按租户和 deleted 标记关联 system_users。'
)
assert.match(
  timelineReadDO,
  /private String submissionReviewLeaderUserName;/,
  '后端读模型必须承载审核通过人姓名。'
)
assert.match(
  timelineRespVO,
  /private String submissionReviewLeaderUserName;/,
  '后端响应 VO 必须返回审核通过人姓名。'
)
assert.match(
  timelineService,
  /\.setSubmissionReviewLeaderUserName\(event\.getSubmissionReviewLeaderUserName\(\)\)/,
  '时间轴服务必须把审核通过人姓名从读模型复制到响应 VO。'
)

console.log('PASS: PQC leader form history tab static contract')
