const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const source = fs.readFileSync(
  path.resolve(
    __dirname,
    '../../src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'
  ),
  'utf8'
)
const deferred = () => {
  let resolve, reject
  const promise = new Promise((a, b) => {
    resolve = a
    reject = b
  })
  return { promise, resolve, reject }
}
function harness() {
  const candidates = [],
    creates = [],
    dispositions = [],
    pages = [],
    uploads = [],
    messages = [],
    unmount = []
  const scope = vue.effectScope()
  const context = {
    ...vue,
    Error,
    defineOptions: () => {},
    useRoute: () => ({ name: 'test', query: {} }),
    useMessage: () => ({
      success: (v) => messages.push(['success', v]),
      error: (v) => messages.push(['error', v])
    }),
    onMounted: () => {},
    onBeforeUnmount: (fn) => unmount.push(fn),
    parseExactIntegerJson: JSON.parse,
    parsePositiveRouteQueryId: () => undefined,
    DISPOSITION_REWORK: 'REWORK',
    DISPOSITION_VOID: 'VOID',
    DISPOSITION_CONCESSION_RELEASE: 'CONCESSION_RELEASE',
    REVIEW_STATUS_PENDING_REVIEW: 'PENDING_REVIEW',
    getNonconformanceReviewActiveOrderList: () => {
      const d = deferred()
      candidates.push(d)
      return d.promise
    },
    createNonconformanceReview: () => {
      const d = deferred()
      creates.push(d)
      return d.promise
    },
    disposeNonconformanceReview: (body) => {
      const d = deferred()
      dispositions.push({ body, ...d })
      return d.promise
    },
    getNonconformanceReviewPage: () => {
      const d = deferred()
      pages.push(d)
      return d.promise
    },
    uploadNonconformanceReviewMaterial: () => {
      const d = deferred()
      uploads.push(d)
      return d.promise
    }
  }
  const script = source
    .match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
    .replace(/^import[\s\S]*?from ['"][^'"]+['"]\r?\n/gm, '')
  scope.run(() =>
    vm.runInNewContext(
      ts.transpileModule(
        script +
          '\nglobalThis.api={openCreateDialog,closeCreateDialog,createDialogVisible,createLoading,activeOrderCandidatesLoading,submitCreateReview,entryForm,selectedActiveOrderId,openReviewDialog,reviewDialogVisible,selectedReview,disposeForm,disposeLoading,errorText,handleDispose,loadReviews,reviews,listLoading,handleMaterialUpload,materialUploadsPending};',
        { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.None } }
      ).outputText,
      context
    )
  )
  return {
    ...context.api,
    creates,
    candidates,
    dispositions,
    pages,
    uploads,
    messages,
    unmount: () => {
      unmount.forEach((fn) => fn())
      scope.stop()
    }
  }
}
const review = (id) => ({
  id,
  reviewStatus: 'PENDING_REVIEW',
  reviewMaterialsJson: JSON.stringify({
    activeMaterials: [{ url: 'file/' + id, fileId: id, fileName: 'evidence.pdf' }]
  })
})
async function open(h, id) {
  h.openReviewDialog(review(id))
  await vue.nextTick()
  h.disposeForm.reviewOpinion = 'approved ' + id
  h.disposeForm.signaturePassword = 'secret'
}
async function settle(h, index = 0) {
  h.dispositions[index].resolve({
    ...review(h.dispositions[index].body.id),
    reviewStatus: 'CLOSED'
  })
  await new Promise(setImmediate)
  if (h.pages.length) h.pages.at(-1).resolve({ list: [], total: 0 })
}
// BDD: Given A is submitting, When B opens or A is reopened, Then obsolete callbacks cannot write the current dialog.
test('A success cannot replace B or refresh its list', async () => {
  const h = harness()
  await open(h, 1)
  const p = h.handleDispose('REWORK')
  await open(h, 2)
  await settle(h)
  await p
  assert.equal(h.selectedReview.value.id, 2)
  assert.equal(h.messages.length, 0)
  assert.equal(h.pages.length, 0)
  h.unmount()
})
test('close and reopen same A invalidates former request', async () => {
  const h = harness()
  await open(h, 1)
  const p = h.handleDispose('REWORK')
  h.reviewDialogVisible.value = false
  await open(h, 1)
  await settle(h)
  await p
  assert.equal(h.selectedReview.value.reviewStatus, 'PENDING_REVIEW')
  assert.equal(h.messages.length, 0)
  h.unmount()
})
test('old failure and finally cannot clear B loading or set errors', async () => {
  const h = harness()
  await open(h, 1)
  const a = h.handleDispose('REWORK')
  await open(h, 2)
  assert.equal(h.disposeLoading.value, false)
  const b = h.handleDispose('VOID')
  h.dispositions[0].reject(Error('old failure'))
  await a
  assert.equal(h.errorText.value, '')
  assert.equal(h.messages.length, 0)
  assert.equal(h.disposeLoading.value, true)
  await settle(h, 1)
  await b
  h.unmount()
})
test('normal success refreshes current review and clears busy', async () => {
  const h = harness()
  await open(h, 1)
  const p = h.handleDispose('REWORK')
  await settle(h)
  await p
  assert.equal(h.selectedReview.value.reviewStatus, 'CLOSED')
  assert.equal(h.messages[0][0], 'success')
  assert.equal(h.pages.length, 1)
  assert.equal(h.disposeLoading.value, false)
  h.unmount()
})
test('normal error remains visible', async () => {
  const h = harness()
  await open(h, 1)
  const p = h.handleDispose('REWORK')
  h.dispositions[0].reject(Error('current failure'))
  await p
  assert.equal(h.errorText.value, 'current failure')
  assert.equal(h.messages[0][0], 'error')
  assert.equal(h.disposeLoading.value, false)
  h.unmount()
})
test('same-tick duplicate click submits only once', async () => {
  const h = harness()
  await open(h, 1)
  const a = h.handleDispose('REWORK')
  const b = h.handleDispose('VOID')
  assert.equal(h.dispositions.length, 1)
  await settle(h)
  await Promise.all([a, b])
  h.unmount()
})
test('old list response cannot replace B detail or clear current list loading', async () => {
  const h = harness()
  await open(h, 1)
  const a = h.loadReviews()
  await open(h, 2)
  const b = h.loadReviews()
  h.pages[0].resolve({ list: [{ ...review(2), reviewStatus: 'CLOSED' }], total: 1 })
  await a
  assert.equal(h.selectedReview.value.reviewStatus, 'PENDING_REVIEW')
  assert.equal(h.listLoading.value, true)
  h.pages[1].resolve({ list: [review(2)], total: 1 })
  await b
  h.unmount()
})
test('old list error cannot set current page error', async () => {
  const h = harness()
  await open(h, 1)
  const a = h.loadReviews()
  await open(h, 2)
  h.pages[0].reject(Error('obsolete list'))
  await a
  assert.equal(h.errorText.value, '')
  h.unmount()
})
test('upload callbacks preserve B materials and pending counter', async () => {
  const h = harness()
  await open(h, 1)
  const a = h.handleMaterialUpload({ target: { files: [{}], value: 'x' } })
  await open(h, 2)
  const b = h.handleMaterialUpload({ target: { files: [{}], value: 'y' } })
  h.uploads[0].reject(Error('old upload'))
  await a
  assert.equal(h.materialUploadsPending.value, 1)
  assert.equal(h.messages.length, 0)
  h.uploads[1].resolve({ url: 'new', fileId: 33, fileName: 'new.pdf' })
  await b
  assert.deepEqual(Array.from(h.disposeForm.reviewMaterialUrls), ['file/2', 'new'])
  assert.equal(h.materialUploadsPending.value, 0)
  h.unmount()
})
test('unmount invalidates pending disposition', async () => {
  const h = harness()
  await open(h, 1)
  const p = h.handleDispose('REWORK')
  h.unmount()
  await settle(h)
  await p
  assert.equal(h.messages.length, 0)
  assert.equal(h.pages.length, 0)
})

test('creation completion cannot replace a subsequently opened review or clear its form', async () => {
  const h = harness()
  h.createDialogVisible.value = true
  h.selectedActiveOrderId.value = 10
  h.entryForm.nonconformanceReason = 'reason'
  const p = h.submitCreateReview()
  await open(h, 2)
  h.creates[0].resolve(review(1))
  await new Promise(setImmediate)
  if (h.pages.length) h.pages[0].resolve({ list: [], total: 0 })
  await p
  assert.equal(h.selectedReview.value.id, 2)
  assert.equal(h.disposeForm.reviewOpinion, 'approved 2')
  assert.equal(h.disposeForm.signaturePassword, 'secret')
  assert.equal(h.messages.length, 0)
  assert.equal(h.pages.length, 0)
  h.unmount()
})
test('old success finally cannot end B submission', async () => {
  const h = harness()
  await open(h, 1)
  const a = h.handleDispose('REWORK')
  await open(h, 2)
  const b = h.handleDispose('VOID')
  h.dispositions[0].resolve(review(1))
  await a
  assert.equal(h.disposeLoading.value, true)
  assert.equal(h.pages.length, 0)
  await settle(h, 1)
  await b
  h.unmount()
})
test('disposition-triggered refresh is invalidated on switch', async () => {
  const h = harness()
  await open(h, 1)
  const a = h.handleDispose('REWORK')
  h.dispositions[0].resolve(review(1))
  await new Promise(setImmediate)
  await open(h, 2)
  h.pages[0].resolve({ list: [{ ...review(2), reviewStatus: 'CLOSED' }], total: 1 })
  await a
  assert.equal(h.selectedReview.value.reviewStatus, 'PENDING_REVIEW')
  assert.equal(h.disposeForm.reviewOpinion, 'approved 2')
  h.unmount()
})
test('upload from closed and reopened A cannot append material', async () => {
  const h = harness()
  await open(h, 1)
  const p = h.handleMaterialUpload({ target: { files: [{}], value: 'x' } })
  h.reviewDialogVisible.value = false
  await open(h, 1)
  h.uploads[0].resolve({ url: 'obsolete', fileId: 33, fileName: 'old.pdf' })
  await p
  assert.deepEqual(Array.from(h.disposeForm.reviewMaterialUrls), ['file/1'])
  assert.equal(h.materialUploadsPending.value, 0)
  h.unmount()
})
test('unmount invalidates list response', async () => {
  const h = harness()
  const p = h.loadReviews()
  h.unmount()
  h.pages[0].resolve({ list: [review(1)], total: 1 })
  await p
  assert.equal(h.reviews.value.length, 0)
})

// BDD: Given create is pending, When another review opens before failure, Then its errors remain untouched.
test('old creation failure cannot write B error or toast', async () => {
  const h = harness()
  h.createDialogVisible.value = true
  h.selectedActiveOrderId.value = 10
  h.entryForm.nonconformanceReason = 'reason'
  const p = h.submitCreateReview()
  await open(h, 2)
  h.creates[0].reject(Error('old create failure'))
  await p
  assert.equal(h.errorText.value, '')
  assert.equal(h.messages.length, 0)
  h.unmount()
})
test('current creation failure remains visible', async () => {
  const h = harness()
  h.createDialogVisible.value = true
  h.selectedActiveOrderId.value = 10
  h.entryForm.nonconformanceReason = 'reason'
  const p = h.submitCreateReview()
  h.creates[0].reject(Error('current create failure'))
  await p
  assert.equal(h.errorText.value, 'current create failure')
  assert.equal(h.messages[0][0], 'error')
  h.unmount()
})
test('current creation success refreshes list and reports success', async () => {
  const h = harness()
  h.createDialogVisible.value = true
  h.selectedActiveOrderId.value = 10
  h.entryForm.nonconformanceReason = 'reason'
  const p = h.submitCreateReview()
  h.creates[0].resolve(review(1))
  await new Promise(setImmediate)
  assert.equal(h.pages.length, 1)
  h.pages[0].resolve({ list: [review(1)], total: 1 })
  await p
  assert.equal(h.messages[0][0], 'success')
  assert.equal(h.reviews.value[0].id, 1)
  h.unmount()
})

async function newCreate(h, id) {
  const p = h.openCreateDialog()
  h.candidates.at(-1).resolve([{ id }])
  await p
  h.entryForm.nonconformanceReason = 'reason ' + id
}
test('old create success cannot close reopened creation or clear B busy', async () => {
  const h = harness()
  await newCreate(h, 10)
  const a = h.submitCreateReview()
  h.createDialogVisible.value = false
  await newCreate(h, 20)
  const b = h.submitCreateReview()
  h.creates[0].resolve(review(1))
  await new Promise(setImmediate)
  if (h.pages.length) h.pages.at(-1).resolve({ list: [], total: 0 })
  await a
  assert.equal(h.createDialogVisible.value, true)
  assert.equal(h.selectedActiveOrderId.value, 20)
  assert.equal(h.entryForm.nonconformanceReason, 'reason 20')
  assert.equal(h.createLoading.value, true)
  assert.equal(h.messages.length, 0)
  h.creates[1].reject(Error('current'))
  await b
  assert.equal(h.createLoading.value, false)
  h.unmount()
})
test('old create failure and finally cannot affect reopened creation', async () => {
  const h = harness()
  await newCreate(h, 10)
  const a = h.submitCreateReview()
  h.closeCreateDialog()
  await newCreate(h, 20)
  const b = h.submitCreateReview()
  h.creates[0].reject(Error('obsolete'))
  await a
  assert.equal(h.errorText.value, '')
  assert.equal(h.messages.length, 0)
  assert.equal(h.createLoading.value, true)
  h.creates[1].reject(Error('current'))
  await b
  assert.equal(h.errorText.value, 'current')
  h.unmount()
})
test('old candidate response cannot select an order in reopened creation', async () => {
  const h = harness()
  const a = h.openCreateDialog()
  h.closeCreateDialog()
  const b = h.openCreateDialog()
  h.candidates[0].resolve([{ id: 10 }])
  await a
  assert.equal(h.selectedActiveOrderId.value, undefined)
  assert.equal(h.activeOrderCandidatesLoading.value, true)
  h.candidates[1].resolve([{ id: 20 }])
  await b
  assert.equal(h.selectedActiveOrderId.value, 20)
  h.unmount()
})

test('creation refresh failure after reopening cannot overwrite B error', async () => {
  const h = harness()
  await newCreate(h, 10)
  const a = h.submitCreateReview()
  h.creates[0].resolve(review(1))
  await new Promise(setImmediate)
  await newCreate(h, 20)
  h.pages[0].reject(Error('old refresh'))
  await a
  assert.equal(h.errorText.value, '')
  assert.equal(h.createDialogVisible.value, true)
  assert.equal(h.createLoading.value, false)
  h.unmount()
})
test('normal successful creation clears loading after its own close', async () => {
  const h = harness()
  await newCreate(h, 10)
  const p = h.submitCreateReview()
  h.creates[0].resolve(review(1))
  await new Promise(setImmediate)
  assert.equal(h.createDialogVisible.value, false)
  assert.equal(h.createLoading.value, false)
  h.pages[0].resolve({ list: [review(1)], total: 1 })
  await p
  assert.equal(h.reviews.value.length, 1)
  h.unmount()
})
test('creation close does not invalidate a pending disposition', async () => {
  const h = harness()
  await open(h, 1)
  const p = h.handleDispose('REWORK')
  await newCreate(h, 20)
  h.closeCreateDialog()
  await settle(h)
  await p
  assert.equal(h.selectedReview.value.reviewStatus, 'CLOSED')
  assert.equal(h.disposeLoading.value, false)
  assert.equal(h.messages[0][0], 'success')
  h.unmount()
})
