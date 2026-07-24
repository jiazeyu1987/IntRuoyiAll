const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const componentPath = path.join(repoRoot, 'src/components/RouterSearch/index.vue')
const component = fs.readFileSync(componentPath, 'utf8')

function assertIncludes(source, expected, message) {
  if (!source.includes(expected)) {
    throw new Error(`${message}: missing ${expected}`)
  }
}

function assertNotIncludes(source, unexpected, message) {
  if (source.includes(unexpected)) {
    throw new Error(`${message}: found ${unexpected}`)
  }
}

assertIncludes(
  component,
  'const ROUTER_SEARCH_HISTORY_LIMIT = 20',
  'router search history keeps the latest 20 records'
)
assertNotIncludes(
  component,
  'const ROUTER_SEARCH_HISTORY_LIMIT = 10',
  'router search history must not keep the old 10 record limit'
)
assertIncludes(
  component,
  '.slice(0, ROUTER_SEARCH_HISTORY_LIMIT)',
  'history load, persist, and record paths reuse the shared limit'
)

const sliceUsageCount = (component.match(/\.slice\(0, ROUTER_SEARCH_HISTORY_LIMIT\)/g) || []).length
if (sliceUsageCount < 3) {
  throw new Error(
    `expected at least 3 shared limit usages for load, persist, and record paths, got ${sliceUsageCount}`
  )
}

console.log('router search history limit static contract passed')
