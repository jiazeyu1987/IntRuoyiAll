const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const source = fs.readFileSync(pagePath, 'utf8')

assert.ok(
  source.includes('const replanScopeRows = computed(() => selectedScheduleOrders.value)'),
  '手动重排范围必须只来自用户显式勾选的排产工单'
)
assert.ok(
  source.includes('未明确选择时禁止检查、预览或应用'),
  '手动重排抽屉必须说明未选择时禁止继续'
)
assert.ok(
  source.includes('未勾选工单不会参与检查、预览或应用'),
  '手动重排抽屉必须说明影响范围'
)
assert.ok(
  !source.includes('selectedScheduleOrders.value.length ? selectedScheduleOrders.value : scheduleOrderList.value'),
  '手动重排不得在未勾选时隐式使用当前表格页或全池列表'
)
assert.ok(source.includes('lastPreflightRequest'), '预检必须记录上一次检查上下文')
assert.ok(source.includes('preflightStale'), '修改范围或参数后必须识别预检已失效')
assert.ok(source.includes('JSON.stringify(lastPreflightRequest.value) !== JSON.stringify(currentRequest)'), '预检上下文必须按范围、起排时间和产能模式比对')
assert.ok(source.includes('preflightResult.value = null'), '切换重排范围时必须清空旧预检结果')
assert.ok(source.includes('runPreflightForRequest(applyRequest)'), '应用重排确认时必须按当前开始日期重新执行排产前检查')
assert.ok(source.includes("throw new Error('排产前检查存在阻断问题，不能应用重排')"), '应用重排前必须阻断本次预检发现的阻断问题')
assert.ok(
  source.includes('重排预览存在阻断问题，请先处理下方问题列表后再应用重排。'),
  '重排预览阻断提示必须引导用户按问题列表处理'
)
assert.ok(
  !source.includes('重排预览存在阻断问题，需先补齐缺失班次后再提交审批。'),
  '重排预览阻断提示不得把所有阻断问题误导为缺失班次'
)
assert.ok(source.includes('calendarContextToken'), '发布/提交必须携带预览绑定的日历上下文令牌')
assert.ok(source.includes(':disabled="!selectedScheduleOrders.length"'), '未勾选排产工单时，手动重排入口必须不可点击')
assert.ok(!source.includes('openPreflightDrawer'), '工具栏不应继续保留独立排产前检查入口方法')
assert.ok(source.includes('请先勾选排产工单'), '未勾选时入口必须给出明确阻断原因')
assert.ok(source.includes('v-model="replanForm.reason"'), '重排抽屉仍应保留可选业务原因输入')
assert.ok(!source.includes("throw new Error('请填写本次重排的业务原因')"), '应用重排不应再因缺少业务原因被前端本地阻断')

console.log('PASS: MES schedule order replan explicit scope contract')
