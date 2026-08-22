const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(root, '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readWorkspace = (relativePath) =>
  fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const profileIndex = read('src/views/Profile/Index.vue')
const componentIndex = read('src/views/Profile/components/index.ts')
const component = read('src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue')
const syncApi = read('src/api/erp/sync/index.ts')
const configApi = read('src/api/erp/config/index.ts')
const jobApi = read('src/api/infra/job/index.ts')
const workOrderPage = read('src/views/mes/pro/workorder/index.vue')
const forbiddenInternalCopyPattern = new RegExp(
  ['\\u6570\\u636e\\u6c34\\u4f4d', '\\u540c\\u6b65\\u6c34\\u4f4d'].join('|')
)
const runPageReqVo = readWorkspace(
  'IntRuoyiBackend/yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/controller/admin/sync/vo/ErpKingdeeSyncRunPageReqVO.java'
)
const runMapper = readWorkspace(
  'IntRuoyiBackend/yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/mysql/sync/ErpKingdeeSyncRunMapper.java'
)

assert.match(
  profileIndex,
  /const GOLDEN_FINGER_PERMISSION = 'mes:pro-batch-record-execution:golden-finger'/,
  '个人工作台配置页签必须继续复用 golden-finger 权限边界。'
)
assert.match(
  profileIndex,
  /<el-tabs[\s\S]*ERP表格自动同步/,
  '配置页签内部必须新增 ERP 表格自动同步 tab。'
)
assert.match(
  profileIndex,
  /<ProfileErpTableAutoSyncSetting\s*\/>/,
  '配置页签必须渲染 ERP 表格自动同步组件。'
)
assert.match(
  componentIndex,
  /ProfileErpTableAutoSyncSetting/,
  'Profile 组件导出必须包含 ERP 表格自动同步组件。'
)

for (const token of [
  '/erp/kingdee-sync/run/page',
  '/erp/kingdee-sync/watermark/list',
  '/erp/kingdee-sync/full-sync',
  'runIncrementalSyncJob',
  'runFullSync',
  'JobApi.getJobPage',
  'JobApi.runJob'
]) {
  assert.match(
    syncApi,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `ERP 同步 API 必须包含正式链路：${token}`
  )
}

for (const token of [
  'ErpKingdeeConnectionType',
  'ErpKingdeeActiveConnectionVO',
  '/erp/kingdee-config/active-connection',
  'getActiveConnection',
  'updateActiveConnection'
]) {
  assert.match(
    configApi,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `ERP 配置 API 必须包含账套切换契约：${token}`
  )
}

for (const token of [
  'import { ErpKingdeeConfigApi',
  'el-segmented',
  '当前连接',
  '测试账套',
  '正式账套',
  '待保存',
  '保存连接',
  'connectionDirty',
  'loadActiveConnection',
  'handleSaveConnection',
  'ErpKingdeeConfigApi.getActiveConnection',
  'ErpKingdeeConfigApi.updateActiveConnection'
]) {
  assert.match(
    component,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `ERP 自动同步页必须包含显式保存的账套切换能力：${token}`
  )
}

assert.match(
  component,
  /@media\s*\(max-width:\s*768px\)[\s\S]*profile-erp-table-sync__connection-setting/,
  'ERP 账套切换区必须在窄屏下提供响应式布局。'
)

for (const token of [
  '/infra/job/page',
  '/infra/job/update',
  '/infra/job/update-status',
  '/infra/job/trigger'
]) {
  assert.match(
    jobApi,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `Job API 必须包含正式调度接口：${token}`
  )
}

assert.match(
  workOrderPage,
  /ErpKingdeeSyncApi\.runIncrementalSyncJob\('kingdeeProductionOrderSyncJob'\)/,
  '生产工单页面必须继续使用正式 Job 增量同步链路作为 Profile 配置参照。'
)

for (const token of [
  'import { ErpKingdeeSyncApi',
  "import * as JobApi from '@/api/infra/job'",
  "import { InfraJobStatusEnum } from '@/utils/constants'",
  'JobApi.getJobPage',
  'JobApi.updateJob',
  'JobApi.updateJobStatus',
  'InfraJobStatusEnum.NORMAL',
  'InfraJobStatusEnum.STOP',
  'ErpKingdeeSyncApi.runIncrementalSyncJob',
  'toDailyCronExpression',
  'parseDailyCronExpression'
]) {
  assert.match(
    component,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `组件必须复用正式 Job 同步链路：${token}`
  )
}

for (const token of [
  'kingdeeProductItemSyncJob',
  'kingdeeStockSyncJob',
  'kingdeePurchaseOrderSyncJob',
  'kingdeeSaleOrderSyncJob',
  'kingdeeProductionOrderSyncJob',
  'kingdeeProductionMaterialListSyncJob',
  'kingdeeBomSyncJob'
]) {
  assert.match(component, new RegExp(token), `组件必须管理 ERP 同步处理器：${token}`)
}

for (const token of [
  'ERP表格自动同步',
  '每日开始时间',
  'ERP 表格',
  'ERP表格名称',
  '本地页签名称',
  '最近执行时间',
  '新增行数',
  '同步成功/失败',
  '失败原因',
  '操作',
  '增量同步',
  '全量同步',
  '立即执行一次',
  'ElMessage.error',
  'ERP 商品',
  '生产工单',
  'BOM'
]) {
  assert.match(
    component,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `组件必须包含用户可见能力：${token}`
  )
}

for (const token of [
  'ref="syncTableRef"',
  '@selection-change="handleSyncTableSelectionChange"',
  'type="selection"',
  'syncTableRows',
  'latestRunBySyncType',
  'localTabName',
  'resolveLatestRunTime(row.latestRun',
  'resolveCreatedCount(row.latestRun',
  'formatLatestSyncStatus(row.latestRun',
  'resolveFailureReason(row.latestRun',
  'handleRunIncremental(row',
  'syncSelectedRows'
]) {
  assert.match(
    component,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `ERP 表格选择区必须用可选列表展示映射、最近执行时间和运行结果：${token}`
  )
}

for (const token of [
  'ErpKingdeeSyncRunVO',
  'ErpKingdeeSyncApi.getRunPage',
  'loadLatestRuns',
  'latestRuns',
  'syncType: item.syncType',
  'pageSize: 1',
  'status === 20',
  'status === 30',
  'status === 10',
  'endedAt',
  "return '成功'",
  "return '失败'",
  "return '运行中'",
  'formatDateTimeValue(latestRun.endedAt || latestRun.startedAt',
  'createdCount',
  "typeof latestRun.createdCount === 'number'",
  'failureMessage'
]) {
  assert.match(
    component,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `列表级同步状态列必须来自正式运行记录并中文展示：${token}`
  )
}

for (const token of [
  '正在进行的同步 Job',
  '暂无正在进行的同步 Job',
  'runningSyncRuns',
  'runningJobLoading',
  'loadRunningSyncRuns',
  'RUNNING_SYNC_STATUS',
  'RUNNING_SYNC_RUN_PAGE_SIZE',
  'status: RUNNING_SYNC_STATUS',
  'pageSize: RUNNING_SYNC_RUN_PAGE_SIZE',
  'row-key="id"',
  'resolveSyncTypeName(row.syncType)',
  'formatDateTimeValue(row.startedAt',
  'resolveRunCount(row.createdCount',
  'resolveRunCount(row.updatedCount',
  'resolveRunCount(row.failedCount'
]) {
  assert.match(
    component,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `正在进行的同步 Job 列表必须展示正式运行中记录：${token}`
  )
}

assert.match(
  runPageReqVo,
  /private Integer status;/,
  'ERP 同步运行分页请求必须支持 status 查询参数。'
)
assert.match(
  runMapper,
  /\.eqIfPresent\(ErpKingdeeSyncRunDO::getStatus,\s*reqVO\.getStatus\(\)\)/,
  'ERP 同步运行分页必须在后端正式按 status 过滤运行记录。'
)

for (const token of [
  'incrementalSyncingType',
  'fullSyncingType',
  'handleRunIncremental(row',
  'handleRunFull(row',
  'ErpKingdeeSyncApi.runIncrementalSyncJob(row.handlerName)',
  'ErpKingdeeSyncApi.runFullSync(row.syncType)',
  'row.erpTableName',
  'incrementalSyncingType === row.syncType',
  'fullSyncingType === row.syncType',
  '单表 ERP 增量同步任务',
  '单表 ERP 全量同步任务',
  '增量同步失败',
  '全量同步失败',
  'waitForSubmittedRun',
  'SUBMITTED_RUN_POLL_ATTEMPTS',
  '运行记录尚未生成，请检查调度任务状态',
  'loadLatestRuns(), loadRunningSyncRuns()'
]) {
  assert.match(
    component,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `每行操作按钮必须触发正式单表增量或全量同步并刷新结果：${token}`
  )
}

for (const token of [
  'ERP商品 / MES物料产品',
  'ERP库存',
  'ERP采购订单',
  'ERP销售订单',
  'MES生产工单',
  'ERP生产领料单列表',
  'ERP生产用料清单',
  'ERP产品BOM'
]) {
  assert.match(
    component,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `ERP 表格列表必须声明本地页签映射：${token}`
  )
}

assert.doesNotMatch(component, /<el-checkbox-group/, 'ERP 表格选择区不得继续使用横向复选框组。')
assert.doesNotMatch(
  component,
  /profile-erp-table-sync__checks/,
  'ERP 表格选择区不得继续保留复选框组样式。'
)
assert.match(
  component,
  /toDateTimeValue/,
  'ERP 同步轮询必须使用统一时间解析工具处理运行时日期值。'
)
assert.match(
  component,
  /resolveRunStartedTimestamp/,
  'ERP 同步轮询必须通过显式时间解析函数判断新运行记录。'
)
assert.doesNotMatch(
  component,
  /latestRun\.startedAt\.replace/,
  'ERP 同步轮询不得假设 startedAt 一定是字符串。'
)
assert.match(
  component,
  /\.profile-erp-table-sync\s*\{[\s\S]*width:\s*100%;[\s\S]*max-width:\s*none;/,
  'ERP 表格自动同步卡片必须占满配置页签可用宽度。'
)
assert.doesNotMatch(
  component,
  /\.profile-erp-table-sync\s*\{[\s\S]*max-width:\s*1080px;/,
  'ERP 表格自动同步卡片不得保留旧 1080px 黄框宽度。'
)
assert.match(
  component,
  /\.profile-erp-table-sync__select-table\s*\{[\s\S]*width:\s*100%;/,
  'ERP 表格列表必须跟随卡片宽度拉伸。'
)

for (const token of [
  '配置来源',
  '已选表格',
  '每日 Cron',
  '启用 Job',
  '最近状态',
  '最近开始时间',
  'Job 调度',
  'label="处理器"',
  'Job ID',
  'Job 状态',
  '当前 Cron',
  '最近执行记录',
  '运行编号',
  'profile-erp-table-sync__summary',
  'profile-erp-table-sync__section-title',
  'jobRows',
  'runs.value',
  'formatRunStatus',
  'formatTriggerType'
]) {
  assert.doesNotMatch(
    component,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `截图要求删除的展示区不得残留：${token}`
  )
}

assert.match(
  component,
  /resolveLatestRunTime\(row\.latestRun\)/,
  '主表最近执行时间必须来自正式运行记录。'
)
assert.doesNotMatch(
  component,
  /formatDateTimeValue\(row\.lastSuccessTime/,
  '主表不得再把内部增量位置显示成最近执行时间。'
)
assert.doesNotMatch(
  component,
  forbiddenInternalCopyPattern,
  '用户可见文案不得使用难理解的内部术语。'
)
assert.doesNotMatch(
  component,
  /ErpKingdeeSyncWatermarkVO|loadWatermarks|watermarkBySyncType/,
  '主表执行时间口径不得继续依赖内部增量位置状态。'
)
assert.doesNotMatch(
  component,
  /最近执行记录[\s\S]*formatDateTimeValue\(row\.startedAt/,
  '不得恢复最近执行记录历史区。'
)
assert.doesNotMatch(
  component,
  /prop="failureMessage"/,
  '列表级失败原因不得直接暴露英文内部字段名 failureMessage 作为表格 prop。'
)
assert.doesNotMatch(
  component,
  /kingdeeTableAutoSync|ErpKingdeeTableAutoSyncApi|kingdee-table-auto-sync/,
  'Profile ERP 表格自动同步不得再调用旧 kingdee-table-auto-sync 接口。'
)

for (const token of [
  "syncType: 'PRODUCTION_PICK_LIST'",
  "erpTableName: '生产领料单列表'",
  "localTabName: 'ERP生产领料单列表'",
  "handlerName: 'kingdeeProductionPickListSyncJob'"
]) {
  assert.match(
    component,
    new RegExp(token.replace(/[.*+?^$()|[\]\\]/g, '\\$&')),
    'ERP 表格自动同步列表必须新增独立生产领料单列表：' + token
  )
}

assert.match(
  component,
  /PRODUCTION_PICK_LIST[\s\S]*PRODUCTION_MATERIAL_LIST/,
  '生产领料单列表必须作为独立同步类型展示，且不能覆盖生产用料清单。'
)

assert.doesNotMatch(
  component,
  /NasTableSync|nas-table-sync|testNasWrite|NAS 目录|文件名规则|mock|placeholder/i,
  'ERP 表格自动同步组件不得混用 NAS 导出 API、NAS 字段或 mock 数据。'
)

console.log('PASS: profile ERP table auto sync static contract')
