const crypto = require('node:crypto')

const RUN_ID_PATTERN = /^AI-EDHR-\d{8}T\d{6}-[A-Z0-9]{4}$/
const FIXED_TEMPLATE_WORK_ORDER_CODE =
  'SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'
const FIXED_TEMPLATE_QUANTITY = 10

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
  templateWorkOrderCode
} = {}) {
  if (!RUN_ID_PATTERN.test(runId)) throw new Error(`invalid AI eDHR runId: ${runId}`)
  if (!['full', 'regression', 'repeatability'].includes(mode)) throw new Error(`invalid AI eDHR mode: ${mode}`)
  const rawTemplateWorkOrderCode = String(templateWorkOrderCode || '').trim()
  const resolvedTemplateWorkOrderCode = rawTemplateWorkOrderCode || FIXED_TEMPLATE_WORK_ORDER_CODE
  const order = (slot) => ({
    slot,
    sourceWorkOrderCode: resolvedTemplateWorkOrderCode,
    simulationRunId: `${runId}-${slot}`,
    expectedWorkOrderCodePrefix: 'SIM-COPY-',
    workOrderCode: null,
    batchCode: null,
    quantity: FIXED_TEMPLATE_QUANTITY
  })
  return Object.freeze({
    schemaVersion: 'AI_EDHR_E2E_RUN_V1', runId, mode,
    templateWorkOrderCode: resolvedTemplateWorkOrderCode,
    templateSource: rawTemplateWorkOrderCode ? 'EXPLICIT_CODE' : 'FIXED_ACTIVE_ORDER_COPY',
    createdAt: new Date().toISOString(),
    orders: ['O01', 'O02', 'O03', 'O04', 'O05'].map(order),
    expected: Object.freeze({ processCount: 2, productionFeedbackCount: 4, pqcTaskCount: 16,
      pqcDetailCount: 40, finishedQuantity: FIXED_TEMPLATE_QUANTITY, reportNodeCount: 4, archiveCount: 1 })
  })
}

module.exports = {
  RUN_ID_PATTERN,
  FIXED_TEMPLATE_WORK_ORDER_CODE,
  FIXED_TEMPLATE_QUANTITY,
  createRunId,
  buildManifest
}
