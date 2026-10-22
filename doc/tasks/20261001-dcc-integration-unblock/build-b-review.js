// Use the existing configured build with task-owned output/cache directories.
const path = require('node:path')
const { createRequire } = require('node:module')
const root = path.resolve(process.cwd())
const expectedRoot = path.resolve('C:/IntRuoyi/20261001-dcc-integration/IntRuoyiFronted')
if (root !== expectedRoot) throw new Error('Run from the manager integration frontend worktree')
const localRequire = createRequire(path.join(root, 'package.json'))
const { build } = localRequire('vite')
const target = path.join(root, 'target')
const outDir = path.join(target, '20261001-dcc-b-review-build')
const cacheDir = path.join(target, '20261001-dcc-b-review-cache')
for (const directory of [outDir, cacheDir]) {
  if (path.dirname(directory) !== target) throw new Error('Build directory escapes the task-owned target')
}
build({ mode: 'env.local', cacheDir, build: { outDir, emptyOutDir: true } })
  .then(() => console.log('PASS: configured integration build with isolated B review output/cache'))
  .catch(error => { console.error(error); process.exitCode = 1 })
