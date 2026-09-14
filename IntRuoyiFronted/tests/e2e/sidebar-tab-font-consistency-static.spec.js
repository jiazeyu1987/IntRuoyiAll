const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractBetween = (source, start, end) => {
  const startIndex = source.indexOf(start)
  assert.notEqual(startIndex, -1, `missing source marker: ${start}`)
  const endIndex = source.indexOf(end, startIndex + start.length)
  assert.notEqual(endIndex, -1, `missing source marker: ${end}`)
  return source.slice(startIndex, endIndex)
}

const variables = readSource('src/styles/var.css')
const globalStyle = readSource('src/styles/index.scss')
const mainSource = readSource('src/main.ts')
const menuSource = readSource('src/layout/components/Menu/src/Menu.vue')

assert.match(
  mainSource,
  /import\s+['"]@\/styles\/index\.scss['"]/,
  'global style entry must be imported by the app'
)

assert.match(
  variables,
  /--app-fixed-tab-font-family:\s*"Microsoft YaHei";/,
  'fixed tab/menu font family must be declared once in global variables'
)

assert.match(
  variables,
  /--app-fixed-tab-font-weight:\s*600;/,
  'fixed tab/menu font weight must be declared once in global variables'
)

assert.match(
  globalStyle,
  /\.el-tabs__item\s*\{[^}]*font-family:\s*var\(--app-fixed-tab-font-family\);[^}]*font-synthesis-weight:\s*none;[^}]*font-weight:\s*var\(--app-fixed-tab-font-weight\);/s,
  'Element Plus tabs must use the fixed font family and weight'
)

assert.match(
  globalStyle,
  /\.el-tabs__item\.is-active\s*\{[^}]*font-weight:\s*var\(--app-fixed-tab-font-weight\);/s,
  'active Element Plus tabs must keep the fixed font weight'
)

const scopedMenuStyle = extractBetween(menuSource, '<style lang="scss" scoped>', '</style>')
assert.match(
  scopedMenuStyle,
  /\.#\{\$elNamespace\}-sub-menu__title,\s*\.#\{\$elNamespace\}-menu-item,\s*\.#\{\$prefix-cls\}__title\s*\{[^}]*font-family:\s*var\(--app-fixed-tab-font-family\);[^}]*font-synthesis-weight:\s*none;[^}]*font-weight:\s*var\(--app-fixed-tab-font-weight\);/s,
  'sidebar menu titles and items must use the fixed font family and weight'
)

const popperMenuStyle = extractBetween(
  menuSource,
  '<style lang="scss">\n$prefix-cls: #{$namespace}-menu-popper;',
  '</style>'
)
assert.match(
  popperMenuStyle,
  /\.el-sub-menu__title,\s*\.el-menu-item,\s*\.v-menu__title\s*\{[^}]*font-family:\s*var\(--app-fixed-tab-font-family\);[^}]*font-synthesis-weight:\s*none;[^}]*font-weight:\s*var\(--app-fixed-tab-font-weight\);/s,
  'sidebar menu popper titles and items must use the fixed font family and weight'
)

assert.doesNotMatch(
  `${variables}\n${globalStyle}\n${menuSource}`,
  /mock|placeholder|fallback|降级|吞异常/i,
  'font consistency fix must not introduce mock, placeholder, fallback, downgrade, or swallowed-error paths'
)

console.log('PASS: sidebar tab font consistency static contract')
