import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'
import ts from 'typescript'

const source = readFileSync(new URL('../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue', import.meta.url), 'utf8')
const guardSource = readFileSync(new URL('../src/store/modules/approvalCenterInvalidationState.ts', import.meta.url), 'utf8')
const extract = (text, begin, end) => {
  const a = text.indexOf(begin), b = text.indexOf(end, a)
  assert.ok(a >= 0 && b > a, begin)
  return text.slice(a, b)
}
const deferred = () => {
  let resolve, reject
  const promise = new Promise((a, b) => { resolve = a; reject = b })
  return { promise, resolve, reject }
}
const tick = () => new Promise(setImmediate)
const event = (id, loss) => ({ id, routeProcessId: 501, outputQuantity: 6000,
  materialDetails: [{ materialId: 11, materialName: '', outputQuantity: 6000, lossQuantity: loss,
    lossDetails: [{ reasonId: 1, quantity: loss }], deviceParameterReadings: [] }],
  lossDetails: [{ reasonId: 1, quantity: loss }], deviceParameterReadings: [],
  displayedRevisionId: 0, displayedPayloadHash: 'hash-' + id })

const harness = () => {
  const reads = new Map(), names = new Map(), errors = [], watchers = [], writes = []
  const context = {
    route: { fullPath: '/leader' }, approvalInvalidation: { syncSession: () => ({ key: 'session1' }) },
    watch: (getter, callback) => watchers.push({ getter, callback }), onBeforeUnmount() {}, onDeactivated() {},
    processConfigRows: { value: [] }, correctionForm: {}, correctionEvent: { value: undefined },
    correctionVisible: { value: false }, correctionSubmitting: { value: false },
    correctionChangePreview: { value: ['changed'] }, correctionFormRef: { value: { validate: async () => true } },
    requireWritableHandoff: () => true, requirePositiveNumber: value => value,
    canCorrectSubmission: () => true, isPqcSubmissionRow: value => value.kind === 'PQC',
    resolveCurrentLeaderType: () => 'PRODUCTION',
    getTeamLeaderSubmissionDetail(id) { const read = deferred(); reads.set(id, read); return read.promise },
    requireLossReasonId: value => value, toCorrectionParameterRow: value => value,
    initializeCorrectionMaterialDeviceTabs() {}, resolveProductionMaterialRows: value => value.materialDetails,
    resolveCorrectionMaterialNames(rows) { const read = deferred(); names.set(rows[0].lossQuantity, read); return read.promise },
    enrichCorrectionMaterialParameterRows: (_event, rows) => rows, resolveCorrectionMaterialDevices: () => [],
    optionalCorrectionNumber: value => Number(value),
    ElMessage: { error: message => errors.push(message), success() {} }, resolveErrorMessage: error => error.message,
    correctProcessPoolProductionReport: async request => writes.push(request), getSubmissionList: async () => {},
    resolvePqcPayloadPair: () => ({ rootPayload: {} }),
    readSubmissionPayloadValue: (_root, keys) => keys[0] === 'inspectionItemPlan' ? { check: 1 }
      : keys[0] === 'inspectionQuantity' ? 1 : 0,
    resolvePqcItemSnapshotDetails: () => [{ itemCode: 'check', sampleValues: [1] }],
    normalizePqcSampleText: values => values.join(',')
  }
  const code = extract(guardSource, 'export const createFormalActionGuard =', '// A completed write').replace('export ', '') +
    extract(source, 'const correctionGuard =', 'const activeOrderOptions =') +
    extract(source, 'const resetCorrectionFormForEvent =', 'const getPqcCorrectionActualQuantity =') +
    extract(source, 'const submitCorrection =', 'const resetAbnormalForm =') +
    '\nglobalThis.open = openCorrection; globalThis.build = buildProductionCorrectionRequest;' +
    'globalThis.submit = submitCorrection; globalThis.reset = resetCorrectionContext;'
  runInNewContext(ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText, context)
  const finish = async (record) => {
    reads.get(record.id).resolve(record)
    await tick()
    names.get(record.materialDetails[0].lossQuantity).resolve(record.materialDetails)
  }
  return { context, reads, names, errors, watchers, writes, finish }
}

test('late material names cannot overwrite the newer report or its outgoing request', async () => {
  const h = harness(), a = event(101, 1), b = event(102, 2)
  const first = h.context.open(a)
  h.reads.get(101).resolve(a)
  await tick()
  const second = h.context.open(b)
  assert.equal(h.context.correctionVisible.value, false)
  assert.equal(h.context.correctionEvent.value, undefined)
  await h.finish(b)
  await second
  h.names.get(1).resolve(a.materialDetails)
  await first
  h.context.correctionForm.changeReason = 'Correct B'
  h.context.correctionForm.signaturePassword = 'test-only'
  const request = h.context.build()
  assert.equal(request.eventId, 102)
  assert.equal(request.expectedPayloadHash, 'hash-102')
  assert.equal(request.materialDetails[0].lossQuantity, 2)
  assert.equal(h.context.correctionForm.lossDetails[0].quantity, 2)
  assert.deepEqual(h.errors, [])
})

test('late submission detail never starts material reads for an obsolete report', async () => {
  const h = harness(), a = event(101, 1), b = event(102, 2)
  const first = h.context.open(a), second = h.context.open(b)
  await h.finish(b)
  await second
  h.reads.get(101).resolve(a)
  await first
  assert.equal(h.names.has(1), false)
  assert.equal(h.context.correctionEvent.value.id, 102)
})

test('a pending production read cannot overwrite a newer PQC correction', async () => {
  const h = harness(), a = event(101, 1), b = { ...event(102, 2), kind: 'PQC' }
  const first = h.context.open(a)
  h.reads.get(101).resolve(a)
  await tick()
  const second = h.context.open(b)
  h.reads.get(102).resolve(b)
  await second
  h.names.get(1).resolve(a.materialDetails)
  await first
  assert.equal(h.context.correctionForm.correctionMode, 'PQC')
  assert.equal(h.context.correctionForm.eventId, 102)
  assert.equal(h.context.correctionForm.materialDetails.length, 0)
})

for (const invalidation of ['close', 'route', 'session']) {
  test(invalidation + ' invalidates an unfinished correction', async () => {
    const h = harness(), a = event(101, 1), pending = h.context.open(a)
    h.reads.get(101).resolve(a)
    await tick()
    if (invalidation === 'close') h.watchers.find(w => w.getter === h.context.correctionVisible).callback(false)
    if (invalidation === 'route') h.context.route.fullPath = '/elsewhere'
    if (invalidation === 'session') h.context.approvalInvalidation.syncSession = () => ({ key: 'session2' })
    h.names.get(1).resolve(a.materialDetails)
    await pending
    assert.equal(h.context.correctionVisible.value, false)
    assert.equal(h.context.correctionEvent.value, undefined)
  })
}

test('current read failure is visible and leaves no editable partial record', async () => {
  const h = harness(), a = event(101, 1), pending = h.context.open(a)
  h.reads.get(101).resolve(a)
  await tick()
  h.names.get(1).reject(new Error('Material name unavailable'))
  await pending
  assert.deepEqual(h.errors, ['Material name unavailable'])
  assert.equal(h.context.correctionVisible.value, false)
  assert.equal(h.context.correctionEvent.value, undefined)
})

test('superseded failures cannot display an error over the newer record', async () => {
  const h = harness(), a = event(101, 1), b = event(102, 2)
  const first = h.context.open(a), second = h.context.open(b)
  await h.finish(b)
  await second
  h.reads.get(101).reject(new Error('Obsolete request'))
  await first
  assert.deepEqual(h.errors, [])
  assert.equal(h.context.correctionEvent.value.id, 102)
})

test('validation finishing after a new correction opens cannot submit that new record', async () => {
  const h = harness(), a = event(101, 1), b = event(102, 2)
  const opened = h.context.open(a)
  await h.finish(a)
  await opened
  const validation = deferred()
  h.context.correctionFormRef.value.validate = () => validation.promise
  const submit = h.context.submit()
  const next = h.context.open(b)
  await h.finish(b)
  await next
  validation.resolve(true)
  await submit
  assert.equal(h.writes.length, 0)
  assert.equal(h.context.correctionForm.eventId, 102)
})

test('concurrent validation cannot send the same correction twice', async () => {
  const h = harness(), a = event(101, 1), opened = h.context.open(a)
  await h.finish(a)
  await opened
  h.context.correctionForm.changeReason = 'Correct A'
  h.context.correctionForm.signaturePassword = 'test-only'
  const validation = deferred(), write = deferred()
  h.context.correctionFormRef.value.validate = () => validation.promise
  h.context.correctProcessPoolProductionReport = request => { h.writes.push(request); return write.promise }
  const first = h.context.submit(), second = h.context.submit()
  validation.resolve(true)
  await tick()
  assert.equal(h.writes.length, 1)
  write.resolve()
  await Promise.all([first, second])
})

for (const outcome of ['success', 'failure']) {
  test('an old write ' + outcome + ' cannot alter a new correction dialog', async () => {
    const h = harness(), a = event(101, 1), b = event(102, 2), opened = h.context.open(a)
    await h.finish(a)
    await opened
    h.context.correctionForm.changeReason = 'Correct A'
    h.context.correctionForm.signaturePassword = 'test-only'
    const write = deferred()
    h.context.correctProcessPoolProductionReport = () => write.promise
    const submitted = h.context.submit()
    await tick()
    h.context.correctionVisible.value = false
    h.watchers.find(w => w.getter === h.context.correctionVisible).callback(false)
    const next = h.context.open(b)
    await h.finish(b)
    await next
    h.context.correctionForm.signaturePassword = 'new-record-credential'
    if (outcome === 'success') write.resolve()
    else write.reject(new Error('Old write error'))
    await submitted
    assert.equal(h.context.correctionVisible.value, true)
    assert.equal(h.context.correctionForm.eventId, 102)
    assert.equal(h.context.correctionForm.signaturePassword, 'new-record-credential')
    assert.deepEqual(h.errors, [])
  })
}
