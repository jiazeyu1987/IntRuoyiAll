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

const reportTabCount = (page.match(/data-production-leader-module-tab-report(?=[\s/>])/g) || []).length
const historyTabCount = (page.match(/data-production-leader-module-tab-report-history(?=[\s/>])/g) || []).length
assert.ok(reportTabCount > 0, 'production leader report tab must exist.')
assert.equal(
  historyTabCount,
  reportTabCount,
  '每一组生产组长模块页签都必须在报工管理旁边暴露报工历史页签。'
)

assert.match(
  page,
  /<el-tab-pane\s+label="报工历史"\s+name="reportHistory"\s+data-production-leader-module-tab-report-history\s*\/>/,
  '报工历史必须是独立 production module tab，不能复用报工管理文案。'
)
assert.match(
  page,
  /const activeProductionModuleTab = ref<[\s\S]*'reportHistory'[\s\S]*>\('report'\)/,
  'activeProductionModuleTab 类型必须包含 reportHistory。'
)
assert.match(
  page,
  /const showProductionReportHistoryModule = computed\([\s\S]*activeProductionModuleTab\.value === 'reportHistory'[\s\S]*\)/,
  '报工历史必须有独立显示状态，不能只靠报工管理条件。'
)
assert.match(
  page,
  /showProductionReportModule\.value \|\|\s*showProductionReportHistoryModule\.value/,
  '报工管理和报工历史必须复用正式报工表格区域。'
)

assert.match(
  page,
  /const PRODUCTION_REPORT_HISTORY_TABLE_KEY = `\$\{SUBMISSION_TABLE_KEY\}\.productionHistory`/,
  '报工历史必须使用独立列配置 tableKey，避免污染报工管理列设置。'
)
assert.match(
  page,
  /const productionReportHistoryDefaultColumns[\s\S]*key: 'approvedBy'[\s\S]*label: '审核通过人'[\s\S]*key: 'approvedAt'[\s\S]*label: '审核通过时间'/,
  '报工历史默认列必须增加审核通过人和审核通过时间。'
)
assert.match(
  page,
  /activeProductionModuleTab\.value === 'reportHistory'\s*\?\s*productionReportHistoryColumnControl\s*:\s*productionSubmissionColumnControl/,
  '生产组长报工管理与报工历史必须选择不同列池。'
)

assert.match(
  page,
  /const isProductionReportHistoryTab = computed\(\(\) =>\s*isProductionLeader\.value && activeProductionModuleTab\.value === 'reportHistory'\s*\)/,
  '报工历史必须有稳定 computed 状态供查询与操作边界复用。'
)
assert.match(
  page,
  /allocationView:\s*isProductionLeader\.value[\s\S]*isProductionReportHistoryTab\.value[\s\S]*'HISTORY'[\s\S]*'WORKBENCH'/,
  '报工管理和报工历史必须通过独立 allocationView 查询。'
)
assert.match(
  page,
  /if \(isProductionReportHistoryTab\.value\) \{[\s\S]*queryParams\.submissionReviewStatus = undefined[\s\S]*return[\s\S]*\}/,
  '报工历史不得按复核通过状态过滤提交事实。'
)

assert.match(
  page,
  /label="审核通过人"[\s\S]*data-team-leader-report-history-approved-by[\s\S]*submissionReviewLeaderUserName/,
  '报工历史表格必须显示审核通过人姓名。'
)
assert.match(
  page,
  /label="审核通过时间"[\s\S]*data-team-leader-report-history-approved-at[\s\S]*formatDateTime\(row\.submissionReviewedAt\)/,
  '报工历史表格必须显示审核通过时间。'
)
assert.match(
  page,
  /const canReviewSubmission = \(row: ProcessPoolTimelineEventVO\) =>\s*(?:!isProductionReportHistoryTab\.value|!\(isProductionReportHistoryTab\.value\s*\|\|\s*isPqcFormHistoryTab\.value\))[\s\S]*row\.submissionReviewStatus === 'PENDING'/,
  '报工历史行不得出现复核入口。'
)
assert.match(
  page,
  /const canCorrectSubmission = \(row: ProcessPoolTimelineEventVO\) =>\s*(?:!isProductionReportHistoryTab\.value|!\(isProductionReportHistoryTab\.value\s*\|\|\s*isPqcFormHistoryTab\.value\))[\s\S]*row\.submissionReviewStatus === 'REJECTED'/,
  '报工历史行不得出现修改入口。'
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

console.log('PASS: production report history tab static contract')
