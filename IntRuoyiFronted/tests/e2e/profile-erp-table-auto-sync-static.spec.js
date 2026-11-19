const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8')
const readBackend = relative => read(`../IntRuoyiBackend/${relative}`)
const component = read('src/views/erp/sync/config/components/TableAutoSyncSetting.vue')
const { descriptor, errors } = parse(component)
assert.deepEqual(errors, [])
const script = descriptor.scriptSetup.content
const template = descriptor.template.content
const contains = (source, tokens) => {
  for (const token of tokens) assert.ok(source.includes(token), `missing contract: ${token}`)
}

test('ERP configuration entry retains test mode and explicit business permissions', () => {
  contains(read('src/views/erp/sync/index.vue'), ['TableAutoSyncSetting v-if="canConfigure"', "testMode.value && hasErpConfigCapability(['erp:kingdee-sync:query'])"])
  contains(read('src/utils/businessConfigAccess.ts'), ["hasExplicitConfigPermission('erp:data-config:operate')", 'permissions.every'])
  contains(read('src/views/erp/sync/config/index.vue'), ["import ErpConfiguration from '../index.vue'", "name: 'ErpTableAutoSyncConfig'"])
  assert.doesNotMatch(read('src/views/Profile/Index.vue'), /ProfileErpTableAutoSyncSetting|GOLDEN_FINGER_PERMISSION/)
  assert.doesNotMatch(read('src/views/Profile/components/index.ts'), /ProfileErpTableAutoSyncSetting/)
})

test('schedules use the ERP job API and its business permission', () => {
  contains(script, ["import * as JobApi from '@/api/erp/config/job'", 'JobApi.getJobPage', 'JobApi.updateJob', 'JobApi.updateJobStatus', 'InfraJobStatusEnum.NORMAL', 'InfraJobStatusEnum.STOP', 'toDailyCronExpression', 'parseDailyCronExpression'])
  contains(read('src/api/erp/config/job.ts'), ['/erp/kingdee-config/job/page', '/erp/kingdee-config/job/update', '/erp/kingdee-config/job/update-status'])
  contains(readBackend('yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/controller/admin/config/ErpKingdeeJobController.java'), ['@RequestMapping("/erp/kingdee-config/job")', "@ss.hasPermission('erp:data-config:operate')"])
  assert.doesNotMatch(script, /from '@\/api\/infra\/job'|JobApi\.(triggerJob|runJob)/)
})

test('connection changes require explicit save and expose validation errors', () => {
  contains(template, ['el-segmented', '当前连接', '保存连接', '!canSaveConnection || !connectionDirty', '@click="handleSaveConnection"', 'v-if="loadError"', 'v-if="latestRunsError"', 'v-if="runningRunsError"'])
  contains(script, ['ErpKingdeeConfigApi.getActiveConnection', 'ErpKingdeeConfigApi.updateActiveConnection', 'REQUIRED_CONNECTION_OPTIONS', 'applyActiveConnection', 'connectionDirty'])
  contains(read('src/api/erp/config/index.ts'), ['/erp/kingdee-config/active-connection', 'getActiveConnection', 'updateActiveConnection'])
  assert.match(component, /@media\s*\(max-width:\s*768px\)[\s\S]*profile-erp-table-sync__connection-setting/)
})

test('each ERP table retains an independent type, handler and destination', () => {
  const ast = ts.createSourceFile('sync.ts', script, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(statement => ts.isVariableStatement(statement) && statement.declarationList.declarations.some(item => item.name.getText(ast) === 'syncTypes'))
  assert.ok(declaration)
  const compiled = ts.transpileModule(declaration.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  const rows = Function(`${compiled}; return syncTypes`)()
  const expected = {
    PRODUCT: 'kingdeeProductItemSyncJob', STOCK: 'kingdeeStockSyncJob',
    STOCK_MOVE: 'kingdeeStockMoveSyncJob', PURCHASE_ORDER: 'kingdeePurchaseOrderSyncJob',
    SALE_ORDER: 'kingdeeSaleOrderSyncJob', PRODUCTION_ORDER: 'kingdeeProductionOrderSyncJob',
    PRODUCTION_PICK_LIST: 'kingdeeProductionPickListSyncJob',
    PRODUCTION_REPLENISHMENT_LIST: 'kingdeeProductionReplenishmentListSyncJob',
    PRODUCTION_MATERIAL_LIST: 'kingdeeProductionMaterialListSyncJob', BOM: 'kingdeeBomSyncJob'
  }
  assert.equal(rows.length, Object.keys(expected).length)
  assert.equal(new Set(rows.map(row => row.syncType)).size, rows.length)
  assert.equal(new Set(rows.map(row => row.handlerName)).size, rows.length)
  for (const row of rows) {
    assert.equal(row.handlerName, expected[row.syncType])
    assert.ok(row.erpTableName.trim())
    assert.ok(row.localTabName.trim())
  }
  for (const [type, local] of [['PRODUCTION_PICK_LIST', '生产领料单'], ['PRODUCTION_REPLENISHMENT_LIST', '生产补料单'], ['PRODUCTION_MATERIAL_LIST', '生产用料清单']]) {
    assert.equal(rows.find(row => row.syncType === type).localTabName, local)
  }
})

test('row actions use formal sync APIs and refresh real run results', () => {
  contains(template, ['ref="syncTableRef"', '@selection-change="handleSyncTableSelectionChange"', 'type="selection"', "handleRunSync(row, 'FULL')", "handleRunSync(row, 'INCREMENTAL')", '!canRunSync || Boolean(syncingType)', 'resolveLatestRunTime(row.latestRun)', 'resolveFailureReason(row.latestRun)'])
  contains(script, ['ErpKingdeeSyncApi.runIncrementalSync(row.syncType)', 'ErpKingdeeSyncApi.runFullSync(row.syncType)', 'waitForSubmittedRun', 'loadLatestRuns(), loadRunningSyncRuns()', 'resolveRunStartedTimestamp', 'toDateTimeValue(value)?.getTime()', 'formatDateTimeValue(latestRun.endedAt || latestRun.startedAt', "return '成功'", "return '失败'", "return '运行中'"])
  const api = read('src/api/erp/sync/index.ts')
  contains(api, ['/erp/kingdee-sync/run/page', '/erp/kingdee-sync/incremental-sync', '/erp/kingdee-sync/full-sync'])
  assert.doesNotMatch(api, /JobApi\.(getJobPage|runJob)|runIncrementalSyncJob/)
  assert.doesNotMatch(script, /ErpKingdeeSyncWatermarkVO|loadWatermarks|watermarkBySyncType|latestRun\.startedAt\.replace/)
})

test('running filtering, visible polling and full width layout retain page contracts', () => {
  contains(template, ['table-key="erp.sync.configuration"', 'table-key="erp.sync.running"', ':rows="runningSyncRuns"', 'row-key="id"', 'resolveSyncTypeName(row.syncType)', 'formatDateTimeValue(row.startedAt'])
  contains(script, ['status: RUNNING_SYNC_STATUS', 'pageSize: RUNNING_SYNC_RUN_PAGE_SIZE', 'useDocumentVisibility()', 'useTimeoutPoll(', "documentVisibility.value === 'visible'", 'onActivated(', 'onDeactivated(', 'onBeforeUnmount(', 'pauseProgressPolling()'])
  contains(readBackend('yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/controller/admin/sync/vo/ErpKingdeeSyncRunPageReqVO.java'), ['private Integer status;'])
  assert.match(readBackend('yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/dal/mysql/sync/ErpKingdeeSyncRunMapper.java'), /\.eqIfPresent\(ErpKingdeeSyncRunDO::getStatus,\s*reqVO\.getStatus\(\)\)/)
  assert.match(component, /\.profile-erp-table-sync\s*\{[\s\S]*width:\s*100%;[\s\S]*max-width:\s*none;/)
  assert.doesNotMatch(component, /<el-checkbox-group|NasTableSync|nas-table-sync|testNasWrite|NAS 目录|文件名规则|mock|placeholder/i)
})
