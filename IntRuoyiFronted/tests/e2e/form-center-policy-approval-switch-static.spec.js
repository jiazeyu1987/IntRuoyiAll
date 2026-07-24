const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const apiSource = readSource('src/api/form-center/policy.ts')
const pageSource = readSource('src/views/form-center/policy/index.vue')

assert.match(
  apiSource,
  /export\s+type\s+FormApprovalMode\s*=\s*'BPM_REQUIRED'\s*\|\s*'DIRECT'/,
  'form policy API must expose explicit form approval modes'
)
assert.match(apiSource, /approvalMode:\s*FormApprovalMode/, 'form policy VO must expose approvalMode')
assert.match(apiSource, /approvalMode\??:\s*FormApprovalMode/, 'form policy save request must submit approvalMode')
assert.match(apiSource, /FormPolicySwitchApprovalModeReqVO/, 'form policy API must expose switch approval mode request')
assert.match(apiSource, /switchPolicyApprovalMode/, 'form policy API must expose one-click approval mode switch')
assert.match(
  apiSource,
  /`\/form-center\/policies\/\$\{policyId\}\/switch-approval-mode`/,
  'form policy API must call the backend switch-approval-mode endpoint'
)

assert.match(pageSource, /审批模式/, 'form policy page must show approval mode')
assert.match(pageSource, /审批开关/, 'form policy page must show an approval switch column')
assert.match(pageSource, /el-switch/, 'form policy page must use a real switch control')
assert.match(pageSource, /handleApprovalModeSwitch/, 'form policy page must route switch changes through one handler')
assert.match(pageSource, /switchPolicyApprovalMode/, 'form policy page must call the switch approval mode API')
assert.match(pageSource, /switchingPolicyId/, 'form policy page must show row-level switch loading state')
assert.match(pageSource, /v-hasPermi="\['form:policy:publish'\]"/, 'switch must use existing publish permission')
assert.match(pageSource, /approvalModeOptions/, 'form policy page must use explicit approval mode options')
assert.match(pageSource, /BPM_REQUIRED/, 'form policy page must support BPM_REQUIRED mode')
assert.match(pageSource, /DIRECT/, 'form policy page must support DIRECT mode')
assert.match(pageSource, /开启审批/, 'form policy page must show enabled approval copy')
assert.match(pageSource, /直接生效/, 'form policy page must show direct mode copy')
assert.match(pageSource, /审批开启时必须填写流程Key/, 'form policy page must fail fast when BPM mode misses process key')
assert.match(pageSource, /getList\(\)/, 'form policy page must refresh after switch success or failure')
assert.match(pageSource, /resolvePolicyErrorMessage/, 'form policy page must render backend errors')
assert.match(pageSource, /message\.error/, 'form policy page must show switch errors')
assert.doesNotMatch(pageSource, /catch\s*\{\s*\}/, 'form policy page must not hide request failures')
assert.doesNotMatch(pageSource, /catch\s*\([^)]*\)\s*\{\s*\}/, 'form policy page must not hide request failures')

console.log('form-center policy approval switch static contract passed')
