const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(frontendRoot, '..')
const read = (relativePath) => fs.readFileSync(path.resolve(repoRoot, relativePath), 'utf8')

const panel = read(
  'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const signatureRecords = read(
  'IntRuoyiFronted/src/views/signature-governance/components/SignatureGovernanceRecordsPane.vue'
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
  /data-active-order-summary-market-release-signature[\s\S]*:disabled="!summaryMarketRelease\.signature\?\.signatureId"[\s\S]*openActiveOrderSignatureRecord\(summaryMarketRelease\.signature\)/,
  '上市放行签名必须按正式 signatureId 跳转到签名记录。'
)
assert.match(
  panel,
  /const formatMarketReleaseSignatureCellText =[\s\S]*formatActiveOrderSignatureCellText\(summary\?\.signature\)/,
  '上市放行签名缺少正式 signatureId 时必须显示未签名，不得只凭操作人和时间显示签名。'
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
assert.match(
  signatureRecords,
  /MARKET_RELEASE:\s*'上市放行'/,
  '正式签名记录页必须识别上市放行签名动作。'
)

console.log('PASS: active-order market release summary static contract')
