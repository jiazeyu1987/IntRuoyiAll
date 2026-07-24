const assert = require('node:assert/strict')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const { spawnSync } = require('node:child_process')

const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'dcc-remediation-runner-'))
const artifactPath = path.join(tmpDir, 'readiness.json')
const baseUrl = 'http://127.0.0.1:8096'

function template(script, baseUrlEnv, allowEnv, approvalEnv, extraEnv) {
  return {
    shell: 'powershell',
    cwd: process.cwd(),
    command: `node ${script}`,
    requiredEnv: {
      DCC_BACKUP_E2E_BASE_URL: baseUrl,
      [baseUrlEnv]: baseUrl,
      [allowEnv]: 'true',
      [approvalEnv]: '<set-after-explicit-user-approval>',
      ...extraEnv
    },
    approvalValuePolicy:
      'omitted-from-readiness-artifact; the target script enforces the exact approval token after explicit user approval'
  }
}

function action(step, actionId, script, baseUrlEnv, allowEnv, approvalEnv, extraEnv) {
  return {
    actionId,
    step,
    script,
    baseUrlEnv,
    requiredAllowEnv: allowEnv,
    requiredApprovalEnv: approvalEnv,
    additionalEnv: extraEnv,
    writeScope: `fixture write scope for ${actionId}`,
    guardrail: `fixture guardrail for ${actionId}`,
    nextRunCommandTemplate: template(script, baseUrlEnv, allowEnv, approvalEnv, extraEnv)
  }
}

fs.writeFileSync(
  artifactPath,
  `${JSON.stringify(
    {
      baseUrl,
      tenant: '测试租户',
      username: 'aoteman',
      writeMode: false,
      ready: false,
      blockedSteps: [
        {
          name: 'test tenant package readiness',
          script: 'tests/e2e/mdm-tenant-package-real-setup.e2e.js',
          blocker: 'fixture package blocker'
        },
        {
          name: 'MDM role menu readiness',
          script: 'tests/e2e/mdm-role-menu-real-setup.e2e.js',
          blocker: 'fixture role blocker'
        }
      ],
      remediationActions: [
        action(
          'test tenant package readiness',
          'prepare_isolated_test_tenant_package',
          'tests/e2e/mdm-tenant-package-real-setup.e2e.js',
          'TENANT_PACKAGE_E2E_BASE_URL',
          'TENANT_PACKAGE_E2E_ALLOW_WRITE',
          'TENANT_PACKAGE_E2E_APPROVAL',
          { TENANT_PACKAGE_E2E_TARGET_TENANT: '测试租户' }
        ),
        action(
          'MDM role menu readiness',
          'assign_test_tenant_mdm_product_menu',
          'tests/e2e/mdm-role-menu-real-setup.e2e.js',
          'MDM_ROLE_E2E_BASE_URL',
          'MDM_ROLE_E2E_ALLOW_ASSIGN',
          'MDM_ROLE_E2E_APPROVAL',
          { MDM_ROLE_E2E_TENANT: '测试租户', MDM_ROLE_E2E_USERNAME: 'aoteman' }
        )
      ],
      manualResolutions: []
    },
    null,
    2
  )}\n`,
  'utf8'
)

function runRunner(extraEnv = {}) {
  return spawnSync(process.execPath, ['scripts/dcc-readiness-remediation-runner.mjs'], {
    cwd: process.cwd(),
    env: {
      ...process.env,
      DCC_READINESS_REMEDIATION_PLAN_ARTIFACT: artifactPath,
      ...extraEnv
    },
    encoding: 'utf8',
    timeout: 30000
  })
}

function assertFail(result, pattern, label) {
  assert.notEqual(result.status, 0, `${label} should fail`)
  assert.match(`${result.stdout}${result.stderr}`, pattern, `${label} failure mismatch`)
}

assertFail(
  runRunner(),
  /DCC_READINESS_REMEDIATION_RUN_ACTION_ID/,
  'runner without action id'
)

assertFail(
  runRunner({
    DCC_READINESS_REMEDIATION_RUN_ACTION_ID: 'assign_test_tenant_mdm_product_menu',
    DCC_READINESS_REMEDIATION_RUN_ALLOW_WRITE: '1'
  }),
  /first pending remediation action/,
  'runner with non-first action'
)

assertFail(
  runRunner({
    DCC_READINESS_REMEDIATION_RUN_ACTION_ID: 'prepare_isolated_test_tenant_package',
    DCC_READINESS_REMEDIATION_RUN_ALLOW_WRITE: '1'
  }),
  /TENANT_PACKAGE_E2E_APPROVAL/,
  'runner without target approval env'
)

console.log('PASS: DCC readiness remediation runner refuses unsafe execution')
