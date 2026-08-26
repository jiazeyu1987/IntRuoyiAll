const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const page = readFileSync(
  resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'),
  'utf8'
)

for (const token of [
  'data-stage4-dossier-summary',
  'dossierReadyForRelease=true',
  'cleanedSimulationRunId',
  'INCOMING_INSPECTION_REPORT',
  'STERILIZATION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_RECORD',
  'stage4SimulationResult.value = result'
]) {
  assert.ok(page.includes(token), 'Stage4 page is missing visible result token: ' + token)
}
