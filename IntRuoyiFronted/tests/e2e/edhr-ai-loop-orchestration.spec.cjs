// Runner unit test only. These doubles are not real-page E2E evidence.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { createRequire } = require('node:module')
const { freezeExecutionBaseline } = require('./edhr-ai-loop/coverage.cjs')
const runnerPath = path.join(__dirname, 'edhr-ai-loop/runner.cjs')
const order = { activeOrderId: '9', workOrderCode: 'UNIT', quantity: 10 }
order.executionBaseline = freezeExecutionBaseline(order, [1, 2, 3].map(id => ({ routeProcessId: String(id), processKey: `P${id}`, targetQuantity: id * 10, outputMaterials: ['M'] })),
  [{ processKey: 'QA-999-77', processLabel: '独立QA工序', tasks: [71, 72].map(id => ({ pqcTaskId: String(id), ruleKey: 'FIRST', type: 'FIRST', businessDate: '2026-09-17', shiftCode: 'D', roundNo: 1, quantity: 2, taskStatus: 'PENDING', inspectionItems: [{ itemCode: String(id) }] })) }])

async function run(failReview = false, deferToTail = false) {
  const calls = []
  let submitted = false
  const hooks = {
    submitOneProductionReport: async (_page, _order, step) => { calls.push(`produce:${step.routeProcessId}:${step.quantity}`); return { routeProcessId: step.routeProcessId, processPoolEventId: step.routeProcessId } },
    reviewProductionReport: async (_page, _order, submission) => { calls.push(`review-production:${submission.processPoolEventId}`); return { eventId: submission.processPoolEventId } },
    discoverPendingPqcTasksForOrder: async (_page, _order, baseline, process) => {
      calls.push(`scan:${process?.routeProcessId || 'tail'}`)
      return !submitted && (!deferToTail || !process) ? baseline.pqcGroups : []
    },
    submitOnePqcInspectionForProcess: async (_page, _order, step) => {
      calls.push(`pqc:${step.processKey}`); submitted = true
      return step.tasks.map(t => ({
        pqcTaskId: t.pqcTaskId,
        pqcEventId: `8${t.pqcTaskId}`,
        formalIdentity: t.formalIdentity
      }))
    },
    reviewPqcInspectionSubmission: async (_page, _order, submission) => {
      calls.push(`review-pqc:${submission.pqcEventId}`)
      if (failReview) throw new Error('review rejected')
      return { eventId: submission.pqcEventId }
    },
    verifyExecutionProgress: async () => { calls.push('double100'); return { productionProgressText: '100%', inspectionProgressText: '100%' } }
  }
  const sandbox = { require: createRequire(runnerPath), module: { exports: {} }, process, console, hooks }
  vm.createContext(sandbox)
  vm.runInContext(fs.readFileSync(runnerPath, 'utf8') + '\n' + Object.keys(hooks).map(name => `${name} = hooks.${name}`).join('\n') + '\nmodule.exports.execute = executeProductionAndPqcInterleaved', sandbox)
  try { return { calls, result: await sandbox.module.exports.execute({}, order, order) } }
  catch (error) { return { calls, error } }
}

;(async () => {
  const normal = await run()
  assert.ifError(normal.error)
  assert.deepEqual(normal.calls, ['produce:1:10', 'review-production:1', 'scan:1', 'pqc:QA-999-77', 'review-pqc:871', 'produce:2:20', 'review-production:2', 'scan:2', 'produce:3:30', 'review-production:3', 'scan:3', 'scan:tail', 'double100'])
  assert.equal(normal.result.production.submissions.length, 3)
  assert.equal(normal.result.pqc.reviews.length, 1)
  const delayed = await run(false, true)
  assert.ifError(delayed.error)
  assert.ok(delayed.calls.indexOf('scan:tail') < delayed.calls.indexOf('pqc:QA-999-77'))
  const failed = await run(true)
  assert.equal(failed.error.stage, 'S03')
  assert.equal(failed.error.expected.processKey, 'QA-999-77')
  assert.ok(!failed.calls.includes('produce:2:20'))
  assert.ok(!failed.calls.includes('double100'))
  console.log('PASS: runner orchestration unit test; all processes, interleaving, grouped reviews, tail scan, stop on failure')
})().catch(error => { console.error(error); process.exitCode = 1 })
