const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const vitePluginConfig = fs.readFileSync(path.join(repoRoot, 'build', 'vite', 'index.ts'), 'utf8')

assert.equal(
  vitePluginConfig.includes('vite-plugin-top-level-await'),
  false,
  'Release builds must not enable vite-plugin-top-level-await unless a real top-level await requirement is added.'
)

assert.equal(
  vitePluginConfig.includes('topLevelAwait('),
  false,
  'Release builds must leave dynamic imports to Vite/Rollup instead of wrapping all chunks with top-level-await transforms.'
)
