import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'
import ts from 'typescript'
import { parse, compileScript, compileTemplate } from 'vue/compiler-sfc'

const filename = 'TeamLeaderWorkbenchPage.vue'
const source = readFileSync(new URL('../src/views/mes/pro/processpool/' + filename, import.meta.url), 'utf8')

test('simplified dialog compiles and limits allocation columns to order, quantity and removal', () => {
  const { descriptor, errors } = parse(source, { filename })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: 'allocation-check' })
  const template = compileTemplate({
    source: descriptor.template.content, filename, id: 'allocation-check',
    compilerOptions: { bindingMetadata: script.bindings }
  })
  assert.deepEqual(template.errors, [])
  const table = source.slice(source.indexOf('data-team-leader-allocation-table'), source.indexOf('data-team-leader-allocation-summary'))
  assert.deepEqual([...table.matchAll(/<el-table-column label="([^"]+)"/g)].map(match => match[1]), ['目标订单', '分配数量', '操作'])
  assert.match(source, /reviewDialogMode !== 'ALLOCATION' \|\| allocationConfirming/)
  assert.match(source, /reviewEvent\?\.actualEmployeeUserName/)
  assert.doesNotMatch(table, /FIFO|最大|一半|设备|物料/)
  assert.match(table, /:max="row\.editable === false \? undefined : getAllocationQuantityMax\(row\)"/)
})

const extract = (start, end) => source.slice(source.indexOf(start), source.indexOf(end, source.indexOf(start)))

test('quantity overage allows release after production and inspection are complete', () => {
  const context = {
    handoffReadOnly: { value: false },
    isActiveOrderProgressComplete: value => Number(value) >= 100
  }
  runInNewContext(ts.transpileModule(
    extract('const canApplyActiveOrderRelease =', 'const resolveActiveOrderReleaseApplyDisabledReason =') +
    '\nglobalThis.canRelease = canApplyActiveOrderRelease',
    { compilerOptions: { target: ts.ScriptTarget.ES2020 } }
  ).outputText, context)
  const complete = { productionProgressPercent: 100, inspectionProgressPercent: 100, hasQuantityConflict: true, quantityConflict: true }
  assert.equal(context.canRelease(complete), true)
  for (const condition of [{ productionProgressPercent: 99 }, { inspectionProgressPercent: 99 }, { abnormal: true }, { readBlocked: true }, { releaseApplicationStatus: 'PENDING' }]) {
    assert.equal(context.canRelease({ ...complete, ...condition }), false)
  }
  context.handoffReadOnly.value = true
  assert.equal(context.canRelease(complete), false)
})

test('overage badge uses actual allocation warnings instead of unallocated report quantity', () => {
  const context = { resolveActiveOrderProcessCeiling: () => 10, findReportSelectedActiveOrder: () => ({}) }
  runInNewContext(ts.transpileModule(
    extract('const resolveProductionReportOverageQuantity =', 'const allocationTotalQuantity =') +
    '\nglobalThis.overage = resolveProductionReportOverageQuantity',
    { compilerOptions: { target: ts.ScriptTarget.ES2020 } }
  ).outputText, context)
  assert.equal(context.overage({ outputQuantity: 20, reportUnallocatedQuantity: 0, reportAllocations: [{ overageQuantity: 3 }, { overageQuantity: 2 }] }), 5)
  assert.equal(context.overage({ outputQuantity: 20, reportUnallocatedQuantity: 15, reportAllocations: [{ overageQuantity: 0 }] }), 0)
  assert.equal(context.overage({ outputQuantity: 20 }), 10)
})

for (const relativePath of ['feedback/FrontlineFixedTemplatePanel.vue', 'processpool/components/ProductionQuantityDrafts.vue']) {
  test(relativePath + ' compiles after quantity warning changes', () => {
    const component = readFileSync(new URL('../src/views/mes/pro/' + relativePath, import.meta.url), 'utf8')
    const { descriptor, errors } = parse(component, { filename: relativePath })
    assert.deepEqual(errors, [])
    const script = compileScript(descriptor, { id: relativePath })
    const template = compileTemplate({ source: descriptor.template.content, filename: relativePath, id: relativePath, compilerOptions: { bindingMetadata: script.bindings } })
    assert.deepEqual(template.errors, [])
  })
}

const harness = (rows, orders = [{ id: 1, workOrderId: 101 }, { id: 2, workOrderId: 102 }]) => {
  const errors = []
  const context = {
    allocationRows: { value: rows }, allocatableActiveOrderOptions: { value: orders },
    allocationSnapshot: { value: { poolQuantity: 10 } }, reviewEvent: { value: { outputQuantity: 10 } },
    allocationTotalQuantity: { get value() { return rows.reduce((sum, row) => sum + Number(row.allocatedQuantity || 0), 0) } },
    allocationConfirming: { value: false }, reviewForm: { reviewSignaturePassword: 'secret' },
    normalizePositiveNumber(value) { const parsed = Number(value); return parsed > 0 ? parsed : undefined },
    requirePositiveNumber(value, message) { if (!(Number(value) > 0)) throw new Error(message); return Number(value) },
    requirePositiveInteger(value, message) { if (!Number.isInteger(value) || value <= 0) throw new Error(message); return value },
    ElMessage: { error: message => errors.push(message) }, resolveErrorMessage: error => error.message
  }
  const code = extract('const normalizeAllocationSubmitQuantity =', '\nconst ') + '\n' +
    extract('const normalizeAllocationInteger =', 'const prefillSelectedOrderAllocation =') + '\n' +
    extract('const getAllocationPoolQuantity =', 'const allocationSaveRequestIdentity =') +
    '\nglobalThis.prepare = prepareAllocationConfirmation; globalThis.back = backToAllocation; globalThis.build = buildAllocationSubmitLines; globalThis.max = getAllocationQuantityMax'
  runInNewContext(ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText, context)
  return { context, errors }
}

test('valid split enters signature confirmation and returning clears the credential', () => {
  const { context, errors } = harness([{ activeOrderId: 1, allocatedQuantity: 6 }, { activeOrderId: 2, allocatedQuantity: 4 }])
  context.prepare()
  assert.equal(context.allocationConfirming.value, true)
  assert.deepEqual(errors, [])
  context.back()
  assert.equal(context.allocationConfirming.value, false)
  assert.equal(context.reviewForm.reviewSignaturePassword, '')
})

for (const [name, rows] of [
  ['over-allocation', [{ activeOrderId: 1, allocatedQuantity: 11 }]],
  ['missing target', [{ allocatedQuantity: 2 }]],
  ['duplicate order', [{ activeOrderId: 1, allocatedQuantity: 2 }, { activeOrderId: 1, allocatedQuantity: 3 }]],
  ['fraction', [{ activeOrderId: 1, allocatedQuantity: 1.5 }]],
  ['negative quantity', [{ activeOrderId: 1, allocatedQuantity: -1 }]]
]) {
  test(name + ' prevents confirmation', () => {
    const { context, errors } = harness(rows)
    context.prepare()
    assert.equal(context.allocationConfirming.value, false)
    assert.equal(errors.length, 1)
  })
}

test('released allocations remain locked and are omitted from editable payload', () => {
  const { context, errors } = harness([
    { activeOrderId: 1, allocatedQuantity: 6, editable: false },
    { activeOrderId: 2, allocatedQuantity: 4 },
    { allocatedQuantity: 0 }
  ])
  context.prepare()
  assert.deepEqual(errors, [])
  assert.equal(JSON.stringify(context.build()), JSON.stringify([{ activeOrderId: 2, allocatedQuantity: 4 }]))
})

test('each input is capped by the total minus all other rows, including released quantities', () => {
  const rows = [
    { activeOrderId: 1, allocatedQuantity: 6, editable: false },
    { activeOrderId: 2, allocatedQuantity: 3 },
    { allocatedQuantity: 0 }
  ]
  const { context } = harness(rows)
  assert.equal(context.max(rows[1]), 4)
  assert.equal(context.max(rows[2]), 1)
  rows[1].allocatedQuantity = 4
  assert.equal(context.max(rows[2]), 0)
  rows[1].allocatedQuantity = 1
  assert.equal(context.max(rows[2]), 3)
})

test('final payload validation rejects overflow even after entering confirmation', () => {
  const rows = [{ activeOrderId: 1, allocatedQuantity: 6 }, { activeOrderId: 2, allocatedQuantity: 4 }]
  const { context } = harness(rows)
  context.prepare()
  assert.equal(context.allocationConfirming.value, true)
  rows[1].allocatedQuantity = 5
  assert.throws(() => context.build(), /分配总量不能超过本次报工数量/)
})
