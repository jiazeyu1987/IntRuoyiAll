const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractBetween = (source, start, end) => {
  const startIndex = source.indexOf(start)
  assert.notEqual(startIndex, -1, `missing source marker: ${start}`)
  const endIndex = source.indexOf(end, startIndex + start.length)
  assert.notEqual(endIndex, -1, `missing source marker: ${end}`)
  return source.slice(startIndex, endIndex)
}

const packageJson = JSON.parse(readSource('package.json'))
const trainingMinePage = readSource('src/views/dcc/controlled-file/training/mine/index.vue')
const trainingPresentation = readSource('src/views/dcc/controlled-file/training/presentation.ts')

assert.equal(
  packageJson.scripts['e2e:dcc:training-summary:static'],
  'node tests/e2e/dcc-training-summary-static.spec.js',
  'package.json must expose the DCC training summary static contract'
)

assert.match(
  trainingMinePage,
  /label="培训摘要"/,
  'training mine list must show a training summary column'
)
assert.match(
  trainingMinePage,
  /data-testid="dcc-training-summary"/,
  'training summary cells must have a stable test id'
)
assert.match(
  trainingMinePage,
  /getTrainingTaskSummary\(item\)/,
  'training mine list must render summary from a presentation helper'
)
assert.match(
  trainingPresentation,
  /export const getTrainingTaskSummary/,
  'training presentation must export the training summary helper'
)

const trainingTableSource = extractBetween(
  trainingMinePage,
  '<el-table v-loading="loading"',
  '</el-table>'
)
for (const label of ['累计时长', '状态', '确认完成时间']) {
  assert.doesNotMatch(
    trainingTableSource,
    new RegExp(`label="${label}"`),
    `training mine list must replace standalone ${label} column`
  )
}

const summaryTemplate = extractBetween(
  trainingMinePage,
  '<el-table-column label="培训摘要"',
  '<el-table-column label="操作"'
)
assert.match(summaryTemplate, /training-summary__main/, 'training summary must have a main line')
assert.match(summaryTemplate, /training-summary__progress/, 'training summary must include compact progress')
assert.match(summaryTemplate, /training-summary__hint/, 'training summary must include readiness hint')
assert.match(summaryTemplate, /trainingSummary\.statusLabel/, 'training summary must show status text')
assert.match(summaryTemplate, /trainingSummary\.progressText/, 'training summary must show progress text')
assert.match(summaryTemplate, /trainingSummary\.hintText/, 'training summary must show remaining or completion hint')
assert.match(summaryTemplate, /trainingSummary\.timeText/, 'training summary must show publish or completion time')
assert.match(summaryTemplate, /trainingSummary\.progressPercent/, 'training summary must expose progress percent')

const summaryHelper = extractBetween(
  trainingPresentation,
  'interface TrainingTaskSummarySource',
  'export const buildResolvedTrainingUsers'
)
const requiredSourceFields = [
  'accumulatedViewSeconds',
  'requiredViewSeconds',
  'eligibleToAcknowledge',
  'acknowledgedAt',
  'publishedTime',
  'status'
]
for (const field of requiredSourceFields) {
  assert.match(summaryHelper, new RegExp(field), `training summary helper must use ${field}`)
}
assert.doesNotMatch(
  summaryHelper,
  /mock|placeholder|deadline|\bSLA\b|接口造数|fallback|降级|吞异常/i,
  'training summary must not invent mock data, deadlines, SLA fields, or fallback behavior'
)

console.log('PASS: DCC training summary static contract')
