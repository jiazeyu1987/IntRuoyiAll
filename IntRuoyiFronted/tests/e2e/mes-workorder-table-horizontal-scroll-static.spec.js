const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const cases = [
  {
    name: '排产工单',
    path: 'src/views/mes/pro/scheduleorder/index.vue',
    tableMarker: ':data="scheduleOrderList"',
    heightMarker: ':height="scheduleOrderTableHeight"'
  },
  {
    name: '生产工单',
    path: 'src/views/mes/pro/workorder/index.vue',
    tableMarker: ':data="list"',
    heightMarker: ':height="workOrderTableHeight"'
  }
]

for (const item of cases) {
  const pagePath = path.resolve(process.cwd(), item.path)
  assert(fs.existsSync(pagePath), `${item.name}页面必须存在：${pagePath}`)

  const source = fs.readFileSync(pagePath, 'utf8')
  const tableIndex = source.indexOf(item.tableMarker)
  assert(tableIndex >= 0, `${item.name}主表必须绑定预期数据源：${item.tableMarker}`)

  const openTagStart = source.lastIndexOf('<el-table', tableIndex)
  const openTagEnd = source.indexOf('>', tableIndex)
  assert(openTagStart >= 0 && openTagEnd > openTagStart, `${item.name}主表必须存在 el-table 起始标签`)

  const openTag = source.slice(openTagStart, openTagEnd)
  assert(
    openTag.includes(item.heightMarker),
    `${item.name}主表必须设置内部滚动高度 ${item.heightMarker}，避免横向滚动条被长列表推到页面底部。`
  )
}

console.log('PASS: MES workorder tables keep horizontal scrollbars visible in table viewport')
