import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  findPresentWriteControlEnvNames,
  redactApprovalTokens,
  stripWriteControlEnv,
  writeControlEnvNames
} from './dcc-write-control-env.mjs'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '..')

function resolveRequiredBaseUrl() {
  const value = process.env.DCC_BACKUP_E2E_BASE_URL || process.env.RUNTIME_CONTROL_E2E_BASE_URL || ''
  const normalized = value.replace(/\/+$/, '')
  assert.ok(
    normalized,
    'DCC_BACKUP_E2E_BASE_URL or RUNTIME_CONTROL_E2E_BASE_URL is required for readiness gate'
  )
  return normalized
}

const BASE_URL = resolveRequiredBaseUrl()
const TENANT = process.env.DCC_BACKUP_E2E_TENANT || process.env.RUNTIME_CONTROL_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_BACKUP_E2E_USERNAME || process.env.RUNTIME_CONTROL_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_BACKUP_E2E_PASSWORD || process.env.RUNTIME_CONTROL_E2E_PASSWORD || 'admin123'
const STEP_TIMEOUT_MS = Number(process.env.DCC_INCREMENTAL_BACKUP_RESTORE_READINESS_STEP_TIMEOUT_MS || 5 * 60 * 1000)
const ARTIFACT_PATH =
  process.env.DCC_INCREMENTAL_BACKUP_RESTORE_READINESS_ARTIFACT ||
  path.resolve(
    repoRoot,
    '../release_and_backup-root/doc/tasks/20260609-release-and-backup-implementation/artifacts/dcc-incremental-backup-restore-readiness.json'
  )

const blockedWriteEnvNames = writeControlEnvNames

function assertReadOnlyBoundary() {
  const url = new URL(BASE_URL)
  assert.notEqual(url.hostname, '172.30.30.57', 'readiness gate must not target protected production server 172.30.30.57')
  assert.ok(
    ['localhost', '127.0.0.1', '172.30.30.58'].includes(url.hostname),
    `readiness gate must target local frontend or test server, got ${url.hostname}`
  )
  assert.equal(TENANT, '测试租户', `readiness gate must use 测试租户, got ${TENANT}`)
  assert.notEqual(USERNAME, 'admin', 'readiness gate must not use 芋道源码/admin for test-tenant checks')
}

function assertNoWriteModeEnv() {
  const presentWriteEnvNames = findPresentWriteControlEnvNames()
  if (presentWriteEnvNames.length > 0) {
    throw new Error(
      `readiness gate refuses write-mode environment variables: ${presentWriteEnvNames.join(', ')}. ` +
        'Unset them before running the read-only readiness gate.'
    )
  }
}

function buildChildEnv(extraEnv = {}) {
  const childEnv = {
    ...process.env,
    DCC_BACKUP_E2E_BASE_URL: BASE_URL,
    DCC_BACKUP_E2E_TENANT: TENANT,
    DCC_BACKUP_E2E_USERNAME: USERNAME,
    DCC_BACKUP_E2E_PASSWORD: PASSWORD,
    RUNTIME_CONTROL_E2E_BASE_URL: BASE_URL,
    RUNTIME_CONTROL_E2E_TENANT: TENANT,
    RUNTIME_CONTROL_E2E_USERNAME: USERNAME,
    RUNTIME_CONTROL_E2E_PASSWORD: PASSWORD,
    ...extraEnv
  }
  return stripWriteControlEnv(childEnv)
}

function outputTail(text) {
  const normalized = String(text || '').replace(/\r\n/g, '\n')
  return redactApprovalTokens(normalized.slice(-6000))
}

function compactBlocker(text) {
  let normalized = String(text || '').replace(/\s+/g, ' ').trim()
  normalized = redactApprovalTokens(normalized)
  const noisyMarkers = ['; tree=', ' body=', ' dropdown=']
  for (const marker of noisyMarkers) {
    const noisyIndex = normalized.indexOf(marker)
    if (noisyIndex !== -1) {
      normalized = normalized.slice(0, noisyIndex)
      break
    }
  }
  return normalized.length > 1000 ? `${normalized.slice(0, 1000)}...` : normalized
}

function resolveBlocker(output, fallback) {
  const normalized = String(output || '').replace(/\r\n/g, '\n')
  const lines = normalized
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
  const errorLine = lines.find((line) => /^(Error|AssertionError)|missing|required|precondition|explicit user approval|not visible|does not exist|blocked/i.test(line))
  return compactBlocker(errorLine || fallback)
}

function runStep(step) {
  const scriptPath = path.resolve(repoRoot, step.script)
  assert.ok(fs.existsSync(scriptPath), `${step.name} script is missing: ${scriptPath}`)
  const result = spawnSync(process.execPath, [scriptPath], {
    cwd: repoRoot,
    env: buildChildEnv(step.env),
    encoding: 'utf8',
    timeout: STEP_TIMEOUT_MS,
    maxBuffer: 64 * 1024 * 1024
  })
  const output = `${result.stdout || ''}${result.stderr || ''}`
  const failed = Boolean(result.error) || result.status !== 0
  const summary = {
    name: step.name,
    script: step.script,
    status: failed ? 'blocked' : 'passed',
    exitCode: result.status,
    signal: result.signal,
    blocker: failed ? resolveBlocker(output, result.error?.message || `${step.name} exited with ${result.status}`) : null,
    outputTail: outputTail(output)
  }
  const printable = {
    name: summary.name,
    status: summary.status,
    blocker: summary.blocker
  }
  console.log(`READINESS_STEP ${JSON.stringify(printable)}`)
  return summary
}

function writeArtifact(payload) {
  fs.mkdirSync(path.dirname(ARTIFACT_PATH), { recursive: true })
  fs.writeFileSync(ARTIFACT_PATH, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

function buildNextRunCommandTemplate(action) {
  return {
    shell: 'powershell',
    cwd: repoRoot,
    command: `node ${action.script}`,
    requiredEnv: {
      DCC_BACKUP_E2E_BASE_URL: BASE_URL,
      [action.baseUrlEnv]: BASE_URL,
      [action.requiredAllowEnv]: 'true',
      [action.requiredApprovalEnv]: '<set-after-explicit-user-approval>',
      ...action.additionalEnv
    },
    approvalValuePolicy:
      'omitted-from-readiness-artifact; the target script enforces the exact approval token after explicit user approval'
  }
}

function buildRemediationActions(blockedSteps) {
  const actionByStepName = new Map([
    [
      'test tenant package readiness',
      {
        actionId: 'prepare_isolated_test_tenant_package',
        step: 'test tenant package readiness',
        script: 'tests/e2e/mdm-tenant-package-real-setup.e2e.js',
        baseUrlEnv: 'TENANT_PACKAGE_E2E_BASE_URL',
        requiredAllowEnv: 'TENANT_PACKAGE_E2E_ALLOW_WRITE',
        requiredApprovalEnv: 'TENANT_PACKAGE_E2E_APPROVAL',
        additionalEnv: {
          TENANT_PACKAGE_E2E_TARGET_TENANT: TENANT
        },
        writeScope: 'Create an isolated DCC E2E tenant package and switch only 测试租户 to that package.',
        guardrail: 'Requires explicit user approval; must not modify shared package 111 or any production server.'
      }
    ],
    [
      'MDM role menu readiness',
      {
        actionId: 'assign_test_tenant_mdm_product_menu',
        step: 'MDM role menu readiness',
        script: 'tests/e2e/mdm-role-menu-real-setup.e2e.js',
        baseUrlEnv: 'MDM_ROLE_E2E_BASE_URL',
        requiredAllowEnv: 'MDM_ROLE_E2E_ALLOW_ASSIGN',
        requiredApprovalEnv: 'MDM_ROLE_E2E_APPROVAL',
        additionalEnv: {
          MDM_ROLE_E2E_TENANT: TENANT,
          MDM_ROLE_E2E_USERNAME: USERNAME
        },
        writeScope: 'Assign the DCC文件类别 and 基础数据 / 产品主数据 menus only to the isolated test tenant role path.',
        guardrail: 'Requires explicit user approval; must use 测试租户/aoteman and must not update 芋道源码/admin data.'
      }
    ],
    [
      'MDM product readiness',
      {
        actionId: 'prepare_test_tenant_dcc_product',
        step: 'MDM product readiness',
        script: 'tests/e2e/mdm-product-real-setup.e2e.js',
        baseUrlEnv: 'MDM_PRODUCT_E2E_BASE_URL',
        requiredAllowEnv: 'MDM_PRODUCT_E2E_ALLOW_CREATE',
        requiredApprovalEnv: 'MDM_PRODUCT_E2E_APPROVAL',
        additionalEnv: {
          MDM_PRODUCT_E2E_TENANT: TENANT,
          MDM_PRODUCT_E2E_USERNAME: USERNAME
        },
        writeScope: 'Create or verify one enabled DCC product master record in 测试租户 for real DCC upload.',
        guardrail: 'Requires explicit user approval; must not create data for 芋道源码/admin or production.'
      }
    ],
    [
      'DCC upload size policy readiness',
      {
        actionId: 'prepare_test_tenant_dcc_upload_size_policy',
        step: 'DCC upload size policy readiness',
        script: 'tests/e2e/dcc-upload-size-policy-real-setup.e2e.js',
        baseUrlEnv: 'DCC_UPLOAD_POLICY_E2E_BASE_URL',
        requiredAllowEnv: 'DCC_UPLOAD_POLICY_E2E_ALLOW_WRITE',
        requiredApprovalEnv: 'DCC_UPLOAD_POLICY_E2E_APPROVAL',
        additionalEnv: {
          DCC_UPLOAD_POLICY_E2E_TENANT: TENANT,
          DCC_UPLOAD_POLICY_E2E_USERNAME: USERNAME
        },
        writeScope:
          'Create or verify the Codex Local DCC Category/SOURCE upload size policy only through the real DCC文件类别 frontend path in 测试租户.',
        guardrail:
          'Requires separate explicit user approval for upload policy data; must use 测试租户/aoteman and must not use direct API writes or production.'
      }
    ]
  ])
  return blockedSteps
    .map((step) => actionByStepName.get(step.name))
    .map((action) =>
      action
        ? {
            ...action,
            nextRunCommandTemplate: buildNextRunCommandTemplate(action)
          }
        : null
    )
    .filter(Boolean)
}

function buildManualResolutions(blockedSteps) {
  const resolutionByStepName = new Map()
  return blockedSteps
    .map((step) => resolutionByStepName.get(step.name))
    .filter(Boolean)
}

assertReadOnlyBoundary()
assertNoWriteModeEnv()

const steps = [
  {
    name: 'DCC read-only page preflight',
    script: 'tests/e2e/dcc-incremental-backup-preflight.e2e.js',
    env: {}
  },
  {
    name: 'test tenant package readiness',
    script: 'tests/e2e/mdm-tenant-package-real-setup.e2e.js',
    env: {
      TENANT_PACKAGE_E2E_BASE_URL: BASE_URL,
      TENANT_PACKAGE_E2E_TARGET_TENANT: TENANT
    }
  },
  {
    name: 'MDM role menu readiness',
    script: 'tests/e2e/mdm-role-menu-real-setup.e2e.js',
    env: {
      MDM_ROLE_E2E_BASE_URL: BASE_URL,
      MDM_ROLE_E2E_TENANT: TENANT,
      MDM_ROLE_E2E_USERNAME: USERNAME,
      MDM_ROLE_E2E_PASSWORD: PASSWORD
    }
  },
  {
    name: 'MDM product readiness',
    script: 'tests/e2e/mdm-product-real-setup.e2e.js',
    env: {
      MDM_PRODUCT_E2E_BASE_URL: BASE_URL,
      MDM_PRODUCT_E2E_TENANT: TENANT,
      MDM_PRODUCT_E2E_USERNAME: USERNAME,
      MDM_PRODUCT_E2E_PASSWORD: PASSWORD
    }
  },
  {
    name: 'DCC upload size policy readiness',
    script: 'tests/e2e/dcc-upload-size-policy-readiness.e2e.js',
    env: {
      DCC_UPLOAD_POLICY_E2E_BASE_URL: BASE_URL,
      DCC_UPLOAD_POLICY_E2E_TENANT: TENANT,
      DCC_UPLOAD_POLICY_E2E_USERNAME: USERNAME,
      DCC_UPLOAD_POLICY_E2E_PASSWORD: PASSWORD
    }
  }
]

const results = steps.map(runStep)
const blockedSteps = results.filter((step) => step.status !== 'passed')
const artifact = {
  baseUrl: BASE_URL,
  tenant: TENANT,
  username: USERNAME,
  writeMode: false,
  ready: blockedSteps.length === 0,
  blockedSteps: blockedSteps.map((step) => ({
    name: step.name,
    script: step.script,
    blocker: step.blocker
  })),
  remediationActions: buildRemediationActions(blockedSteps),
  manualResolutions: buildManualResolutions(blockedSteps),
  steps: results,
  artifactPath: ARTIFACT_PATH,
  completedAt: new Date().toISOString()
}

writeArtifact(artifact)
const consoleArtifact = {
  baseUrl: artifact.baseUrl,
  tenant: artifact.tenant,
  username: artifact.username,
  writeMode: artifact.writeMode,
  ready: artifact.ready,
  blockedSteps: artifact.blockedSteps,
  remediationActions: artifact.remediationActions,
  manualResolutions: artifact.manualResolutions,
  artifactPath: artifact.artifactPath,
  completedAt: artifact.completedAt
}
console.log(`DCC_INCREMENTAL_BACKUP_RESTORE_READINESS_RESULT ${JSON.stringify(consoleArtifact)}`)

if (!artifact.ready) {
  throw new Error(`DCC incremental backup/restore readiness blocked: ${blockedSteps.map((step) => step.name).join(', ')}`)
}
