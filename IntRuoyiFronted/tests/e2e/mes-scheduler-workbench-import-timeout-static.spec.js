const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const apiSource = readText('src/api/mes/pro/schedulerWorkbench/index.ts')
const pageSource = readText('src/views/mes/pro/scheduler-workbench/index.vue')
const packageJson = JSON.parse(readText('package.json'))

assert.equal(
  packageJson.scripts?.['e2e:mes:scheduler-workbench-import-timeout:static'],
  'node tests/e2e/mes-scheduler-workbench-import-timeout-static.spec.js',
  'package.json must expose the scheduler workbench import timeout static gate'
)

for (const fragment of [
  'export const SCHEDULER_WORKBENCH_IMPORT_TIMEOUT = 120000',
  "url: '/mes/pro/scheduler-workbench/route-config/import'",
  "url: '/mes/pro/scheduler-workbench/full-config/import'",
  'timeout: SCHEDULER_WORKBENCH_IMPORT_TIMEOUT',
  'replanMasterDataCount: number',
  'replanScheduleOrderDataCount: number',
  'replanRuntimeDataCount: number',
  'policySettingsCount: number'
]) {
  assert.ok(apiSource.includes(fragment), `missing scheduler workbench import timeout contract: ${fragment}`)
}

assert.match(
  apiSource,
  /importRouteConfigPackage[\s\S]*timeout:\s*SCHEDULER_WORKBENCH_IMPORT_TIMEOUT/,
  'route config import must override the global 30s timeout'
)
assert.match(
  apiSource,
  /importFullConfigPackage[\s\S]*timeout:\s*SCHEDULER_WORKBENCH_IMPORT_TIMEOUT/,
  'full config import must override the global 30s timeout'
)

for (const fragment of [
  '手动重排主数据 ${result.replanMasterDataCount} 条',
  '排产工单数据 ${result.replanScheduleOrderDataCount} 条',
  '运行态数据 ${result.replanRuntimeDataCount} 条',
  '策略设置 ${result.policySettingsCount} 条'
]) {
  assert.ok(pageSource.includes(fragment), `full config import message must include replan count: ${fragment}`)
}

console.log('mes-scheduler-workbench-import-timeout-static: PASS')
