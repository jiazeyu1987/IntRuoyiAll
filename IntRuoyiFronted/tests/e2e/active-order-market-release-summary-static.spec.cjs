const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(frontendRoot, '..')
const read = (relativePath) => fs.readFileSync(path.resolve(repoRoot, relativePath), 'utf8')

const panel = read(
  'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)

assert.match(
  panel,
  /data-active-order-summary-market-release-table/,
  '总表必须包含单独的上市放行摘要表。'
)
assert.match(
  panel,
  /上市放行状态[\s\S]*summaryMarketRelease\.statusLabel/,
  '上市放行摘要必须展示“已上市放行”等状态文案。'
)
assert.match(
  panel,
  /上市放行电子签名[\s\S]*data-active-order-summary-market-release-signature[\s\S]*formatMarketReleaseSignatureCellText\(summaryMarketRelease\)/,
  '上市放行摘要必须展示上市放行电子签名信息。'
)
assert.match(
  panel,
  /const summaryMarketRelease = computed[\s\S]*BATCH_RECORD_RELEASE_APPROVED[\s\S]*批记录上市放行[\s\S]*resultStatus === 'SUCCESS'/,
  '上市放行摘要只能来自成功的批记录上市放行事实。'
)
assert.doesNotMatch(
  panel,
  /CLOSE_ACTIVE_ORDER_BY_RELEASE[\s\S]{0,400}summaryMarketRelease/,
  'CLOSE_ACTIVE_ORDER_BY_RELEASE 不得作为上市放行摘要来源。'
)

console.log('PASS: active-order market release summary static contract')
