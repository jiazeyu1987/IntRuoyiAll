const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const toolHeaderPath = 'src/layout/components/ToolHeader.vue'

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`missing ${label}: ${expected}`)
  }
}

function assertNotContains(source, forbidden, label) {
  if (source.includes(forbidden)) {
    throw new Error(`forbidden ${label}: ${forbidden}`)
  }
}

const toolHeader = readUtf8(toolHeaderPath)

for (const [label, token] of [
  ['tenant visit import or usage', 'TenantVisit'],
  ['size dropdown import or usage', 'SizeDropdown'],
  ['locale dropdown import or usage', 'LocaleDropdown']
]) {
  assertNotContains(toolHeader, token, label)
}

for (const [label, token] of [
  ['tenant permission state', 'canVisitTenant'],
  ['user store import', 'useUserStore'],
  ['size state', 'appStore.getSize'],
  ['locale state', 'appStore.getLocale']
]) {
  assertNotContains(toolHeader, token, label)
}

assertContains(toolHeader, "import RouterSearch from '@/components/RouterSearch/index.vue'", 'module search import remains')
assertContains(toolHeader, 'appStore.search', 'module search state remains')
assertContains(toolHeader, '<RouterSearch', 'module search usage remains')
assertContains(toolHeader, 'alwaysVisible={true}', 'module search stays visible')
assertContains(toolHeader, 'Message', 'message entry remains')
assertContains(toolHeader, '<UserInfo>', 'user info entry remains')
assertContains(toolHeader, 'useAppStore', 'layout state remains for existing header controls')

console.log('PASS: tenant, size, and locale controls are removed while module search, message, and user info remain')
