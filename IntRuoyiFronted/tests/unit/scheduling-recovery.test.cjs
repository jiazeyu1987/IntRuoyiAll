const assert = require('node:assert/strict')
const test = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { ref, reactive } = require('vue')

// Execute the actual SFC handlers with controllable request completion order.
function handlers(page, names, setup, bindings = {}) {
  const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro', page, 'index.vue'), 'utf8')
  const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const ast = ts.createSourceFile('page.ts', script, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const selected = names.map((name) => {
    const node = ast.statements.find((item) => item.name?.text === name ||
      item.declarationList?.declarations.some((declaration) => declaration.name.text === name))
    assert.ok(node, `missing real handler ${name}`)
    return node.getText(ast)
  }).join('\n')
  const js = ts.transpileModule(`${setup}\n${selected}\nreturn {${names.join(',')}}`, {
    compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.CommonJS }
  }).outputText
  return new Function('ref', 'reactive', ...Object.keys(bindings), js)(ref, reactive, ...Object.values(bindings))
}
function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

test('detail: failures clear old rows; retry, object switches and late responses retain correct owner', async () => {
  const pending = []
  const state = {
    currentScheduleOrder: ref(), processDialogVisible: ref(false), processList: ref([{ owner: 'old' }]),
    processError: ref(''), processLoading: ref(false), processRouteQuickFilterParams: reactive({})
  }
  const h = handlers('scheduleorder', ['openProcessDialog'], 'let processRequestSerial = 0', {
    ...state, MesProScheduleOrderApi: { getProcessList: (id) => { const d = deferred(); pending.push({ ...d, id }); return d.promise } }
  })
  const a = h.openProcessDialog({ id: '104106', code: '104106' })
  assert.deepEqual(state.processList.value, [])
  pending[0].resolve([{ owner: '104106' }]); await a
  const failed = h.openProcessDialog({ id: '105183', code: '105183' })
  assert.deepEqual(state.processList.value, [])
  pending[1].reject(new Error('500')); await failed
  assert.match(state.processError.value, /105183.*500/)
  assert.deepEqual(state.processList.value, [])
  const retry = h.openProcessDialog({ id: '105183', code: '105183' })
  pending[2].resolve([]); await retry
  assert.equal(state.processError.value, '')
  assert.equal(state.processLoading.value, false)
  const late = h.openProcessDialog({ id: '104106', code: '104106' })
  const current = h.openProcessDialog({ id: '105183', code: '105183' })
  pending[4].resolve([{ owner: '105183' }]); await current
  pending[3].resolve([{ owner: '104106' }]); await late
  assert.deepEqual(state.processList.value, [{ owner: '105183' }])
  const lateError = h.openProcessDialog({ id: '104106', code: '104106' })
  const empty = h.openProcessDialog({ id: '105183', code: '105183' })
  pending[6].resolve([]); await empty
  pending[5].reject(new Error('late500')); await lateError
  assert.equal(state.processError.value, '')
  assert.deepEqual(state.processList.value, [])
})

test('settings: partial load failure disables all saves, successful retry enables them', async () => {
  let fail = true, calls = 0
  const state = { schedulerSettingsLoaded: ref(false), schedulerSettingsLoading: ref(false), schedulerSettingsError: ref('') }
  const h = handlers('scheduler-workbench', ['ensureSchedulerSettingsLoaded', 'saveShiftHoursSetting'],
    'let schedulerSettingsRequest = null', {
      ...state, loadShiftHoursSetting: async () => {}, loadPolicySettings: async () => {},
      loadScheduleRules: async () => { if (fail) throw new Error('规则500') }, loadRuntimeStatus: async () => {},
      resolveSchedulerWorkbenchErrorMessage: (e) => e.message,
      SchedulerWorkbenchApi: { saveShiftHoursSetting: async () => { calls++ } }
    })
  await h.ensureSchedulerSettingsLoaded()
  assert.equal(state.schedulerSettingsLoaded.value, false)
  assert.match(state.schedulerSettingsError.value, /500/)
  await assert.rejects(h.saveShiftHoursSetting(), /尚未|成功加载/)
  assert.equal(calls, 0)
  fail = false; await h.ensureSchedulerSettingsLoaded()
  assert.equal(state.schedulerSettingsLoaded.value, true)
  assert.equal(state.schedulerSettingsError.value, '')
})

test('workbench: summary failure does not skip the independent process request', async () => {
  let processCalls = 0
  const h = handlers('scheduler-workbench', ['reloadWorkbench'], 'let schedulerWorkbenchRequestSerial = 0', {
    loadSummary: async () => { throw new Error('500') },
    loadSchedulerWorkbenchSecondaryData: async () => { processCalls++ }
  })
  await h.reloadWorkbench()
  assert.equal(processCalls, 1)
})

test('calendar: rule failure invalidates task totals and prevents saving defaults', async () => {
  let writes = 0
  const state = { monthLoading: ref(true), detailLoading: ref(true), monthLoaded: ref(true),
    visibleMonthDays: ref([{ taskCount: 24 }]), dayDetail: ref({ taskCount: 24 }),
    rulesLoaded: ref(false), rulesLoading: ref(false) }
  const h = handlers('task/calendar', ['initializePage', 'saveRules'],
    'let monthRequestSerial = 0; let detailRequestSerial = 0; let initializationSerial = 0', {
      ...state, loadRules: async () => { throw new Error('500') },
      ProScheduleCalendarApi: { updateRules: async () => { writes++ } }
    })
  await h.initializePage()
  assert.equal(state.monthLoaded.value, false)
  assert.deepEqual(state.visibleMonthDays.value, [])
  assert.equal(state.dayDetail.value, null)
  await assert.rejects(h.saveRules(), /禁止保存/)
  assert.equal(writes, 0)
})

test('calendar: late day results and late failures cannot replace the selected date', async () => {
  const pending = [], state = { dayDetail: ref(), detailLoading: ref(false), detailErrorMessage: ref(''), calendarRecoveryState: ref() }
  const h = handlers('task/calendar', ['loadDayDetail'], 'let detailRequestSerial = 0', {
    ...state, normalizeDate: (x) => x, clearCalendarRecovery() {},
    buildCalendarRecoveryState: () => ({ title: '加载失败' }),
    ProScheduleCalendarApi: { getDayDetail: () => { const d = deferred(); pending.push(d); return d.promise } }
  })
  const first = h.loadDayDetail('2026-11-26'), second = h.loadDayDetail('2026-11-27')
  pending[1].resolve({ date: '2026-11-27' }); await second
  pending[0].resolve({ date: '2026-11-26' }); await first
  assert.equal(state.dayDetail.value.date, '2026-11-27')
  const third = h.loadDayDetail('2026-11-26'), fourth = h.loadDayDetail('2026-11-28')
  pending[3].resolve({ date: '2026-11-28' }); await fourth
  pending[2].reject(new Error('500')); await third
  assert.equal(state.dayDetail.value.date, '2026-11-28')
  assert.equal(state.detailErrorMessage.value, '')
})

test('calendar: late rules cannot overwrite a successful retry or enable saving early', async () => {
  const pending = []
  const state = { rulesLoading: ref(false), rulesLoaded: ref(false), rulesError: ref(''),
    rulesForm: reactive({}), rulesSnapshot: ref(''), selectedDate: ref(''), currentMonth: ref() }
  const h = handlers('task/calendar', ['loadRules'], 'let rulesRequestSerial = 0', {
    ...state, defaultRules: () => ({}), normalizeDate: x => x,
    buildRulesPayload: () => ({ ...state.rulesForm }), extractErrorMessage: e => e.message,
    ProScheduleCalendarApi: { getRules: () => { const d = deferred(); pending.push(d); return d.promise } }
  })
  const old = h.loadRules(), retry = h.loadRules()
  pending[1].resolve({ weekendRestMode: 'SINGLE' }); await retry
  pending[0].resolve({ weekendRestMode: 'DOUBLE' }); await old
  assert.equal(state.rulesForm.weekendRestMode, 'SINGLE')
  assert.equal(state.rulesLoaded.value, true)
  const failed = h.loadRules()
  assert.equal(state.rulesLoaded.value, false)
  pending[2].reject(new Error('500')); await assert.rejects(failed, /500/)
  assert.equal(state.rulesLoaded.value, false)
  assert.match(state.rulesError.value, /500/)
})

test('calendar: late month data cannot replace a newer month', async () => {
  const pending = []
  const state = { monthLoaded: ref(false), monthLoading: ref(false), monthErrorMessage: ref(''),
    currentMonth: ref('2026-11'), monthData: ref(), visibleMonthDays: ref([]), rulesForm: reactive({}) }
  const h = handlers('task/calendar', ['loadMonthCalendar'], 'let monthRequestSerial = 0', {
    ...state, resolveCalendarVisibleMonths: month => [month.format()], normalizeMonthCalendarData: x => x,
    clearCalendarRecovery() {}, ProScheduleCalendarApi: { getMonthCalendar: () => { const d = deferred(); pending.push(d); return d.promise } }
  })
  state.currentMonth.value = { format: () => '2026-11' }
  const old = h.loadMonthCalendar()
  state.currentMonth.value = { format: () => '2026-12' }
  const current = h.loadMonthCalendar()
  pending[1].resolve({ days: [{ date: 'new' }] }); await current
  pending[0].resolve({ days: [{ date: 'old' }] }); await old
  assert.deepEqual(state.visibleMonthDays.value, [{ date: 'new' }])
  assert.equal(state.monthLoaded.value, true)
})

test('ERP precheck: dialog opens immediately, failure stays visible, retry never deletes', async () => {
  let pending = deferred(), deletes = 0
  const state = { erpWorkOrderCleanupAnalyzing: ref(false), erpWorkOrderCleanupDeleting: ref(false),
    erpWorkOrderCleanupPreview: ref(), erpWorkOrderCleanupError: ref(''), erpWorkOrderCleanupDialogVisible: ref(false) }
  const h = handlers('scheduleorder', ['analyzeErpMissingWorkOrders'], '', {
    ...state, MesProScheduleOrderApi: { analyzeErpMissingWorkOrders: () => pending.promise, deleteErpMissingWorkOrders: () => { deletes++ } },
    message: { error() {}, success() {}, warning() {} }
  })
  const attempt = h.analyzeErpMissingWorkOrders()
  assert.equal(state.erpWorkOrderCleanupDialogVisible.value, true)
  pending.reject(new Error('ERP不可用')); await attempt
  assert.match(state.erpWorkOrderCleanupError.value, /ERP不可用/)
  pending = deferred(); const retry = h.analyzeErpMissingWorkOrders()
  pending.resolve({ analyzedCount: 2, missingWorkOrders: [], unverifiableCount: 0 }); await retry
  assert.equal(state.erpWorkOrderCleanupError.value, '')
  assert.equal(state.erpWorkOrderCleanupPreview.value.analyzedCount, 2)
  assert.equal(deletes, 0)
})
