const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

function read(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const gate = read('scripts/controlled-content-release-claim-gate.mjs')
const packageJson = JSON.parse(read('package.json'))

assert.equal(
  packageJson.scripts['e2e:controlled-content:release-gate'],
  'node scripts/controlled-content-release-claim-gate.mjs',
  '完整受控内容状态机放行必须有独立 release gate 入口，不得复用只读状态展示 E2E 冒充。'
)

assert.equal(
  packageJson.scripts['e2e:controlled-content:release-gate:expect-blocked'],
  'node scripts/controlled-content-release-claim-gate.mjs --expect-blocked',
  '当前缺少写入型真实 E2E 时，必须提供 expect-blocked 审计命令用于证明门禁会阻断。'
)

for (const marker of [
  'controlled-content-dcc-sop-release-real.json',
  'controlled-content-dcc-work-instruction-review-readonly-real.json',
  'controlled-content-dcc-inspection-withdraw-draft-real.json',
  'controlled-content-dcc-drawing-obsolete-real.json',
  'controlled-content-mes-route-version-full-flow-real.json',
  'controlled-content-live-migration-real.json',
  'controlled-content-health-check-readonly-real.json'
]) {
  assert.match(gate, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `${marker} must be required by release gate`)
}

assert.match(gate, /expectBlocked/, 'release gate must support an explicit blocked-mode audit without turning blockers into PASS.')
assert.match(
  gate,
  /e2e:controlled-content:state-view:real\|controlled-content-state-view-real-readonly/,
  'release gate must recognize the existing readonly DCC evidence without treating it as full write E2E.'
)
assert.match(
  gate,
  /requireTestTenantPlaywrightWriteArtifact/,
  'write E2E artifacts must prove they came from the test tenant Playwright UI path, not a hand-written PASS file.'
)
for (const marker of [
  "tenant === '测试租户'",
  "username === 'aoteman'",
  "executionMode === 'playwright-ui'",
  'directApiWrites === 0',
  'sqlBusinessDataWritePerformed === false',
  'mockDataUsed === false'
]) {
  assert.match(gate, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `${marker} must be part of write artifact validation`)
}
for (const marker of [
  'SUPERSEDED',
  'directEditRejected',
  'newDraftCount',
  'masterCurrentActiveCleared',
  'editedAfterWithdraw',
  'failedAssertions',
  'artifactBlockers',
  'uniqueMessages',
  'controlled_content_transition_audit',
  'scriptSha256',
  'migrationScriptSha256'
]) {
  assert.match(gate, new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `${marker} must be required by scenario-specific release evidence`)
}
assert.match(gate, /createHash\('sha256'\)/, 'release gate must hash the current migration SQL to reject stale migration artifacts.')
assert.match(gate, /process\.exit\(result\.status === 'PASS' \? 0 : 1\)/, 'normal release gate must fail fast until all requirements pass.')
assert.match(
  gate,
  /found but no PASS artifact satisfied contract/,
  'release gate diagnostics must explain when artifacts exist but fail the evidence contract.'
)
assert.match(gate, /latestArtifactOnly/, 'release gate must use the newest matching artifact only.')
assert.match(gate, /mtimeMs/, 'release gate must compare artifact timestamps to prevent stale PASS evidence.')
assert.match(
  gate,
  /newest matching artifact failed contract/,
  'release gate diagnostics must explain when the newest artifact exists but does not satisfy the evidence contract.'
)
assert.match(gate, /parseError/, 'release gate must retain invalid JSON parse details instead of swallowing them.')
assert.match(gate, /invalid JSON artifact/, 'release gate diagnostics must identify invalid JSON artifacts.')
assert.match(gate, /failedAssertions=/, 'release gate diagnostics must surface failed assertion keys from newest artifacts.')
assert.match(gate, /artifactBlockers=/, 'release gate diagnostics must surface blocker reasons from newest artifacts.')
assert.doesNotMatch(gate, /catch\s*\{\s*\/\/ Invalid JSON is not accepted as evidence\.\s*\}/, 'release gate must not silently swallow artifact JSON parse errors.')
assert.doesNotMatch(gate, /fallback|mock success|默认通过/, 'release gate must not contain fallback or mock-success wording.')

console.log('PASS: controlled content release claim gate static contract')
