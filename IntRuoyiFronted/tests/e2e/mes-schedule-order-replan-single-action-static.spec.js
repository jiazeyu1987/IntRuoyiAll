const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const autoScheduleApiPath = path.resolve(process.cwd(), 'src/api/mes/pro/task/autoSchedule/index.ts')
assert(fs.existsSync(pagePath), '排产工单页面必须存在。')
assert(fs.existsSync(autoScheduleApiPath), '自动排产 API 模块必须存在。')

const source = fs.readFileSync(pagePath, 'utf8')
const autoScheduleApiSource = fs.readFileSync(autoScheduleApiPath, 'utf8')
const previewReqStart = autoScheduleApiSource.indexOf('export interface ProTaskAutoSchedulePreviewReqVO')
assert.notEqual(previewReqStart, -1, '自动排产 / 重排前端请求类型必须存在。')
const previewReqEnd = autoScheduleApiSource.indexOf('\n}', previewReqStart)
assert.ok(previewReqEnd > previewReqStart, '自动排产 / 重排前端请求类型范围必须可解析。')
const previewReqSource = autoScheduleApiSource.slice(previewReqStart, previewReqEnd)

const actionsStart = source.indexOf('class="schedule-order-pool__replan-actions"')
const actionsEnd = source.indexOf('class="schedule-order-pool__replan-progress"', actionsStart)
assert.ok(actionsStart >= 0 && actionsEnd > actionsStart, '重排动作区必须存在。')
const actionSource = source.slice(actionsStart, actionsEnd)

assert.ok(actionSource.includes('@click="applyReplan"'), '重排动作区必须使用一个按钮触发完整重排。')
assert.ok(actionSource.includes('> 开始重排') || actionSource.includes('/> 开始重排'), '重排按钮文案必须是开始重排。')
assert.ok(!actionSource.includes('预览重排'), '重排动作区不能保留单独的预览重排按钮。')
assert.ok(!actionSource.includes('应用重排'), '重排动作区不能保留单独的应用重排按钮。')
assert.doesNotMatch(
  previewReqSource,
  /workOrderIds\??:/,
  '自动排产 / 重排前端请求类型不能继续暴露生产工单编号入口。'
)
assert.match(
  previewReqSource,
  /scheduleOrderIds:\s*number\[\]/,
  '自动排产 / 重排前端请求类型必须以排产工单编号作为唯一范围入口。'
)

assert.ok(
  source.includes('const previewReplanForRequest = async'),
  '完整重排仍必须复用预览方法生成阻断明细。'
)
assert.ok(
  source.includes('const applyResult = await ProTaskAutoScheduleApi.replanApply'),
  '完整重排在无阻断时必须调用直接应用接口。'
)
assert.ok(
  source.includes('排产前检查存在阻断问题，不能应用重排') &&
    source.includes('重排预览存在阻断问题，不能应用重排'),
  '完整重排必须在检查或预览存在阻断时停止直接应用。'
)

console.log('PASS: MES schedule order replan single action static contract')
