const assert = require('node:assert/strict')

function deepFreeze(value) {
  if (value && typeof value === 'object') {
    Object.values(value).forEach(deepFreeze)
    Object.freeze(value)
  }
  return value
}
function positive(value, label) {
  assert.ok(value !== null && value !== undefined && Number.isFinite(Number(value)) && Number(value) > 0, `${label} must be positive`)
  return Number(value)
}
function unique(values, label) {
  assert.ok(values.every(v => v !== undefined && v !== null && String(v).length > 0), `${label} missing identity`)
  assert.equal(new Set(values.map(String)).size, values.length, `${label} duplicate identity`)
}
function taskInspectionItemIdentity(task) {
  return (task.inspectionItems || [])
    .map(item => String(item.itemCode || item.itemName || '').trim())
    .filter(Boolean)
    .sort()
    .join('|')
}
function pqcGroupIdentity(processKey, task) {
  return JSON.stringify([processKey, task.type, task.businessDate, task.shiftCode, String(task.roundNo)])
}
function pqcTaskFormalIdentity(processKey, task) {
  return JSON.stringify([processKey, task.ruleKey, task.type, task.businessDate, task.shiftCode, String(task.roundNo), taskInspectionItemIdentity(task)])
}
function buildPqcTaskGroups(pqcProcesses, options = {}) {
  const groups = []
  const tasks = []
  for (const process of pqcProcesses) {
    const scoped = new Map()
    for (const task of process.tasks) {
      if (options.requirePending) assert.equal(task.taskStatus, 'PENDING', `initial task not PENDING: ${task.pqcTaskId}`)
      const key = pqcGroupIdentity(process.processKey, task)
      if (!scoped.has(key)) scoped.set(key, { processKey: process.processKey, processLabel: process.processLabel, ruleKey: task.ruleKey, type: task.type, tasks: [] })
      const groupedTask = { ...task, processKey: process.processKey, formalIdentity: pqcTaskFormalIdentity(process.processKey, task) }
      const group = scoped.get(key)
      const identity = taskInspectionItemIdentity(task)
      if (!group.tasks.some(existing => taskInspectionItemIdentity(existing) === identity)) {
        group.tasks.push(groupedTask)
      }
      tasks.push(groupedTask)
    }
    groups.push(...scoped.values())
  }
  return { groups, tasks }
}
function freezeExecutionBaseline(order, productionProcesses, pqcProcesses) {
  assert.ok(productionProcesses.length > 0, 'productionProcesses empty')
  unique(productionProcesses.map(p => p.routeProcessId), 'productionProcesses')
  const production = productionProcesses.map((p, processIndex) => {
    assert.ok(p.outputMaterials.length > 0 || p.quantityMode === 'PROCESS_QUANTITY', `outputMaterials missing: ${p.processKey}`)
    return { ...p, processIndex, quantity: positive(p.targetQuantity, 'targetQuantity') }
  })
  for (const process of pqcProcesses) {
    for (const task of process.tasks) {
      positive(task.quantity, 'PQC quantity')
      assert.ok(Number.isInteger(Number(task.quantity)), 'PQC quantity must be integer')
      assert.ok(task.inspectionItems.length > 0, 'PQC inspectionItems missing')
    }
  }
  const { groups, tasks } = buildPqcTaskGroups(pqcProcesses, { requirePending: true })
  assert.ok(tasks.length > 0, 'pqcTasks empty')
  unique(tasks.map(t => t.pqcTaskId), 'pqcTasks')
  const baseline = { activeOrderId: String(order.activeOrderId), workOrderCode: order.workOrderCode, batchCode: order.batchCode, quantity: order.quantity,
    productionProcesses: production, pqcTasks: tasks, pqcGroups: groups,
    expected: { productionProcessCount: production.length, productionFeedbackCount: production.length, productionReviewCount: production.length,
      pqcTaskCount: tasks.length,
      pqcSubmissionCount: groups.reduce((sum, group) => sum + group.tasks.length, 0),
      pqcReviewCount: groups.length,
      pqcUiSubmissionCount: groups.length, pqcPieceResultCount: tasks.reduce((sum, t) => sum + Number(t.quantity) * t.inspectionItems.length, 0),
      productionProgressPercent: 100, inspectionProgressPercent: 100 } }
  return deepFreeze(JSON.parse(JSON.stringify(baseline)))
}
function assertCoverage(baseline, productionIds, submittedPqcTasks) {
  const expectedProduction = baseline.productionProcesses.map(p => String(p.routeProcessId)).sort()
  const expectedPqcFormalIdentities = baseline.pqcGroups.flatMap(group => group.tasks).map(t => String(t.formalIdentity)).sort()
  const actualProduction = productionIds.map(String).sort()
  const actualPqcFormalIdentities = submittedPqcTasks.map(task => String(task.formalIdentity || task)).sort()
  if (JSON.stringify(actualProduction) !== JSON.stringify(expectedProduction) || JSON.stringify(actualPqcFormalIdentities) !== JSON.stringify(expectedPqcFormalIdentities)) {
    const error = new Error('production/PQC coverage mismatch')
    error.stage = 'S03'
    error.errorType = 'BUSINESS_ASSERTION'
    error.action = '冻结工序和任务覆盖核验'
    error.expected = baseline
    error.actual = { workOrderCode: baseline.workOrderCode, activeOrderId: baseline.activeOrderId,
      productionIds: actualProduction, pqcFormalIdentities: actualPqcFormalIdentities,
      missingProductionIds: expectedProduction.filter(id => !actualProduction.includes(id)),
      missingPqcFormalIdentities: expectedPqcFormalIdentities.filter(id => !actualPqcFormalIdentities.includes(id)) }
    throw error
  }
}
function assertDouble100(status) {
  for (const key of ['productionProgressText', 'inspectionProgressText']) {
    assert.match(status[key].trim(), /^100(?:\.0+)?\s*%$/, `${key} must be exactly 100%`)
  }
}
function resolveProductionIdentity(env) {
  const employeeId = String(env.EDHR_E2E_PRODUCTION_EMPLOYEE_ID || '').trim()
  if (!/^[1-9]\d*$/.test(employeeId)) throw new Error('Missing/invalid EDHR_E2E_PRODUCTION_EMPLOYEE_ID')
  const signaturePassword = env.EDHR_E2E_PRODUCTION_SIGNATURE_PASSWORD
  if (!signaturePassword || !signaturePassword.trim()) throw new Error('Missing EDHR_E2E_PRODUCTION_SIGNATURE_PASSWORD')
  return { employeeId, signaturePassword }
}
module.exports = { freezeExecutionBaseline, assertCoverage, assertDouble100, resolveProductionIdentity, buildPqcTaskGroups, pqcTaskFormalIdentity, taskInspectionItemIdentity }
