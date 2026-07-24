const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')

assert.equal(fs.existsSync(pagePath), true, '排产工单页面必须存在。')

const source = fs.readFileSync(pagePath, 'utf8')
const scheduleTabStart = source.indexOf('<el-tab-pane label="排产工单" name="scheduleOrders">')
const admissionTabStart = source.indexOf('<el-tab-pane label="同步工单" name="workOrderAdmission">')

assert.ok(scheduleTabStart >= 0, '必须存在排产工单页签。')
assert.ok(admissionTabStart > scheduleTabStart, '同步工单页签必须继续存在。')

const scheduleTabSource = source.slice(scheduleTabStart, admissionTabStart)
const actionsStart = scheduleTabSource.indexOf('<template #actions>')
const actionsEnd = scheduleTabSource.indexOf('</template>', actionsStart)

assert.ok(actionsStart >= 0 && actionsEnd > actionsStart, '排产工单页签必须存在 actions 工具栏。')

const actionsSource = scheduleTabSource.slice(actionsStart, actionsEnd)

for (const forbidden of ['同步工单', '批量冻结', '批量解冻', '批量删除']) {
  assert.equal(actionsSource.includes(forbidden), false, `排产工单页签工具栏不能继续渲染 ${forbidden}。`)
}

assert.match(
  actionsSource,
  /导出[\s\S]*手动重排[\s\S]*UserTableColumnSettings/,
  '排产工单页签工具栏必须继续保留导出、手动重排和显示字段入口。'
)
assert.equal(
  actionsSource.includes('handleBatchFreeze'),
  false,
  '删除批量冻结按钮后，工具栏不能继续绑定 handleBatchFreeze。'
)
assert.equal(
  actionsSource.includes('handleBatchUnfreeze'),
  false,
  '删除批量解冻按钮后，工具栏不能继续绑定 handleBatchUnfreeze。'
)
assert.equal(
  actionsSource.includes('handleBatchDelete'),
  false,
  '删除批量删除按钮后，工具栏不能继续绑定 handleBatchDelete。'
)

console.log('PASS: MES schedule order selected toolbar buttons removed')
