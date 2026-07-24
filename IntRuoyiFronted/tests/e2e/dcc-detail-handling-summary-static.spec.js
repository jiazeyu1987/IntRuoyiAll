const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const handlingSummary = readSource('src/views/dcc/controlled-file/shared/handlingSummary.ts')
const summaryTemplateMatch = detailPage.match(
  /<div\s+v-if="fileDetail"\s+class="detail-handling-summary"[\s\S]*?<\/ContentWrap>/
)
const summaryLogicMatch = detailPage.match(
  /const detailHandlingSummary = computed[\s\S]*?const getPreviewApprovalProgress/
)

assert.equal(
  packageJson.scripts['e2e:dcc:detail-handling-summary:static'],
  'node tests/e2e/dcc-detail-handling-summary-static.spec.js',
  'package.json must expose the DCC detail handling summary static contract'
)

assert.match(
  handlingSummary,
  /export const getControlledFileHandlingSummary/,
  'shared handling summary must keep the next-step summary helper'
)

assert.match(
  detailPage,
  /data-testid="dcc-detail-handling-summary"/,
  'detail page must render a stable handling summary region'
)
assert.match(detailPage, /下一步/, 'handling summary must expose next step')
assert.match(detailPage, /责任面/, 'handling summary must expose responsibility')
assert.match(detailPage, /当前阶段/, 'handling summary must expose current stage')
assert.match(detailPage, /阻塞原因/, 'handling summary must expose blocker reason')
assert.match(
  detailPage,
  /getControlledFileHandlingSummary/,
  'detail page must reuse the shared handling summary helper'
)
assert.match(
  detailPage,
  /detailHandlingSummary/,
  'detail page must compute the handling summary from current file state'
)
assert.match(detailPage, /finalizationError/, 'handling summary must include publish failure blockers')
assert.match(detailPage, /rejectReason/, 'handling summary must include rejected-file blockers')
assert.ok(summaryTemplateMatch, 'detail page must keep the handling summary template together')
assert.ok(summaryLogicMatch, 'detail page must keep the handling summary logic together')
const summaryContractSource = `${summaryTemplateMatch[0]}\n${summaryLogicMatch[0]}`
assert.doesNotMatch(
  summaryContractSource,
  /detailHandlingDeadline|detailHandlingSla|消息送达状态|截止时间|SLA/,
  'handling summary must not invent deadline, SLA, or message delivery fields'
)
assert.doesNotMatch(
  summaryContractSource,
  /mock|placeholder/i,
  'detail summary must not use mock or placeholder logic'
)

console.log('PASS: DCC detail handling summary static contract')
