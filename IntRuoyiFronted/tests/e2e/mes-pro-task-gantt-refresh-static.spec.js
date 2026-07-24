const assert = require('node:assert/strict')
const { existsSync, readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const repoRoot = resolve(__dirname, '../..')
const pagePath = resolve(repoRoot, 'src/views/mes/pro/task/index.vue')
const scheduleOrderPath = resolve(repoRoot, 'src/views/mes/pro/scheduleorder/index.vue')
const eventModulePath = resolve(repoRoot, 'src/views/mes/pro/shared/scheduleEvents.ts')

assert.ok(existsSync(pagePath), 'missing MES production gantt page')
assert.ok(existsSync(scheduleOrderPath), 'missing schedule-order page')
assert.ok(existsSync(eventModulePath), 'missing shared MES schedule event module')

const page = readFileSync(pagePath, 'utf8')
const scheduleOrderPage = readFileSync(scheduleOrderPath, 'utf8')
const eventModule = readFileSync(eventModulePath, 'utf8')

assert.match(
  eventModule,
  /export const MES_SCHEDULE_ORDER_REFRESH_EVENT = 'mes-schedule-order-refresh'/,
  'shared event module must keep the existing schedule-order refresh event name'
)
assert.match(
  eventModule,
  /export const MES_PRO_TASK_GANTT_REFRESH_EVENT = 'mes-pro-task-gantt-refresh'/,
  'shared event module must expose a dedicated gantt refresh event name'
)

assert.match(page, /from '\.\.\/shared\/scheduleEvents'/, 'gantt page must reuse the shared schedule event module')
assert.match(page, /MES_PRO_TASK_GANTT_REFRESH_EVENT/, 'gantt page must subscribe to the gantt refresh event')
assert.match(page, /useEmitt\(\{\s*name:\s*MES_PRO_TASK_GANTT_REFRESH_EVENT/, 'gantt page must listen for refresh events')
assert.match(page, /onActivated/, 'gantt page must reload when keep-alive pages reactivate')
assert.match(page, /requestFullscreen/, 'gantt page must enter fullscreen through the Fullscreen API')
assert.match(page, /exitFullscreen/, 'gantt page must exit fullscreen through the Fullscreen API')
assert.match(page, /fullscreenchange/, 'gantt page must sync fullscreen state when the browser exits fullscreen')
assert.match(page, /最大化/, 'gantt page must expose a maximize action')
assert.match(page, /恢复/, 'gantt page must expose a restore action')
assert.match(page, /订单/, 'fullscreen control rail must keep the order view hint')
assert.match(page, /工序/, 'fullscreen control rail must keep the process view hint')

assert.match(
  scheduleOrderPage,
  /from '\.\.\/shared\/scheduleEvents'/,
  'schedule-order page must reuse the shared schedule event module'
)
assert.match(scheduleOrderPage, /const \{\s*emitter\s*\} = useEmitt\(\)/, 'schedule-order page must get the emitter to publish gantt refresh events')
assert.match(
  scheduleOrderPage,
  /emitter\.emit\(MES_PRO_TASK_GANTT_REFRESH_EVENT,\s*\{/,
  'schedule-order replan success must emit a gantt refresh payload'
)
assert.match(
  scheduleOrderPage,
  /await getScheduleOrderList\(\)[\s\S]*emitter\.emit\(MES_PRO_TASK_GANTT_REFRESH_EVENT,\s*\{/,
  'schedule-order page must refresh its own list and then trigger gantt reload after a successful replan'
)

console.log('mes-pro-task gantt refresh/persistence static contract passed')
