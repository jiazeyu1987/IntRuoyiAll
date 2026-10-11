const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')

const sourcePath = path.resolve(__dirname, '../../src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const source = parse(fs.readFileSync(sourcePath, 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('actual-batch-detail-selection.ts', source, ts.ScriptTarget.Latest, true)
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

function harness(options = {}) {
  const review = deferred()
  const workbenchResponse = deferred()
  const openedTasks = [], previewRequests = []
  const assemblyPreview = { taskId: 12661, formViewModel: { report: 'assembly' } }
  const tasks = [{ id: 12655, report: 'incoming' }, { id: 12661, report: 'assembly' }]
  const context = {
    ...vue,
    submittedExecutionReviews: vue.ref([]),
    sortedTasks: vue.ref(tasks),
    assertBatchExecutionId: () => '900000001252',
    getEdhrBatchTaskPreview: async (_, taskId) => {
      previewRequests.push(taskId)
      return options.previewResponses?.[taskId]?.promise || { taskId, formViewModel: { report: tasks.find(x => x.id === taskId).report } }
    },
    getEdhrBatchReviewTimeline: () => review.promise,
    getEdhrBatchWorkbench: () => workbenchResponse.promise,
    resolveDetailFocus: () => options.focus || '',
    hasBatchLevelWorkTaskRouteContext: () => false,
    resolveRouteQueryTaskSelection: () => options.openRouteForm ? tasks[0] : undefined,
    resolveDefaultTaskSelection: () => tasks[0],
    route: { query: { openRouteForm: options.openRouteForm ? '1' : '0' } },
    releaseUiContextKey: () => JSON.stringify(['timeline-selection-test-session', context.route.query]),
    parseRouteQueryText: value => String(value || ''),
    canAutoOpenRouteFormTask: () => true,
    resolveRouteFormAutoOpenKey: () => '900000001252:12655',
    handleOpenTask: async task => { openedTasks.push(task.id) },
    processDetailDialogVisible: vue.ref(false),
    shouldLoadTaskPreview: () => true,
    canOpenTask: () => false,
    isSameRouteFormTask: (task, execution) => task.executionId === execution.executionId,
    resolveErrorMessage: (error) => error.message,
    loadReleaseCheckItems: async () => { await options.releaseResponse?.promise },
    loading: vue.ref(false), loadError: vue.ref(''), secondaryLoadError: vue.ref(''),
    detail: vue.ref(), workbench: vue.ref(),
    cancelDeferredBatchDetailSecondaryLoad: () => {},
    getEdhrBatchExecution: async () => ({ id: '900000001253' }),
    loadInlineActiveOrderSubmissionDetail: async () => {},
    syncSpecialNodePendingAttachmentsFromDetail: () => {},
    loadProductionReleaseReportCandidates: async () => {},
    deferInitialBatchDetailSecondaryLoad: () => {},
    requestAnimationFrame: callback => { context.frames.push(callback); return context.frames.length },
    cancelAnimationFrame: () => {},
    frames: []
  }
  const names = ['selectedExecutionId', 'selectedTaskId', 'selectedTaskPreview', 'taskPreviewLoading', 'taskPreviewError',
    'selectedReleaseStep', 'viewedReleaseStageKey', 'reviewTimeline', 'reviewLoading', 'reviewError',
    'taskPreviewRequestSerial', 'taskPreviewFrameId', 'cancelDeferredTaskPreviewLoad', 'clearTaskPreview',
    'loadTaskPreview', 'deferTaskPreviewLoad', 'selectProcessTask', 'applyReleaseProcessSelection',
    'selectReleaseProcess', 'loadReviewTimeline', 'applyInitialBatchTaskSelection', 'applyRouteFocus',
    'autoOpenRouteFormFromRoute', 'routeFormAutoOpenKey', 'loadBatchDetailSecondaryData',
    'loadDetail', 'batchDetailRequestSerial', 'batchTaskSelectionChanged', 'isStaleBatchDetailRequest']
  const statements = ast.statements.filter(statement => ts.isVariableStatement(statement) && statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(ast))))
  assert.equal(statements.length, names.length, 'execute the actual component state/functions')
  const code = ts.transpileModule(statements.map(x => x.getText(ast)).join('\n'), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.None } }).outputText
  const state = Function(...Object.keys(context), `${code};batchDetailRequestSerial=1;return {${names.join(',')}}`)(...Object.values(context))
  state.selectedTaskId.value = String(tasks[0].id)
  return { ...state, review, tasks, assemblyPreview, workbenchResponse, openedTasks, previewRequests,
    processDetailDialogVisible: context.processDetailDialogVisible, workbench: context.workbench,
    secondaryLoadError: context.secondaryLoadError, frames: context.frames }
}

test('late timeline success keeps the clicked assembly task and its exact readonly preview', async () => {
  const h = harness(), pending = h.loadReviewTimeline(1)
  h.selectProcessTask(h.tasks[1]); await Promise.resolve(); await Promise.resolve()
  assert.equal(h.selectedTaskPreview.value.taskId, 12661)
  h.review.resolve({ executionReviews: [] }); await pending
  assert.equal(h.selectedTaskId.value, '12661')
  assert.equal(h.selectedTaskPreview.value.taskId, 12661)
  assert.equal(h.frames.length, 0, 'must not defer a default incoming preview over assembly')
  assert.equal(h.reviewLoading.value, false)
})

test('selection made before the timeline starts is also preserved', async () => {
  const h = harness(); h.selectProcessTask(h.tasks[1]); await Promise.resolve()
  const pending = h.loadReviewTimeline(1); h.review.resolve({ executionReviews: [] }); await pending
  assert.equal(h.selectedTaskId.value, '12661')
  assert.equal(h.selectedTaskPreview.value.taskId, 12661)
})

test('timeline rejection exposes its error without clearing a newer selected preview', async () => {
  const h = harness(), pending = h.loadReviewTimeline(1)
  h.selectProcessTask(h.tasks[1]); await Promise.resolve()
  h.review.reject(new Error('authoritative timeline 403')); await pending
  assert.equal(h.reviewError.value, 'authoritative timeline 403')
  assert.equal(h.selectedTaskId.value, '12661')
  assert.equal(h.selectedTaskPreview.value.taskId, 12661)
  assert.equal(h.reviewLoading.value, false)
})

test('choosing the release process while the timeline loads keeps release selected', async () => {
  const h = harness(), pending = h.loadReviewTimeline(1)
  h.selectReleaseProcess(); h.review.resolve({ executionReviews: [] }); await pending
  assert.equal(h.selectedReleaseStep.value, true)
  assert.equal(h.selectedTaskId.value, '')
  assert.equal(h.frames.length, 0)
})

test('without an explicit choice the initial default selection and preview still load', async () => {
  const h = harness(), pending = h.loadReviewTimeline(1)
  h.review.resolve({ executionReviews: [] }); await pending
  assert.equal(h.selectedTaskId.value, '12655')
  assert.equal(h.frames.length, 1)
})

test('a fresh detail load resets the explicit-choice guard and restores initial selection', async () => {
  const h = harness(); h.selectProcessTask(h.tasks[1]); await Promise.resolve()
  await h.loadDetail()
  const pending = h.loadReviewTimeline(2); h.review.resolve({ executionReviews: [] }); await pending
  assert.equal(h.selectedTaskId.value, '12655')
  assert.equal(h.frames.length, 1)
  assert.equal(h.selectedTaskPreview.value, undefined)
})

for (const choice of ['process', 'release']) {
  for (const rejected of [false, true]) {
    test(`late secondary timeline ${rejected ? 'error' : 'success'} cannot auto-open a route form after an explicit ${choice} choice`, async () => {
      const h = harness({ focus: 'approval', openRouteForm: true })
      h.applyInitialBatchTaskSelection()
      const pending = h.loadBatchDetailSecondaryData('900000001252', 1)
      h.workbenchResponse.resolve({ batchExecutionId: '900000001252' })
      await Promise.resolve(); await Promise.resolve()
      if (choice === 'process') h.selectProcessTask(h.tasks[1])
      else h.selectReleaseProcess()
      await Promise.resolve(); await Promise.resolve()
      if (rejected) h.review.reject(new Error('authoritative timeline 403'))
      else h.review.resolve({ executionReviews: [] })
      await pending
      assert.deepEqual(h.openedTasks, [], 'late automatic form opening must not navigate away')
      assert.equal(h.selectedTaskId.value, choice === 'process' ? '12661' : '')
      assert.equal(h.selectedReleaseStep.value, choice === 'release')
      assert.equal(h.selectedTaskPreview.value?.taskId, choice === 'process' ? 12661 : undefined)
      if (rejected) assert.equal(h.reviewError.value, 'authoritative timeline 403')
    })
  }
}

for (const focus of ['', 'precheck', 'approval']) {
  test(`initial automatic selection with focus=${focus || 'default'} still permits the route form entry`, async () => {
    const h = harness({ focus, openRouteForm: true })
    h.applyInitialBatchTaskSelection()
    const pending = h.loadBatchDetailSecondaryData('900000001252', 1)
    h.workbenchResponse.resolve({ batchExecutionId: '900000001252' })
    h.review.resolve({ executionReviews: [] }); await pending
    assert.deepEqual(h.openedTasks, [12655], 'automatic selection is not a user choice')
    if (focus) assert.equal(h.viewedReleaseStageKey.value, focus === 'precheck' ? 'precheck' : 'release-approval')
    await h.autoOpenRouteFormFromRoute()
    assert.deepEqual(h.openedTasks, [12655], 'the same route entry opens only once')
  })
}

test('process route focus opens its dialog initially but does not reopen it after a user choice', async () => {
  const initial = harness({ focus: 'process' })
  initial.applyInitialBatchTaskSelection(); initial.applyRouteFocus()
  assert.equal(initial.processDetailDialogVisible.value, true)
  for (const choice of ['process', 'release']) {
    const h = harness({ focus: 'process' })
    h.applyInitialBatchTaskSelection()
    const pending = h.loadBatchDetailSecondaryData('900000001252', 1)
    if (choice === 'process') h.selectProcessTask(h.tasks[1])
    else h.selectReleaseProcess()
    h.workbenchResponse.resolve({ batchExecutionId: '900000001252' })
    h.review.resolve({ executionReviews: [] }); await pending
    assert.equal(h.processDetailDialogVisible.value, false, 'late focus must not reopen a dismissed context')
  }
})

test('fresh detail resets a previous user choice and allows initial focus and automatic form entry again', async () => {
  const h = harness({ focus: 'precheck', openRouteForm: true })
  h.selectProcessTask(h.tasks[1]); await Promise.resolve()
  await h.loadDetail()
  assert.equal(h.selectedReleaseStep.value, true)
  assert.equal(h.viewedReleaseStageKey.value, 'precheck')
  assert.equal(h.selectedTaskPreview.value, undefined)
  const pending = h.loadBatchDetailSecondaryData('900000001252', 2)
  h.workbenchResponse.resolve({ batchExecutionId: '900000001252' })
  h.review.resolve({ executionReviews: [] }); await pending
  assert.deepEqual(h.openedTasks, [12655])
})

test('a late incoming preview cannot replace the newer assembly preview or its loading state', async () => {
  const incoming = deferred(), assembly = deferred()
  const h = harness({ previewResponses: { 12655: incoming, 12661: assembly } })
  const initialPreview = h.loadTaskPreview(h.tasks[0])
  h.selectProcessTask(h.tasks[1])
  incoming.resolve({ taskId: 12655, formViewModel: { report: 'incoming' } }); await initialPreview
  assert.equal(h.selectedTaskPreview.value, undefined)
  assert.equal(h.taskPreviewLoading.value, true, 'older finally must not stop the newer request')
  assembly.resolve(h.assemblyPreview); await Promise.resolve(); await Promise.resolve()
  assert.equal(h.selectedTaskPreview.value.taskId, 12661)
  assert.equal(h.taskPreviewLoading.value, false)
  const pending = h.loadReviewTimeline(1)
  h.review.resolve({ executionReviews: [] }); await pending
  assert.equal(h.selectedTaskPreview.value.taskId, 12661)
})

test('an older secondary load waiting for release checks cannot auto-open after a new detail resets selection', async () => {
  const releaseResponse = deferred()
  const h = harness({ focus: 'precheck', openRouteForm: true, releaseResponse })
  h.applyInitialBatchTaskSelection()
  const pending = h.loadBatchDetailSecondaryData('900000001252', 1)
  h.workbenchResponse.resolve({ batchExecutionId: '900000001252' })
  h.review.resolve({ executionReviews: [] })
  await new Promise(resolve => setImmediate(resolve))
  await h.loadDetail()
  releaseResponse.resolve(); await pending
  assert.deepEqual(h.openedTasks, [], 'the old detail pipeline must retain its request identity across every await')
  assert.equal(h.selectedReleaseStep.value, true)
  assert.equal(h.viewedReleaseStageKey.value, 'precheck')
})

test('late workbench rejection exposes its error without clearing an explicitly selected assembly preview', async () => {
  const h = harness()
  h.applyInitialBatchTaskSelection()
  const pending = h.loadBatchDetailSecondaryData('900000001252', 1)
  h.selectProcessTask(h.tasks[1]); await Promise.resolve(); await Promise.resolve()
  assert.equal(h.selectedTaskPreview.value.taskId, 12661)
  h.workbenchResponse.reject(new Error('authoritative workbench403')); await pending
  assert.equal(h.selectedTaskId.value, '12661')
  assert.equal(h.selectedTaskPreview.value?.taskId, 12661)
  assert.equal(h.selectedReleaseStep.value, false)
  assert.equal(h.workbench.value, undefined)
  assert.equal(h.reviewTimeline.value, undefined)
  assert.equal(h.secondaryLoadError.value, 'authoritative workbench403')
  assert.deepEqual(h.openedTasks, [])
})

test('workbench rejection before an explicit choice still clears the automatic preview and shows its error', async () => {
  const h = harness()
  h.applyInitialBatchTaskSelection()
  await h.loadTaskPreview(h.tasks[0])
  assert.equal(h.selectedTaskPreview.value.taskId, 12655)
  const pending = h.loadBatchDetailSecondaryData('900000001252', 1)
  h.workbenchResponse.reject(new Error('authoritative workbench403')); await pending
  assert.equal(h.selectedTaskId.value, '12655')
  assert.equal(h.selectedTaskPreview.value, undefined)
  assert.equal(h.taskPreviewLoading.value, false)
  assert.equal(h.workbench.value, undefined)
  assert.equal(h.reviewTimeline.value, undefined)
  assert.equal(h.secondaryLoadError.value, 'authoritative workbench403')
})
