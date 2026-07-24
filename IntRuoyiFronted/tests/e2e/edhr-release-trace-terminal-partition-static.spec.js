const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(workspaceRoot, 'yudao-ui-admin-vue3')
const backendRoot = path.join(workspaceRoot, 'ruoyi-vue-pro')

const batchListPage = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'),
  'utf8'
)
const releaseTraceTab = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/edhr/form-trace/FormTraceReleaseTab.vue'),
  'utf8'
)
const changeTraceTab = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/edhr/form-trace/FormTraceChangeTab.vue'),
  'utf8'
)
const releaseApi = fs.readFileSync(
  path.join(frontendRoot, 'src/api/mes/pro/edhr/release.ts'),
  'utf8'
)
const batchExecutionApi = fs.readFileSync(
  path.join(frontendRoot, 'src/api/mes/pro/edhr/batchExecution.ts'),
  'utf8'
)
const releasePageReqVo = fs.readFileSync(
  path.join(
    backendRoot,
    'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProEdhrReleasePageReqVO.java'
  ),
  'utf8'
)
const batchPageReqVo = fs.readFileSync(
  path.join(
    backendRoot,
    'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/EdhrBatchExecutionPageReqVO.java'
  ),
  'utf8'
)
const batchMapper = fs.readFileSync(
  path.join(
    backendRoot,
    'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrBatchExecutionMapper.java'
  ),
  'utf8'
)
const releaseService = fs.readFileSync(
  path.join(
    backendRoot,
    'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java'
  ),
  'utf8'
)

assert.match(
  batchExecutionApi,
  /excludeStatuses\?:\s*number\[\]/,
  '批次执行分页 API 必须支持后端排除终态批次状态，不能只在前端过滤。'
)
assert.match(
  batchExecutionApi,
  /excludeReleased\?:\s*boolean/,
  '批次执行分页 API 必须支持后端排除已放行事务。'
)
assert.match(
  batchExecutionApi,
  /completedTraceOnly\?:\s*boolean/,
  '批次执行分页 API 必须支持表单追溯已结束口径的后端分页查询。'
)
assert.match(
  releaseApi,
  /batchExecutionStatuses\?:\s*number\[\]/,
  '放行追溯分页 API 必须支持按批次终态集合查询。'
)
assert.match(
  releaseApi,
  /excludeBatchExecutionStatuses\?:\s*number\[\]/,
  '放行追溯分页 API 必须支持排除质量终态，确保放行 tab 不显示质量终态。'
)
assert.match(
  releaseApi,
  /completedTraceOnly\?:\s*boolean/,
  '放行追溯分页 API 必须支持已结束批记录口径，包含已放行未归档和批次终态。'
)

assert.match(
  batchListPage,
  /const EDHR_BATCH_EXECUTION_TRACE_ONLY_STATUSES = \[\s*EDHR_BATCH_STATUS_ARCHIVED,\s*EDHR_BATCH_STATUS_REJECTED\s*\] as const/,
  '批次执行页必须集中声明只归入追溯放行 tab 的批次终态。'
)
assert.match(
  batchListPage,
  /excludeStatuses:\s*\[\.\.\.EDHR_BATCH_EXECUTION_TRACE_ONLY_STATUSES\]/,
  '批次执行页查询必须把已归档和质量终态交给后端排除，保证分页 total 正确。'
)
assert.match(
  batchListPage,
  /excludeReleased:\s*true/,
  '批次执行页查询必须明确排除已放行事务。'
)

const statusFilterStart = batchListPage.indexOf("key: 'status'")
const statusFilterEnd = batchListPage.indexOf("{ key: 'createTime'", statusFilterStart)
assert.ok(statusFilterStart > 0 && statusFilterEnd > statusFilterStart, '必须能定位批次执行状态筛选配置。')
const statusFilterBlock = batchListPage.slice(statusFilterStart, statusFilterEnd)
assert.doesNotMatch(statusFilterBlock, /已归档|质量终态/, '批次执行状态筛选不得继续提供已归档或质量终态入口。')

assert.match(
  batchPageReqVo,
  /private List<Integer> excludeStatuses;/,
  '后端批次执行分页请求必须接收排除状态列表。'
)
assert.match(
  batchPageReqVo,
  /private Boolean excludeReleased;/,
  '后端批次执行分页请求必须接收排除已放行事务开关。'
)
assert.match(
  batchPageReqVo,
  /private Boolean completedTraceOnly;/,
  '后端批次执行分页请求必须接收表单追溯已结束口径开关。'
)
assert.match(
  releasePageReqVo,
  /private List<Integer> batchExecutionStatuses;/,
  '后端放行分页请求必须接收表单追溯放行 tab 的批次终态集合。'
)
assert.match(
  releasePageReqVo,
  /private List<Integer> excludeBatchExecutionStatuses;/,
  '后端放行分页请求必须接收排除批次状态集合。'
)
assert.match(
  releasePageReqVo,
  /private Boolean completedTraceOnly;/,
  '后端放行分页请求必须接收表单追溯已结束口径开关。'
)
assert.match(
  batchPageReqVo,
  /private List<Integer> statuses;/,
  '后端批次执行分页请求必须支持包含状态集合，供追溯放行 tab 复用分页查询。'
)
assert.match(
  batchMapper,
  /reqVO\.getExcludeStatuses\(\)\s*!=\s*null[\s\S]*!reqVO\.getExcludeStatuses\(\)\.isEmpty\(\)[\s\S]*queryWrapper\.notIn\(MesProEdhrBatchExecutionDO::getStatus,\s*reqVO\.getExcludeStatuses\(\)\)/,
  '后端 mapper 必须在 SQL 查询层排除已归档和质量终态批次状态。'
)
assert.match(
  batchMapper,
  /Boolean\.TRUE\.equals\(reqVO\.getExcludeReleased\(\)\)[\s\S]*queryWrapper\.notExists\("SELECT 1 FROM mes_pro_edhr_release_transaction rt[\s\S]*rt\.release_status = 'RELEASED'/,
  '后端 mapper 必须在 SQL 查询层排除已放行事务，不能依赖页面过滤。'
)
assert.match(
  batchMapper,
  /Boolean\.TRUE\.equals\(reqVO\.getCompletedTraceOnly\(\)\)[\s\S]*queryWrapper\.and\([\s\S]*MesProEdhrBatchExecutionDO::getStatus[\s\S]*BATCH_STATUS_ARCHIVED[\s\S]*BATCH_STATUS_REJECTED[\s\S]*\.or\(\)[\s\S]*\.exists\([\s\S]*rt\.release_status = 'RELEASED'/,
  '后端 mapper 必须在 SQL 查询层用“已归档/质量终态 OR 已放行事务”定义表单追溯已结束口径，保证分页 total 正确。'
)
assert.match(
  batchMapper,
  /reqVO\.getStatuses\(\)\s*!=\s*null[\s\S]*!reqVO\.getStatuses\(\)\.isEmpty\(\)[\s\S]*queryWrapper\.in\(MesProEdhrBatchExecutionDO::getStatus,\s*reqVO\.getStatuses\(\)\)/,
  '后端 mapper 必须在 SQL 查询层支持只包含已归档和质量终态批次状态。'
)

const releaseBatchReqStart = releaseService.indexOf('private EdhrBatchExecutionPageReqVO toBatchPageReq')
const releaseBatchReqEnd = releaseService.indexOf('private boolean statusMatches', releaseBatchReqStart)
assert.ok(releaseBatchReqStart > 0 && releaseBatchReqEnd > releaseBatchReqStart, '必须能定位放行追溯转换批次查询的方法。')
const releaseBatchReqBlock = releaseService.slice(releaseBatchReqStart, releaseBatchReqEnd)
assert.match(
  releaseBatchReqBlock,
  /setCompletedTraceOnly\(reqVO\.getCompletedTraceOnly\(\)\)/,
  '表单追溯放行 tab 必须把已结束口径开关下推到后端分页查询。'
)
assert.match(
  releaseBatchReqBlock,
  /setExcludeStatuses\(reqVO\.getExcludeBatchExecutionStatuses\(\)\)/,
  '表单追溯放行 tab 必须把质量终态排除集合下推到后端分页查询。'
)
assert.match(
  releaseService,
  /batchExecutionStatusMatches\(reqVO,\s*item\)/,
  '放行事务筛选分支也必须继续校验已结束口径，不能混入仍在执行数据。'
)
assert.match(
  releaseService,
  /excludedStatuses\s*!=\s*null[\s\S]*excludedStatuses\.contains\(item\.getBatchExecutionStatus\(\)\)[\s\S]*return false/,
  '放行事务筛选分支必须先排除质量终态，不能只依赖批次分页 SQL。'
)

assert.match(
  releaseTraceTab,
  /resolveBatchExecutionTraceStatusLabel\(row\.batchExecutionStatus\)/,
  '放行追溯列表必须展示批次状态，确保质量终态记录在放行 tab 中可识别。'
)
assert.match(
  releaseTraceTab,
  /EDHR_BATCH_STATUS_REJECTED[\s\S]*'质量终态'/,
  '放行追溯列表必须把 REJECTED=50 显示为质量终态。'
)
assert.match(
  releaseTraceTab,
  /completedTraceOnly:\s*true/,
  '放行 tab 查询必须固定下推已结束批记录口径。'
)
assert.match(
  releaseTraceTab,
  /const EDHR_REJECT_TRACE_BATCH_STATUSES = \[\s*EDHR_BATCH_STATUS_REJECTED\s*\] as const/,
  '驳回 tab 必须集中声明只显示质量终态批次状态。'
)
assert.match(
  releaseTraceTab,
  /const EDHR_RELEASE_TRACE_EXCLUDED_BATCH_STATUSES = \[\s*EDHR_BATCH_STATUS_REJECTED\s*\] as const/,
  '放行 tab 必须集中声明排除质量终态批次状态。'
)
assert.match(
  releaseTraceTab,
  /batchExecutionStatuses:\s*isRejectTrace\s*\? \[\.\.\.EDHR_REJECT_TRACE_BATCH_STATUSES\] : undefined/,
  '驳回 tab 查询必须固定下推质量终态状态集合。'
)
assert.match(
  releaseTraceTab,
  /excludeBatchExecutionStatuses:\s*isRejectTrace\s*\? undefined : \[\.\.\.EDHR_RELEASE_TRACE_EXCLUDED_BATCH_STATUSES\]/,
  '放行 tab 查询必须固定下推质量终态排除集合。'
)
assert.doesNotMatch(
  releaseTraceTab,
  /releaseStatusOptions|queryParamKey:\s*'releaseStatus'/,
  '放行 tab 不得继续暴露待预检、待审批、撤回等非终态放行状态筛选入口。'
)

assert.match(
  changeTraceTab,
  /const EDHR_FORM_TRACE_CHANGE_TYPE = EDHR_CHANGE_TYPE_VOID/,
  '变更 tab 必须集中声明只承载作废变更。'
)
assert.match(
  changeTraceTab,
  /changeType:\s*EDHR_FORM_TRACE_CHANGE_TYPE/,
  '变更 tab 查询必须固定下推作废类型。'
)
const changeTypeOptionsStart = changeTraceTab.indexOf('const changeTypeOptions = [')
const changeTypeOptionsEnd = changeTraceTab.indexOf('const changeStatusOptions', changeTypeOptionsStart)
assert.ok(changeTypeOptionsStart > 0 && changeTypeOptionsEnd > changeTypeOptionsStart, '必须能定位变更类型筛选配置。')
const changeTypeOptionsBlock = changeTraceTab.slice(changeTypeOptionsStart, changeTypeOptionsEnd)
assert.match(changeTypeOptionsBlock, /作废/, '变更 tab 类型筛选必须保留作废入口。')
assert.doesNotMatch(changeTypeOptionsBlock, /重开|补录/, '变更 tab 不得继续提供重开或补录入口。')

console.log('PASS: eDHR form trace tab responsibility partition static contract')
