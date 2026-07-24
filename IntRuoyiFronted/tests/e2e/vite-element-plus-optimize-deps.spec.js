const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const optimizePath = path.join(root, 'build', 'vite', 'optimize.ts')
const optimizeSource = fs.readFileSync(optimizePath, 'utf8')
const includeEntries = new Set(
  [...optimizeSource.matchAll(/'([^']+)'/g)].map((match) => match[1])
)

const nasRouteElementPlusStyleDeps = [
  'element-plus/es/components/base/style/css',
  'element-plus/es/components/loading/style/css',
  'element-plus/es/components/date-picker/style/css',
  'element-plus/es/components/tree/style/css',
  'element-plus/es/components/divider/style/css',
  'element-plus/es/components/message-box/style/css'
]

for (const dep of nasRouteElementPlusStyleDeps) {
  assert.ok(
    includeEntries.has(dep),
    `Vite optimizeDeps.include must pre-optimize ${dep} for the lazy NAS route`
  )
}

console.log('PASS: Element Plus lazy route style deps are covered by Vite optimizeDeps.include')
