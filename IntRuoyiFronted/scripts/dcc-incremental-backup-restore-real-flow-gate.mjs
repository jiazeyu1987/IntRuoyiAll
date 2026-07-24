import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  redactApprovalTokens,
  stripWriteControlEnv
} from './dcc-write-control-env.mjs'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '..')
const APPROVAL_TOKEN = 'ALLOW_TEST_DCC_INCREMENTAL_BACKUP_RESTORE'
const ALLOW_REAL_WRITE = process.env.DCC_INCREMENTAL_BACKUP_RESTORE_E2E_ALLOW_REAL_WRITE === '1'
const APPROVAL = process.env.DCC_INCREMENTAL_BACKUP_RESTORE_E2E_APPROVAL || ''
const STEP_TIMEOUT_MS = Number(process.env.DCC_INCREMENTAL_BACKUP_RESTORE_E2E_STEP_TIMEOUT_MS || 2 * 60 * 60 * 1000)
const RUN_PREREQ_SETUP = process.env.DCC_INCREMENTAL_BACKUP_RESTORE_E2E_RUN_PREREQ_SETUP !== '0'
const DCC_BASE_URL = (process.env.DCC_BACKUP_E2E_BASE_URL || '').replace(/\/+$/, '')
const RUNTIME_BASE_URL = (process.env.RUNTIME_CONTROL_E2E_BASE_URL || '').replace(/\/+$/, '')
const ACTION_ORIGIN = (process.env.RUNTIME_CONTROL_E2E_ACTION_ORIGIN || '').replace(/\/+$/, '')
const TENANT = process.env.DCC_BACKUP_E2E_TENANT || process.env.RUNTIME_CONTROL_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_BACKUP_E2E_USERNAME || process.env.RUNTIME_CONTROL_E2E_USERNAME || 'aoteman'
const READINESS_SCRIPT = 'scripts/dcc-incremental-backup-restore-readiness-gate.mjs'
const REMEDIATION_RUNNER_SCRIPT = 'scripts/dcc-readiness-remediation-runner.mjs'
const READINESS_REMEDIATION_ACTION_BY_STEP = new Map([
  [
    'test tenant package readiness',
    {
      actionId: 'prepare_isolated_test_tenant_package',
      script: 'tests/e2e/mdm-tenant-package-real-setup.e2e.js',
      baseUrlEnv: 'TENANT_PACKAGE_E2E_BASE_URL',
      allowEnv: 'TENANT_PACKAGE_E2E_ALLOW_WRITE',
      approvalEnv: 'TENANT_PACKAGE_E2E_APPROVAL',
      approvalToken: 'ALLOW_TEST_TENANT_PACKAGE_WRITE'
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
      approvalToken: 'ALLOW_TEST_MDM_ROLE_MENU_WRITE'
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
      approvalToken: 'ALLOW_TEST_MDM_PRODUCT_WRITE'
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
      requiresExternalApproval: true
    }
  ]
])
const READINESS_MANUAL_RESOLUTION_BY_STEP = new Map()
const ARTIFACT_PATH =
  process.env.DCC_INCREMENTAL_BACKUP_RESTORE_E2E_ARTIFACT ||
  path.resolve(
    repoRoot,
    '../release_and_backup-root/doc/tasks/20260609-release-and-backup-implementation/artifacts/dcc-incremental-backup-restore-real-flow.json'
  )
const readinessRemediationResults = []

function requireRealWriteApproval() {
  if (!ALLOW_REAL_WRITE || APPROVAL !== APPROVAL_TOKEN) {
    throw new Error(
      `Set DCC_INCREMENTAL_BACKUP_RESTORE_E2E_ALLOW_REAL_WRITE=1 and ` +
        `DCC_INCREMENTAL_BACKUP_RESTORE_E2E_APPROVAL=${APPROVAL_TOKEN} after explicit user approval.`
    )
  }
  assert.ok(DCC_BASE_URL, 'DCC_BACKUP_E2E_BASE_URL is required')
  assert.ok(RUNTIME_BASE_URL, 'RUNTIME_CONTROL_E2E_BASE_URL is required')
  assert.ok(ACTION_ORIGIN, 'RUNTIME_CONTROL_E2E_ACTION_ORIGIN is required')
  assert.equal(TENANT, '测试租户', `DCC incremental backup/restore E2E must use 测试租户, got ${TENANT}`)
  assert.notEqual(USERNAME, 'admin', 'DCC incremental backup/restore E2E must not use 芋道源码/admin')
  for (const [name, value] of [
    ['DCC_BACKUP_E2E_BASE_URL', DCC_BASE_URL],
    ['RUNTIME_CONTROL_E2E_BASE_URL', RUNTIME_BASE_URL],
    ['RUNTIME_CONTROL_E2E_ACTION_ORIGIN', ACTION_ORIGIN],
    ['RUNTIME_CONTROL_TEST_BACKEND_HEALTH_URL', process.env.RUNTIME_CONTROL_TEST_BACKEND_HEALTH_URL || ''],
    ['RUNTIME_CONTROL_TEST_FRONTEND_URL', process.env.RUNTIME_CONTROL_TEST_FRONTEND_URL || ''],
    ['RUNTIME_CONTROL_TEST_WEBSITE_URL', process.env.RUNTIME_CONTROL_TEST_WEBSITE_URL || ''],
    ['RUNTIME_CONTROL_TEST_SHOWROOM_URL', process.env.RUNTIME_CONTROL_TEST_SHOWROOM_URL || '']
  ]) {
    if (value) {
      assert.notEqual(new URL(value).hostname, '172.30.30.57', `${name} must not target protected production server 172.30.30.57`)
    }
  }
}

function buildStepEnv(extraEnv = {}) {
  const env = {
    ...process.env,
    DCC_BACKUP_E2E_BASE_URL: DCC_BASE_URL,
    RUNTIME_CONTROL_E2E_BASE_URL: RUNTIME_BASE_URL,
    RUNTIME_CONTROL_E2E_ACTION_ORIGIN: ACTION_ORIGIN,
    DCC_BACKUP_E2E_TENANT: TENANT,
    RUNTIME_CONTROL_E2E_TENANT: TENANT,
    DCC_BACKUP_E2E_USERNAME: USERNAME,
    RUNTIME_CONTROL_E2E_USERNAME: USERNAME
  }
  stripWriteControlEnv(env)
  return {
    ...env,
    ...extraEnv
  }
}

function outputTail(value, maxLength = 8000) {
  if (!value) return ''
  return value.length > maxLength ? value.slice(value.length - maxLength) : value
}

function buildStepFailure(name, result, output) {
  const sanitizedTail = redactApprovalTokens(outputTail(output))
  const error = new Error(`${name} failed with exit ${result.status}\n${sanitizedTail}`)
  error.name = 'DccRealFlowStepError'
  error.stepName = name
  error.exitCode = result.status
  error.outputTail = redactApprovalTokens(outputTail(output))
  return error
}

function runNodeStep(name, relativeScript, extraEnv = {}) {
  const scriptPath = path.resolve(repoRoot, relativeScript)
  assert.ok(fs.existsSync(scriptPath), `${name} script is missing: ${scriptPath}`)
  const result = spawnSync(process.execPath, [scriptPath], {
    cwd: repoRoot,
    env: buildStepEnv(extraEnv),
    encoding: 'utf8',
    timeout: STEP_TIMEOUT_MS,
    maxBuffer: 64 * 1024 * 1024
  })
  const output = `${result.stdout || ''}${result.stderr || ''}`
  process.stdout.write(redactApprovalTokens(output))
  if (result.error) {
    throw result.error
  }
  if (result.status !== 0) {
    throw buildStepFailure(name, result, output)
  }
  return output
}

function buildReadinessEnv() {
  const readinessEnv = {
    ...process.env,
    DCC_BACKUP_E2E_BASE_URL: DCC_BASE_URL,
    RUNTIME_CONTROL_E2E_BASE_URL: RUNTIME_BASE_URL,
    DCC_BACKUP_E2E_TENANT: TENANT,
    RUNTIME_CONTROL_E2E_TENANT: TENANT,
    DCC_BACKUP_E2E_USERNAME: USERNAME,
    RUNTIME_CONTROL_E2E_USERNAME: USERNAME
  }
  stripWriteControlEnv(readinessEnv)
  return readinessEnv
}

function parseMarkedJson(output, marker) {
  const markerIndex = output.lastIndexOf(marker)
  assert.notEqual(markerIndex, -1, `missing ${marker} in step output`)
  const start = output.indexOf('{', markerIndex)
  assert.notEqual(start, -1, `missing JSON object after ${marker}`)
  let depth = 0
  let inString = false
  let escape = false
  for (let index = start; index < output.length; index += 1) {
    const char = output[index]
    if (escape) {
      escape = false
      continue
    }
    if (char === '\\') {
      escape = true
      continue
    }
    if (char === '"') {
      inString = !inString
      continue
    }
    if (inString) continue
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return JSON.parse(output.slice(start, index + 1))
      }
    }
  }
  throw new Error(`unterminated JSON object after ${marker}`)
}

function formatReadinessBlockers(readiness) {
  const blockedSteps = Array.isArray(readiness?.blockedSteps) ? readiness.blockedSteps : []
  if (blockedSteps.length === 0) {
    return 'no blockedSteps were reported'
  }
  return blockedSteps
    .map((step) => `${step.name || 'unknown step'}: ${step.blocker || 'blocked without reason'}`)
    .join(' | ')
}

function formatReadinessRemediationActions(readiness) {
  const blockedSteps = Array.isArray(readiness?.blockedSteps) ? readiness.blockedSteps : []
  const actions = Array.isArray(readiness?.remediationActions) ? readiness.remediationActions : []
  const manualResolutions = Array.isArray(readiness?.manualResolutions) ? readiness.manualResolutions : []
  const expectedActionIds = blockedSteps
    .map((step) => READINESS_REMEDIATION_ACTION_BY_STEP.get(step.name))
    .map((action) => action?.actionId)
    .filter(Boolean)
  const expectedManualSteps = blockedSteps
    .map((step) => READINESS_MANUAL_RESOLUTION_BY_STEP.has(step.name) ? step.name : null)
    .filter(Boolean)
  const reportedActionIds = new Set(actions.map((action) => action.actionId).filter(Boolean))
  const reportedManualSteps = new Set(manualResolutions.map((resolution) => resolution.step).filter(Boolean))
  const missingActionIds = expectedActionIds.filter((actionId) => !reportedActionIds.has(actionId))
  const missingManualSteps = expectedManualSteps.filter((step) => !reportedManualSteps.has(step))
  const actionSummary =
    actions.length === 0
      ? 'no remediationActions were reported'
      : actions
          .map(
            (action) =>
              `${action.actionId || '(missing actionId)'}:` +
              `${action.script || '(missing script)'}:` +
              `${action.requiredAllowEnv || '(missing allow env)'}`
          )
          .join(' | ')
  const manualSummary =
    manualResolutions.length === 0
      ? 'no manualResolutions were reported'
      : manualResolutions
          .map((resolution) => `${resolution.step || '(missing step)'}:${resolution.blockerCode || '(missing code)'}`)
          .join(' | ')
  if (missingActionIds.length > 0) {
    return `missing remediation actionId(s): ${missingActionIds.join(', ')}; reported=${actionSummary}`
  }
  if (missingManualSteps.length > 0) {
    return `missing manual resolution step(s): ${missingManualSteps.join(', ')}; reported=${manualSummary}`
  }
  return `${actionSummary}; manualResolutions=${manualSummary}`
}

function validateReadinessRemediationPlan(readiness) {
  try {
    const blockedSteps = Array.isArray(readiness?.blockedSteps) ? readiness.blockedSteps : []
    if (blockedSteps.length === 0) return
    const actions = Array.isArray(readiness?.remediationActions) ? readiness.remediationActions : []
    const manualResolutions = Array.isArray(readiness?.manualResolutions) ? readiness.manualResolutions : []
    const expectedActions = blockedSteps
      .map((step) => READINESS_REMEDIATION_ACTION_BY_STEP.get(step.name))
      .filter(Boolean)
    const expectedManualResolutions = blockedSteps
      .map((step) => {
        const expected = READINESS_MANUAL_RESOLUTION_BY_STEP.get(step.name)
        return expected ? { step: step.name, ...expected } : null
      })
      .filter(Boolean)
    assert.ok(
      expectedActions.length + expectedManualResolutions.length > 0,
      'blocked artifact must contain at least one known remediation action or manual resolution'
    )
    assert.deepEqual(
      actions.map((action) => action.actionId),
      expectedActions.map((action) => action.actionId),
      'remediation action order does not match known blocked step order'
    )
    assert.deepEqual(
      manualResolutions.map((resolution) => resolution.step),
      expectedManualResolutions.map((resolution) => resolution.step),
      'manual resolution order does not match known blocked step order'
    )
    actions.forEach((action, index) => {
      const expected = expectedActions[index]
      assert.equal(action.script, expected.script, `${expected.actionId} script mismatch`)
      assert.equal(action.requiredAllowEnv, expected.allowEnv, `${expected.actionId} allow env mismatch`)
      assert.equal(action.requiredApprovalEnv, expected.approvalEnv, `${expected.actionId} approval env mismatch`)
      const template = action.nextRunCommandTemplate
      assert.ok(template, `${expected.actionId} nextRunCommandTemplate missing`)
      assert.equal(template.shell, 'powershell', `${expected.actionId} shell mismatch`)
      assert.equal(template.cwd, repoRoot, `${expected.actionId} cwd mismatch`)
      assert.equal(template.command, `node ${expected.script}`, `${expected.actionId} command mismatch`)
      assert.equal(
        template.approvalValuePolicy,
        'omitted-from-readiness-artifact; the target script enforces the exact approval token after explicit user approval',
        `${expected.actionId} approvalValuePolicy mismatch`
      )
      const env = template.requiredEnv || {}
      assert.equal(env.DCC_BACKUP_E2E_BASE_URL, readiness.baseUrl, `${expected.actionId} shared base URL mismatch`)
      assert.equal(env[expected.baseUrlEnv], readiness.baseUrl, `${expected.actionId} action base URL mismatch`)
      assert.equal(env[expected.allowEnv], 'true', `${expected.actionId} allow env must be true`)
      assert.equal(
        env[expected.approvalEnv],
        '<set-after-explicit-user-approval>',
        `${expected.actionId} approval env must be placeholder`
      )
      assert.ok(!/ALLOW_TEST_[A-Z0-9_]+/.test(JSON.stringify(template)), `${expected.actionId} leaks approval token`)
    })
    manualResolutions.forEach((resolution, index) => {
      const expected = expectedManualResolutions[index]
      assert.equal(resolution.step, expected.step, `${expected.step} manual resolution step mismatch`)
      assert.equal(
        resolution.blockerCode,
        expected.blockerCode,
        `${expected.step} manual resolution blockerCode mismatch`
      )
      assert.ok(resolution.requiredDecision, `${expected.step} manual resolution requiredDecision missing`)
      assert.ok(resolution.guardrail, `${expected.step} manual resolution guardrail missing`)
      assert.ok(resolution.writeScope, `${expected.step} manual resolution writeScope missing`)
      assert.ok(!/ALLOW_(TEST|PROD)_[A-Z0-9_]+/.test(JSON.stringify(resolution)), `${expected.step} manual resolution leaks approval token`)
    })
  } catch (error) {
    throw new Error(`readiness remediation plan invalid before DCC upload: ${error.message}`)
  }
}

function runReadinessProbe(label) {
  const scriptPath = path.resolve(repoRoot, READINESS_SCRIPT)
  assert.ok(fs.existsSync(scriptPath), `${label} script is missing: ${scriptPath}`)
  const result = spawnSync(process.execPath, [scriptPath], {
    cwd: repoRoot,
    env: buildReadinessEnv(),
    encoding: 'utf8',
    timeout: STEP_TIMEOUT_MS,
    maxBuffer: 64 * 1024 * 1024
  })
  const output = `${result.stdout || ''}${result.stderr || ''}`
  process.stdout.write(redactApprovalTokens(output))
  if (result.error) {
    throw result.error
  }
  let readiness
  try {
    readiness = parseMarkedJson(output, 'DCC_INCREMENTAL_BACKUP_RESTORE_READINESS_RESULT')
  } catch (error) {
    assert.equal(result.status, 0, `${label} failed with exit ${result.status}; readiness result marker is missing`)
    throw error
  }
  assert.equal(readiness.writeMode, false, `${label} must run the readiness gate in read-only mode`)
  if (result.status !== 0 && readiness.ready === true) {
    assert.equal(result.status, 0, `${label} failed with exit ${result.status} despite ready=true`)
  }
  if (readiness.ready !== true) {
    validateReadinessRemediationPlan(readiness)
  }
  return readiness
}

function blockedReadinessError(label, readiness) {
  return (
    `${label} readiness gate blocked before DCC upload: ${formatReadinessBlockers(readiness)}; ` +
    `remediationActions=${formatReadinessRemediationActions(readiness)}; ` +
    `artifactPath=${readiness.artifactPath || '(missing)'}`
  )
}

function createBlockedReadinessError(label, readiness) {
  const error = new Error(blockedReadinessError(label, readiness))
  error.readiness = readiness
  return error
}

function runReadinessGate(label) {
  const readiness = runReadinessProbe(label)
  if (readiness.ready !== true) {
    throw createBlockedReadinessError(label, readiness)
  }
  return readiness
}

function findRemediationContract(actionId) {
  const contract = Array.from(READINESS_REMEDIATION_ACTION_BY_STEP.values()).find((item) => item.actionId === actionId)
  assert.ok(contract, `readiness remediation action has no real-flow contract: ${actionId}`)
  return contract
}

function runReadinessRemediationAction(action) {
  const contract = findRemediationContract(action.actionId)
  const approvalValue = contract.approvalToken || process.env[contract.approvalEnv]
  assert.ok(
    approvalValue,
    `${contract.approvalEnv} is required for ${action.actionId}; this approval is separate from ${APPROVAL_TOKEN}`
  )
  const output = runNodeStep(`readiness remediation ${action.actionId}`, REMEDIATION_RUNNER_SCRIPT, {
    DCC_READINESS_REMEDIATION_RUN_ACTION_ID: action.actionId,
    DCC_READINESS_REMEDIATION_RUN_ALLOW_WRITE: '1',
    [contract.approvalEnv]: approvalValue
  })
  const result = parseMarkedJson(output, 'DCC_READINESS_REMEDIATION_RUNNER_RESULT')
  assert.equal(result.actionId, action.actionId, `${action.actionId} runner result action mismatch`)
  assert.equal(result.actionCleared, true, `${action.actionId} runner did not clear action`)
  const remainingActionIds = Array.isArray(result.remainingActionIds) ? result.remainingActionIds : []
  assert.ok(
    !remainingActionIds.includes(action.actionId),
    `${action.actionId} remains pending after guarded remediation runner`
  )
  return result
}

function assertRunnerRefreshMatchesReadiness(action, runnerResult, readiness) {
  const runnerArtifactPath = path.resolve(runnerResult.refreshedArtifactPath || '')
  const readinessArtifactPath = path.resolve(readiness.artifactPath || '')
  assert.equal(
    runnerArtifactPath,
    readinessArtifactPath,
    `${action.actionId} runner refreshedArtifactPath mismatch`
  )
  const runnerRemainingActionIds = Array.isArray(runnerResult.remainingActionIds)
    ? runnerResult.remainingActionIds
    : []
  const readinessActionIds = Array.isArray(readiness.remediationActions)
    ? readiness.remediationActions.map((item) => item.actionId).filter(Boolean)
    : []
  assert.deepEqual(
    runnerRemainingActionIds,
    readinessActionIds,
    `${action.actionId} runner remainingActionIds mismatch`
  )
  assert.equal(
    runnerResult.nextActionId || null,
    readinessActionIds[0] || null,
    `${action.actionId} runner nextActionId mismatch`
  )
  assert.equal(
    runnerResult.refreshedReady === true,
    readiness.ready === true,
    `${action.actionId} runner refreshedReady mismatch`
  )
  const readinessBlockedStepCount = Array.isArray(readiness.blockedSteps) ? readiness.blockedSteps.length : 0
  assert.equal(
    runnerResult.refreshedBlockedStepCount,
    readinessBlockedStepCount,
    `${action.actionId} runner refreshedBlockedStepCount mismatch`
  )
}

function runReadinessRemediationActions() {
  if (!RUN_PREREQ_SETUP) {
    return runReadinessGate('DCC B3/B4/B5 readiness before DCC upload')
  }
  const completedActionIds = new Set()
  let readiness = runReadinessProbe('DCC B3/B4/B5 readiness before prerequisite remediation')
  while (readiness.ready !== true) {
    const actions = Array.isArray(readiness.remediationActions) ? readiness.remediationActions : []
    if (actions.length === 0) {
      throw createBlockedReadinessError('DCC B3/B4/B5', readiness)
    }
    const action = actions[0]
    assert.ok(!completedActionIds.has(action.actionId), `readiness remediation did not clear action: ${action.actionId}`)
    completedActionIds.add(action.actionId)
    const runnerResult = runReadinessRemediationAction(action)
    readinessRemediationResults.push(runnerResult)
    assert.ok(
      completedActionIds.size <= READINESS_REMEDIATION_ACTION_BY_STEP.size,
      'readiness remediation exceeded known prerequisite action count'
    )
    readiness = runReadinessProbe(`DCC B3/B4/B5 readiness after remediation ${action.actionId}`)
    assertRunnerRefreshMatchesReadiness(action, runnerResult, readiness)
  }
  return readiness
}

function extractControlledFileId(uploadResult) {
  const candidates = [
    uploadResult?.submitResult?.data?.id,
    uploadResult?.submitResult?.data?.controlledFileId,
    uploadResult?.submitResult?.data,
    uploadResult?.submitPayload?.id,
    uploadResult?.submitPayload?.controlledFileId
  ]
  const value = candidates.find((item) => item !== undefined && item !== null && String(item).trim())
  assert.ok(value, `DCC_UPLOAD_RESULT did not expose controlled file id: ${JSON.stringify(uploadResult)}`)
  return String(value)
}

function runBackup(label) {
  const output = runNodeStep(label, 'tests/e2e/runtime-control-real-test-backup-setup.e2e.js', {
    RUNTIME_CONTROL_ALLOW_REAL_TEST_BACKUP_SETUP: '1',
    RUNTIME_CONTROL_REAL_TEST_BACKUP_APPROVAL: 'ALLOW_TEST_RUNTIME_BACKUP_WRITE'
  })
  const artifact = parseMarkedJson(output, 'REAL_TEST_BACKUP_ARTIFACT')
  assert.ok(artifact.backupId, `${label} did not produce backupId`)
  return artifact
}

function runRehearsal(label, backupId) {
  const output = runNodeStep(`${label} rehearsal`, 'tests/e2e/runtime-control-real-rehearsal.e2e.js', {
    RUNTIME_CONTROL_ALLOW_REAL_REHEARSAL: '1',
    RUNTIME_CONTROL_REAL_REHEARSAL_APPROVAL: 'ALLOW_TEST_RUNTIME_REHEARSAL_WRITE',
    RUNTIME_CONTROL_REAL_REHEARSAL_BACKUP_ID: backupId
  })
  const artifact = parseMarkedJson(output, 'REAL_REHEARSAL_ARTIFACT')
  assert.equal(artifact.backupId, backupId, `${label} rehearsal backupId mismatch`)
  assert.equal(artifact.status, 'succeeded', `${label} rehearsal did not succeed`)
  assert.ok(artifact.operationId, `${label} rehearsal did not expose operationId`)
  return artifact
}

function runRestoreAndVerify(label, backupId, expectations) {
  runNodeStep(`${label} restore-data`, 'tests/e2e/runtime-control-real-restore-data.e2e.js', {
    RUNTIME_CONTROL_ALLOW_REAL_RESTORE_DATA: '1',
    RUNTIME_CONTROL_REAL_RESTORE_DATA_APPROVAL: 'ALLOW_TEST_RUNTIME_RESTORE_WRITE',
    RUNTIME_CONTROL_REAL_RESTORE_BACKUP_ID: backupId,
    RUNTIME_CONTROL_REAL_RESTORE_TARGET_ENV: 'test'
  })
  const output = runNodeStep(`${label} DCC restore verify`, 'tests/e2e/dcc-restore-verify.e2e.js', {
    DCC_RESTORE_E2E_PRESENT: JSON.stringify(expectations.present),
    DCC_RESTORE_E2E_ABSENT: JSON.stringify(expectations.absent)
  })
  const result = parseMarkedJson(output, 'DCC_RESTORE_VERIFY_RESULT')
  assert.equal(result.tenant, '测试租户')
  assert.ok(Array.isArray(result.present), `${label} present result missing`)
  assert.ok(Array.isArray(result.absent), `${label} absent result missing`)
  return {
    restoreVerified: true,
    present: result.present,
    absent: result.absent
  }
}

function writeArtifact(payload) {
  fs.mkdirSync(path.dirname(ARTIFACT_PATH), { recursive: true })
  fs.writeFileSync(ARTIFACT_PATH, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

function buildFailureEvidence(error) {
  return {
    name: error?.name || 'Error',
    message: redactApprovalTokens(error?.message || String(error)),
    stack: redactApprovalTokens(error?.stack || ''),
    stepName: error?.stepName || undefined,
    exitCode: error?.exitCode ?? undefined,
    outputTail: error?.outputTail ? redactApprovalTokens(error.outputTail) : undefined
  }
}

function buildFailureArtifactWriteError(originalError, artifactWriteError) {
  const originalFailure = buildFailureEvidence(originalError)
  const artifactWriteFailure = buildFailureEvidence(artifactWriteError)
  const error = new Error(
    `DCC real-flow failed at ${flowArtifact.currentStage}; additionally failed to write failure artifact. ` +
      `Original failure: ${originalFailure.message}; artifact write failure: ${artifactWriteFailure.message}`
  )
  error.name = 'DccRealFlowFailureArtifactWriteError'
  error.failedStage = flowArtifact.currentStage
  error.artifactPath = ARTIFACT_PATH
  error.originalFailure = originalFailure
  error.artifactWriteFailure = artifactWriteFailure
  return error
}

requireRealWriteApproval()

const stamp = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
const fileName = process.env.DCC_INCREMENTAL_BACKUP_RESTORE_E2E_FILE_NAME || `codex-dr-${stamp}.docx`
const fileNumber = process.env.DCC_INCREMENTAL_BACKUP_RESTORE_E2E_FILE_NUMBER || `CDR-${stamp.slice(-6)}`
const flowArtifact = {
  baseUrl: DCC_BASE_URL,
  actionOrigin: ACTION_ORIGIN,
  tenant: TENANT,
  fileName,
  fileNumber,
  readinessRemediationResults,
  controlledFileIds: {},
  uploads: {},
  backups: {},
  rehearsals: {},
  restoreVerified: {},
  currentStage: 'initialized',
  stageHistory: [],
  status: 'running',
  startedAt: new Date().toISOString()
}

function setFlowStage(stage) {
  flowArtifact.currentStage = stage
  flowArtifact.stageHistory.push({
    stage,
    at: new Date().toISOString()
  })
}

try {
  setFlowStage('readiness remediation')
  flowArtifact.readiness = runReadinessRemediationActions()

  setFlowStage('DCC upload B3 V1')
  const uploadV1Output = runNodeStep('DCC upload B3 V1', 'tests/e2e/dcc-upload-test-file.e2e.js', {
    DCC_BACKUP_E2E_ALLOW_WRITE: '1',
    DCC_BACKUP_E2E_APPROVAL: 'ALLOW_TEST_DCC_FILE_WRITE',
    DCC_BACKUP_E2E_FILE_NAME: fileName,
    DCC_BACKUP_E2E_FILE_NUMBER: fileNumber,
    DCC_BACKUP_E2E_VERSION_NO: 'V1.0',
    DCC_BACKUP_E2E_PRODUCT_KEYWORD: process.env.DCC_BACKUP_E2E_PRODUCT_KEYWORD || '__FIRST_VISIBLE__'
  })
  const uploadV1 = parseMarkedJson(uploadV1Output, 'DCC_UPLOAD_RESULT')
  flowArtifact.uploads.B3 = uploadV1
  const controlledFileIdV1 = extractControlledFileId(uploadV1)
  flowArtifact.controlledFileIds.B3 = controlledFileIdV1
  setFlowStage('B3 backup')
  const B3 = runBackup('B3 backup after DCC V1 upload')
  flowArtifact.backups.B3 = B3

  setFlowStage('DCC upload B4 V2')
  const uploadV2Output = runNodeStep('DCC upload B4 V2', 'tests/e2e/dcc-upload-test-file.e2e.js', {
    DCC_BACKUP_E2E_ALLOW_WRITE: '1',
    DCC_BACKUP_E2E_APPROVAL: 'ALLOW_TEST_DCC_FILE_WRITE',
    DCC_BACKUP_E2E_FILE_NAME: fileName,
    DCC_BACKUP_E2E_FILE_NUMBER: fileNumber,
    DCC_BACKUP_E2E_VERSION_NO: 'V2.0',
    DCC_BACKUP_E2E_PRODUCT_KEYWORD: process.env.DCC_BACKUP_E2E_PRODUCT_KEYWORD || '__FIRST_VISIBLE__'
  })
  const uploadV2 = parseMarkedJson(uploadV2Output, 'DCC_UPLOAD_RESULT')
  flowArtifact.uploads.B4 = uploadV2
  const controlledFileIdV2 = extractControlledFileId(uploadV2)
  flowArtifact.controlledFileIds.B4 = controlledFileIdV2
  setFlowStage('B4 backup')
  const B4 = runBackup('B4 backup after DCC V2 upload')
  flowArtifact.backups.B4 = B4

  setFlowStage('DCC delete B5')
  const deleteOutput = runNodeStep('DCC withdraw/delete B5', 'tests/e2e/dcc-withdraw-delete-file.e2e.js', {
    DCC_BACKUP_E2E_ALLOW_WRITE: '1',
    DCC_BACKUP_E2E_APPROVAL: 'ALLOW_TEST_DCC_FILE_WRITE',
    DCC_BACKUP_E2E_CONTROLLED_FILE_ID: controlledFileIdV2 || controlledFileIdV1
  })
  const deleteResult = parseMarkedJson(deleteOutput, 'DCC_WITHDRAW_DELETE_RESULT')
  assert.equal(deleteResult.deleted, true, 'DCC B5 delete stage must delete the controlled file')
  flowArtifact.deleteResult = deleteResult
  setFlowStage('B5 backup')
  const B5 = runBackup('B5 backup after DCC delete')
  flowArtifact.backups.B5 = B5

  setFlowStage('B3 rehearsal')
  flowArtifact.rehearsals.B3 = runRehearsal('B3', B3.backupId)
  setFlowStage('B3 restore verify')
  flowArtifact.restoreVerified.B3 = runRestoreAndVerify('B3', B3.backupId, {
    present: [{ id: controlledFileIdV1, fileName, versionNo: 'V1.0', expectDownload: true, allowDownloadAccessDenied: true }],
    absent: []
  })
  setFlowStage('B4 rehearsal')
  flowArtifact.rehearsals.B4 = runRehearsal('B4', B4.backupId)
  setFlowStage('B4 restore verify')
  flowArtifact.restoreVerified.B4 = runRestoreAndVerify('B4', B4.backupId, {
    present: [{ id: controlledFileIdV2, fileName, versionNo: 'V2.0', expectDownload: true, allowDownloadAccessDenied: true }],
    absent: []
  })
  setFlowStage('B5 rehearsal')
  flowArtifact.rehearsals.B5 = runRehearsal('B5', B5.backupId)
  setFlowStage('B5 restore verify')
  flowArtifact.restoreVerified.B5 = runRestoreAndVerify('B5', B5.backupId, {
    present: [],
    absent: [{ id: controlledFileIdV2 || controlledFileIdV1, fileName }]
  })

  flowArtifact.completedStage = flowArtifact.currentStage
  assert.equal(flowArtifact.completedStage, 'B5 restore verify', 'DCC B3/B4/B5 flow must finish at B5 restore verify')
  flowArtifact.status = 'passed'
  flowArtifact.completedAt = new Date().toISOString()
  writeArtifact(flowArtifact)
  console.log(`DCC_INCREMENTAL_BACKUP_RESTORE_RESULT ${JSON.stringify(flowArtifact)}`)
} catch (error) {
  if (error?.readiness) {
    flowArtifact.readiness = error.readiness
  }
  flowArtifact.status = 'failed'
  flowArtifact.failedStage = flowArtifact.currentStage
  flowArtifact.error = buildFailureEvidence(error)
  flowArtifact.failedAt = new Date().toISOString()
  try {
    writeArtifact(flowArtifact)
  } catch (artifactWriteError) {
    throw buildFailureArtifactWriteError(error, artifactWriteError)
  }
  throw error
}
