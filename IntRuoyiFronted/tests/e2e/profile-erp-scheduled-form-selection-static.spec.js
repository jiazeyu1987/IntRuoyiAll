const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const componentPath = path.resolve(
  __dirname,
  '../../src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue'
)
const source = fs.readFileSync(componentPath, 'utf8')
const loadJobsStart = source.indexOf('const loadJobs = async')
const loadJobsEnd = source.indexOf('const loadLatestRuns', loadJobsStart)
assert.notEqual(loadJobsStart, -1, '必须存在正式 Job 配置加载函数')
assert.notEqual(loadJobsEnd, -1, '无法定位 Job 配置加载函数边界')
const loadJobs = source.slice(loadJobsStart, loadJobsEnd)
assert.match(loadJobs, /await syncSelectedRows\(\)/, '加载 Job 状态后必须同步表格勾选回显')
const selectionHandlerStart = source.indexOf('const handleSyncTableSelectionChange =')
const selectionHandlerEnd = source.indexOf('const fetchJobByHandlerName', selectionHandlerStart)
assert.notEqual(selectionHandlerStart, -1, '必须存在 ERP 表格选择变更处理函数')
assert.notEqual(selectionHandlerEnd, -1, '无法定位 ERP 表格选择变更处理函数边界')
const selectionHandler = source.slice(selectionHandlerStart, selectionHandlerEnd)
assert.match(
  selectionHandler,
  /loading\.value/,
  '加载或保存阶段的程序化 selection-change 不得覆盖已加载的 Job 选择状态'
)
assert.match(selectionHandler, /jobLoading\.value/, '加载阶段必须忽略程序化 selection-change')
assert.match(selectionHandler, /saving\.value/, '保存阶段必须忽略程序化 selection-change')
const helperStart = source.indexOf('const updateJobForAutoSync = async')
assert.notEqual(helperStart, -1, '保存定时同步配置必须抽出 Job 状态与 cron 的更新顺序')
const helperEnd = source.indexOf('\n\nconst handleSave = async', helperStart)
assert.notEqual(helperEnd, -1, '无法定位 ERP 定时同步保存处理函数')
const helper = source.slice(helperStart, helperEnd)

const enableBranchStart = helper.indexOf('if (targetStatus === InfraJobStatusEnum.NORMAL)')
const enableStatusIndex = helper.indexOf('await JobApi.updateJobStatus(job.id, targetStatus)', enableBranchStart)
const enableUpdateIndex = helper.indexOf('await JobApi.updateJob({', enableBranchStart)
const stopBranchStart = helper.indexOf('if (job.status === InfraJobStatusEnum.NORMAL)')
const stopUpdateIndex = helper.indexOf('await JobApi.updateJob({', stopBranchStart)
const stopStatusIndex = helper.indexOf('await JobApi.updateJobStatus(job.id, targetStatus)', stopUpdateIndex)

assert.notEqual(enableBranchStart, -1, '选中的停止 Job 必须先启用')
assert.notEqual(enableStatusIndex, -1, '选中的停止 Job 必须先启用')
assert.notEqual(enableUpdateIndex, -1, '选中的 ERP Job 必须更新每日 cron')
assert.notEqual(stopBranchStart, -1, '取消选择的 Job 必须停止')
assert.notEqual(stopUpdateIndex, -1, '取消选择的 Job 必须更新每日 cron')
assert.notEqual(stopStatusIndex, -1, '取消选择的 Job 必须停止')
assert.ok(
  enableStatusIndex < enableUpdateIndex,
  '选中的停止 Job 必须先启用，再更新 cron'
)
assert.ok(
  stopUpdateIndex < stopStatusIndex,
  '取消选择的 Job 必须先更新 cron，再停止'
)

console.log('PASS: ERP scheduled form selection static contract')
