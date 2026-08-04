const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const apiSource = readSource('src/api/bpm/businessApprovalPolicy/index.ts')
const messageSource = readSource('src/hooks/web/useMessage.ts')
const pageSource = readSource('src/views/bpm/businessApprovalPolicy/index.vue')
const policyRealE2eSource = readSource('tests/e2e/bpm-business-approval-policy-real.e2e.js')
const routeVersionRealFlowSource = readSource('tests/e2e/mes-pro-route-version-real-flow.e2e.js')
const remainingRouterSource = readSource('src/router/modules/remaining.ts')

assert.match(
  apiSource,
  /export\s+type\s+BusinessApprovalPolicyMode\s*=\s*'BPM_REQUIRED'\s*\|\s*'SIGNATURE_REQUIRED'\s*\|\s*'DIRECT'/,
  'business approval API must keep all backend policy modes'
)
assert.match(apiSource, /BusinessApprovalPolicySwitchModeReqVO/, 'business approval API must expose switch request VO')
assert.match(apiSource, /signaturePassword\?:\s*string/, 'switch-mode VO must carry electronic signature password')
assert.match(apiSource, /switchPolicyMode/, 'business approval API must expose one-click switch-mode endpoint')
assert.match(messageSource, /prompt\(content:\s*string,\s*tip:\s*string,\s*options:/, 'message.prompt must support password input options')

assert.match(pageSource, /UnifiedListTemplate/, 'policy page must use the unified list template')
assert.match(pageSource, /table-key="bpm\.business-approval-policy\.main"/, 'policy page must keep stable table key')
assert.match(pageSource, /审批开关/, 'policy page must expose a visible approval switch column')
assert.match(pageSource, /el-switch/, 'policy page must use a real switch control')
assert.match(pageSource, /:model-value="row\.policyMode === 'BPM_REQUIRED'"/, 'switch on-state must mean normal BPM approval is enabled')
assert.match(
  pageSource,
  /const targetMode:\s*BusinessApprovalPolicyMode\s*=\s*enabled\s*\?\s*'BPM_REQUIRED'\s*:\s*'DIRECT'/,
  'policy switch must map enabled to BPM_REQUIRED and disabled to DIRECT'
)
assert.match(pageSource, /message\.prompt\('请输入电子签名密码',\s*'审批开关电子签名',\s*\{/, 'policy switch must prompt for electronic signature password')
assert.match(pageSource, /inputType:\s*'password'/, 'policy switch password prompt must use password input type')
assert.match(pageSource, /signaturePassword/, 'policy switch must submit electronic signature password')
assert.match(pageSource, /signaturePassword\s*[,}]/, 'switch-mode request payload must include signaturePassword')
assert.match(pageSource, /审批流程已开启/, 'policy switch must show BPM approval enabled copy')
assert.match(pageSource, /审批已关闭/, 'policy switch must show approval disabled copy')
assert.match(pageSource, /BPM审批/, 'BPM_REQUIRED policies must display as normal BPM approval')
assert.match(
  pageSource,
  /approvalSwitchScope:\s*true/,
  'policy page must default to the approval-switch scope instead of a single mode'
)
assert.doesNotMatch(
  pageSource,
  /policyMode:\s*'BPM_REQUIRED'\s+as\s+BusinessApprovalPolicyMode\s*\|\s*undefined/,
  'policy page must not hide closed approval policies by defaulting policyMode to BPM_REQUIRED'
)
assert.match(pageSource, /历史签名模式/, 'legacy SIGNATURE_REQUIRED policies must not be presented as approval switch enabled')
assert.match(pageSource, /关闭审批/, 'policy form must expose disabled approval mode')
assert.match(pageSource, /VOID:\s*'作废'/, 'VOID action code must display as Chinese void label')
assert.match(pageSource, /CLOSED:\s*'已关闭'/, 'CLOSED object state must display as Chinese closed label')
assert.match(pageSource, /REJECTED:\s*'已驳回'/, 'REJECTED object state must display as Chinese rejected label')
assert.match(pageSource, /READY:\s*'就绪'/, 'READY object state must display as Chinese ready label')
assert.match(pageSource, /PUBLISHED:\s*'已发布'/, 'PUBLISHED object state must display as Chinese published label')
assert.match(pageSource, /DISABLED:\s*'已禁用'/, 'DISABLED object state must display as Chinese disabled label')
assert.match(pageSource, /EDHR_BATCH_VOID:\s*'批次执行作废'/, 'EDHR batch void executor code must display as Chinese label')
assert.match(pageSource, /FORM_TEMPLATE_OBSOLETE:\s*'表单模板作废'/, 'form template obsolete executor code must display as Chinese label')
assert.match(
  pageSource,
  /const policyModeSegmentOptions = computed\(\(\) =>[\s\S]*item\.value !== 'SIGNATURE_REQUIRED'/,
  'manual form selector must hide SIGNATURE_REQUIRED as a business approval mode'
)
assert.match(pageSource, /policyMode:\s*'DIRECT'/, 'new policy form must default to direct mode when no process key is inherited')
assert.match(
  pageSource,
  /policyMode:\s*source\.policyMode === 'SIGNATURE_REQUIRED'\s*\?\s*'DIRECT'\s*:\s*source\.policyMode/,
  'copying a legacy SIGNATURE_REQUIRED policy must convert it to DIRECT instead of keeping signature as a business mode'
)
assert.match(pageSource, /processDefinitionKey:\s*source\.processDefinitionKey/, 'copying a BPM policy must preserve the existing process key without manual input')
assert.match(pageSource, /resolveBusinessApprovalPolicyErrorMessage/, 'backend errors must be visible to the user')
assert.doesNotMatch(pageSource, /流程定义 Key/, 'policy page must not show a process definition key input scheme')
assert.doesNotMatch(pageSource, /审批流程电子签名/, 'BPM approval must not be mislabeled as electronic signature')
assert.doesNotMatch(pageSource, /catch\s*\{\s*\}/, 'policy page must not hide request failures')

assert.match(
  remainingRouterSource,
  /path:\s*'manager\/business-approval-policy'[\s\S]*component:\s*\(\)\s*=>\s*import\('@\/views\/bpm\/businessApprovalPolicy\/index\.vue'\)[\s\S]*name:\s*'BpmBusinessApprovalPolicy'/,
  'hidden BPM route must expose the business approval policy page'
)

assert.match(
  policyRealE2eSource,
  /POLICY_MODES = new Set\(\['BPM_REQUIRED', 'DIRECT'\]\)/,
  'policy real E2E must verify BPM_REQUIRED and DIRECT switch modes'
)
assert.match(policyRealE2eSource, /BPM_POLICY_E2E_SIGNATURE_PASSWORD/, 'policy real E2E must require electronic signature password')
assert.match(policyRealE2eSource, /ROUTE_VERSION_E2E_APPROVER_SIGNATURE_PASSWORD/, 'policy real E2E may reuse the approved reviewer signature password env var')
assert.match(policyRealE2eSource, /resolveSwitchTargetModes[\s\S]*'DIRECT'[\s\S]*'BPM_REQUIRED'/, 'policy real E2E must switch off and back to BPM approval by default')
assert.match(policyRealE2eSource, /BPM_POLICY_E2E_SWITCH_TARGET_MODES/, 'policy real E2E must support explicit target-mode sequences for business-action setup')
assert.match(policyRealE2eSource, /BPM_POLICY_E2E_ALLOW_YUDAO_ADMIN_WRITE/, 'policy real E2E must require an explicit yudao-admin write override')
assert.match(policyRealE2eSource, /isExplicitYudaoAdminWrite[\s\S]*switchTargetModes\[0\]\s*===\s*'DIRECT'[\s\S]*switchTargetModes\[1\]\s*===\s*'BPM_REQUIRED'/, 'yudao-admin override must be constrained to close then reopen')
assert.match(policyRealE2eSource, /assert\.equal\(requestPayload\?\.signaturePassword,\s*config\.signaturePassword/, 'policy real E2E must assert switch payload includes signature password')
assert.match(policyRealE2eSource, /signature-governance\/signature-records\/page/, 'policy real E2E must verify unified signature record page API')
assert.match(policyRealE2eSource, /sourceCode:\s*'BPM'/, 'policy real E2E must verify BPM signature source code')
assert.match(policyRealE2eSource, /PASSWORD_VERIFIED/, 'policy real E2E must verify password evidence status')
assert.match(policyRealE2eSource, /businessRecordCode/, 'policy real E2E must verify signature business record code')
assert.doesNotMatch(policyRealE2eSource, /BPM_POLICY_E2E_PROCESS_DEFINITION_KEY/, 'policy real E2E must not accept process definition key env var')
assert.doesNotMatch(policyRealE2eSource, /流程定义 Key|processDefinitionKey/, 'policy real E2E must not fill or assert process definition key')

assert.doesNotMatch(
  routeVersionRealFlowSource,
  /promptRouteVersionSignaturePassword|电子签名发布|ROUTE_VERSION_E2E_SIGNATURE_PASSWORD/,
  'MES route submit action must not request submitter electronic signature or restore two-stage direct signature publish'
)
assert.match(
  routeVersionRealFlowSource,
  /\/admin-api\/mes\/pro\/route-version\/submit-publish/,
  'route version real E2E must call the one-click submit-publish endpoint'
)
assert.match(
  routeVersionRealFlowSource,
  /approveRouteVersionCandidateThroughApprovalCenter[\s\S]*signaturePassword/,
  'route version real E2E must keep reviewer signature in approval-center approval action'
)

console.log('PASS: BPM business approval policy static contract')
