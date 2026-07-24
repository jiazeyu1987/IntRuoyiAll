const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.ok(fs.existsSync(absolutePath), `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

function assertContains(source, expected, label) {
  assert.ok(source.includes(expected), `missing ${label}: ${expected}`)
}

function assertNotContains(source, forbidden, label) {
  assert.ok(!source.includes(forbidden), `unexpected ${label}: ${forbidden}`)
}

const packageJson = JSON.parse(readUtf8('package.json'))
const page = readUtf8('src/views/system/config-package/index.vue')
const api = readUtf8('src/api/system/configPackage/index.ts')
const manifest = readUtf8('src/utils/frontendComponentManifest.ts')

assert.equal(
  packageJson.scripts?.['e2e:system:config-package:static'],
  'node tests/e2e/system-config-package-static.spec.js',
  'package.json must expose the system config package static gate'
)

for (const fragment of [
  "defineOptions({ name: 'SystemConfigPackage' })",
  'getFrontendComponentPaths',
  'precheckConfigPackage',
  'importConfigPackage',
  "v-hasPermi=\"['system:config-package:export']\"",
  "v-hasPermi=\"['system:config-package:import']\"",
  '预检通过后才能覆盖导入',
  '当前前端构建组件清单为空'
]) {
  assertContains(page, fragment, 'config package page contract')
}

for (const fragment of [
  '/system/config-package/export-excel',
  '/system/config-package/precheck',
  '/system/config-package/import',
  "data.append('availableComponents', availableComponents.join(','))",
  "data.append('confirmed', 'true')"
]) {
  assertContains(api, fragment, 'config package API contract')
}

assertContains(
  manifest,
  "import.meta.glob('../views/**/*.{vue,tsx}')",
  'frontend component manifest must scan current views'
)
assertContains(manifest, 'normalizeViewPath', 'frontend component manifest must normalize paths')
assertNotContains(page, 'catch {}', 'empty catch in config package page')
assertNotContains(page, 'catch{}', 'empty catch in config package page')
assertNotContains(page, '含密码', 'password hash export option')
assertNotContains(page, 'includePasswordHash', 'password hash export state')
assertNotContains(api, 'includePasswordHash', 'password hash export API parameter')
assertNotContains(api, "params: { includePasswordHash }", 'password hash export request parameter')

console.log('PASS: system config package static contract is present')
