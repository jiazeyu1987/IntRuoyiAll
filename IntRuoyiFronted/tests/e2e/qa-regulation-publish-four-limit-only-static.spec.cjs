const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/QaRegulationPage.vue'
)
const source = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')

const checksStart = source.indexOf('const qaRegulationPublishChecks = computed')
const checksEnd = source.indexOf('const qaPublishBlockers = computed', checksStart)
assert.ok(
  checksStart >= 0 && checksEnd > checksStart,
  'QA publish must have a dedicated publish-only check list.'
)
const checksSource = source.slice(checksStart, checksEnd)

for (const label of ['DCC 项目代码', 'MDM 产品绑定', '正式工艺路线', '版本不可变']) {
  assert.match(checksSource, new RegExp('label:\\s*\'' + label + '\''), 'Missing publish check: ' + label)
}

for (const oldLabel of [
  '规程版本信息',
  '首检/巡检/末检规则',
  '检验项目字段',
  '数值上下限',
  '原文依据摘录'
]) {
  assert.doesNotMatch(
    checksSource,
    new RegExp(oldLabel),
    'Publish checks must not include removed content blocker: ' + oldLabel
  )
}

const blockersStart = source.indexOf('const qaPublishBlockers = computed')
const blockersEnd = source.indexOf('const pagedQaRegulationCompletenessChecks = computed', blockersStart)
assert.ok(blockersStart >= 0 && blockersEnd > blockersStart, 'QA publish blockers must be declared.')
const blockersSource = source.slice(blockersStart, blockersEnd)

assert.match(
  blockersSource,
  /qaRegulationPublishChecks\.value\.filter/,
  'Publish blockers must come only from the four retained checks.'
)
assert.doesNotMatch(
  blockersSource,
  /qaRegulationCompletenessChecks/,
  'Publish blockers must not use old content completeness checks.'
)

const routeResolverStart = source.indexOf('const resolveQaRegulationItemRouteProcesses =')
const routeResolverEnd = source.indexOf('const buildQaProcessRegulationCode =', routeResolverStart)
assert.ok(
  routeResolverStart >= 0 && routeResolverEnd > routeResolverStart,
  'QA route-process resolver must exist.'
)
const routeResolverSource = source.slice(routeResolverStart, routeResolverEnd)

assert.match(
  source,
  /const normalizeQaProcessBindingName[\s\S]*\.replace\(\/工序\$\/[^,]*,\s*''\)/,
  'QA process matching must treat display suffix “工序” as presentation text.'
)
assert.doesNotMatch(
  routeResolverSource,
  /publishing[\s\S]*routeProcess:\s*source\.routeProcess/,
  'Publish must not guess an unmatched item belongs to the current route process.'
)

const payloadBuilderStart = source.indexOf('const buildQaRegulationSavePayloads =')
const payloadBuilderEnd = source.indexOf('const previewQaRegulationDraft =', payloadBuilderStart)
assert.ok(
  payloadBuilderStart >= 0 && payloadBuilderEnd > payloadBuilderStart,
  'QA payload builder must exist.'
)
const payloadBuilderSource = source.slice(payloadBuilderStart, payloadBuilderEnd)

assert.match(
  payloadBuilderSource,
  /resolveQaRegulationItemRouteProcesses\(item, source\)/,
  'Publish must use the same formal route-process identity resolution as draft save.'
)

const publishStart = source.indexOf('const runQaPublishPrecheck = async')
const publishEnd = source.indexOf('</script>', publishStart)
assert.ok(publishStart >= 0 && publishEnd > publishStart, 'Publish handler must exist.')
const publishSource = source.slice(publishStart, publishEnd)

assert.match(
  publishSource,
  /buildQaRegulationSavePayloads\(\{ publishing: true \}\)/,
  'Publish must use the relaxed publishing payload builder.'
)
assert.doesNotMatch(
  publishSource,
  /规则需补齐|末检不适用时必须填写正式依据|检验项目字段|数值上下限|原文依据/,
  'Publish handler must not keep removed content blocker messages.'
)

console.log('PASS qa-regulation-publish-four-limit-only-static')
