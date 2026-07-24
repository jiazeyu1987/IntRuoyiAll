const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const packageJsonPath = path.join(repoRoot, 'package.json')
const runnerPath = path.join(repoRoot, 'tests/e2e/smart-scheduling-clickable-coverage.e2e.js')

function readUtf8(filePath) {
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const packageJson = JSON.parse(readUtf8(packageJsonPath))
assert.equal(
  packageJson.scripts?.['e2e:mes:smart-scheduling-clickable:check'],
  'node tests/e2e/smart-scheduling-clickable-coverage-static.spec.js',
  'package.json must expose the smart scheduling clickable static check'
)
assert.equal(
  packageJson.scripts?.['e2e:mes:smart-scheduling-clickable'],
  'node tests/e2e/smart-scheduling-clickable-coverage.e2e.js',
  'package.json must expose the smart scheduling clickable real runner'
)

const source = readUtf8(runnerPath)

for (const fragment of [
  'PAGE_SPECS',
  '/mes/pro/scheduler-workbench',
  '/mes/pro/schedule-order',
  '/mes/pro/task',
  '/mes/pro/schedule-calendar',
  '/mes/pro/route?tab=schedule-config',
  '/mes/pro/feedback',
  '/mes/pro/puhui-schedule',
  '/mes/home/index',
  'DANGEROUS_WRITE_TEXT',
  'NON_MUTATING_POST_PATHS',
  'cancelOpenOverlays',
  'collectClickableControls',
  'clickControl',
  'writeJsonArtifact',
  'smart-scheduling-clickable-coverage-report.json'
]) {
  assert.ok(source.includes(fragment), `clickable coverage runner must include fragment: ${fragment}`)
}

for (const envName of [
  'MES_CLICKABLE_BASE_URL',
  'MES_CLICKABLE_TENANT',
  'MES_CLICKABLE_PLANNER_USERNAME',
  'MES_CLICKABLE_SUPERVISOR_USERNAME',
  'MES_CLICKABLE_DEFAULT_PASSWORD',
  'MES_CLICKABLE_ARTIFACT_DIR'
]) {
  assert.ok(source.includes(envName), `clickable coverage runner must expose environment: ${envName}`)
}

for (const forbiddenPattern of [
  /fetch\([^)]*\/mes\/pro\/task\/auto-schedule\/apply/,
  /fetch\([^)]*\/mes\/pro\/feedback\/approve/,
  /fetch\([^)]*\/mes\/pro\/feedback\/import-record\/attribute/,
  /fetch\([^)]*\/mes\/pro\/schedule-order\/batch-create-from-work-order/
]) {
  assert.ok(!forbiddenPattern.test(source), `clickable runner must not bypass frontend write paths: ${forbiddenPattern}`)
}

console.log('PASS: smart scheduling clickable coverage static contract')
