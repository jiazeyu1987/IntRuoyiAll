const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const workOrderPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/workorder/index.vue')
const source = fs.readFileSync(workOrderPagePath, 'utf8')
const formStart = source.indexOf('<el-form')
const formEnd = source.indexOf('</el-form>', formStart)

assert.notEqual(formStart, -1, '生产工单页面必须保留查询表单。')
assert.notEqual(formEnd, -1, '生产工单查询表单必须正常闭合。')

const formSource = source.slice(formStart, formEnd)
const quickFilterStart = formSource.indexOf('<TableQuickFilter')
const actionsStart = formSource.indexOf('class="work-order-query-actions"')
assert.notEqual(quickFilterStart, -1, '生产工单查询区必须保留快速过滤组件。')
assert.notEqual(actionsStart, -1, '生产工单查询区必须保留业务操作区。')

const removedRedBoxLabels = ['快速过滤', '工单编号', '产品名称', '产品编码', '需求日期']
for (const label of removedRedBoxLabels) {
  assert.equal(
    formSource.includes(`label="${label}"`),
    false,
    `生产工单查询区必须删除红框内的 ${label} 显式表单标签。`
  )
}

const removedPlaceholders = [
  'placeholder="请输入工单编号"',
  'placeholder="请输入产品名称"',
  'placeholder="请输入产品编码"',
  'start-placeholder="开始日期"',
  'end-placeholder="结束日期"'
]
for (const placeholder of removedPlaceholders) {
  assert.equal(
    formSource.includes(placeholder),
    false,
    `生产工单查询区必须删除红框内重复输入控件：${placeholder}。`
  )
}

assert.equal(
  /<el-button[^>]*(?:@pointerdown\.prevent|@click)="handleQuery"[\s\S]*?查询<\/el-button>/.test(formSource),
  false,
  '生产工单查询区必须删除红框内重复的查询按钮。'
)
assert.equal(
  /<el-button[^>]*@click="resetQuery"[\s\S]*?重置<\/el-button>/.test(formSource),
  false,
  '生产工单查询区必须删除红框内重复的重置按钮。'
)

const quickFilterDefinitions = source.slice(
  source.indexOf('const workOrderQuickFilterDefinitions'),
  source.indexOf('const workOrderQuickFilter = useTableQuickFilter')
)
for (const key of ['code', 'productName', 'productCode', 'requestDate']) {
  assert(
    quickFilterDefinitions.includes(`key: '${key}'`),
    `生产工单快速过滤必须继续覆盖 ${key}。`
  )
}

const keptBusinessControls = ['handleExport', 'handleSyncKingdeeWorkOrders', 'UserTableColumnSettings']
for (const control of keptBusinessControls) {
  assert(
    formSource.includes(control),
    `生产工单查询区必须保留业务控件 ${control}。`
  )
}

console.log('PASS: work order red-box filter cleanup static contract')
