const assert = require('node:assert/strict')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const { spawnSync } = require('node:child_process')

const repoRoot = path.resolve(__dirname, '../..')
const checker = path.join(repoRoot, 'scripts/dcc-readiness-remediation-plan-check.mjs')
const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'dcc-remediation-plan-'))
const baseUrl = 'http://127.0.0.1:8096'

function template(script, baseUrlEnv, allowEnv, approvalEnv, extraEnv) {
  return {
    shell: 'powershell',
    cwd: repoRoot,
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

function action(step, actionId) {
  const definitions = {
    prepare_isolated_test_tenant_package: {
      script: 'tests/e2e/mdm-tenant-package-real-setup.e2e.js',
      baseUrlEnv: 'TENANT_PACKAGE_E2E_BASE_URL',
      allowEnv: 'TENANT_PACKAGE_E2E_ALLOW_WRITE',
      approvalEnv: 'TENANT_PACKAGE_E2E_APPROVAL',
      extraEnv: { TENANT_PACKAGE_E2E_TARGET_TENANT: '测试租户' }
    },
    assign_test_tenant_mdm_product_menu: {
      script: 'tests/e2e/mdm-role-menu-real-setup.e2e.js',
      baseUrlEnv: 'MDM_ROLE_E2E_BASE_URL',
      allowEnv: 'MDM_ROLE_E2E_ALLOW_ASSIGN',
      approvalEnv: 'MDM_ROLE_E2E_APPROVAL',
      extraEnv: { MDM_ROLE_E2E_TENANT: '测试租户', MDM_ROLE_E2E_USERNAME: 'aoteman' }
    },
    prepare_test_tenant_dcc_product: {
      script: 'tests/e2e/mdm-product-real-setup.e2e.js',
      baseUrlEnv: 'MDM_PRODUCT_E2E_BASE_URL',
      allowEnv: 'MDM_PRODUCT_E2E_ALLOW_CREATE',
      approvalEnv: 'MDM_PRODUCT_E2E_APPROVAL',
      extraEnv: { MDM_PRODUCT_E2E_TENANT: '测试租户', MDM_PRODUCT_E2E_USERNAME: 'aoteman' }
    },
    prepare_test_tenant_dcc_upload_size_policy: {
      script: 'tests/e2e/dcc-upload-size-policy-real-setup.e2e.js',
      baseUrlEnv: 'DCC_UPLOAD_POLICY_E2E_BASE_URL',
      allowEnv: 'DCC_UPLOAD_POLICY_E2E_ALLOW_WRITE',
      approvalEnv: 'DCC_UPLOAD_POLICY_E2E_APPROVAL',
      extraEnv: { DCC_UPLOAD_POLICY_E2E_TENANT: '测试租户', DCC_UPLOAD_POLICY_E2E_USERNAME: 'aoteman' }
    }
  }
  const item = definitions[actionId]
  assert.ok(item, `unknown action fixture ${actionId}`)
  return {
    actionId,
    step,
    script: item.script,
    baseUrlEnv: item.baseUrlEnv,
    requiredAllowEnv: item.allowEnv,
    requiredApprovalEnv: item.approvalEnv,
    additionalEnv: item.extraEnv,
    writeScope: `fixture scope for ${actionId}`,
    guardrail: `fixture guardrail for ${actionId}`,
    nextRunCommandTemplate: template(item.script, item.baseUrlEnv, item.allowEnv, item.approvalEnv, item.extraEnv)
  }
}

function artifact(name, patch) {
  const payload = {
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
      },
      {
        name: 'MDM product readiness',
        script: 'tests/e2e/mdm-product-real-setup.e2e.js',
        blocker: 'fixture product blocker'
      }
    ],
    remediationActions: [
      action('test tenant package readiness', 'prepare_isolated_test_tenant_package'),
      action('MDM role menu readiness', 'assign_test_tenant_mdm_product_menu'),
      action('MDM product readiness', 'prepare_test_tenant_dcc_product')
    ],
    manualResolutions: [],
    ...patch
  }
  const filePath = path.join(tmpDir, `${name}.json`)
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
  return filePath
}

function runChecker(filePath) {
  return spawnSync(process.execPath, [checker], {
    cwd: repoRoot,
    env: {
      ...process.env,
      DCC_READINESS_REMEDIATION_PLAN_ARTIFACT: filePath
    },
    encoding: 'utf8',
    timeout: 30000
  })
}

function assertPass(filePath, label) {
  const result = runChecker(filePath)
  assert.equal(result.status, 0, `${label} should pass: ${result.stdout}${result.stderr}`)
  assert.match(result.stdout, /DCC_READINESS_REMEDIATION_PLAN_CHECK/, `${label} must emit marker`)
}

function assertFail(filePath, pattern, label) {
  const result = runChecker(filePath)
  assert.notEqual(result.status, 0, `${label} should fail`)
  assert.match(`${result.stdout}${result.stderr}`, pattern, `${label} failure mismatch`)
}

const remainingRoleAndProduct = artifact('remaining-role-and-product', {
  blockedSteps: [
    {
      name: 'MDM role menu readiness',
      script: 'tests/e2e/mdm-role-menu-real-setup.e2e.js',
      blocker: 'fixture role blocker'
    },
    {
      name: 'MDM product readiness',
      script: 'tests/e2e/mdm-product-real-setup.e2e.js',
      blocker: 'fixture product blocker'
    }
  ],
  remediationActions: [
    action('MDM role menu readiness', 'assign_test_tenant_mdm_product_menu'),
    action('MDM product readiness', 'prepare_test_tenant_dcc_product')
  ]
})
assertPass(remainingRoleAndProduct, 'remaining blocker subset')

const derivedDccPreflightAndPackage = artifact('derived-dcc-preflight-and-package', {
  blockedSteps: [
    {
      name: 'DCC read-only page preflight',
      script: 'tests/e2e/dcc-incremental-backup-preflight.e2e.js',
      blocker: 'DCC preflight is blocked because the isolated package is missing DCC menus'
    },
    {
      name: 'test tenant package readiness',
      script: 'tests/e2e/mdm-tenant-package-real-setup.e2e.js',
      blocker: 'dedicated package is missing required E2E menus'
    }
  ],
  remediationActions: [action('test tenant package readiness', 'prepare_isolated_test_tenant_package')]
})
assertPass(derivedDccPreflightAndPackage, 'derived DCC preflight blocker with package remediation')

const unknownOnlyNoAction = artifact('unknown-only-no-action', {
  blockedSteps: [
    {
      name: 'DCC read-only page preflight',
      script: 'tests/e2e/dcc-incremental-backup-preflight.e2e.js',
      blocker: 'DCC preflight is blocked without any known prerequisite remediation'
    }
  ],
  remediationActions: []
})
assertFail(
  unknownOnlyNoAction,
  /blocked artifact must contain at least one known remediation action or manual resolution/,
  'unknown-only blocker'
)

const contradictoryReady = artifact('contradictory-ready', {
  ready: true,
  blockedSteps: [
    {
      name: 'MDM role menu readiness',
      script: 'tests/e2e/mdm-role-menu-real-setup.e2e.js',
      blocker: 'fixture role blocker'
    }
  ],
  remediationActions: []
})
assertFail(contradictoryReady, /ready artifact must not contain blockedSteps/, 'contradictory ready artifact')

const leakingTemplate = artifact('leaking-template', {
  remediationActions: [
    action('test tenant package readiness', 'prepare_isolated_test_tenant_package'),
    action('MDM role menu readiness', 'assign_test_tenant_mdm_product_menu'),
    action('MDM product readiness', 'prepare_test_tenant_dcc_product')
  ]
})
const leakingTemplatePayload = JSON.parse(fs.readFileSync(leakingTemplate, 'utf8'))
leakingTemplatePayload.remediationActions[0].nextRunCommandTemplate.requiredEnv.TENANT_PACKAGE_E2E_APPROVAL =
  'ALLOW_TEST_TENANT_PACKAGE_WRITE'
fs.writeFileSync(leakingTemplate, `${JSON.stringify(leakingTemplatePayload, null, 2)}\n`, 'utf8')
assertFail(leakingTemplate, /leaks an approval token value/, 'approval token leak')

const leakingBlockedStep = artifact('leaking-blocked-step', {
  blockedSteps: [
    {
      name: 'test tenant package readiness',
      script: 'tests/e2e/mdm-tenant-package-real-setup.e2e.js',
      blocker: 'set TENANT_PACKAGE_E2E_APPROVAL=ALLOW_TEST_TENANT_PACKAGE_WRITE'
    }
  ],
  remediationActions: [action('test tenant package readiness', 'prepare_isolated_test_tenant_package')],
  steps: [
    {
      name: 'test tenant package readiness',
      outputTail: 'TENANT_PACKAGE_E2E_APPROVAL=ALLOW_TEST_TENANT_PACKAGE_WRITE'
    }
  ]
})
assertFail(leakingBlockedStep, /readiness artifact leaks an approval token value/, 'blocked step token leak')

const leakingProdBlockedStep = artifact('leaking-prod-blocked-step', {
  blockedSteps: [
    {
      name: 'test tenant package readiness',
      script: 'tests/e2e/mdm-tenant-package-real-setup.e2e.js',
      blocker: 'set RUNTIME_CONTROL_REAL_PROMOTE_PROD_APPROVAL=ALLOW_PROD_RUNTIME_PROMOTE_WRITE'
    }
  ],
  remediationActions: [action('test tenant package readiness', 'prepare_isolated_test_tenant_package')]
})
assertFail(leakingProdBlockedStep, /readiness artifact leaks an approval token value/, 'production token leak')

const uploadPolicyOnly = artifact('upload-policy-only', {
  blockedSteps: [
    {
      name: 'DCC upload size policy readiness',
      script: 'tests/e2e/dcc-upload-size-policy-readiness.e2e.js',
      blocker: 'DCC upload size policy is missing or invalid for Codex Local DCC Category/SOURCE'
    }
  ],
  remediationActions: [
    action('DCC upload size policy readiness', 'prepare_test_tenant_dcc_upload_size_policy')
  ],
  manualResolutions: []
})
assertPass(uploadPolicyOnly, 'upload policy blocker')

console.log('PASS: DCC readiness remediation plan checker validates subset and invalid artifacts')
