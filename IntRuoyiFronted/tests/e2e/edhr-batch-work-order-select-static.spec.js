const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

assert(
  source.includes(':remote-method="searchSelectableWorkOrders"'),
  'Open/create dialog must use a remote searchable work order selector.'
)

assert(
  source.includes('ProWorkOrderApi.getWorkOrderPage'),
  'Work order selector must use the real production work order page API.'
)

assert(
  !source.includes('status: MesProWorkOrderStatusEnum.CONFIRMED'),
  'Work order selector must not force confirmed-only filtering; valid means unfrozen and not canceled.'
)

assert(
  source.includes('temporaryFrozen: false'),
  'Work order selector must exclude temporarily frozen work orders.'
)

assert(
  source.includes('buildSelectableWorkOrderQueries'),
  'Work order selector must build separate work order number and product name queries.'
)

assert(
  source.includes('productNameKeyword: normalizedKeyword'),
  'Work order selector must send the dropdown keyword to productNameKeyword so product names are searchable.'
)

assert(
  source.includes('code: normalizedKeyword'),
  'Work order selector must keep sending the dropdown keyword to code so work order numbers remain searchable.'
)

assert(
  source.includes('Promise.all(') && source.includes('buildSelectableWorkOrderQueries(keyword).map'),
  'Work order selector must query work order number and product name separately so either field can match.'
)

assert(
  source.includes('dedupeSelectableWorkOrders'),
  'Work order selector must merge separate query results without duplicate work orders.'
)

assert(
  source.includes('输入工单号或产品名称搜索并选择未冻结工单'),
  'Open/create dialog placeholder must tell users they can search by work order number or product name.'
)

assert(
  source.includes('@change="handleWorkOrderChange"'),
  'Open/create dialog must react when a work order is selected.'
)

assert(
  /const\s+handleWorkOrderChange\s*=\s*(?:async\s*)?\(workOrderId\?: number\)/.test(source),
  'Work order selector must define a change handler for selected work order values.'
)

assert(
  source.includes('selectedWorkOrder?.batchCode') && source.includes('createForm.batchCode = selectedWorkOrder.batchCode'),
  'Work order selector must fill the batch number when the selected work order has a batchCode.'
)

assert(
  source.includes('createForm.workOrderId == null'),
  'Submit guard must require selecting a valid work order from the dropdown.'
)

console.log('PASS: eDHR batch work order selector static contract')
