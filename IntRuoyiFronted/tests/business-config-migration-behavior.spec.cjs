const assert = require('node:assert/strict')
const test = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const { createRouter, createMemoryHistory } = require('vue-router')
const root = path.resolve(__dirname, '..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const compile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const evaluate = (source, dependencies) => {
  const module = { exports: {} }
  Function('require', 'module', 'exports', compile(source))(id => {
    assert.ok(Object.hasOwn(dependencies, id), `unexpected dependency ${id}`)
    return dependencies[id]
  }, module, module.exports)
  return module.exports
}
function component(file, bindings, exposed) {
  const source = parse(read(file)).descriptor.scriptSetup.content
  const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true)
  const body = ast.statements.filter(s => !ts.isImportDeclaration(s)).map(s => s.getText(ast)).join('\n')
  const errors = [], successes = [], context = { ...vue, onMounted() {}, watch() {}, defineExpose() {}, defineOptions() {}, defineProps: () => ({ canUpdate: true }), useMessage: () => ({ error: x => errors.push(x), success: x => successes.push(x) }), ...bindings }
  const instance = Function(...Object.keys(context), compile(body) + `; return { ${exposed} };`)(...Object.values(context))
  return { instance, errors, successes }
}
const idleFile = 'src/views/infra/config/basic/components/IdleLogoutSetting.vue'
const scheduleFile = 'src/views/dcc/registration-certificate/config/components/ReminderScheduleSetting.vue'
const erpFile = 'src/views/erp/sync/config/components/TableAutoSyncSetting.vue'
const golden = 'mes:pro-batch-record-execution:golden-finger'

test('published config menus replace registered static shells and resolve to their page components', async () => {
  const staticRoutes = evaluate(read('src/router/modules/businessConfig.ts'), { '@/utils/routerHelper': { Layout: { render() {} } } }).default
  const dynamicRoutes = staticRoutes.map((route, index) => ({
    path: `/module-${index}`,
    name: `Module${index}`,
    component: { render() {} },
    children: [{ ...route.children[0], path: route.path, component: { render() {} }, meta: {} }]
  }))
  const options = evaluate(read('src/store/modules/permission.ts'), {
    pinia: { defineStore: (_name, options) => options },
    './testMode': {},
    '@/store': { store: {} },
    'lodash-es': await import('lodash-es'),
    '@/router/modules/remaining': { default: staticRoutes },
    '@/router/modules/showroom': { default: [] },
    '@/utils/routerHelper': { generateRoute: routes => routes, flatMultiLevelRoutes: routes => routes },
    '@/hooks/web/useCache': { CACHE_KEY: { ROLE_ROUTERS: 'routes' }, useCache: () => ({ wsCache: { get: () => dynamicRoutes } }) },
    '@/utils/is': { isUrl: value => /^https?:/.test(value) }
  }).usePermissionStore
  const state = options.state()
  await options.actions.generateRoutes.call(state)
  const router = createRouter({ history: createMemoryHistory(), routes: staticRoutes })
  // Exercise the actual login registration order, including removal from Vue Router.
  const guard = read('src/permission.ts')
  const registration = guard.slice(guard.indexOf('permissionStore.staticRouteNamesToRemove.forEach'), guard.indexOf('        const redirectPath ='))
  Function('router', 'permissionStore', compile(registration))(router, { ...state, getAddRouters: state.addRouters })
  for (const route of staticRoutes) {
    const resolved = router.resolve(route.path)
    assert.equal(router.hasRoute(route.name), false)
    assert.equal(resolved.name, route.children[0].name)
    assert.equal(resolved.matched.length, 2)
    assert.ok(resolved.matched.at(-1).components.default)
  }
})

test('access policies retain explicit MES/DCC and existing infra administrator semantics; ERP requires every capability', () => {
  const user = { permissions: new Set(), getRoles: [], getUser: { username: 'ordinary' } }
  const access = evaluate(read('src/utils/businessConfigAccess.ts'), { '@/store/modules/user': { useUserStore: () => user }, '@/utils/permission': { checkPermi: permissions => user.permissions.has('*:*:*') || permissions.some(p => user.permissions.has(p)) } })
  assert.equal(access.hasExplicitConfigPermission(golden), false)
  assert.equal(access.hasInfraConfigPermission('infra:config:query'), false)
  user.getUser.username = 'admin'
  assert.equal(access.hasInfraConfigPermission('infra:config:query'), true)
  assert.equal(access.hasExplicitConfigPermission(golden), false)
  user.getUser.username = 'ordinary'
  user.permissions = new Set([golden, 'erp:kingdee-sync:query'])
  assert.equal(access.hasErpConfigCapability(['infra:job:query', 'erp:kingdee-sync:query']), false)
  assert.equal(access.hasErpConfigCapability(['erp:kingdee-sync:query']), true)
  user.permissions.add('infra:job:query')
  assert.equal(access.hasErpConfigCapability(['infra:job:query', 'erp:kingdee-sync:query']), true)
  assert.equal(access.hasErpConfigCapability(['infra:job:update']), false)
})

test('idle editor rejects duplicates, invisible values and out-of-range data, then preserves full global row on save', async () => {
  const row = { id: 8, key: 'system.login.idle-timeout-minutes', visible: true, value: '15', name: 'global', type: 1 }
  let rows = [row], writes = []
  const h = component(idleFile, { useTestModeStore: () => ({ apply() {} }), ConfigApi: { getConfigPage: async () => ({ list: rows }), updateConfig: async value => writes.push(value) } }, 'loadConfig, handleSave, form, loaded')
  for (const invalid of [[row, row], [{ ...row, visible: false }], [{ ...row, value: '1441' }]]) {
    rows = invalid
    await h.instance.loadConfig()
    assert.equal(h.instance.loaded.value, false)
    await h.instance.handleSave()
    assert.equal(writes.length, 0)
  }
  rows = [row]
  await h.instance.loadConfig()
  h.instance.form.idleLogoutMinutes = 60
  await h.instance.handleSave()
  assert.deepEqual(writes, [{ ...row, value: '60' }])
  const denied = component(idleFile, { useTestModeStore: () => ({ apply() {} }), defineProps: () => ({ canUpdate: false }), ConfigApi: { getConfigPage: async () => ({ list: [row] }), updateConfig: async () => assert.fail('unauthorized write') } }, 'loadConfig, handleSave')
  await denied.instance.loadConfig()
  await denied.instance.handleSave()
  assert.match(denied.errors[0], /无权/)
})

test('DCC schedule save uses loaded tenant rowVersion and all recipients; conflict stays visible and does not consume draft', async () => {
  const config = { enabled: true, dailyRunTime: '09:00', timezone: 'Asia/Shanghai', thresholdDaysJson: '[30,8,2,1]', thresholdRecipientUserIds: { T_30: [1], T_8: [2], T_2: [3], T_1: [4] }, rowVersion: 7 }
  const writes = []
  let fail = false
  const h = component(scheduleFile, { getRegistrationCertificateReminderConfig: async () => config, updateRegistrationCertificateReminderConfig: async value => { writes.push(value); if (fail) throw new Error('版本冲突，请刷新'); return { ...config, ...value, rowVersion: 8 } } }, 'loadConfig, handleSave, form')
  await h.instance.loadConfig()
  h.instance.form.dailyRunTime = '10:00'
  await h.instance.handleSave()
  assert.equal(writes[0].expectedRowVersion, 7)
  assert.deepEqual(writes[0].thresholdRecipientUserIds, config.thresholdRecipientUserIds)
  assert.equal(h.instance.form.rowVersion, 8)
  fail = true
  h.instance.form.dailyRunTime = '11:00'
  await h.instance.handleSave()
  assert.match(h.errors[0], /版本冲突/)
  assert.equal(h.instance.form.dailyRunTime, '11:00')
  assert.equal(h.instance.form.rowVersion, 8)
  const denied = component(scheduleFile, { defineProps: () => ({ canUpdate: false }), getRegistrationCertificateReminderConfig: async () => config, updateRegistrationCertificateReminderConfig: async () => assert.fail('unauthorized write') }, 'loadConfig, handleSave')
  await denied.instance.loadConfig()
  await denied.instance.handleSave()
  assert.match(denied.errors[0], /无权/)
})

test('ERP guards every mutation before requests; permitted cron/status update retains enable and stop ordering', async () => {
  const calls = []
  let allowed = false
  const h = component(erpFile, {
    hasErpConfigCapability: () => allowed,
    ElMessage: { error: x => calls.push(['error', x]), success() {}, warning() {} },
    InfraJobStatusEnum: { NORMAL: 1, STOP: 2 },
    JobApi: { updateJobStatus: async (...args) => calls.push(['status', ...args]), updateJob: async job => calls.push(['update', job]) },
    ErpKingdeeConfigApi: {}, ErpKingdeeSyncApi: {}, formatDateTimeValue: x => x, toDateTimeValue: x => x
  }, 'handleSave, handleSaveConnection, handleRunOnce, handleRunIncremental, handleRunFull, updateJobForAutoSync, form, dailyCronExpression')
  for (const action of ['handleSave', 'handleSaveConnection', 'handleRunOnce', 'handleRunIncremental', 'handleRunFull']) await h.instance[action]({ syncType: 'PRODUCT' })
  assert.equal(calls.length, 5)
  assert.ok(calls.every(call => call[0] === 'error'))
  calls.length = 0
  h.instance.form.enabled = true
  h.instance.form.dailyStartTime = '03:04:05'
  await h.instance.updateJobForAutoSync({ syncType: 'PRODUCT' }, { id: 9, status: 2 }, new Set(['PRODUCT']))
  assert.deepEqual(calls.map(x => x[0]), ['status', 'update'])
  assert.equal(calls[1][1].cronExpression, h.instance.dailyCronExpression.value)
  calls.length = 0
  h.instance.form.enabled = false
  await h.instance.updateJobForAutoSync({ syncType: 'PRODUCT' }, { id: 9, status: 1 }, new Set(['PRODUCT']))
  assert.deepEqual(calls.map(x => x[0]), ['update', 'status'])
})

test('all four explicit targets resolve by name; no automatic redirect; fixed materials expose no editable write surface', () => {
  const routes = evaluate(read('src/router/modules/businessConfig.ts'), { '@/utils/routerHelper': { Layout: { render() {} } } }).default
  const router = createRouter({ history: createMemoryHistory(), routes })
  const paths = { MesEdhrGlobalConfig: '/mes/pro/edhr-config', ErpTableAutoSyncConfig: '/erp/sync/config', DccRegistrationCertificateConfig: '/dcc/registration-certificate/config', InfraBasicConfig: '/infra/config/basic' }
  for (const [name, expected] of Object.entries(paths)) {
    const route = router.resolve({ name })
    assert.equal(route.path, expected)
    assert.equal(route.meta.activeMenu, expected)
    assert.equal(route.matched[route.matched.length - 1].redirect, undefined)
  }
  const notice = read('src/views/system/config-migration/BusinessConfigMigrationNotice.vue')
  for (const name of Object.keys(paths)) assert.ok(notice.includes(`name: '${name}'`))
  const fixed = parse(read('src/views/mes/pro/edhr-config/components/ReleaseDossierRequirements.vue')).descriptor
  assert.equal((fixed.scriptSetup.content.match(/'[^']+'/g) || []).length, 4)
  assert.doesNotMatch(fixed.template.content, /el-switch|el-button|@click|v-model/)
  assert.doesNotMatch(fixed.scriptSetup.content, /update|request|fetch/)
})


test('recipient dialog blocks unloaded/denied writes and duplicate saves; preserves optimistic version and emits refresh', async () => {
  let allowed = true, release
  const writes = [], events = []
  const h = component('src/views/dcc/registration-certificate/config/ReminderConfigDialog.vue', {
    defineEmits: () => (...args) => events.push(args),
    hasExplicitConfigPermission: () => allowed,
    getSimpleUserList: async () => [],
    getRegistrationCertificateReminderConfig: async () => ({ enabled: true, dailyRunTime: '09:00', rowVersion: 3, thresholdRecipientUserIds: { T_30: [1], T_8: [2], T_2: [3], T_1: [4] } }),
    updateRegistrationCertificateReminderConfig: value => { writes.push(value); return new Promise(resolve => { release = resolve }) }
  }, 'open, submit, formRef, loaded, saving, formData')
  h.instance.formRef.value = { validate: async () => true }
  await h.instance.submit()
  assert.equal(writes.length, 0)
  await h.instance.open()
  allowed = false
  await h.instance.submit()
  assert.equal(writes.length, 0)
  allowed = true
  const one = h.instance.submit(), two = h.instance.submit()
  await Promise.resolve(); await Promise.resolve()
  assert.equal(writes.length, 1)
  assert.equal(writes[0].expectedRowVersion, 3)
  release({ rowVersion: 4 })
  await Promise.all([one, two])
  assert.equal(h.instance.formData.rowVersion, 4)
  assert.deepEqual(events, [['success']])
})

test('recordbook uses loaded global setting, cancel makes no write and server failure restores displayed value', async () => {
  let cancel = false, fail = false
  const writes = []
  const h = component('src/views/mes/pro/edhr-config/components/RecordbookGlobalSetting.vue', {
    ElMessageBox: { confirm: async () => { if (cancel) throw 'cancel' } },
    hasExplicitConfigPermission: () => true,
    getEdhrRecordbookGlobalSetting: async () => ({ enabled: true, configKey: 'mes.edhr.recordbook.global.enabled' }),
    updateEdhrRecordbookGlobalSetting: async value => { writes.push(value); if (fail) throw new Error('server conflict'); return value }
  }, 'loadSetting, handleToggleClick, settingEnabled')
  await h.instance.loadSetting()
  cancel = true
  await h.instance.handleToggleClick()
  assert.equal(writes.length, 0)
  assert.equal(h.instance.settingEnabled.value, true)
  cancel = false; fail = true
  await h.instance.handleToggleClick()
  assert.deepEqual(writes, [{ enabled: false }])
  assert.equal(h.instance.settingEnabled.value, true)
  assert.match(h.errors[0], /server conflict/)
})


test('MES rejects missing or invalid global facts and serializes confirmation with final permission recheck', async () => {
  for (const response of [new Error('GET rejected'), { enabled: 'true', configKey: 'mes.edhr.recordbook.global.enabled' }, { enabled: true, configKey: 'wrong' }]) {
    const h = component('src/views/mes/pro/edhr-config/components/RecordbookGlobalSetting.vue', {
      hasExplicitConfigPermission: () => true,
      ElMessageBox: { confirm: async () => assert.fail('unknown facts must not confirm') },
      getEdhrRecordbookGlobalSetting: async () => { if(response instanceof Error) throw response; return response },
      updateEdhrRecordbookGlobalSetting: async () => assert.fail('unknown facts must not write')
    }, 'loadSetting, handleToggleClick, loaded, loadError')
    await h.instance.loadSetting(); await h.instance.handleToggleClick()
    assert.equal(h.instance.loaded.value, false); assert.ok(h.instance.loadError.value)
  }
  let allowed = true, confirm, confirmations = 0, writes = 0
  const h = component('src/views/mes/pro/edhr-config/components/RecordbookGlobalSetting.vue', {
    hasExplicitConfigPermission: () => allowed,
    ElMessageBox: { confirm: () => { confirmations++; return new Promise(resolve => { confirm = resolve }) } },
    getEdhrRecordbookGlobalSetting: async () => ({ enabled: true, configKey: 'mes.edhr.recordbook.global.enabled' }),
    updateEdhrRecordbookGlobalSetting: async () => { writes++; return { enabled: false, configKey: 'mes.edhr.recordbook.global.enabled' } }
  }, 'loadSetting, handleToggleClick, saving')
  await h.instance.loadSetting()
  const pending = h.instance.handleToggleClick(); await h.instance.handleToggleClick()
  assert.equal(confirmations, 1); allowed = false; confirm(); await pending
  assert.equal(writes, 0); assert.equal(h.instance.saving.value, false)
})

const erpHarness = bindings => component(erpFile, {
  hasErpConfigCapability: () => true, ElMessage: { error() {}, success() {}, warning() {} },
  InfraJobStatusEnum: { NORMAL: 1, STOP: 2 }, ErpKingdeeConfigApi: {},
  formatDateTimeValue: x => x, toDateTimeValue: x => x,
  ...bindings
}, 'loadJobs, handleSave, form, jobsLoaded, jobsByHandlerName, loadLatestRuns, loadRunningSyncRuns, latestRuns, latestRunsLoaded, latestRunsError, runningSyncRuns, runningRunsLoaded, runningRunsError, formatLatestSyncStatus, waitForSubmittedRun, syncTypes, selectedSyncTypes, handleRunOnce, toDailyCronExpression')

test('ERP rejected run facts invalidate old facts and fail polling rather than reporting absent runs', async () => {
  for (const message of ['denied', 'timeout']) {
    const h = erpHarness({ JobApi: {}, ErpKingdeeSyncApi: { getRunPage: async () => { throw new Error(message) } } })
    assert.equal(h.instance.formatLatestSyncStatus(), '运行事实未知')
    await assert.rejects(h.instance.loadLatestRuns(), new RegExp(message))
    await assert.rejects(h.instance.loadRunningSyncRuns(), new RegExp(message))
    assert.equal(h.instance.latestRunsLoaded.value, false)
    assert.equal(h.instance.runningRunsLoaded.value, false)
    h.instance.latestRuns.value = [{ id: 5, syncType: 'PRODUCT', status: 20 }]
    h.instance.runningSyncRuns.value = [{ id: 6 }]
    h.instance.latestRunsLoaded.value = true; h.instance.runningRunsLoaded.value = true
    await assert.rejects(h.instance.loadLatestRuns(), new RegExp(message))
    await assert.rejects(h.instance.loadRunningSyncRuns(), new RegExp(message))
    assert.deepEqual(h.instance.latestRuns.value, []); assert.deepEqual(h.instance.runningSyncRuns.value, [])
    assert.equal(h.instance.latestRunsLoaded.value, false); assert.equal(h.instance.runningRunsLoaded.value, false)
    assert.equal(h.instance.latestRunsError.value, message); assert.equal(h.instance.runningRunsError.value, message)
    assert.equal(h.instance.formatLatestSyncStatus(), '运行事实查询失败')
    await assert.rejects(h.instance.waitForSubmittedRun('PRODUCT', 5, Date.now()), new RegExp(message))
  }
})

test('ERP only loads consistent strict daily cron and blocks invalid or unknown schedules before any Job write', async () => {
  for (const crons of [['0 0 2 * * MON'], ['0 0 2 * * ?', '0 0 3 * * ?'], ['0 0 24 * * ?'], ['0 60 0 * * ?']]) {
    let index = 0, writes = 0
    const h = erpHarness({ JobApi: {
      getJobPage: async query => ({ list: [{ id: ++index, handlerName: query.handlerName, status: 1, cronExpression: crons[(index-1)%crons.length] }] }),
      updateJob: async () => { writes++ }, updateJobStatus: async () => { writes++ }
    }, ErpKingdeeSyncApi: {} })
    await assert.rejects(h.instance.loadJobs(), /合法且一致/)
    h.instance.form.dailyStartTime = '02:00:00'; await h.instance.handleSave()
    assert.equal(h.instance.jobsLoaded.value, false); assert.equal(writes, 0)
  }
  const h = erpHarness({ JobApi: { updateJob: async () => assert.fail('invalid time writes') }, ErpKingdeeSyncApi: {} })
  h.instance.jobsLoaded.value = true
  for (const time of ['24:00:00', '00:60:00', '00:00:60']) {
    h.instance.form.dailyStartTime = time; assert.equal(h.instance.toDailyCronExpression(time), ''); await h.instance.handleSave()
  }
  assert.equal(h.instance.toDailyCronExpression('23:59:59'), '59 59 23 * * ?')
})


test('ERP valid uniform source loads exactly and a changed incompatible source blocks save without writes', async () => {
  let cron = '5 4 3 * * ?', writes = 0
  const h = erpHarness({ JobApi: {
    getJobPage: async query => ({ list: [{ id: 7, handlerName: query.handlerName, status: 1, cronExpression: cron }] }),
    updateJob: async () => { writes++ }, updateJobStatus: async () => { writes++ }
  }, ErpKingdeeSyncApi: {} })
  await h.instance.loadJobs()
  assert.equal(h.instance.jobsLoaded.value, true)
  assert.equal(h.instance.form.dailyStartTime, '03:04:05')
  cron = '0 0 2 * * MON'
  await h.instance.handleSave()
  assert.equal(writes, 0, 'write-time fresh source facts must still satisfy the unchanged contract')
})


test('MES only treats real cancel/close actions silently; other confirmation rejection surfaces and never writes', async () => {
  for (const rejection of ['cancel', 'close', new Error('confirm broken'), 'unexpected rejection']) {
    const h = component('src/views/mes/pro/edhr-config/components/RecordbookGlobalSetting.vue', {
      hasExplicitConfigPermission: () => true,
      ElMessageBox: { confirm: async () => { throw rejection } },
      getEdhrRecordbookGlobalSetting: async () => ({ enabled: true, configKey: 'mes.edhr.recordbook.global.enabled' }),
      updateEdhrRecordbookGlobalSetting: async () => assert.fail('rejected confirmation cannot write')
    }, 'loadSetting, handleToggleClick, saving, settingEnabled')
    await h.instance.loadSetting(); await h.instance.handleToggleClick()
    assert.equal(h.instance.saving.value, false); assert.equal(h.instance.settingEnabled.value, true)
    if (rejection === 'cancel' || rejection === 'close') assert.equal(h.errors.length, 0)
    else { assert.equal(h.errors.length, 1); assert.match(h.errors[0], /confirm broken|确认失败/) }
  }
})

test('ERP manual once preserves selected types despite incompatible schedules; save still writes no jobs', async () => {
  for (const mode of ['weekly', 'different', 'unselected-weekly']) {
    const submitted = [], messages = [], writes = []
    let selectedHandler
    const h = erpHarness({
      ElMessage: { error: text => messages.push(['error', text]), success: text => messages.push(['success', text]), warning() {} },
      JobApi: {
        getJobPage: async query => ({ list: [{ id: 1, handlerName: query.handlerName, status: 1,
          cronExpression: mode === 'weekly' || (mode === 'unselected-weekly' && query.handlerName !== selectedHandler) ? '0 0 2 * * MON' : query.handlerName === selectedHandler ? '0 0 2 * * ?' : '0 0 3 * * ?' }] }),
        updateJob: async value => writes.push(value), updateJobStatus: async value => writes.push(value)
      },
      ErpKingdeeSyncApi: { runIncrementalSync: async type => submitted.push(type), getRunPage: async () => ({ list: [] }) }
    })
    const selected = h.instance.syncTypes[0]; selectedHandler = selected.handlerName
    h.instance.selectedSyncTypes.value = [selected.syncType]
    await h.instance.handleRunOnce()
    assert.deepEqual(submitted, [selected.syncType]); assert.equal(writes.length, 0)
    assert.ok(messages.some(([kind, text]) => kind === 'success' && text.includes('已提交')))
    assert.ok(messages.some(([kind, text]) => kind === 'error' && text.includes('任务已提交，但')))
    assert.equal(messages.some(([, text]) => text.includes('立即执行一次失败')), false)
    h.instance.jobsLoaded.value = true; h.instance.form.dailyStartTime = '02:00:00'
    await h.instance.handleSave(); assert.equal(writes.length, 0)
  }
})
