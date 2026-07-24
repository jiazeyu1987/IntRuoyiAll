const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()
const pagePath = path.resolve(root, 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const helperPath = path.resolve(root, 'src/views/mes/pro/route-flow-config-panel/dualTrackBatchRecord.ts')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const helperSource = fs.existsSync(helperPath) ? fs.readFileSync(helperPath, 'utf8') : ''

assert(helperSource, 'Route use page must isolate dual-track batch record metadata rules in dualTrackBatchRecord.ts.')

assert(
  helperSource.includes('export const resolveDefaultValidationProfile'),
  'Dual-track helper must export resolveDefaultValidationProfile.'
)

assert(
  helperSource.includes('export const assertBatchRecordTrackMetadata'),
  'Dual-track helper must export assertBatchRecordTrackMetadata.'
)

assert(
  helperSource.includes('EDHR_RECORD_PROFILE_MISMATCH'),
  'Dual-track helper must expose an explicit mismatch marker for route-flow-config-panel save validation.'
)

assert(
  /recordCategory === 'INTERNAL_RECORD'[\s\S]*'INTERNAL_TRACE'/.test(helperSource),
  'INTERNAL_RECORD must map to INTERNAL_TRACE.'
)

assert(
  /recordCategory === 'BATCH_RECORD'[\s\S]*'CONTROLLED_BATCH'/.test(helperSource),
  'BATCH_RECORD must map to CONTROLLED_BATCH.'
)

assert(
  pageSource.includes('assertBatchRecordTrackMetadata(report)'),
  'RouteFlowConfigPanel save path must call assertBatchRecordTrackMetadata(report) before submitting.'
)

assert(
  !/validationProfile:\s*resolveDefaultValidationProfile\(recordCategory\)/.test(pageSource),
  'RouteFlowConfigPanel normalization must preserve backend-returned validationProfile and fail on mismatch instead of silently rewriting it.'
)

assert(
  pageSource.includes('handleRecordCategoryChange(report)'),
  'RouteFlowConfigPanel must still update validation profile immediately when the manager changes record category.'
)

console.log('PASS: eDHR dual-track route-flow-config-panel static contract')
