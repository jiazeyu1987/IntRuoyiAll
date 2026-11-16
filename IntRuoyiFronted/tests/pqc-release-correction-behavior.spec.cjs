const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const filename = path.join(__dirname, '../src/views/Profile/components/PqcReleaseCorrection.vue')
const script = compileScript(parse(fs.readFileSync(filename, 'utf8'), { filename }).descriptor, { id: 'pqc-correction' }).content
const js = ts.transpileModule(script, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
const flush = () => new Promise(resolve => setImmediate(resolve))
const detail = (id, extra = {}) => ({ workTaskId: id, applicationId: '50', activeOrderId: '20', expectedVersion: 3,
  taskStatus: 'TODO', rejectedDossierRevision: 0, currentDossierRevision: 1, writable: true, ...extra })
function instance(api, id = '101') {
  const route = vue.reactive({ query: { releaseCorrectionTaskId: id }, path: '/user/profile' })
  const unmount = []
  const modules = {
    vue: { ...vue, onBeforeUnmount: callback => unmount.push(callback) },
    'vue-router': { useRoute: () => route, useRouter: () => ({ replace: async () => {} }) },
    '@/utils/formatTime': { formatDate: String },
    '@/store/modules/profileWorkbenchTodoBadge': { useProfileWorkbenchTodoBadgeStore: () => ({ refreshTodoTotal: async () => {} }) },
    '@/api/mes/pro/productionRelease/correction': { getPqcReleaseCorrectionFiles: async () => ({ categories: [] }), ...api }
  }
  const module = { exports: {} }
  new Function('require', 'module', 'exports', js)(name => {
    if (!(name in modules)) throw new Error(`Unexpected dependency: ${name}`)
    return modules[name]
  }, module, module.exports)
  const scope = vue.effectScope()
  const app = scope.run(() => module.exports.default.setup({}, { expose() {} }))
  return { app, route, dispose: () => { unmount.forEach(callback => callback()); scope.stop() } }
}
test('stale task reads cannot replace the next selected correction task', async () => {
  let old
  const context = instance({ getPqcReleaseCorrection: id => id === '101'
    ? new Promise(resolve => { old = resolve }) : Promise.resolve(detail(id)) })
  context.route.query.releaseCorrectionTaskId = '102'
  await vue.nextTick(); await flush()
  assert.equal(context.app.task.value.workTaskId, '102')
  old(detail('101')); await flush()
  assert.equal(context.app.task.value.workTaskId, '102')
  context.dispose()
})
test('resubmit needs changed dossier, preserves request identity on retry, and renders authoritative completion', async () => {
  const calls = []
  let done = false
  const context = instance({ getPqcReleaseCorrection: async id => detail(id, done ? { taskStatus: 'DONE', writable: false, nextApplicationId: '51' } : {}),
    resubmitPqcReleaseCorrection: async request => {
      calls.push(request)
      if (calls.length === 1) throw new Error('request outcome unavailable')
      done = true
      return { applicationId: '51' }
    } })
  await flush()
  context.app.correctionNote.value = '已补齐成品检文件'
  context.app.task.value.currentDossierRevision = 0
  assert.equal(context.app.canResubmit.value, false)
  await context.app.resubmit()
  assert.equal(calls.length, 0)
  context.app.task.value.currentDossierRevision = 1
  await context.app.resubmit()
  assert.match(context.app.error.value, /request outcome unavailable/)
  await context.app.resubmit()
  assert.equal(calls[0].idempotencyKey, calls[1].idempotencyKey)
  assert.equal(calls[1].expectedVersion, 3)
  assert.equal(context.app.task.value.taskStatus, 'DONE')
  assert.equal(context.app.task.value.nextApplicationId, '51')
  assert.equal(context.app.canResubmit.value, false)
  context.dispose()
})
test('closing the correction component invalidates its in-flight read', async () => {
  let resolve
  const context = instance({ getPqcReleaseCorrection: () => new Promise(done => { resolve = done }) })
  context.dispose()
  resolve(detail('101')); await flush()
  assert.equal(context.app.task.value, undefined)
})
