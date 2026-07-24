const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const apiPath = path.join(root, 'src/api/mes/pro/scheduleorder/index.ts')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const apiSource = fs.readFileSync(apiPath, 'utf8')
const pageSource = fs.readFileSync(pagePath, 'utf8')

assert.ok(
  apiSource.includes('unfreezeScheduleOrders'),
  '排产工单 API 必须提供解冻接口方法'
)
assert.ok(
  apiSource.includes('/mes/pro/schedule-order/unfreeze'),
  '排产工单解冻必须调用后端解冻接口'
)
assert.ok(pageSource.includes('批量解冻'), '排产工单列表必须提供批量解冻入口')
assert.ok(pageSource.includes('openUnfreezeDialog'), '排产工单行操作必须提供解冻入口')
assert.ok(pageSource.includes('handleBatchUnfreeze'), '批量解冻必须有独立处理函数')
assert.ok(pageSource.includes('openUnfreezeRows'), '解冻必须只筛选已冻结排产工单')
assert.ok(pageSource.includes('submitScheduleOrderUnfreeze'), '解冻弹框必须有提交动作')
assert.ok(pageSource.includes('unfreezeDialogVisible'), '解冻必须使用独立弹框状态')
assert.ok(pageSource.includes('解冻原因'), '解冻必须要求用户填写原因')
assert.ok(
  pageSource.includes('请选择已冻结的排产工单'),
  '解冻空选择或选择未冻结工单时必须给出明确提示'
)
assert.ok(
  pageSource.includes('MesProScheduleOrderApi.unfreezeScheduleOrders'),
  '前端解冻提交必须调用解冻 API'
)
assert.ok(pageSource.includes("UNFREEZE: '解冻'"), '操作日志必须展示解冻类型')
assert.ok(!pageSource.includes('catch {}'), '排产工单页不得吞掉解冻失败异常')

console.log('PASS: MES schedule order unfreeze frontend contract')
