const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const optimizePath = path.join(root, 'build', 'vite', 'optimize.ts')
const viteConfigPath = path.join(root, 'vite.config.ts')

const optimizeSource = fs.readFileSync(optimizePath, 'utf8')
const optimizeIncludeEntries = new Set(
  [...optimizeSource.matchAll(/'([^']+)'/g)].map((match) => match[1])
)

assert.ok(
  optimizeIncludeEntries.has('randomcolor'),
  'Vite default optimizeDeps.include must pre-optimize randomcolor for BPM token simulation'
)

const viteConfigSource = fs.readFileSync(viteConfigPath, 'utf8')
const windowsSafeIncludeMatch = viteConfigSource.match(
  /const windowsSafeOptimizeInclude = \[([\s\S]*?)\]\s*const devWatchIgnored/
)
assert.ok(windowsSafeIncludeMatch, 'vite.config.ts must declare windowsSafeOptimizeInclude before use')

const windowsSafeIncludeEntries = new Set(
  [...windowsSafeIncludeMatch[1].matchAll(/'([^']+)'/g)].map((match) => match[1])
)

assert.ok(
  windowsSafeIncludeEntries.has('randomcolor'),
  'Vite windows-safe optimizeDeps.include must pre-optimize randomcolor for BPM detail route startup'
)

assert.match(
  viteConfigSource,
  /createRequire\(\s*require\.resolve\('bpmn-js-token-simulation\/package\.json'\)\s*\)/,
  'vite.config.ts must resolve randomcolor from the BPM token simulation package instead of hardcoding pnpm internals'
)

assert.match(
  viteConfigSource,
  /find:\s*'randomcolor'[\s\S]*replacement:\s*randomColorPath/,
  'Vite resolve.alias must route randomcolor through the package-relative resolved CJS entry'
)

console.log('PASS: BPM randomcolor dependency is covered by Vite optimizeDeps.include')
