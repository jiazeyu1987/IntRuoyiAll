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
const detailPresentation = readSource('src/views/dcc/controlled-file/detail/presentation.ts')

assert.equal(
  packageJson.scripts['e2e:dcc:detail-lifecycle-timeline:static'],
  'node tests/e2e/dcc-detail-lifecycle-timeline-static.spec.js',
  'package.json must expose the DCC detail lifecycle timeline static contract'
)

assert.match(
  detailPresentation,
  /export const buildDetailLifecycleTimelineItems/,
  'detail presentation must expose a real-data lifecycle timeline builder'
)
assert.match(
  detailPresentation,
  /submittedTime/,
  'timeline builder must include submitted time when available'
)
assert.match(
  detailPresentation,
  /publishedTime/,
  'timeline builder must include published time when available'
)
assert.match(detailPresentation, /effectiveDate/, 'timeline builder must include effective date')
assert.match(detailPresentation, /distributionStatuses/, 'timeline builder must include distribution events')
assert.match(detailPresentation, /trainingStatuses/, 'timeline builder must include training events')
assert.match(detailPresentation, /signatureSummaries/, 'timeline builder must include signature events')
assert.match(detailPresentation, /versionHistory/, 'timeline builder must include version events')
assert.match(detailPresentation, /externalReview/, 'timeline builder must include external review events')

assert.match(
  detailPage,
  /data-testid="dcc-detail-lifecycle-timeline"/,
  'detail page must render a stable lifecycle timeline region'
)
assert.match(detailPage, /关键记录时间线/, 'detail page must show the lifecycle timeline heading')
assert.match(
  detailPage,
  /detailLifecycleTimelineItems/,
  'detail page must compute lifecycle timeline items from current file detail'
)
assert.match(
  detailPage,
  /buildDetailLifecycleTimelineItems/,
  'detail page must reuse the presentation timeline builder'
)

const timelineTemplateMatch = detailPage.match(
  /<ContentWrap>\s*<div class="mb-12px flex items-center justify-between gap-12px">[\s\S]*?dcc-detail-lifecycle-timeline[\s\S]*?<\/ContentWrap>/
)
assert.ok(timelineTemplateMatch, 'detail page must keep the lifecycle timeline template together')
const timelineBuilderMatch = detailPresentation.match(
  /export const buildDetailLifecycleTimelineItems[\s\S]*?const userNameMapValue/
)
assert.ok(timelineBuilderMatch, 'detail presentation must keep the lifecycle timeline builder together')
const timelineContractSource = `${timelineTemplateMatch[0]}\n${timelineBuilderMatch[0]}`
assert.doesNotMatch(
  timelineContractSource,
  /mock|placeholder|访问事件|消息送达|detailLifecycleDeadline|detailLifecycleSla|截止时间|\bSLA\b/i,
  'lifecycle timeline must not invent unsupported events, deadlines, SLA, or mock data'
)

console.log('PASS: DCC detail lifecycle timeline static contract')
