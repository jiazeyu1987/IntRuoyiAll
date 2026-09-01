const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const source = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const extractCssBlock = (selector) => {
  const start = source.indexOf(selector)
  assert.notEqual(start, -1, `Expected style selector: ${selector}`)
  const openBrace = source.indexOf('{', start)
  assert.notEqual(openBrace, -1, `Expected opening brace for selector: ${selector}`)

  let depth = 0
  for (let index = openBrace; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(start, index + 1)
      }
    }
  }

  assert.fail(`Unterminated style block for selector: ${selector}`)
}

const tabsNavWrap = extractCssBlock('.team-leader-workbench__module-tabs :deep(.el-tabs__nav-wrap)')
const tabsItem = extractCssBlock('.team-leader-workbench__module-tabs :deep(.el-tabs__item)')

assert.match(
  tabsNavWrap,
  /min-width:\s*0/,
  'Shared team-leader module tabs must keep the nav wrap from forcing multi-line layout.'
)

assert.match(
  tabsItem,
  /max-width:\s*240px/,
  'Shared team-leader module tabs must cap each tab item so long labels do not expand into wrapped rows.'
)
assert.match(
  tabsItem,
  /overflow:\s*hidden/,
  'Shared team-leader module tabs must hide overflow instead of wrapping text.'
)
assert.match(
  tabsItem,
  /text-overflow:\s*ellipsis/,
  'Shared team-leader module tabs must show ellipsis for long labels.'
)
assert.match(
  tabsItem,
  /white-space:\s*nowrap/,
  'Shared team-leader module tabs must keep each existing and future tab label on one line.'
)

console.log('PASS: team leader module tabs single-line static contract')
