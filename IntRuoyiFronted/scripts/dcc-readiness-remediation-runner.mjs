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
const artifactPath =
  process.env.DCC_READINESS_REMEDIATION_PLAN_ARTIFACT ||
  path.resolve(
    repoRoot,
    '../release_and_backup-root/doc/tasks/20260609-release-and-backup-implementation/artifacts/dcc-incremental-backup-restore-readiness.json'
  )
const actionId = process.env.DCC_READINESS_REMEDIATION_RUN_ACTION_ID || ''
const allowWrite = process.env.DCC_READINESS_REMEDIATION_RUN_ALLOW_WRITE === '1'

function parseMarkedJson(output, marker) {
  const markerIndex = output.indexOf(marker)
  assert.notEqual(markerIndex, -1, `${marker} marker is missing`)
  const start = output.indexOf('{', markerIndex)
  assert.notEqual(start, -1, `${marker} JSON payload is missing`)
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

function runPlanCheck() {
  const result = spawnSync(process.execPath, ['scripts/dcc-readiness-remediation-plan-check.mjs'], {
    cwd: repoRoot,
    env: buildPlanCheckEnv(),
    encoding: 'utf8',
    timeout: 60_000,
    maxBuffer: 64 * 1024 * 1024
  })
  if (result.status !== 0 || result.error) {
    throw new Error(`remediation plan check failed: ${redactApprovalTokens(`${result.error?.message || ''}\n${result.stdout}${result.stderr}`)}`)
  }
}

function buildPlanCheckEnv() {
  const env = {
    ...process.env,
    DCC_READINESS_REMEDIATION_PLAN_ARTIFACT: artifactPath
  }
  stripWriteControlEnv(env)
  return env
}

function buildReadinessRefreshEnv(artifact) {
  const env = {
    ...process.env,
    DCC_BACKUP_E2E_BASE_URL: artifact.baseUrl,
    DCC_BACKUP_E2E_TENANT: artifact.tenant,
    DCC_BACKUP_E2E_USERNAME: artifact.username,
    DCC_INCREMENTAL_BACKUP_RESTORE_READINESS_ARTIFACT: artifactPath,
    DCC_READINESS_REMEDIATION_PLAN_ARTIFACT: artifactPath
  }
  stripWriteControlEnv(env)
  return env
}

function runReadinessRefresh(artifact) {
  const result = spawnSync(process.execPath, ['scripts/dcc-incremental-backup-restore-readiness-gate.mjs'], {
    cwd: repoRoot,
    env: buildReadinessRefreshEnv(artifact),
    encoding: 'utf8',
    timeout: Number(process.env.DCC_READINESS_REMEDIATION_REFRESH_TIMEOUT_MS || 5 * 60 * 1000),
    maxBuffer: 64 * 1024 * 1024
  })
  const output = `${result.stdout || ''}${result.stderr || ''}`
  process.stdout.write(redactApprovalTokens(output))
  if (result.error) {
    throw result.error
  }
  const refreshed = parseMarkedJson(output, 'DCC_INCREMENTAL_BACKUP_RESTORE_READINESS_RESULT')
  assert.equal(refreshed.writeMode, false, 'readiness refresh must remain read-only')
  assertRefreshedArtifactPath(refreshed)
  runPlanCheck()
  return refreshed
}

function assertRefreshedArtifactPath(refreshed) {
  const actualPath = path.resolve(refreshed.artifactPath || '')
  const expectedPath = path.resolve(artifactPath)
  assert.equal(
    actualPath,
    expectedPath,
    `readiness refresh artifactPath mismatch: expected ${expectedPath}, got ${actualPath}`
  )
}

function readArtifact() {
  assert.ok(fs.existsSync(artifactPath), `readiness artifact is missing: ${artifactPath}`)
  return JSON.parse(fs.readFileSync(artifactPath, 'utf8'))
}

function assertSafeArtifact(artifact) {
  assert.equal(artifact.writeMode, false, 'runner only accepts read-only readiness artifacts')
  assert.notEqual(artifact.ready, true, 'readiness is already ready; no remediation action is allowed')
  const url = new URL(artifact.baseUrl)
  assert.notEqual(url.hostname, '172.30.30.57', 'runner must not target production server 172.30.30.57')
  assert.equal(artifact.tenant, '测试租户', `runner must target 测试租户, got ${artifact.tenant}`)
}

function resolveAction(artifact) {
  assert.ok(actionId, 'DCC_READINESS_REMEDIATION_RUN_ACTION_ID is required')
  const actions = Array.isArray(artifact.remediationActions) ? artifact.remediationActions : []
  assert.ok(actions.length > 0, 'readiness artifact has no remediation actions')
  const firstAction = actions[0]
  assert.equal(
    actionId,
    firstAction.actionId,
    `requested action must match first pending remediation action: ${firstAction.actionId}`
  )
  return firstAction
}

function buildChildEnv(action) {
  const template = action.nextRunCommandTemplate
  assert.ok(template, `${action.actionId} nextRunCommandTemplate is required`)
  const requiredApprovalEnv = action.requiredApprovalEnv
  assert.ok(process.env[requiredApprovalEnv], `${requiredApprovalEnv} is required after explicit user approval`)
  const env = {
    ...process.env,
    ...(template.requiredEnv || {})
  }
  stripWriteControlEnv(env)
  return {
    ...env,
    [requiredApprovalEnv]: process.env[requiredApprovalEnv],
    [action.requiredAllowEnv]: 'true'
  }
}

function runAction(action) {
  assert.ok(allowWrite, 'DCC_READINESS_REMEDIATION_RUN_ALLOW_WRITE=1 is required after explicit user approval')
  const template = action.nextRunCommandTemplate
  assert.equal(template.shell, 'powershell', `${action.actionId} template shell must be powershell`)
  assert.equal(template.cwd, repoRoot, `${action.actionId} template cwd mismatch`)
  assert.equal(template.command, `node ${action.script}`, `${action.actionId} template command mismatch`)
  const result = spawnSync(process.execPath, [action.script], {
    cwd: repoRoot,
    env: buildChildEnv(action),
    encoding: 'utf8',
    timeout: Number(process.env.DCC_READINESS_REMEDIATION_RUN_TIMEOUT_MS || 10 * 60 * 1000),
    maxBuffer: 64 * 1024 * 1024
  })
  process.stdout.write(redactApprovalTokens(result.stdout || ''))
  process.stderr.write(redactApprovalTokens(result.stderr || ''))
  if (result.status !== 0 || result.error) {
    throw new Error(`${action.actionId} failed with exit ${result.status}: ${redactApprovalTokens(result.error?.message || '')}`)
  }
  return result.status
}

runPlanCheck()
const artifact = readArtifact()
assertSafeArtifact(artifact)
const action = resolveAction(artifact)
const beforeActionIds = Array.isArray(artifact.remediationActions)
  ? artifact.remediationActions.map((item) => item.actionId).filter(Boolean)
  : []
const beforeBlockedStepCount = Array.isArray(artifact.blockedSteps) ? artifact.blockedSteps.length : 0
runAction(action)
const refreshed = runReadinessRefresh(artifact)
const remainingActionIds = Array.isArray(refreshed.remediationActions)
  ? refreshed.remediationActions.map((item) => item.actionId).filter(Boolean)
  : []
const actionCleared = !remainingActionIds.includes(action.actionId)
assert.ok(actionCleared, `readiness remediation action did not clear: ${action.actionId}`)
const refreshedBlockedStepCount = Array.isArray(refreshed.blockedSteps) ? refreshed.blockedSteps.length : 0
const nextActionId = remainingActionIds[0] || null
console.log(
  `DCC_READINESS_REMEDIATION_RUNNER_RESULT ${JSON.stringify({
    actionId: action.actionId,
    doesNotRunWithoutApproval: true,
    beforeReady: artifact.ready === true,
    beforeBlockedStepCount,
    beforeActionIds,
    refreshedReady: refreshed.ready === true,
    refreshedBlockedStepCount,
    remainingActionIds,
    actionCleared,
    refreshedArtifactPath: refreshed.artifactPath || artifactPath,
    nextActionId,
    status: 'passed'
  })}`
)
