const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const menuPath = path.join(repoRoot, 'src/layout/components/Menu/src/Menu.vue')
const menuSource = fs.readFileSync(menuPath, 'utf8')

const extractBetween = (source, start, end) => {
  const startIndex = source.indexOf(start)
  assert.notEqual(startIndex, -1, `missing source marker: ${start}`)
  const endIndex = source.indexOf(end, startIndex + start.length)
  assert.notEqual(endIndex, -1, `missing source marker: ${end}`)
  return source.slice(startIndex, endIndex)
}

const scopedMenuStyle = extractBetween(menuSource, '<style lang="scss" scoped>', '</style>')
const popperMenuStyle = extractBetween(
  menuSource,
  '<style lang="scss">\n$prefix-cls: #{$namespace}-menu-popper;',
  '</style>'
)

assert.match(
  scopedMenuStyle,
  /\.#\{\$elNamespace\}-sub-menu__title,\s*\.#\{\$elNamespace\}-menu-item,\s*\.#\{\$prefix-cls\}__title\s*\{[^{}]*font-family:\s*var\(--app-fixed-tab-font-family\);[^{}]*font-weight:\s*var\(--app-fixed-tab-font-weight\);[^{}]*\}/s,
  'main sidebar title typography must remain shared without a nested title hover background'
)

assert.match(
  scopedMenuStyle,
  /\.#\{\$elNamespace\}-sub-menu__title,\s*\.#\{\$elNamespace\}-menu-item\s*\{\s*&:hover\s*\{[^}]*background-color:\s*var\(--left-menu-bg-color\)\s*!important;/s,
  'main sidebar hover background must be owned by the complete interactive menu row'
)

assert.match(
  scopedMenuStyle,
  /\.#\{\$elNamespace\}-menu-item\.is-active\s*\{[^}]*background-color:\s*var\(--left-menu-bg-active-color\)\s*!important;\s*&:hover\s*\{[^}]*background-color:\s*var\(--left-menu-bg-active-color\)\s*!important;/s,
  'selected main sidebar rows must keep the active background while hovered'
)

assert.match(
  popperMenuStyle,
  /\.el-sub-menu__title,\s*\.el-menu-item,\s*\.v-menu__title\s*\{[^{}]*font-family:\s*var\(--app-fixed-tab-font-family\);[^{}]*font-weight:\s*var\(--app-fixed-tab-font-weight\);[^{}]*\}/s,
  'popper title typography must remain shared without a nested title hover background'
)

assert.match(
  popperMenuStyle,
  /\.el-sub-menu__title,\s*\.el-menu-item\s*\{\s*&:hover\s*\{[^}]*background-color:\s*var\(--left-menu-bg-color\)\s*!important;/s,
  'popper hover background must be owned by the complete interactive menu row'
)

assert.match(
  popperMenuStyle,
  /\.el-menu-item\.is-active\s*\{[^}]*background-color:\s*var\(--left-menu-bg-active-color\)\s*!important;\s*&:hover\s*\{[^}]*background-color:\s*var\(--left-menu-bg-active-color\)\s*!important;/s,
  'selected popper rows must keep the active background while hovered'
)

console.log('PASS: sidebar selected hover background remains uniform')
