const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const viteConfigPath = path.join(root, 'vite.config.ts')
const viteConfigSource = fs.readFileSync(viteConfigPath, 'utf8')

assert.match(
  viteConfigSource,
  /noDiscovery:\s*true/,
  'This regression covers the Windows safe Vite optimizer profile with noDiscovery enabled'
)

const windowsSafeIncludeMatch = viteConfigSource.match(
  /const windowsSafeOptimizeInclude = \[([\s\S]*?)\]\s*if \(isBatchRecordPreviewMode/
)
assert.ok(windowsSafeIncludeMatch, 'vite.config.ts must declare windowsSafeOptimizeInclude before use')

const windowsSafeIncludeEntries = new Set(
  [...windowsSafeIncludeMatch[1].matchAll(/'([^']+)'/g)].map((match) => match[1])
)

const pnpmDir = path.join(root, 'node_modules', '.pnpm')
assert.ok(fs.existsSync(pnpmDir), `Missing dependency directory: ${pnpmDir}`)

const elementPlusPackageName = fs
  .readdirSync(pnpmDir)
  .find((entry) => entry.startsWith('element-plus@'))
assert.ok(elementPlusPackageName, 'Missing pnpm package for element-plus')

const elementPlusEsDir = path.join(
  pnpmDir,
  elementPlusPackageName,
  'node_modules',
  'element-plus',
  'es'
)
assert.ok(fs.existsSync(elementPlusEsDir), `Missing Element Plus ES directory: ${elementPlusEsDir}`)

const walkFiles = (dir) => {
  const entries = fs.readdirSync(dir, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      files.push(...walkFiles(fullPath))
      continue
    }
    if (entry.isFile() && entry.name.endsWith('.mjs')) {
      files.push(fullPath)
    }
  }
  return files
}

const dayjsPluginDeps = new Set()
for (const file of walkFiles(elementPlusEsDir)) {
  const source = fs.readFileSync(file, 'utf8')
  for (const match of source.matchAll(/['"](dayjs\/plugin\/[^'"]+\.js)['"]/g)) {
    dayjsPluginDeps.add(match[1])
  }
}

assert.ok(
  dayjsPluginDeps.has('dayjs/plugin/localeData.js'),
  'Element Plus calendar/date components must import dayjs/plugin/localeData.js for this regression'
)

for (const dep of [...dayjsPluginDeps].sort()) {
  assert.ok(
    windowsSafeIncludeEntries.has(dep),
    `Vite windows-safe optimizeDeps.include must pre-optimize ${dep}`
  )
}

console.log('PASS: Element Plus dayjs plugin deps are covered by Vite windows-safe optimizeDeps.include')
