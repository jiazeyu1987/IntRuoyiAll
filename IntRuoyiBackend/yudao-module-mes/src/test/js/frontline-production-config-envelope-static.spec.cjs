const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../../..')
const source = fs.readFileSync(path.join(root,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigServiceImpl.java'), 'utf8')

assert.match(source, /validateProductionConfigEnvelopeIfPresent\(snapshot/)
assert.match(source, /getProductionConfigSnapshotSha256\(\)[\s\S]*DigestUtil\.sha256Hex/)
assert.match(source, /getLossReasonSnapshotSha256\(\)[\s\S]*getOveragePercentSnapshot/)
assert.match(source, /PRODUCTION_CONTEXT_PREFIX \+ "productionConfigSnapshot"/)

console.log('PASS: frontline validates the unified production configuration envelope')
