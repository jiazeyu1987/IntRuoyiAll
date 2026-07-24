import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  redactApprovalTokens,
  stripWriteControlEnv
} from './dcc-write-control-env.mjs'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '..')

const safeSteps = [
  {
    name: 'readiness static gate',
    args: ['tests/e2e/dcc-incremental-backup-restore-readiness-gate-static.spec.js']
  },
  {
    name: 'remediation plan static gate',
    args: ['tests/e2e/dcc-readiness-remediation-plan-static.spec.js']
  },
  {
    name: 'remediation plan unit gate',
    args: ['tests/e2e/dcc-readiness-remediation-plan-check.spec.js']
  },
  {
    name: 'remediation runner static gate',
    args: ['tests/e2e/dcc-readiness-remediation-runner-static.spec.js']
  },
  {
    name: 'remediation runner no-approval gate',
    args: ['tests/e2e/dcc-readiness-remediation-runner.spec.js']
  },
  {
    name: 'shared write-control env gate',
    args: ['tests/e2e/dcc-write-control-env-static.spec.js']
  },
  {
    name: 'remediation plan artifact gate',
    args: ['scripts/dcc-readiness-remediation-plan-check.mjs']
  },
  {
    name: 'real-flow static gate',
    args: ['tests/e2e/dcc-incremental-backup-restore-real-flow-gate-static.spec.js']
  },
  {
    name: 'write guard static gate',
    args: ['tests/e2e/dcc-real-operation-write-guards-static.spec.js']
  },
  {
    name: 'restore verifier static gate',
    args: ['tests/e2e/dcc-restore-verify-static.spec.js']
  },
  {
    name: 'MDM prerequisite write guard static gate',
    args: ['tests/e2e/mdm-real-data-prerequisite-guards-static.spec.js']
  }
]

function sanitizeWriteEnv(extraEnv = {}) {
  const env = { ...process.env, ...extraEnv }
  return stripWriteControlEnv(env)
}

function runNodeStep(step) {
  const result = spawnSync(process.execPath, step.args, {
    cwd: repoRoot,
    env: sanitizeWriteEnv(),
    encoding: 'utf8',
    timeout: 60_000,
    maxBuffer: 64 * 1024 * 1024
  })
  if (result.status !== 0 || result.error) {
    throw new Error(
      redactApprovalTokens(
        `${step.name} failed with exit ${result.status}: ${result.error?.message || ''}\n${result.stdout || ''}${
          result.stderr || ''
        }`
      )
    )
  }
  return {
    name: step.name,
    status: 'passed'
  }
}

function runExpectedNoApprovalFailFast() {
  const result = spawnSync(process.execPath, ['scripts/dcc-incremental-backup-restore-real-flow-gate.mjs'], {
    cwd: repoRoot,
    env: sanitizeWriteEnv(),
    encoding: 'utf8',
    timeout: 60_000,
    maxBuffer: 64 * 1024 * 1024
  })
  const output = `${result.stdout || ''}${result.stderr || ''}`
  assert.notEqual(result.status, 0, 'real-flow gate must fail without explicit real-write approval')
  assert.match(
    output,
    /DCC_INCREMENTAL_BACKUP_RESTORE_E2E_ALLOW_REAL_WRITE/,
    'real-flow no-approval failure must name the missing approval guard'
  )
  assert.ok(!output.includes("runNodeStep('DCC upload"), 'no-approval preflight must fail before DCC upload')
  assert.ok(!output.includes('DCC_UPLOAD_RESULT'), 'no-approval preflight must not upload DCC files')
  return {
    name: 'real-flow no-approval fail-fast',
    status: 'passed'
  }
}

const passedSteps = safeSteps.map(runNodeStep)
passedSteps.push(runExpectedNoApprovalFailFast())

console.log(
  `DCC_INCREMENTAL_BACKUP_RESTORE_PREFLIGHT_RESULT ${JSON.stringify({
    status: 'passed',
    doesNotExecuteRealWrite: true,
    steps: passedSteps.map((step) => step.name)
  })}`
)
