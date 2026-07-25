const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const api = read('src/api/system/codexTestManagement/index.ts')
const page = read('src/views/system/codex-test-management/index.vue')
const runner = read('scripts/codex-test-runner.mjs')

for (const endpoint of [
  '/system/codex-test-case/page',
  '/system/codex-test-case/create',
  '/system/codex-test-execution/start',
  '/system/codex-test-execution/artifact'
]) {
  assert.ok(api.includes(endpoint), `missing API endpoint ${endpoint}`)
}

for (const permission of [
  'system:codex-test:create',
  'system:codex-test:update',
  'system:codex-test:delete',
  'system:codex-test:execute',
  'system:codex-test:artifact'
]) {
  assert.ok(page.includes(permission), `missing page permission ${permission}`)
}

assert.match(page, /测试租户/)
assert.match(page, /测试方法项/)
assert.match(page, /测试目标项/)
assert.match(page, /formatMethodItems/)
assert.match(page, /formatTargetItems/)
assert.match(page, /caseTableRows/)
assert.match(page, /caseRowSpanMethod/)
assert.match(page, /displayMethodItem/)
assert.match(page, /displayTargetItem/)
assert.match(page, /:span-method="caseRowSpanMethod"/)
assert.match(page, /startSingleCaseExecution/)
assert.match(page, /@click="startSingleCaseExecution\(row\)"/)
assert.match(page, /caseIds:\s*\[caseId\]/)
assert.match(page, /executionMode:\s*row\.defaultExecutionMode/)
assert.match(page, /检查点/)
assert.match(page, /通过/)
assert.match(page, /失败/)
assert.match(page, /失败截图/)
assert.match(page, /并行执行/)
assert.ok(!page.includes('catch {}'), 'request failures must remain visible')

assert.match(runner, /codex(?:\.cmd)?\s+exec/)
assert.match(runner, /playwright/)
assert.match(runner, /CODEX_TEST_TENANT_ID/)
assert.match(runner, /tenant-id/)
assert.match(runner, /CODEX_TEST_POLL_INTERVAL_MS/)
assert.match(runner, /CODEX_TEST_HEARTBEAT_INTERVAL_MS/)
assert.match(runner, /CODEX_TEST_CODEX_TIMEOUT_MS/)
assert.match(runner, /await sleep\(POLL_INTERVAL_MS\)/)
assert.match(runner, /function spawnCodex/)
assert.match(runner, /isWindowsCommandScript/)
assert.match(runner, /cmd\.exe/)
assert.match(runner, /'\/d', '\/s', '\/c'/)
assert.match(runner, /spawnSync\('taskkill\.exe'/)
assert.match(runner, /function stopWindowsProcessTree/)
assert.match(runner, /CommandLine\.Contains\(\$needle\)/)
assert.match(runner, /await heartbeat\(runnerSessionId, runningExecutionCaseIds\)/)
assert.match(runner, /finally\s*{[\s\S]*clearInterval\(heartbeatTimer\)[\s\S]*clearTimeout\(timeoutTimer\)[\s\S]*}/)
assert.match(runner, /function reportTaskBlocked/)
assert.match(runner, /status:\s*'BLOCKED'/)
assert.match(runner, /reportTaskBlocked\(task, error\)/)
assert.match(runner, /checkpoint-result/)
assert.match(runner, /complete-case/)
