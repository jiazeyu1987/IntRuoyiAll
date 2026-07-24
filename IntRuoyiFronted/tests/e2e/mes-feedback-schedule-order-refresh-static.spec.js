const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const feedbackPagePath = path.resolve(frontendRoot, 'src/views/mes/pro/feedback/index.vue')
const scheduleOrderPagePath = path.resolve(frontendRoot, 'src/views/mes/pro/scheduleorder/index.vue')
const emitterHookPath = path.resolve(frontendRoot, 'src/hooks/web/useEmitt.ts')

for (const filePath of [feedbackPagePath, scheduleOrderPagePath, emitterHookPath]) {
  assert(fs.existsSync(filePath), `相关文件必须存在：${filePath}`)
}

const feedbackPageSource = fs.readFileSync(feedbackPagePath, 'utf8')
const scheduleOrderPageSource = fs.readFileSync(scheduleOrderPagePath, 'utf8')
const emitterHookSource = fs.readFileSync(emitterHookPath, 'utf8')

assert(
  emitterHookSource.includes('const emitter = mitt()'),
  '项目必须继续使用统一 mitt 事件总线实现跨页面刷新。'
)

for (const fragment of [
  "import { useEmitt } from '@/hooks/web/useEmitt'",
  "const MES_SCHEDULE_ORDER_REFRESH_EVENT = 'mes-schedule-order-refresh'",
  'const emitScheduleOrderRefresh = (payload?: MesScheduleOrderRefreshPayload) => {',
  'emitter.emit(MES_SCHEDULE_ORDER_REFRESH_EVENT, payload)'
]) {
  assert(feedbackPageSource.includes(fragment), `报工页必须在成功后派发排产工单刷新事件：${fragment}`)
}

assert(
  /await ProFeedbackApi\.confirmImportRecordBatch\(payload\)[\s\S]*await getImportRecordList\(\)[\s\S]*await getList\(\)[\s\S]*emitScheduleOrderRefresh\(\)[\s\S]*activeTab\.value = 'feedback'[\s\S]*message\.alertSuccess\('报工成功'\)/.test(
    feedbackPageSource
  ),
  '确认报工成功后必须在提示成功前触发排产工单刷新事件，并切回正式报工 tab。'
)

for (const fragment of [
  "import { useEmitt } from '@/hooks/web/useEmitt'",
  "const MES_SCHEDULE_ORDER_REFRESH_EVENT = 'mes-schedule-order-refresh'",
  'useEmitt({',
  'name: MES_SCHEDULE_ORDER_REFRESH_EVENT',
  'callback: handleScheduleOrderRefresh',
  'const handleScheduleOrderRefresh = async',
  'getScheduleOrderList()'
]) {
  assert(scheduleOrderPageSource.includes(fragment), `排产工单页必须监听报工成功刷新事件：${fragment}`)
}

console.log('PASS: MES feedback schedule order refresh static contract')
