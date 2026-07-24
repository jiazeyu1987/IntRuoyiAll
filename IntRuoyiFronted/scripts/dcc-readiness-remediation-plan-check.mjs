import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '..')
const artifactPath =
  process.env.DCC_READINESS_REMEDIATION_PLAN_ARTIFACT ||
  path.resolve(
    repoRoot,
    '../release_and_backup-root/doc/tasks/20260609-release-and-backup-implementation/artifacts/dcc-incremental-backup-restore-readiness.json'
  )

const expectedActionOrder = [
  'prepare_isolated_test_tenant_package',
  'assign_test_tenant_mdm_product_menu',
  'prepare_test_tenant_dcc_product',
  'prepare_test_tenant_dcc_upload_size_policy'
]

const expectedActionByStep = new Map([
  [
    'test tenant package readiness',
    {
      actionId: 'prepare_isolated_test_tenant_package',
      script: 'tests/e2e/mdm-tenant-package-real-setup.e2e.js',
      baseUrlEnv: 'TENANT_PACKAGE_E2E_BASE_URL',
      allowEnv: 'TENANT_PACKAGE_E2E_ALLOW_WRITE',
      approvalEnv: 'TENANT_PACKAGE_E2E_APPROVAL',
      additionalEnv: ['TENANT_PACKAGE_E2E_TARGET_TENANT']
    }
  ],
  [
    'MDM role menu readiness',
    {
      actionId: 'assign_test_tenant_mdm_product_menu',
      script: 'tests/e2e/mdm-role-menu-real-setup.e2e.js',
      baseUrlEnv: 'MDM_ROLE_E2E_BASE_URL',
      allowEnv: 'MDM_ROLE_E2E_ALLOW_ASSIGN',
      approvalEnv: 'MDM_ROLE_E2E_APPROVAL',
      additionalEnv: ['MDM_ROLE_E2E_TENANT', 'MDM_ROLE_E2E_USERNAME']
    }
  ],
  [
    'MDM product readiness',
    {
      actionId: 'prepare_test_tenant_dcc_product',
      script: 'tests/e2e/mdm-product-real-setup.e2e.js',
      baseUrlEnv: 'MDM_PRODUCT_E2E_BASE_URL',
      allowEnv: 'MDM_PRODUCT_E2E_ALLOW_CREATE',
      approvalEnv: 'MDM_PRODUCT_E2E_APPROVAL',
      additionalEnv: ['MDM_PRODUCT_E2E_TENANT', 'MDM_PRODUCT_E2E_USERNAME']
    }
  ],
  [
    'DCC upload size policy readiness',
    {
      actionId: 'prepare_test_tenant_dcc_upload_size_policy',
      script: 'tests/e2e/dcc-upload-size-policy-real-setup.e2e.js',
      baseUrlEnv: 'DCC_UPLOAD_POLICY_E2E_BASE_URL',
      allowEnv: 'DCC_UPLOAD_POLICY_E2E_ALLOW_WRITE',
      approvalEnv: 'DCC_UPLOAD_POLICY_E2E_APPROVAL',
      additionalEnv: ['DCC_UPLOAD_POLICY_E2E_TENANT', 'DCC_UPLOAD_POLICY_E2E_USERNAME']
    }
  ]
])

const expectedManualResolutionByStep = new Map()

function readArtifact() {
  assert.ok(fs.existsSync(artifactPath), `readiness artifact is missing: ${artifactPath}`)
  return JSON.parse(fs.readFileSync(artifactPath, 'utf8'))
}

function assertNoApprovalValueLeak(value, label) {
  const text = JSON.stringify(value)
  assert.ok(!/ALLOW_(TEST|PROD)_[A-Z0-9_]+/.test(text), `${label} leaks an approval token value`)
}

function expectedActionsFor(blockedSteps) {
  return blockedSteps
    .map((step) => {
    const expected = expectedActionByStep.get(step.name)
    return expected
  })
    .filter(Boolean)
}

function expectedManualResolutionsFor(blockedSteps) {
  return blockedSteps
    .map((step) => {
      const expected = expectedManualResolutionByStep.get(step.name)
      return expected ? { step: step.name, ...expected } : null
    })
    .filter(Boolean)
}

function assertOrderedSubset(actionIds) {
  let previousIndex = -1
  for (const actionId of actionIds) {
    const currentIndex = expectedActionOrder.indexOf(actionId)
    assert.notEqual(currentIndex, -1, `unexpected remediation actionId: ${actionId}`)
    assert.ok(
      currentIndex > previousIndex,
      `remediation action order must follow expectedActionOrder: ${actionIds.join(', ')}`
    )
    previousIndex = currentIndex
  }
}

function validateAction(action, expected, artifact) {
  assert.equal(action.actionId, expected.actionId, `${expected.actionId} actionId mismatch`)
  assert.equal(action.step, [...expectedActionByStep.entries()].find(([, item]) => item === expected)?.[0])
  assert.equal(action.script, expected.script, `${expected.actionId} script mismatch`)
  assert.equal(action.baseUrlEnv, expected.baseUrlEnv, `${expected.actionId} baseUrlEnv mismatch`)
  assert.equal(action.requiredAllowEnv, expected.allowEnv, `${expected.actionId} allow env mismatch`)
  assert.equal(action.requiredApprovalEnv, expected.approvalEnv, `${expected.actionId} approval env mismatch`)
  assert.ok(action.writeScope, `${expected.actionId} writeScope is required`)
  assert.ok(action.guardrail, `${expected.actionId} guardrail is required`)

  const template = action.nextRunCommandTemplate
  assert.ok(template, `${expected.actionId} nextRunCommandTemplate is required`)
  assert.equal(template.shell, 'powershell', `${expected.actionId} template shell must be powershell`)
  assert.equal(template.cwd, repoRoot, `${expected.actionId} template cwd must be the frontend worktree`)
  assert.equal(template.command, `node ${expected.script}`, `${expected.actionId} template command mismatch`)
  assert.equal(
    template.approvalValuePolicy,
    'omitted-from-readiness-artifact; the target script enforces the exact approval token after explicit user approval',
    `${expected.actionId} approvalValuePolicy mismatch`
  )
  assertNoApprovalValueLeak(template, `${expected.actionId} nextRunCommandTemplate`)

  const env = template.requiredEnv || {}
  assert.equal(env.DCC_BACKUP_E2E_BASE_URL, artifact.baseUrl, `${expected.actionId} missing shared base URL env`)
  assert.equal(env[expected.baseUrlEnv], artifact.baseUrl, `${expected.actionId} missing action base URL env`)
  assert.equal(env[expected.allowEnv], 'true', `${expected.actionId} allow env must be true in the template`)
  assert.equal(
    env[expected.approvalEnv],
    '<set-after-explicit-user-approval>',
    `${expected.actionId} approval env must be a placeholder`
  )
  for (const envName of expected.additionalEnv) {
    assert.ok(env[envName], `${expected.actionId} missing required env ${envName}`)
  }
}

function validateManualResolution(resolution, expected) {
  assert.equal(resolution.step, expected.step, `${expected.step} manual resolution step mismatch`)
  assert.equal(
    resolution.blockerCode,
    expected.blockerCode,
    `${expected.step} manual resolution blockerCode mismatch`
  )
  for (const field of expected.requiredFields) {
    assert.ok(resolution[field], `${expected.step} manual resolution missing ${field}`)
  }
}

function main() {
  const artifact = readArtifact()
  assertNoApprovalValueLeak(artifact, 'readiness artifact')
  assert.equal(artifact.writeMode, false, 'readiness remediation plan check only accepts read-only readiness artifacts')
  const blockedSteps = Array.isArray(artifact.blockedSteps) ? artifact.blockedSteps : []
  const actions = Array.isArray(artifact.remediationActions) ? artifact.remediationActions : []
  const manualResolutions = Array.isArray(artifact.manualResolutions) ? artifact.manualResolutions : []
  if (artifact.ready === true) {
    assert.equal(blockedSteps.length, 0, 'ready artifact must not contain blockedSteps')
    assert.equal(actions.length, 0, 'ready artifact must not ask for remediation actions')
    assert.equal(manualResolutions.length, 0, 'ready artifact must not ask for manual resolutions')
  } else {
    assert.ok(blockedSteps.length > 0, 'blocked artifact must contain blockedSteps')
    const expectedActions = expectedActionsFor(blockedSteps)
    const expectedManualResolutions = expectedManualResolutionsFor(blockedSteps)
    assert.ok(
      expectedActions.length + expectedManualResolutions.length > 0,
      'blocked artifact must contain at least one known remediation action or manual resolution'
    )
    assert.deepEqual(
      actions.map((action) => action.actionId),
      expectedActions.map((action) => action.actionId),
      'remediation action order must follow known blocked step order'
    )
    assert.deepEqual(
      manualResolutions.map((resolution) => resolution.step),
      expectedManualResolutions.map((resolution) => resolution.step),
      'manual resolution order must follow known blocked step order'
    )
    assertOrderedSubset(expectedActions.map((action) => action.actionId))
    actions.forEach((action, index) => validateAction(action, expectedActions[index], artifact))
    manualResolutions.forEach((resolution, index) =>
      validateManualResolution(resolution, expectedManualResolutions[index])
    )
  }
  assertNoApprovalValueLeak(actions.map((action) => action.nextRunCommandTemplate), 'remediation command templates')
  assertNoApprovalValueLeak(manualResolutions, 'manual resolutions')
  console.log(
    `DCC_READINESS_REMEDIATION_PLAN_CHECK ${JSON.stringify({
      ready: artifact.ready,
      artifactPath,
      actionIds: actions.map((action) => action.actionId),
      manualResolutionSteps: manualResolutions.map((resolution) => resolution.step),
      doesNotExecuteRemediationActions: true
    })}`
  )
}

// This check intentionally does not execute remediation actions.
main()
