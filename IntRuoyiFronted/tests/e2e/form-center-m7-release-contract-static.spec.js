const assert = require('node:assert/strict')
const childProcess = require('node:child_process')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')

const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')
const exists = (file) => fs.existsSync(path.join(root, file))

const packageJson = JSON.parse(read('package.json'))

assert.equal(packageJson.name, 'yudao-ui-admin-vue3')
assert.equal(packageJson.scripts['ts:check'], 'cross-env NODE_OPTIONS=--max-old-space-size=8192 node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json')

for (const scriptName of ['build:local', 'build:dev', 'build:test', 'build:stage', 'build:prod']) {
  assert.ok(packageJson.scripts[scriptName], `missing release build script ${scriptName}`)
  assert.match(packageJson.scripts[scriptName], /vite build --mode /)
}

for (const file of [
  'src/api/form-center/actionProjection.ts',
  'tests/e2e/form-center-action-projection-static.spec.js',
  'tests/e2e/form-center-official-page-projection-static.spec.js',
  'tests/e2e/form-center-official-pages-projection-static.spec.js',
  'tests/e2e/form-center-official-pages-projection-real.e2e.js',
  'tests/e2e/vite-bpmn-randomcolor-optimize-deps.spec.js'
]) {
  assert.ok(exists(file), `missing frontend release contract artifact: ${file}`)
}

const packageDiff = childProcess
  .execFileSync('git', ['diff', '--name-only', 'int_main...HEAD', '--', 'package.json', 'pnpm-lock.yaml'], {
    cwd: root,
    encoding: 'utf8'
  })
  .trim()

assert.equal(packageDiff, '', 'M7 frontend release inventory must not introduce package or lockfile drift')

const viteConfig = read('vite.config.ts')
const optimize = read('build/vite/optimize.ts')

assert.match(viteConfig, /createRequire\(\s*require\.resolve\('bpmn-js-token-simulation\/package\.json'\)\s*\)/)
assert.match(viteConfig, /find:\s*'randomcolor'[\s\S]*replacement:\s*randomColorPath/)
assert.ok(optimize.includes("'randomcolor'"), 'default optimizeDeps include must keep randomcolor release-safe')

console.log('PASS: M7 frontend release contract inventory is static and package-safe')
