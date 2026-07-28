const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const globalStyle = fs.readFileSync(path.join(repoRoot, 'src/styles/index.scss'), 'utf8')
const mainSource = fs.readFileSync(path.join(repoRoot, 'src/main.ts'), 'utf8')

assert.match(
  mainSource,
  /import\s+['"]@\/styles\/index\.scss['"]/,
  'global style entry must be imported by the app'
)

assert.match(
  globalStyle,
  /\.el-tabs__item\s*\{[^}]*font-family:\s*var\(--app-fixed-tab-font-family\);[^}]*font-synthesis-weight:\s*none;[^}]*font-weight:\s*var\(--app-fixed-tab-font-weight\);/,
  'Element Plus tab labels must use the global fixed font family and weight'
)

assert.match(
  globalStyle,
  /\.el-tabs__item\.is-active\s*\{[^}]*font-weight:\s*var\(--app-fixed-tab-font-weight\);/,
  'active Element Plus tab labels must keep the same fixed bold weight'
)

console.log('PASS: Element Plus tabs fixed bold static contract')
