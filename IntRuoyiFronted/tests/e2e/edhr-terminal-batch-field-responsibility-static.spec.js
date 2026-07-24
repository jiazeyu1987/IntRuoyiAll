const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)
const fieldAuditPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr', 'FieldAuditPage.vue')

const detail = fs.readFileSync(detailPath, 'utf8')
const fieldAudit = fs.readFileSync(fieldAuditPath, 'utf8')

assert.match(
  detail,
  /const TRACE_RECORD_FIELD_RESPONSIBILITY_STATUSES = \[\s*EDHR_BATCH_STATUS_CLOSED,\s*EDHR_BATCH_STATUS_ARCHIVED,\s*EDHR_BATCH_STATUS_REJECTED,\s*EDHR_BATCH_STATUS_VOIDED\s*\] as const/,
  '终态批次字段责任入口必须集中覆盖已放行/已归档、质量终态和作废状态。'
)
assert.match(
  detail,
  /const showFieldResponsibilityTab = computed\([\s\S]*TRACE_RECORD_FIELD_RESPONSIBILITY_STATUSES[\s\S]*hasReleaseTransaction\.value[\s\S]*\)/,
  '已放行未归档和批次终态都必须允许在追溯记录中查看字段责任。'
)

const traceDrawerMatch = detail.match(
  /<el-drawer v-model="traceRecordDrawerVisible" title="追溯记录"[\s\S]*?<\/el-drawer>/
)
assert.ok(traceDrawerMatch, '批次详情必须保留追溯记录抽屉。')
const traceDrawer = traceDrawerMatch[0]
assert.match(
  traceDrawer,
  /<el-tab-pane\s+v-if="showFieldResponsibilityTab"\s+label="字段责任"\s+name="fieldResponsibility"/,
  '追溯记录抽屉必须为终态批次提供字段责任页签。'
)
assert.match(
  traceDrawer,
  /fieldResponsibilityEntries[\s\S]*openFieldResponsibility/,
  '字段责任页签必须按批次执行记录列出责任汇总入口。'
)
assert.match(
  detail,
  /type TraceRecordTab = 'release' \| 'change' \| 'audit' \| 'domain' \| 'fieldResponsibility'/,
  '追溯记录 tab 状态必须显式包含字段责任页签。'
)
assert.match(
  detail,
  /const fieldResponsibilityEntries = computed<TraceRecordFieldResponsibilityEntry\[\]>\(\(\) => \{[\s\S]*executionReviews\.value[\s\S]*sortedTasks\.value[\s\S]*executionId/,
  '字段责任入口必须从复盘执行记录和批次任务中收集真实 executionId，不能凭批次创建人或管理员推断。'
)
assert.match(
  detail,
  /path:\s*'\/mes\/pro\/feedback\/edhr-field-audit'[\s\S]*view:\s*'responsibility'[\s\S]*executionId:\s*String\(entry\.executionId\)/,
  '字段责任入口必须跳转现有字段审计责任汇总页，并携带真实 executionId 与 view=responsibility。'
)
const responsibilityOpenMatch = detail.match(
  /const openFieldResponsibility = async \(entry: TraceRecordFieldResponsibilityEntry\) => \{[\s\S]*?\n\}/
)
assert.ok(responsibilityOpenMatch, '必须能定位终态字段责任跳转函数。')
assert.doesNotMatch(
  responsibilityOpenMatch[0],
  /saveEdhrFieldChanges|submitEdhrRelease|qualityRejectEdhrBatchExecution|syncEdhrBatchExecutionStatus/,
  '终态字段责任查看入口只能跳转只读汇总，不得触发保存、放行、拒收或同步状态动作。'
)
assert.match(
  fieldAudit,
  /const resolveInitialView = \(\) => \{[\s\S]*props\.initialView === 'responsibility'[\s\S]*route\.query\.view === 'responsibility' \? 'responsibility' : 'audit'[\s\S]*const activeView = ref<'responsibility' \| 'audit'>\(resolveInitialView\(\)\)/,
  '字段审计页必须支持通过 view=responsibility 直接进入当前责任汇总。'
)

console.log('PASS: terminal batch field responsibility static contract')
