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
assert.match(page, /自然语言测试方法/)
assert.match(page, /检查点/)
assert.match(page, /通过/)
assert.match(page, /失败/)
assert.match(page, /失败截图/)
assert.match(page, /并行执行/)
assert.ok(!page.includes('catch {}'), 'request failures must remain visible')

assert.match(runner, /codex(?:\.cmd)?\s+exec/)
assert.match(runner, /playwright/)
assert.match(runner, /checkpoint-result/)
assert.match(runner, /complete-case/)
