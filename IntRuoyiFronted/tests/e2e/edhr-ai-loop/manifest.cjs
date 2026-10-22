const crypto = require('node:crypto')

const RUN_ID_PATTERN = /^AI-EDHR-\d{8}T\d{6}-[A-Z0-9]{4}$/
const FIXED_RESET_WORK_ORDER_CODE =
  'SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'
const FIXED_RESET_QUANTITY = 10

function createRunId(now = new Date(), nonce) {
  const iso = now.toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, '')
  const resolvedNonce = String(nonce || crypto.randomBytes(2).toString('hex')).toUpperCase()
  const runId = `AI-EDHR-${iso}-${resolvedNonce}`
  if (!RUN_ID_PATTERN.test(runId)) throw new Error(`invalid AI eDHR runId: ${runId}`)
  return runId
}

function buildManifest({
  runId = createRunId(),
  mode = 'full',
  resetWorkOrderCode
} = {}) {
  if (!RUN_ID_PATTERN.test(runId)) throw new Error(`invalid AI eDHR runId: ${runId}`)
  if (!['full', 'regression', 'repeatability'].includes(mode)) throw new Error(`invalid AI eDHR mode: ${mode}`)
  const rawResetWorkOrderCode = String(resetWorkOrderCode || '').trim()
  const resolvedResetWorkOrderCode = rawResetWorkOrderCode || FIXED_RESET_WORK_ORDER_CODE
  const order = (slot) => ({
    slot,
    resetWorkOrderCode: resolvedResetWorkOrderCode,
    simulationRunId: `${runId}-${slot}`,
    workOrderCode: null,
    batchCode: null,
    quantity: FIXED_RESET_QUANTITY
  })
  return Object.freeze({
    schemaVersion: 'AI_EDHR_E2E_RUN_V1', runId, mode,
    resetWorkOrderCode: resolvedResetWorkOrderCode,
    resetSource: rawResetWorkOrderCode ? 'EXPLICIT_CODE' : 'FIXED_ACTIVE_ORDER_RESET',
    createdAt: new Date().toISOString(),
    orders: ['O01', 'O02', 'O03', 'O04', 'O05'].map(order),
    expected: Object.freeze({ finishedQuantity: FIXED_RESET_QUANTITY, reportNodeCount: 4, archiveCount: 1 })
  })
}

module.exports = {
  RUN_ID_PATTERN,
  FIXED_RESET_WORK_ORDER_CODE,
  FIXED_RESET_QUANTITY,
  createRunId,
  buildManifest
}
