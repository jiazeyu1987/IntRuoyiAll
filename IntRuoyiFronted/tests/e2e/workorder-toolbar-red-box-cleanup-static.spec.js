const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const workOrderPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/workorder/index.vue')
const source = fs.readFileSync(workOrderPagePath, 'utf8')
const formStart = source.indexOf('<UnifiedListTemplate')
const formEnd = source.indexOf('</UnifiedListTemplate>', formStart)

assert.notEqual(formStart, -1, '生产工单页面必须接入标准列表模板工具栏。')
assert.notEqual(formEnd, -1, '生产工单标准列表模板必须正常闭合。')

const formSource = source.slice(formStart, formEnd)

assert.equal(formSource.includes('TreeExpandActions'), false, '生产工单工具栏必须删除全部展开/全部折叠入口。')
assert.equal(formSource.includes('全部展开'), false, '生产工单工具栏不能显示全部展开。')
assert.equal(formSource.includes('全部折叠'), false, '生产工单工具栏不能显示全部折叠。')
assert.equal(formSource.includes('重置列'), false, '生产工单工具栏不能显示重置列。')

assert(
  /<UnifiedListTemplate[\s\S]*?:show-column-reset="false"/.test(formSource),
  '生产工单页必须通过标准列表模板隐藏显示字段组件的重置列按钮。'
)

for (const keptControl of ['handleExport', 'handleSyncKingdeeWorkOrders', ':columns="workOrderColumns"', '@column-change="saveWorkOrderColumnConfig"']) {
  assert(formSource.includes(keptControl), `生产工单工具栏必须保留 ${keptControl}。`)
}

assert.equal(
  formSource.includes('@save="saveWorkOrderColumnConfig"'),
  false,
  '生产工单显示字段不得继续显示或绑定手动保存按钮。'
)

assert(
  source.includes('const { isExpandAll, refreshTable } = useTreeTableExpand(true)'),
  '生产工单表格仍需保留默认展开状态与刷新控制。'
)

assert.equal(
  source.includes("const workOrderTableMaxHeight = 'calc(100vh - 360px)'"),
  false,
  '生产工单删除顶部同步卡片后，表格高度不能继续使用过大的 360px 视口扣减。'
)

assert(
  source.includes("const workOrderTableMaxHeight = 'calc(100vh - 240px)'"),
  '生产工单表格必须扩大高度，占满分页下方原黄色空白区域。'
)

assert.equal(
  source.includes("import TreeExpandActions from '@/components/TreeExpandActions/index.vue'"),
  false,
  '生产工单页删除展开/折叠入口后不得继续导入 TreeExpandActions。'
)

assert.equal(
  source.includes('resetConfig: resetWorkOrderColumnConfig'),
  false,
  '生产工单页隐藏重置列后不得继续绑定列重置处理。'
)

console.log('PASS: work order toolbar red-box cleanup static contract')
