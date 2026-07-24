const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(path.join(root, 'src/views/mes/pro/workorder/index.vue'), 'utf8')

assert.match(source, /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index.vue'/)
assert.match(source, /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.workorder\.main"/)
assert.match(source, /:quick-filter-state="workOrderQuickFilter\.state"/)
assert.match(source, /@quick-filter-query="workOrderQuickFilter\.applyQuickFilter"/)
assert.match(source, /:columns="workOrderColumns"/)
assert.match(source, /@column-change="saveWorkOrderColumnConfig"/)
assert.match(source, /#actions[\s\S]*导出[\s\S]*增量同步[\s\S]*重置/)
assert.match(source, /#table[\s\S]*<el-table[\s\S]*data-user-table-key="mes\.pro\.workorder\.main"[\s\S]*@header-dragend="handleWorkOrderHeaderDragend"/)
assert.match(source, /v-model:page="queryParams\.pageNo"/)
assert.match(source, /v-model:limit="queryParams\.pageSize"/)
assert.match(source, /@pagination="getList"/)

for (const field of [
  'code',
  'productCode',
  'productName',
  'productSpecification',
  'quantity',
  'batchCode',
  'workshopName',
  'plannedStartTime',
  'plannedEndTime',
  'businessStatus',
  'drawingNumber',
  'auxiliaryCode',
  'scheduleStatus',
  'quantityProduced',
  'status',
  'clientName',
  'productionMaterialList',
  'createTime'
]) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`))
  assert.match(source, new RegExp(`isWorkOrderColumnVisible\\('${field}'\\)`))
}

for (const filterField of ['code', 'productCode', 'productName', 'productSpecification', 'requestDate']) {
  assert.match(source, new RegExp(`key:\\s*'${filterField}'`))
}

assert.doesNotMatch(source, /import TableQuickFilter from '@\/components\/TableQuickFilter\/index.vue'/)
assert.doesNotMatch(source, /import UserTableColumnSettings from '@\/components\/UserTableColumnSettings\/index.vue'/)
assert.doesNotMatch(source, /localStorage|sessionStorage/)

console.log('PASS: mes pro workorder unified list template static contract')
