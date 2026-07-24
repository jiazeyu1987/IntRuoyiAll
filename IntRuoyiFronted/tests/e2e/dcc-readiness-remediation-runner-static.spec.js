const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const packageJsonPath = path.join(repoRoot, 'package.json')
const runnerPath = path.join(repoRoot, 'scripts/dcc-readiness-remediation-runner.mjs')

function readUtf8(filePath) {
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const packageJson = JSON.parse(readUtf8(packageJsonPath))
assert.equal(
  packageJson.scripts?.['e2e:dcc:incremental-backup-restore:readiness:remediate'],
  'node scripts/dcc-readiness-remediation-runner.mjs',
  'package.json must expose the guarded remediation runner'
)
assert.equal(
  packageJson.scripts?.['e2e:dcc:incremental-backup-restore:readiness:remediate:check'],
  'node tests/e2e/dcc-readiness-remediation-runner-static.spec.js',
  'package.json must expose the remediation runner static check'
)
assert.equal(
  packageJson.scripts?.['e2e:dcc:incremental-backup-restore:readiness:remediate:unit'],
  'node tests/e2e/dcc-readiness-remediation-runner.spec.js',
  'package.json must expose the remediation runner unit check'
)

const source = readUtf8(runnerPath)
for (const fragment of [
  'DCC_READINESS_REMEDIATION_RUNNER_RESULT',
  'DCC_READINESS_REMEDIATION_RUN_ACTION_ID',
  'DCC_READINESS_REMEDIATION_RUN_ALLOW_WRITE',
  'runPlanCheck',
  'buildPlanCheckEnv',
  'runReadinessRefresh',
  'stripWriteControlEnv',
  'assertRefreshedArtifactPath',
  'DCC_INCREMENTAL_BACKUP_RESTORE_READINESS_RESULT',
  'DCC_INCREMENTAL_BACKUP_RESTORE_READINESS_ARTIFACT',
  'readiness refresh artifactPath mismatch',
  'parseMarkedJson',
  'nextActionId',
  'beforeReady',
  'beforeBlockedStepCount',
  'beforeActionIds',
  'refreshedReady',
  'refreshedBlockedStepCount',
  'remainingActionIds',
  'actionCleared',
  'readiness remediation action did not clear',
  'refreshedArtifactPath',
  'first pending remediation action',
  'spawnSync',
  'nextRunCommandTemplate',
  'doesNotRunWithoutApproval',
  '172.30.30.57'
]) {
  assert.ok(source.includes(fragment), `runner must include ${fragment}`)
}

assert.match(
  source,
  /import \{[\s\S]*redactApprovalTokens[\s\S]*stripWriteControlEnv[\s\S]*\} from '\.\/dcc-write-control-env\.mjs'/,
  'runner write-control env list and approval-token redactor must come from the shared module'
)
assert.match(
  source,
  /function runPlanCheck\(\) \{[\s\S]*env: buildPlanCheckEnv\(\)/,
  'runner plan checker env must be built through the write-control sanitizer'
)
assert.match(
  source,
  /function runPlanCheck\(\) \{[\s\S]*throw new Error\(`remediation plan check failed:[\s\S]*redactApprovalTokens\(`\$\{result\.error\?\.message \|\| ''\}\\n\$\{result\.stdout\}\$\{result\.stderr\}`\)/,
  'runner must redact remediation plan checker failure stdout/stderr before throwing'
)

assert.match(
  source,
  /function buildChildEnv\(action\) \{[\s\S]*stripWriteControlEnv\(env\)[\s\S]*\[requiredApprovalEnv\]: process\.env\[requiredApprovalEnv\][\s\S]*\[action\.requiredAllowEnv\]: 'true'/,
  'runner child env must strip unrelated write controls before passing only the current action approval'
)
assert.match(
  source,
  /function runReadinessRefresh\(artifact\) \{[\s\S]*process\.stdout\.write\(redactApprovalTokens\(output\)\)/,
  'runner must redact readiness refresh output before writing it to stdout'
)
assert.match(
  source,
  /function runAction\(action\) \{[\s\S]*process\.stdout\.write\(redactApprovalTokens\(result\.stdout \|\| ''\)\)[\s\S]*process\.stderr\.write\(redactApprovalTokens\(result\.stderr \|\| ''\)\)/,
  'runner must redact remediation child stdout/stderr before forwarding them'
)
assert.match(
  source,
  /function runAction\(action\) \{[\s\S]*throw new Error\(`\$\{action\.actionId\} failed with exit \$\{result\.status\}: \$\{redactApprovalTokens\(result\.error\?\.message \|\| ''\)\}`\)/,
  'runner must redact remediation child spawn error message before throwing'
)
assert.match(
  source,
  /const beforeActionIds = Array\.isArray\(artifact\.remediationActions\)[\s\S]*artifact\.remediationActions\.map\(\(item\) => item\.actionId\)\.filter\(Boolean\)/,
  'runner result must include the pending action ids observed before execution'
)
assert.match(
  source,
  /const remainingActionIds = Array\.isArray\(refreshed\.remediationActions\)[\s\S]*refreshed\.remediationActions\.map\(\(item\) => item\.actionId\)\.filter\(Boolean\)/,
  'runner result must include the remaining action ids after readiness refresh'
)
assert.match(
  source,
  /const actionCleared = !remainingActionIds\.includes\(action\.actionId\)[\s\S]*assert\.ok\(actionCleared, `readiness remediation action did not clear: \$\{action\.actionId\}`\)/,
  'runner must fail fast if the completed action is still pending after readiness refresh'
)

for (const forbiddenToken of [
  'ALLOW_TEST_DCC_INCREMENTAL_BACKUP_RESTORE',
  'ALLOW_TEST_TENANT_PACKAGE_WRITE',
  'ALLOW_TEST_MDM_ROLE_MENU_WRITE',
  'ALLOW_TEST_MDM_PRODUCT_WRITE',
  'ALLOW_TEST_DCC_UPLOAD_POLICY_WRITE',
  'ALLOW_TEST_DCC_FILE_WRITE',
  'ALLOW_TEST_RUNTIME_BACKUP_WRITE',
  'ALLOW_TEST_RUNTIME_RESTORE_WRITE'
]) {
  assert.ok(!source.includes(forbiddenToken), `runner must not contain approval token ${forbiddenToken}`)
}

console.log('PASS: DCC readiness remediation runner static contract is present')
