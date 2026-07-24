const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const formPath = path.resolve(process.cwd(), 'src/views/mes/pro/workorder/WorkOrderForm.vue')
const listPath = path.resolve(process.cwd(), 'src/views/mes/pro/workorder/index.vue')
const selectDialogPath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/workorder/components/ProWorkOrderSelectDialog.vue'
)
const taskListPath = path.resolve(process.cwd(), 'src/views/mes/pro/task/index.vue')
const taskDetailPath = path.resolve(process.cwd(), 'src/views/mes/pro/task/WorkOrderForm2.vue')

const formSource = fs.readFileSync(formPath, 'utf8')
const listSource = fs.readFileSync(listPath, 'utf8')
const selectDialogSource = fs.readFileSync(selectDialogPath, 'utf8')
const taskListSource = fs.readFileSync(taskListPath, 'utf8')
const taskDetailSource = fs.readFileSync(taskDetailPath, 'utf8')

assert(
  !formSource.includes('AutoCodeRecordApi'),
  'Production work order form must not call auto-code generation for ERP work order code.'
)

assert(
  !formSource.includes('MesAutoCodeRuleCode.PRO_WORK_ORDER_CODE'),
  'Production work order form must not use PRO_WORK_ORDER_CODE auto-code rule.'
)

assert(
  !formSource.includes('@click="generateCode"'),
  'Production work order code input must not render a generate button.'
)

assert(
  !/label="来源单据编号"[\s\S]*v-model="formData\.orderSourceCode"/.test(formSource),
  'Production work order form must not expose orderSourceCode as a second visible code field.'
)

assert(
  !listSource.includes('getDisplayCode'),
  'Production work order list must not use orderSourceCode || code display logic.'
)

assert(
  listSource.includes('{{ scope.row.code }}'),
  'Production work order list must render row.code directly in the 工单编码 column.'
)

assert(
  !listSource.includes("@click=\"openForm('create')\""),
  'Production work order list must not render a local create entry; work orders are Kingdee-synced only.'
)

assert(
  !listSource.includes("openForm('update'") &&
    !listSource.includes('handleDelete(') &&
    !listSource.includes('handleToggleTemporaryFrozen') &&
    !listSource.includes("openForm('finish'") &&
    !listSource.includes('handleCancel('),
  'Production work order list must not render local edit/delete/freeze/finish/cancel operations.'
)

assert(
  !listSource.includes('handleTemporaryFreezeChange') &&
    !listSource.includes('updateTemporaryFreeze') &&
    !listSource.includes('updateWorkOrderTemporaryFrozen'),
  'Production work order list must not expose temporary freeze operations.'
)

assert(
  listSource.includes('handleSyncKingdeeWorkOrders') &&
    listSource.includes("runIncrementalSyncJob('kingdeeProductionOrderSyncJob')"),
  'Production work order list must keep Kingdee sync import entry.'
)

assert(
  listSource.includes("import UserTableColumnSettings from '@/components/UserTableColumnSettings/index.vue'") &&
    listSource.includes('<UserTableColumnSettings') &&
    listSource.includes(':show-column-settings="false"') &&
    listSource.includes('class="work-order-column-settings"'),
  'Production work order list must render the explicit 显示字段 button beside the quick filters.'
)

assert(
  !listSource.includes('sync-status-bar') &&
    !listSource.includes('getKingdeeSyncStatus') &&
    !listSource.includes('syncStatusText') &&
    !listSource.includes('syncAutoText'),
  'Production work order list must not render the ERP sync status card.'
)

assert(
  !/label="来源单据"[\s\S]*v-model="queryParams\.orderSourceCode"/.test(listSource),
  'Production work order list must not expose orderSourceCode search as a second code filter.'
)

assert(
  !selectDialogSource.includes('prop="orderSourceCode"'),
  'Production work order selector must not expose orderSourceCode as an order number column.'
)

assert(
  !/label="来源单据"[\s\S]*v-model="queryParams\.orderSourceCode"/.test(taskListSource),
  'Production scheduling page must not expose orderSourceCode search as a second work order code filter.'
)

assert(
  !taskListSource.includes('prop="orderSourceCode"'),
  'Production scheduling page must not expose orderSourceCode as a visible work order column.'
)

assert(
  !/label="来源单据编号"[\s\S]*v-model="formData\.orderSourceCode"/.test(taskDetailSource),
  'Production scheduling work order detail must not expose orderSourceCode as a second visible code field.'
)

console.log('PASS: production work order ERP code static contract')
