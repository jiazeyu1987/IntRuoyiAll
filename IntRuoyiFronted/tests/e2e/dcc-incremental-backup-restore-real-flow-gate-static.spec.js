const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const gatePath = path.join(repoRoot, 'scripts/dcc-incremental-backup-restore-real-flow-gate.mjs')
const packageJsonPath = path.join(repoRoot, 'package.json')

function readUtf8(filePath) {
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const packageJson = JSON.parse(readUtf8(packageJsonPath))
const gateSource = fs.existsSync(gatePath) ? readUtf8(gatePath) : ''

assert.equal(
  packageJson.scripts?.['e2e:dcc:incremental-backup-restore:check'],
  'node tests/e2e/dcc-incremental-backup-restore-real-flow-gate-static.spec.js',
  'package.json must expose the DCC incremental backup/restore static gate'
)
assert.equal(
  packageJson.scripts?.['e2e:dcc:incremental-backup-restore'],
  'node scripts/dcc-incremental-backup-restore-real-flow-gate.mjs',
  'package.json must expose the DCC incremental backup/restore real flow gate'
)

for (const fragment of [
  'DCC_INCREMENTAL_BACKUP_RESTORE_E2E_ALLOW_REAL_WRITE',
  'ALLOW_TEST_DCC_INCREMENTAL_BACKUP_RESTORE',
  '172.30.30.57',
  '测试租户',
  'mdm-tenant-package-real-setup.e2e.js',
  'mdm-role-menu-real-setup.e2e.js',
  'mdm-product-real-setup.e2e.js',
  'dcc-upload-test-file.e2e.js',
  'runtime-control-real-test-backup-setup.e2e.js',
  'runtime-control-real-rehearsal.e2e.js',
  'dcc-withdraw-delete-file.e2e.js',
  'runtime-control-real-restore-data.e2e.js',
  'dcc-restore-verify.e2e.js',
  'dcc-incremental-backup-restore-readiness-gate.mjs',
  'dcc-readiness-remediation-runner.mjs',
  'DCC_INCREMENTAL_BACKUP_RESTORE_READINESS_RESULT',
  'DCC_READINESS_REMEDIATION_RUNNER_RESULT',
  'runReadinessGate',
  'runReadinessRemediationActions',
  'DCC_UPLOAD_RESULT',
  'DCC_WITHDRAW_DELETE_RESULT',
  'DCC_RESTORE_VERIFY_RESULT',
  'B3',
  'B4',
  'B5',
  'restoreVerified',
  'rehearsals',
  'readinessRemediationResults',
  'buildFailureEvidence',
  'buildFailureArtifactWriteError',
  'buildStepFailure',
  'outputTail',
  'setFlowStage',
  'currentStage',
  'completedStage',
  'failedStage',
  'stageHistory',
  "status: 'running'",
  "status = 'passed'",
  "status = 'failed'",
  'failedAt',
  'present',
  'absent'
]) {
  assert.ok(gateSource.includes(fragment), `real flow gate must include ${fragment}`)
}

assert.ok(
  gateSource.includes('DCC_BACKUP_E2E_VERSION_NO') &&
    gateSource.includes('V1.0') &&
    gateSource.includes('V2.0'),
  'real flow gate must create and verify distinct DCC V1/V2 states'
)
assert.ok(
  gateSource.includes('RUNTIME_CONTROL_REAL_RESTORE_BACKUP_ID') &&
    gateSource.includes('RUNTIME_CONTROL_REAL_RESTORE_TARGET_ENV'),
  'real flow gate must drive restore-data with explicit candidate and target inputs'
)
assert.ok(
  gateSource.includes('RUNTIME_CONTROL_REAL_REHEARSAL_BACKUP_ID') &&
    gateSource.includes('RUNTIME_CONTROL_ALLOW_REAL_REHEARSAL') &&
    gateSource.includes('ALLOW_TEST_RUNTIME_REHEARSAL_WRITE'),
  'real flow gate must run restore rehearsal with explicit candidate and approval before restore-data'
)
assert.ok(
  gateSource.includes("redactApprovalTokens") &&
    gateSource.includes("stripWriteControlEnv") &&
    gateSource.includes("from './dcc-write-control-env.mjs'") &&
    gateSource.includes('stripWriteControlEnv(readinessEnv)'),
  'real flow gate must use the shared write-control sanitizer and redactor before invoking child steps'
)
assert.match(
  gateSource,
  /function runNodeStep\(name, relativeScript, extraEnv = \{\}\) \{[\s\S]*process\.stdout\.write\(redactApprovalTokens\(output\)\)/,
  'real flow gate must redact child step output before writing it to stdout'
)
assert.match(
  gateSource,
  /function runReadinessProbe\(label\) \{[\s\S]*process\.stdout\.write\(redactApprovalTokens\(output\)\)/,
  'real flow gate must redact readiness probe output before writing it to stdout'
)
assert.ok(
  gateSource.includes('function buildStepEnv(extraEnv = {})') &&
    gateSource.includes('stripWriteControlEnv(env)') &&
    gateSource.includes('...extraEnv') &&
    gateSource.indexOf('stripWriteControlEnv(env)') < gateSource.indexOf('...extraEnv'),
  'real flow gate must remove inherited write-mode variables before injecting the current child step approval'
)
assert.ok(
    gateSource.includes('formatReadinessBlockers') &&
    gateSource.includes('formatReadinessRemediationActions') &&
    gateSource.includes('validateReadinessRemediationPlan') &&
    gateSource.includes('READINESS_MANUAL_RESOLUTION_BY_STEP') &&
    gateSource.includes('manualResolutions') &&
    gateSource.includes('prepare_test_tenant_dcc_upload_size_policy') &&
    gateSource.includes('dcc-upload-size-policy-real-setup.e2e.js') &&
    gateSource.includes('DCC_UPLOAD_POLICY_E2E_APPROVAL') &&
    gateSource.includes('requiresExternalApproval') &&
    gateSource.includes('createBlockedReadinessError') &&
    gateSource.includes('readiness remediation did not clear action') &&
    gateSource.includes('runner did not clear action') &&
    gateSource.includes('remainingActionIds') &&
    gateSource.includes('actionCleared') &&
    gateSource.includes('assertRunnerRefreshMatchesReadiness') &&
    gateSource.includes('runner refreshedArtifactPath mismatch') &&
    gateSource.includes('runner remainingActionIds mismatch') &&
    gateSource.includes('runner nextActionId mismatch') &&
    gateSource.includes('runner refreshedReady mismatch') &&
    gateSource.includes('runner refreshedBlockedStepCount mismatch') &&
    gateSource.includes('nextRunCommandTemplate') &&
    gateSource.includes('omitted-from-readiness-artifact') &&
    gateSource.includes('readiness remediation plan invalid before DCC upload') &&
    gateSource.includes('remediationActions=') &&
    gateSource.includes('prepare_isolated_test_tenant_package') &&
    gateSource.includes('assign_test_tenant_mdm_product_menu') &&
    gateSource.includes('prepare_test_tenant_dcc_product') &&
    gateSource.includes('prepare_test_tenant_dcc_upload_size_policy') &&
    gateSource.includes('readiness gate blocked') &&
    gateSource.includes('artifactPath=') &&
    gateSource.indexOf("const readiness = parseMarkedJson(output, 'DCC_INCREMENTAL_BACKUP_RESTORE_READINESS_RESULT')") <
      gateSource.indexOf('assert.equal(result.status, 0'),
  'real flow gate must parse blocked readiness output and report blockers plus artifact path before upload'
)
assert.match(
  gateSource,
  /const result = parseMarkedJson\(output, 'DCC_READINESS_REMEDIATION_RUNNER_RESULT'\)[\s\S]*assert\.equal\(result\.actionCleared, true/,
  'real flow gate must require the guarded runner to report actionCleared=true'
)
assert.match(
  gateSource,
  /const remainingActionIds = Array\.isArray\(result\.remainingActionIds\)[\s\S]*assert\.ok\([\s\S]*!remainingActionIds\.includes\(action\.actionId\)/,
  'real flow gate must reject runner results where the executed action is still pending'
)
assert.match(
  gateSource,
  /function assertRunnerRefreshMatchesReadiness\(action, runnerResult, readiness\) \{[\s\S]*path\.resolve\(runnerResult\.refreshedArtifactPath \|\| ''\)[\s\S]*path\.resolve\(readiness\.artifactPath \|\| ''\)/,
  'real flow gate must compare runner refreshed artifact path with the next readiness probe'
)
assert.match(
  gateSource,
  /const runnerResult = runReadinessRemediationAction\(action\)[\s\S]*readiness = runReadinessProbe\(`DCC B3\/B4\/B5 readiness after remediation \$\{action\.actionId\}`\)[\s\S]*assertRunnerRefreshMatchesReadiness\(action, runnerResult, readiness\)/,
  'real flow gate must validate runner refresh evidence against the next readiness probe before continuing'
)
assert.match(
  gateSource,
  /const readinessRemediationResults = \[\]/,
  'real flow gate must initialize remediation runner evidence collection'
)
assert.match(
  gateSource,
  /readinessRemediationResults\.push\(runnerResult\)/,
  'real flow gate must retain each guarded runner result for final evidence'
)
assert.match(
  gateSource,
  /const flowArtifact = \{[\s\S]*readinessRemediationResults,[\s\S]*status: 'running'/,
  'real flow final artifact must include remediation runner evidence next to readiness'
)
assert.match(
  gateSource,
  /function buildFailureEvidence\(error\) \{[\s\S]*redactApprovalTokens\(error\?\.message/,
  'real flow failure artifact must redact approval tokens from error evidence'
)
assert.match(
  gateSource,
  /function buildStepFailure\(name, result, output\) \{[\s\S]*error\.name = 'DccRealFlowStepError'[\s\S]*error\.stepName = name[\s\S]*error\.exitCode = result\.status[\s\S]*error\.outputTail = redactApprovalTokens/,
  'real flow step failures must preserve sanitized child stdout/stderr tail'
)
assert.match(
  gateSource,
  /if \(result\.status !== 0\) \{[\s\S]*throw buildStepFailure\(name, result, output\)/,
  'real flow runNodeStep must throw a rich step failure instead of plain exit-code assertion'
)
assert.match(
  gateSource,
  /function buildFailureArtifactWriteError\(originalError, artifactWriteError\) \{[\s\S]*originalFailure = buildFailureEvidence\(originalError\)[\s\S]*artifactWriteFailure = buildFailureEvidence\(artifactWriteError\)/,
  'real flow must build a combined failure when failed artifact writing fails'
)
assert.match(
  gateSource,
  /function buildFailureArtifactWriteError\(originalError, artifactWriteError\) \{[\s\S]*error\.name = 'DccRealFlowFailureArtifactWriteError'[\s\S]*error\.failedStage = flowArtifact\.currentStage[\s\S]*error\.artifactPath = ARTIFACT_PATH/,
  'real flow combined failed-artifact-write error must expose machine-readable name, failedStage, and artifactPath'
)
assert.match(
  gateSource,
  /function setFlowStage\(stage\) \{[\s\S]*flowArtifact\.currentStage = stage[\s\S]*flowArtifact\.stageHistory\.push/,
  'real flow must track the current stage and append stage history'
)
for (const expectedStage of [
  'readiness remediation',
  'DCC upload B3 V1',
  'B3 backup',
  'DCC upload B4 V2',
  'B4 backup',
  'DCC delete B5',
  'B5 backup',
  'B3 rehearsal',
  'B3 restore verify',
  'B4 rehearsal',
  'B4 restore verify',
  'B5 rehearsal',
  'B5 restore verify'
]) {
  assert.ok(gateSource.includes(`setFlowStage('${expectedStage}')`), `real flow must set stage ${expectedStage}`)
}
assert.match(
  gateSource,
  /setFlowStage\('B3 rehearsal'\)[\s\S]*runRehearsal\('B3', B3\.backupId\)[\s\S]*setFlowStage\('B3 restore verify'\)/,
  'real flow must rehearse B3 before B3 restore-data'
)
assert.match(
  gateSource,
  /setFlowStage\('B4 rehearsal'\)[\s\S]*runRehearsal\('B4', B4\.backupId\)[\s\S]*setFlowStage\('B4 restore verify'\)/,
  'real flow must rehearse B4 before B4 restore-data'
)
assert.match(
  gateSource,
  /setFlowStage\('B5 rehearsal'\)[\s\S]*runRehearsal\('B5', B5\.backupId\)[\s\S]*setFlowStage\('B5 restore verify'\)/,
  'real flow must rehearse B5 before B5 restore-data'
)
assert.match(
  gateSource,
  /try \{[\s\S]*flowArtifact\.status = 'passed'[\s\S]*writeArtifact\(flowArtifact\)[\s\S]*DCC_INCREMENTAL_BACKUP_RESTORE_RESULT/,
  'real flow must write a passed artifact only after all stages complete'
)
assert.match(
  gateSource,
  /flowArtifact\.completedStage = flowArtifact\.currentStage[\s\S]*assert\.equal\(flowArtifact\.completedStage, 'B5 restore verify'/,
  'real flow passed artifact must identify B5 restore verify as the completed stage'
)
assert.match(
  gateSource,
  /catch \(error\) \{[\s\S]*if \(error\?\.readiness\) \{[\s\S]*flowArtifact\.readiness = error\.readiness[\s\S]*flowArtifact\.status = 'failed'[\s\S]*flowArtifact\.failedStage = flowArtifact\.currentStage[\s\S]*flowArtifact\.error = buildFailureEvidence\(error\)[\s\S]*try \{[\s\S]*writeArtifact\(flowArtifact\)[\s\S]*\} catch \(artifactWriteError\) \{[\s\S]*throw buildFailureArtifactWriteError\(error, artifactWriteError\)[\s\S]*\}[\s\S]*throw error/,
  'real flow must write blocked readiness evidence into failed artifacts, preserve the original failure if artifact writing fails, and rethrow otherwise'
)
assert.ok(
  !gateSource.includes("runNodeStep('test tenant package setup'") &&
    !gateSource.includes("runNodeStep('role menu setup'") &&
    !gateSource.includes("runNodeStep('product master setup'"),
  'real flow gate must use the guarded remediation runner instead of directly running all prerequisite setup scripts'
)
const readinessCallIndex = gateSource.indexOf("runReadinessGate('DCC B3/B4/B5 readiness before DCC upload')")
const uploadB3Index = gateSource.indexOf("runNodeStep('DCC upload B3 V1'")
assert.ok(
  readinessCallIndex !== -1 && uploadB3Index !== -1 && readinessCallIndex < uploadB3Index,
  'real flow gate must verify readiness after prerequisite setup and before DCC upload'
)
assert.ok(
  !gateSource.includes('page.route(') &&
    !gateSource.includes('fulfill(') &&
    !gateSource.includes('mock') &&
    !gateSource.includes('default-success'),
  'real flow gate must not mock, intercept, or default-success any step'
)
assert.ok(
  !gateSource.includes('ALLOW_TEST_DCC_UPLOAD_POLICY_WRITE') &&
    gateSource.includes('this approval is separate from') &&
    gateSource.includes('${APPROVAL_TOKEN}'),
  'real flow gate must not hard-code the upload policy approval token; it must require separate external approval'
)

console.log('PASS: DCC incremental backup/restore real flow gate contract is present')
