const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/task/calendar/index.vue'),
  'utf8'
)
const template = source.slice(0, source.indexOf('</template>'))

const toolbarStart = template.indexOf('<div class="calendar-toolbar">')
const toolbarShellStart = template.indexOf('<div class="calendar-shell">')
const statusStripStart = template.indexOf('<div class="calendar-status-strip">')
const titleGroupStart = template.indexOf('<div class="toolbar-title-group">')
const monthSwitchStart = template.indexOf('<div class="month-switch">')

assert.notEqual(toolbarStart, -1, '排程日历必须保留顶部工具栏。')
assert.notEqual(toolbarShellStart, -1, '排程日历必须保留月历容器。')
assert.notEqual(statusStripStart, -1, '排程日历必须保留任务总数和最近更新时间控件。')
assert.notEqual(titleGroupStart, -1, '排程日历必须保留标题区域。')
assert.notEqual(monthSwitchStart, -1, '排程日历必须保留月份切换控件。')

assert.ok(
  statusStripStart > titleGroupStart && statusStripStart < monthSwitchStart,
  '任务总数和最近更新时间必须放在标题区域与月份切换之间的顶部蓝框位置。'
)
assert.ok(
  statusStripStart > toolbarStart && statusStripStart < toolbarShellStart,
  '任务总数和最近更新时间必须位于 calendar-toolbar 内，不得继续位于 calendar-shell 月历上方。'
)

const statusMarkup = template.slice(statusStripStart, monthSwitchStart)
for (const fragment of ['<label>任务总数</label>', '<label>最近更新时间</label>']) {
  assert.equal(statusMarkup.includes(fragment), true, `蓝框位置必须包含控件：${fragment}`)
}

const shellMarkup = template.slice(toolbarShellStart)
assert.equal(
  shellMarkup.includes('<div class="calendar-status-strip">'),
  false,
  '月历容器内不得继续保留旧的红框控件。'
)

console.log('PASS: MES task calendar status controls are placed in toolbar center')
