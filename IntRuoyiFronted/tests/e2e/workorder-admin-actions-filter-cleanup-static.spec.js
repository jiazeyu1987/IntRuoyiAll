const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const workOrderPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/workorder/index.vue')
const source = fs.readFileSync(workOrderPagePath, 'utf8')

const formStart = source.indexOf('<el-form')
const formEnd = source.indexOf('</el-form>', formStart)
assert.notEqual(formStart, -1, 'Production work order page must include a query form.')
assert.notEqual(formEnd, -1, 'Production work order page must close the query form.')
const formSource = source.slice(formStart, formEnd)

const tableStart = source.indexOf('<el-table')
const tableEnd = source.indexOf('<Pagination', tableStart)
assert.notEqual(tableStart, -1, 'Production work order page must include the work order table.')
assert.notEqual(tableEnd, -1, 'Production work order page must include pagination after the table.')
const tableSource = source.slice(tableStart, tableEnd)

const removedFilters = [
  { label: '工单名称', prop: 'name' },
  { label: '产品', prop: 'productId' },
  { label: '客户', prop: 'clientId' },
  { label: '工单类型', prop: 'type' }
]

for (const filter of removedFilters) {
  assert(
    !formSource.includes(`<el-form-item label="${filter.label}" prop="${filter.prop}"`),
    `Query form must remove the ${filter.label} filter from the blue-box area.`
  )
}

for (const keptFilter of ['工单编号', '产品名称', '产品编码', '需求日期']) {
  assert(formSource.includes(`label="${keptFilter}"`), `Query form must keep ${keptFilter}.`)
}

assert(
  /<el-form[\s\S]*class="[^"]*\bwork-order-query-form\b[^"]*"/.test(formSource),
  'Query form must use the work-order-query-form layout class so actions can share the first filter row.'
)

assert(
  /<el-form-item[^>]*class="[^"]*\bwork-order-query-actions\b[^"]*"[\s\S]*?@click="handleQuery"[\s\S]*?@click="resetQuery"[\s\S]*?<\/el-form-item>/.test(
    formSource
  ),
  'Query and reset actions must be grouped in the right-side query action form item.'
)

assert(
  /\.work-order-query-form\s*\{[\s\S]*display:\s*flex[\s\S]*flex-wrap:\s*wrap/.test(source),
  'Query form layout must be flex-wrapped so filters and actions stay on the same toolbar row when space allows.'
)

assert(
  /\.work-order-query-actions\s*\{[\s\S]*margin-left:\s*auto/.test(source),
  'Query action group must use margin-left: auto to move into the right-side empty area of the first row.'
)

assert(
  !/<el-table[\s\S]*:height="workOrderTableHeight"/.test(tableSource),
  'Work order table must not use a fixed height that leaves a large blank area below ten rows.'
)

assert(
  /<el-table[\s\S]*:max-height="workOrderTableMaxHeight"/.test(tableSource),
  'Work order table must use max-height so ten-row pages shrink naturally while larger pages can still scroll.'
)

assert(
  source.includes("import { checkRole } from '@/utils/permission'") &&
    /const isAdminUser = computed\(\(\) => checkRole\(\['admin'\]\)\)/.test(source),
  'Production work order page must derive admin visibility from the existing role permission utility.'
)

const buttonBlockContaining = (needle) => {
  const index = formSource.indexOf(needle)
  assert.notEqual(index, -1, `Query form must still include ${needle} for admins.`)
  const start = formSource.lastIndexOf('<el-button', index)
  const nextButton = formSource.indexOf('<el-button', index + needle.length)
  const nextFormItem = formSource.indexOf('</el-form-item>', index)
  assert.notEqual(start, -1, `${needle} must be inside an el-button.`)
  const endCandidates = [nextButton, nextFormItem].filter((value) => value !== -1)
  assert(endCandidates.length > 0, `${needle} button block must have a boundary.`)
  return formSource.slice(start, Math.min(...endCandidates))
}

assert(
  buttonBlockContaining('@click="handleExport"').includes('v-if="isAdminUser"'),
  'Export button must be visible only to admins.'
)

assert(
  buttonBlockContaining('@click="handleSyncKingdeeWorkOrders"').includes('v-if="isAdminUser"'),
  'Incremental sync button must be visible only to admins.'
)

assert(
  /<TreeExpandActions[^>]*v-if="isAdminUser"/.test(formSource),
  'Expand/collapse controls must be visible only to admins.'
)

assert(
  /<el-table-column[\s\S]*?v-if="isAdminUser"[\s\S]*?label="操作"[\s\S]*?创建 ERP 测试单/.test(
    tableSource
  ),
  'Create ERP test order operation column must be visible only to admins.'
)

assert(!source.includes('MdItemSelect'), 'Removed product selector must not leave an unused MdItemSelect import.')
assert(!source.includes('MdClientSelect'), 'Removed client selector must not leave an unused MdClientSelect import.')
assert(!source.includes('getIntDictOptions'), 'Removed work order type selector must not leave unused dict option imports.')
assert(!/catch\s*\{\s*\}/.test(source), 'Production work order page must not silently swallow failures.')

console.log('PASS: work order admin actions and filter cleanup static contract')
