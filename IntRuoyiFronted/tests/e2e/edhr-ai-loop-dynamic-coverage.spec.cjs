const assert = require('node:assert/strict')
const { freezeExecutionBaseline, assertCoverage, assertDouble100 } = require('./edhr-ai-loop/coverage.cjs')

const order = { activeOrderId: '900', workOrderCode: 'TEST', quantity: 10, batchCode: 'B' }
const productionProcesses = [1, 2, 3].map((id) => ({ routeProcessId: String(id), processId: String(id + 10), processKey: `MES-900-1-${id}-${id + 10}`, processLabel: `工序${id}`, targetQuantity: id * 10, outputMaterials: [`物料${id}`, `副产${id}`] }))
const task = (id, quantity) => ({ pqcTaskId: String(id), ruleKey: 'FIRST', type: 'FIRST', businessDate: '2026-09-17', shiftCode: 'DAY', roundNo: 1, quantity, taskStatus: 'PENDING', inspectionItems: [{ itemCode: `I${id}`, itemName: `项目${id}`, resultType: 'NUMBER', standardLowerLimit: 2, standardUpperLimit: 4 }] })
const pqcProcesses = [{ processKey: 'QA-80-50', processLabel: '独立QA工序', tasks: [task(71, 2), task(72, 3)] }]
const baseline = freezeExecutionBaseline(order, productionProcesses, pqcProcesses)
assert.equal(baseline.productionProcesses.length, 3)
assert.deepEqual(baseline.productionProcesses.map(p => p.quantity), [10, 20, 30])
assert.equal(baseline.expected.productionFeedbackCount, 3)
assert.equal(baseline.expected.pqcTaskCount, 2)
assert.equal(baseline.expected.pqcSubmissionCount, 2)
assert.equal(baseline.pqcGroups.length, 1, 'one UI submission may produce multiple task receipts')
assert.equal(baseline.expected.pqcReviewCount, 1, 'one formal UI submission group is reviewed once')
assert.equal(baseline.expected.pqcPieceResultCount, 5)
assert.ok(Object.isFrozen(baseline.pqcGroups[0].tasks[0].inspectionItems))
productionProcesses[0].targetQuantity = 999
assert.equal(baseline.productionProcesses[0].quantity, 10)
assert.throws(() => freezeExecutionBaseline(order, [{ ...productionProcesses[0], targetQuantity: undefined }], pqcProcesses), /targetQuantity/)
assert.throws(() => freezeExecutionBaseline(order, productionProcesses, [{ ...pqcProcesses[0], tasks: [task(71, 2), task(71, 2)] }]), /duplicate/)
assert.throws(() => freezeExecutionBaseline(order, productionProcesses, [{ ...pqcProcesses[0], tasks: [{ ...task(71, 2), quantity: 0 }] }]), /quantity/)
const submitted71 = { formalIdentity: baseline.pqcGroups[0].tasks[0].formalIdentity, pqcTaskId: '100071' }
const submitted72 = { formalIdentity: baseline.pqcGroups[0].tasks[1].formalIdentity, pqcTaskId: '100072' }
assert.throws(() => assertCoverage(baseline, ['1', '2'], [submitted71]), /coverage/)
assert.throws(() => assertCoverage(baseline, ['1', '2'], [submitted71]), error => {
  assert.deepEqual(error.actual.missingProductionIds, ['3'])
  assert.deepEqual(error.actual.missingPqcFormalIdentities, [baseline.pqcGroups[0].tasks[1].formalIdentity])
  return true
})
assert.throws(() => assertCoverage(baseline, ['1', '2', '3'], [submitted71, submitted71]), /coverage/)
assertCoverage(baseline, ['1', '2', '3'], [submitted71, submitted72])
assertDouble100({ productionProgressText: '100%', inspectionProgressText: '100.00%' })
for (const text of ['1000%', '未完成100', '99.99%', '100% / 90%']) {
  assert.throws(() => assertDouble100({ productionProgressText: text, inspectionProgressText: '100%' }), /100/)
}
console.log('PASS: dynamic coverage behavior (3 production processes, independent QA identity, grouped tasks, immutable expectations, missing/duplicate coverage, exact progress)')

const noMaterial = freezeExecutionBaseline(order, [{ ...productionProcesses[0], outputMaterials: [], quantityMode: 'PROCESS_QUANTITY' }], pqcProcesses)
assert.equal(noMaterial.productionProcesses[0].quantityMode, 'PROCESS_QUANTITY')
