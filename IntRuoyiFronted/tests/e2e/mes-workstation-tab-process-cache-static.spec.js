const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routerHelper = read('src/utils/routerHelper.ts')
const tagsViewStore = read('src/store/modules/tagsView.ts')
const processPage = read('src/views/mes/pro/process/index.vue')
const workstationPage = read('src/views/mes/md/workstation/index.vue')

const extractFunction = (source, functionName) => {
  const start = source.indexOf(`const ${functionName}`)
  assert.notEqual(start, -1, `missing function ${functionName}`)
  const nextConst = source.indexOf('\nconst ', start + 1)
  const nextWatch = source.indexOf('\nwatch(', start + 1)
  const candidates = [nextConst, nextWatch].filter((index) => index > start)
  const end = candidates.length ? Math.min(...candidates) : source.length
  return source.slice(start, end)
}

const extractRouteWatcher = (source, routePathConstant) => {
  const start = source.indexOf('watch(\n  () => route.fullPath')
  assert.notEqual(start, -1, `${routePathConstant} page must watch route.fullPath`)
  const end = source.indexOf('\n</script>', start)
  assert.notEqual(end, -1, `${routePathConstant} page route watcher end not found`)
  return source.slice(start, end)
}

assert.match(
  tagsViewStore,
  /view\.meta\?\.tagsViewKeyMode\s*===\s*'path'[\s\S]*return normalizedPath \? `\/\$\{normalizedPath\}` : view\.path/,
  'tagsView store must support path identity so query-only workstation jumps reuse one tab'
)
assert.match(
  tagsViewStore,
  /TAGS_VIEW_PATH_IDENTITY_PATHS\s*=\s*new Set\(\[[\s\S]*'mes\/md\/workstation'[\s\S]*'md\/workstation'[\s\S]*\]\)/,
  'tagsView store must explicitly protect workstation path identity in runtime'
)
assert.match(
  tagsViewStore,
  /const normalizedPath = normalizeTagsViewPath\(view\.path\)[\s\S]*TAGS_VIEW_PATH_IDENTITY_PATHS\.has\(normalizedPath\)[\s\S]*return normalizedPath \? `\/\$\{normalizedPath\}` : view\.path/,
  'workstation path identity must ignore query even if dynamic route meta is absent at runtime'
)
assert.match(
  routerHelper,
  /WORKSTATION_ROUTE_COMPONENTS\s*=\s*new Set\(\[\s*'mes\/md\/workstation\/index',\s*'mes\/md\/workstation'\s*\]\)/,
  'dynamic router must identify workstation component paths'
)
assert.match(
  routerHelper,
  /WORKSTATION_ROUTE_PATHS\s*=\s*new Set\(\[\s*'mes\/md\/workstation',\s*'md\/workstation'\s*\]\)/,
  'dynamic router must identify workstation route paths with and without MES prefix'
)
assert.match(
  routerHelper,
  /WORKSTATION_ROUTE_PATHS\.has\(routePath\)[\s\S]*WORKSTATION_ROUTE_COMPONENTS\.has\(componentPath\)[\s\S]*meta\.tagsViewKeyMode\s*=\s*'path'/,
  'workstation dynamic route must set tagsViewKeyMode path'
)

assert.match(
  processPage,
  /const PROCESS_ROUTE_PATH = '\/mes\/pro\/process'/,
  'process page must declare its own route path'
)
assert.match(
  processPage,
  /const lastAppliedProcessRouteQuerySignature = ref\(''\)/,
  'process page must remember the last applied route query signature'
)
assert.match(
  processPage,
  /const buildProcessRouteQuerySignature = \(\) =>/,
  'process page must build a bounded query signature'
)
assert.match(
  extractFunction(processPage, 'buildProcessRouteQuerySignature'),
  /code:[\s\S]*name:[\s\S]*openId:/,
  'process route signature must only include process-owned query keys'
)

const processWatcher = extractRouteWatcher(processPage, 'PROCESS_ROUTE_PATH')
assert.match(
  processWatcher,
  /if \(route\.path !== PROCESS_ROUTE_PATH\) \{\s*return\s*\}/,
  'process route watcher must ignore workstation route changes'
)
assert.match(
  processWatcher,
  /const nextSignature = buildProcessRouteQuerySignature\(\)[\s\S]*if \(nextSignature === lastAppliedProcessRouteQuerySignature\.value\) \{\s*return\s*\}/,
  'process route watcher must skip duplicate signatures and avoid list reloads on tab return'
)
assert.match(
  processWatcher,
  /lastAppliedProcessRouteQuerySignature\.value = nextSignature[\s\S]*syncQueryParamsFromRoute\(\)[\s\S]*await getList\(\)[\s\S]*tryOpenDetailFromRoute\(\)/,
  'process route watcher must only refresh after current-route signature changes'
)
assert.doesNotMatch(
  processWatcher,
  /\(\) => \[route\.query\.code,\s*route\.query\.name,\s*route\.query\.openId\]/,
  'process route watcher must not watch bare query fields without route boundary'
)

assert.match(
  workstationPage,
  /const WORKSTATION_ROUTE_PATH = '\/mes\/md\/workstation'/,
  'workstation page must declare its own route path'
)
assert.match(
  workstationPage,
  /const lastAppliedWorkstationRouteQuerySignature = ref\(''\)/,
  'workstation page must remember the last applied route query signature'
)
assert.match(
  workstationPage,
  /const buildWorkstationRouteQuerySignature = \(\) =>/,
  'workstation page must build a bounded query signature'
)
assert.match(
  extractFunction(workstationPage, 'buildWorkstationRouteQuerySignature'),
  /code:[\s\S]*name:[\s\S]*processId:[\s\S]*openId:/,
  'workstation route signature must include only workstation-owned query keys'
)

const workstationWatcher = extractRouteWatcher(workstationPage, 'WORKSTATION_ROUTE_PATH')
assert.match(
  workstationWatcher,
  /if \(route\.path !== WORKSTATION_ROUTE_PATH\) \{\s*return\s*\}/,
  'workstation route watcher must ignore process route changes'
)
assert.match(
  workstationWatcher,
  /const nextSignature = buildWorkstationRouteQuerySignature\(\)[\s\S]*if \(nextSignature === lastAppliedWorkstationRouteQuerySignature\.value\) \{\s*return\s*\}/,
  'workstation route watcher must skip duplicate signatures'
)
assert.match(
  workstationWatcher,
  /lastAppliedWorkstationRouteQuerySignature\.value = nextSignature[\s\S]*syncQueryParamsFromRoute\(\)[\s\S]*await getList\(\)[\s\S]*tryOpenDetailFromRoute\(\)/,
  'workstation route watcher must refresh only after current-route signature changes'
)
assert.doesNotMatch(
  workstationWatcher,
  /\(\) => \[route\.query\.code,\s*route\.query\.name,\s*route\.query\.processId,\s*route\.query\.openId\]/,
  'workstation route watcher must not watch bare query fields without route boundary'
)

assert.match(
  processPage,
  /path:\s*'\/mes\/md\/workstation'[\s\S]*query:\s*\{[\s\S]*code:\s*workstationCode[\s\S]*processId:\s*String\(row\.id\)[\s\S]*\}/,
  'process workstation click must continue sending code and positive current processId to workstation page'
)

console.log('PASS: mes workstation tab and process cache static contract')
