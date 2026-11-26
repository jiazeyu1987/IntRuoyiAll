const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const source = fs.readFileSync('src/views/mes/pro/scheduleorder/index.vue', 'utf8')
const start = source.indexOf('const confirmApplyReplanStartChoice = async () => {')
const end = source.indexOf('\nconst openDailyCompareDialog', start)
const ref = (value) => ({ value })
let previewCalls = 0
let key = 0
let fail = true
let failure = new Error('network unknown')
const sent = []
const noop = () => {}
const context = {
  Error, REPLAN_UNAPPLIED_CALENDAR_ERROR_CODES: new Set(['1040250011', '1040250012']),
  pendingReplanApply: ref(null), replanApplyLoading: ref(false), replanForm: { reason: 'original' },
  replanStartDate: ref('2026-11-25'), dayjs: () => ({ format: () => '2026-11-25' }),
  buildWholeDayReplanStartTime: (x) => x,
  buildReplanRequest: () => ({ scheduleOrderIds: [1], startTime: '2026-11-25', runtimeCapacityBasis: 'PLANNED' }),
  startReplanApplyProgress: noop, resetReplanApplyProgress: noop, finishReplanApplyProgress: async () => {},
  runPreflightForRequest: async () => ({ result: 'PASS' }), preflightHasGlobalBlockedIssue: ref(false),
  hasErpSourceWarning: () => false,
  previewReplanForRequest: async () => ({ calendarContextToken: `token-${++previewCalls}` }),
  hasGlobalReplanBlockingIssue: () => false, notifySkippedSelectedReplanRows: noop,
  buildReplanApplyIdempotencyKey: () => `key-${++key}`,
  ProTaskAutoScheduleApi: { replanApply: async (payload) => { sent.push(structuredClone(payload)); if (fail) throw failure; return {} } },
  updateLastReplanParticipatingScheduleOrders: noop, buildReplanApplySuccessMessage: () => 'success',
  message: { success: noop, error: noop }, console: { error: noop },
  replanStartDateDialogVisible: ref(true), replanSettingsDialogVisible: ref(true), replanDrawerVisible: ref(true),
  clearScheduleOrderSelection: noop, getScheduleOrderList: async () => {}, loadLatestSuccessfulScheduleApplyTime: async () => {},
  emitter: { emit: noop }, MES_PRO_TASK_GANTT_REFRESH_EVENT: 'refresh', lastReplanParticipatingScheduleOrderIds: ref([1])
}
vm.createContext(context)
vm.runInContext(ts.transpile(source.slice(start, end) + '\nglobalThis.confirm = confirmApplyReplanStartChoice'), context)
;(async () => {
  await context.confirm()
  assert.ok(context.pendingReplanApply.value)
  context.replanForm.reason = 'changed'
  context.buildReplanRequest = () => { throw new Error('current orders no longer eligible') }
  fail = false
  await context.confirm()
  assert.deepEqual(sent[1], sent[0], 'retry must replay the complete original payload')
  assert.equal(previewCalls, 1, 'retry must not refresh the context token')
  assert.equal(context.pendingReplanApply.value, null)
  context.buildReplanRequest = () => ({ scheduleOrderIds: [2], startTime: '2026-11-26', runtimeCapacityBasis: 'ACTUAL' })
  await context.confirm()
  assert.notEqual(sent[2].idempotencyKey, sent[1].idempotencyKey)
  assert.equal(sent[2].reason, 'changed')
  assert.equal(previewCalls, 2)
  for (const code of [500, 401, 403, 'UNKNOWN']) {
    failure = Object.assign(new Error('unknown response'), { code })
    fail = true
    await context.confirm()
    const original = structuredClone(context.pendingReplanApply.value.payload)
    await context.confirm()
    assert.deepEqual(sent.at(-1), original)
    fail = false
    await context.confirm()
  }
  for (const code of [1040250011, '1040250012']) {
    failure = Object.assign(new Error('calendar rejected before apply'), { code })
    fail = true
    await context.confirm()
    const rejectedKey = sent.at(-1).idempotencyKey
    assert.equal(context.pendingReplanApply.value, null)
    const previousPreviews = previewCalls
    fail = false
    await context.confirm()
    assert.equal(previewCalls, previousPreviews + 1)
    assert.notEqual(sent.at(-1).idempotencyKey, rejectedKey)
  }
  const keyStart = source.indexOf('const buildReplanApplyIdempotencyKey =')
  const keyEnd = source.indexOf('\n', keyStart)
  const keyContext = vm.createContext({ generateUUID: () => '12345678-1234-1234-1234-123456789012' })
  vm.runInContext(ts.transpile(source.slice(keyStart, keyEnd) + '\nglobalThis.buildKey = buildReplanApplyIdempotencyKey'), keyContext)
  assert.ok(keyContext.buildKey({ scheduleOrderIds: Array.from({ length: 1000 }, (_, i) => i) }).length <= 128)
  console.log('replan apply retry behavior: PASS')
})().catch((error) => { console.error(error); process.exitCode = 1 })
