const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../../..')
const source = fs.readFileSync(path.join(root,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigServiceImpl.java'), 'utf8')
const parameterCodec = fs.readFileSync(path.join(root,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesDeviceParameterSnapshotCodec.java'), 'utf8')

assert.match(source, /validateProductionConfigEnvelopeIfPresent\(snapshot/)
assert.match(source, /getProductionConfigSnapshotSha256\(\)[\s\S]*DigestUtil\.sha256Hex/)
assert.match(source, /getLossReasonSnapshotSha256\(\)[\s\S]*getOveragePercentSnapshot/)
assert.match(source, /PRODUCTION_CONTEXT_PREFIX \+ "productionConfigSnapshot"/)
assert.match(source, /compareProductionConfigOverage\([\s\S]*\.compareTo\(/)
assert.match(source, /productionParameterRulesMatch\(/)
assert.match(source, /MesDeviceParameterSnapshotCodec\.matchesCanonicalSnapshot/)
assert.match(parameterCodec, /matchesCanonicalSnapshot\([\s\S]*canonicalizeSnapshotRules\([\s\S]*candidateCanonical/)
assert.doesNotMatch(source,
  /Objects\.equals\(parameterRules,\s*envelope\.getJSONArray\("parameterRules"\)\)/,
  'parameterRules must be compared through canonical snapshot rules, not raw JSONArray equality')

console.log('PASS: frontline validates the unified production configuration envelope')
