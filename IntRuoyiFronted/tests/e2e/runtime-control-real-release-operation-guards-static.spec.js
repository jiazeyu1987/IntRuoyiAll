const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  const filePath = path.join(repoRoot, relativePath)
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const scripts = [
  {
    label: 'publish-test real flow',
    path: 'tests/e2e/runtime-control-publish-test-real-flow.e2e.js',
    allowName: 'RUNTIME_CONTROL_ALLOW_REAL_PUBLISH',
    approvalName: 'RUNTIME_CONTROL_REAL_PUBLISH_APPROVAL',
    approvalToken: 'ALLOW_TEST_RUNTIME_PUBLISH_WRITE'
  },
  {
    label: 'promote-backup real flow',
    path: 'tests/e2e/runtime-control-promote-backup-real-flow.e2e.js',
    allowName: 'RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_BACKUP',
    approvalName: 'RUNTIME_CONTROL_REAL_PROMOTE_BACKUP_APPROVAL',
    approvalToken: 'ALLOW_TEST_RUNTIME_PROMOTE_BACKUP_WRITE'
  },
  {
    label: 'promote-prod real flow',
    path: 'tests/e2e/runtime-control-promote-prod-real-flow.e2e.js',
    allowName: 'RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_PROD',
    approvalName: 'RUNTIME_CONTROL_REAL_PROMOTE_PROD_APPROVAL',
    approvalToken: 'ALLOW_PROD_RUNTIME_PROMOTE_WRITE'
  }
]

for (const item of scripts) {
  const source = readUtf8(item.path)
  assert.ok(source.includes(item.allowName), `${item.label} must keep an explicit allow switch`)
  assert.ok(source.includes(item.approvalName), `${item.label} must require an explicit approval token`)
  assert.ok(source.includes(item.approvalToken), `${item.label} must check the expected approval token`)
  assert.ok(
    source.includes('only after explicit user approval'),
    `${item.label} must explain that approval token is only set after explicit user approval`
  )
  const guardDefinitionIndex = source.indexOf('function requireExplicitApproval()')
  const approvalCheckIndex = source.indexOf('if (APPROVAL !== APPROVAL_TOKEN)')
  const guardCallIndex = source.lastIndexOf('requireExplicitApproval()')
  const browserFlowIndex = source.lastIndexOf('runRuntimeControlE2E(')
  assert.ok(guardDefinitionIndex >= 0, `${item.label} must define requireExplicitApproval`)
  assert.ok(
    approvalCheckIndex > guardDefinitionIndex && approvalCheckIndex < guardCallIndex,
    `${item.label} must check approval inside requireExplicitApproval`
  )
  assert.ok(guardCallIndex > guardDefinitionIndex, `${item.label} must call requireExplicitApproval`)
  assert.ok(
    guardCallIndex < browserFlowIndex,
    `${item.label} must check approval before browser launch`
  )
}

console.log('PASS: runtime-control real release operation guards require explicit approval tokens')
