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
const source = readSource('src/views/bpm/processInstance/detail/ProcessInstanceOperationButton.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:bpm-approval-viewer:static'],
  'node tests/e2e/dcc-bpm-dcc-approval-viewer-static.spec.js',
  'package.json must expose the DCC BPM approval viewer static contract'
)

assert.match(
  source,
  /CONTROLLED_FILE_PROCESS_DEFINITION_KEY/,
  'BPM process-instance operation button must keep DCC process detection'
)
assert.match(
  source,
  /message\.warning\('DCC受控文件审批请返回文控中心完成电子签名。'\)/,
  'DCC BPM approval branch must keep the warning message'
)
assert.match(
  source,
  /openControlledFileViewer\(router,\s*route,\s*businessKey,\s*'bpm-dcc-approval'\)/,
  'DCC BPM approval branch must route to the shared controlled file viewer helper'
)
assert.doesNotMatch(
  source,
  /router\.push\(\{[\s\S]*name:\s*'DccControlledFileDetail'[\s\S]*businessKey/,
  'DCC BPM approval branch must not route to the normal detail page'
)
assert.doesNotMatch(source, /mock|fallback|降级|吞异常/i, 'BPM DCC approval viewer routing must stay fail-fast')

console.log('PASS: DCC BPM approval viewer static contract')
