const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const vue = require('vue')
const { renderToString } = require('node:module').createRequire(require.resolve('vue'))('@vue/server-renderer')
const ts = require('typescript')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'), 'utf8')
const ref = value => ({ value })

const statusSource = fs.readFileSync(path.resolve(__dirname, '../../src/api/mes/pro/edhr/batchExecution.ts'), 'utf8')
const statusContext = Object.fromEntries([...statusSource.matchAll(/export const (EDHR_BATCH_STATUS_\w+) = (\d+)/g)].map(match => [match[1], Number(match[2])]))
const statusStart = source.indexOf('const resolveBatchStatusLabel =')
const statusEnd = source.indexOf('\nconst ', statusStart + 1)
assert.ok(statusStart >= 0 && statusEnd > statusStart)
vm.createContext(statusContext)
vm.runInContext(ts.transpileModule(source.slice(statusStart, statusEnd) + '\nglobalThis.statusLabel = resolveBatchStatusLabel', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, statusContext)

test('review load failures remain visible for normal and release-focused entries', async () => {
  const start = source.indexOf('const loadReviewTimeline = async')
  const end = source.indexOf('const cancelDeferredBatchDetailSecondaryLoad', start)
  for (const [focus, workTask] of [['', false], ['precheck', false], ['approval', false], ['', true]]) {
    let reject
    const request = new Promise((_, fail) => { reject = fail })
    const context = {
      isStaleBatchDetailRequest: () => false,
      reviewLoading: ref(false), reviewError: ref(''), reviewTimeline: ref({ batchEvents: [] }),
      selectedExecutionId: ref(''), selectedTaskId: ref(''), selectedReleaseStep: ref(false), viewedReleaseStageKey: ref(''),
      getEdhrBatchReviewTimeline: () => request, assertBatchExecutionId: () => 1,
      resolveDetailFocus: () => focus, hasBatchLevelWorkTaskRouteContext: () => workTask,
      selectReleaseProcess: () => {}, resolveRouteQueryTaskSelection: () => undefined,
      resolveDefaultTaskSelection: () => undefined, clearTaskPreview: () => {},
      resolveErrorMessage: error => error.message
    }
    vm.createContext(context)
    vm.runInContext(ts.transpileModule(source.slice(start, end) + '\nglobalThis.run = loadReviewTimeline', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
    const pending = context.run(1)
    assert.equal(context.reviewLoading.value, true)
    reject(new Error('timeline unavailable'))
    await pending
    assert.equal(context.reviewError.value, 'timeline unavailable', `focus=${focus}, workTask=${workTask}`)
    assert.equal(context.reviewLoading.value, false)
    assert.equal(context.reviewTimeline.value, undefined)
  }
})

const renderPane = async (state) => {
  const match = source.match(/<el-tab-pane label="批次信息" name="batch">([\s\S]*?)<\/el-tab-pane>/)
  assert.ok(match, 'Reachable trace drawer must contain batch information pane')
  const app = vue.createSSRApp({
    render: vue.compile(match[1]),
    setup: () => ({ reviewLoading: false, reviewError: '', reviewTimeline: { batchEvents: [] }, formatReviewTime: value => `TIME:${value}`, resolveBatchStatusLabel: statusContext.statusLabel, ...state })
  })
  for (const name of ['ElDescriptions', 'ElDescriptionsItem']) {
    app.component(name, { props: ['label'], setup: (props, { slots }) => () => vue.h('div', [props.label, slots.default?.()]) })
  }
  app.component('ElAlert', { props: ['title'], setup: props => () => vue.h('div', props.title) })
  app.component('ElEmpty', { props: ['description'], setup: props => () => vue.h('div', props.description) })
  app.component('ElSkeleton', { setup: () => () => vue.h('div', 'LOADING') })
  return renderToString(app)
}
test('batch pane renders recorded facts and explicit missing fields', async () => {
  const event = { batchExecutionId: 41, batchExecutionCode: 'BATCH-X', status: 30, aggregateHash: 'hash-X', createTime: 'created-X', closedBy: 71, closedAt: 'closed-X', closeSignatureId: 81, rejectedBy: 72, rejectedAt: 'rejected-X', rejectSignatureId: 82, rejectReason: 'reason-X' }
  const html = await renderPane({ reviewTimeline: { batchEvents: [event] } })
  for (const [field, value] of Object.entries(event)) {
    if (field !== 'status') assert.ok(html.includes(String(value)), `Missing formal fact ${field}`)
  }
  assert.match(html, /已关闭/)
  const missing = await renderPane({ reviewTimeline: { batchEvents: [{}] } })
  assert.ok((missing.match(/未记录/g) || []).length >= 12)
})
test('batch pane distinguishes loading, failure and successful empty response', async () => {
  assert.match(await renderPane({ reviewLoading: true }), /LOADING/)
  const failed = await renderPane({ reviewError: 'timeline unavailable' })
  assert.match(failed, /timeline unavailable/)
  assert.doesNotMatch(failed, /暂无批次信息/)
  assert.match(await renderPane({}), /暂无批次信息/)
})


test('batch status is readable for rejected and voided records', async () => {
  for (const [status, label] of [[50, '质量已拒收'], [60, '已作废']]) {
    assert.ok((await renderPane({ reviewTimeline: { batchEvents: [{ status }] } })).includes(label))
  }
})
