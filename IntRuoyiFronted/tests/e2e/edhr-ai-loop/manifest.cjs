const crypto = require('node:crypto')

const RUN_ID_PATTERN = /^AI-EDHR-\d{8}T\d{6}-[A-Z0-9]{4}$/

function createRunId(now = new Date(), nonce) {
  const iso = now.toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, '')
  const resolvedNonce = String(nonce || crypto.randomBytes(2).toString('hex')).toUpperCase()
  const runId = `AI-EDHR-${iso}-${resolvedNonce}`
  if (!RUN_ID_PATTERN.test(runId)) throw new Error(`invalid AI eDHR runId: ${runId}`)
  return runId
}

function buildManifest({ runId = createRunId(), mode = 'full', templateWorkOrderCode }) {
  if (!RUN_ID_PATTERN.test(runId)) throw new Error(`invalid AI eDHR runId: ${runId}`)
  if (!['full', 'regression', 'repeatability'].includes(mode)) throw new Error(`invalid AI eDHR mode: ${mode}`)
  const resolvedTemplateWorkOrderCode = String(templateWorkOrderCode || '').trim() || null
  const order = (slot) => ({
    slot,
    workOrderCode: `${runId}-${slot}`,
    batchCode: `${runId}-B${slot.slice(1)}`,
    quantity: 100
  })
  return Object.freeze({
    schemaVersion: 'AI_EDHR_E2E_RUN_V1', runId, mode,
    templateWorkOrderCode: resolvedTemplateWorkOrderCode,
    templateSource: resolvedTemplateWorkOrderCode ? 'EXPLICIT_CODE' : 'CONFIGURED_TEMPLATE',
    createdAt: new Date().toISOString(),
    orders: ['O01', 'O02', 'O03', 'O04', 'O05'].map(order),
    expected: Object.freeze({ processCount: 2, productionFeedbackCount: 4, pqcTaskCount: 16,
      pqcDetailCount: 40, finishedQuantity: 100, reportNodeCount: 4, archiveCount: 1 })
  })
}

module.exports = { RUN_ID_PATTERN, createRunId, buildManifest }
