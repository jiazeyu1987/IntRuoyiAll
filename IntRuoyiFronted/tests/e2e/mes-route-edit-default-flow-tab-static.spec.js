const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routeFormContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')

const assertMatch = (content, pattern, label) => {
  if (!pattern.test(content)) {
    throw new Error(`${label} missing`)
  }
}

assertMatch(
  routeFormContent,
  /type RouteFormInitialTab =[\s\S]*\| 'basic'[\s\S]*\| 'flow'[\s\S]*\| 'product'/,
  'route form initial tab union type'
)

assertMatch(
  routeFormContent,
  /const open = async \(type: string, id\?: MesRouteId, initialTab: RouteFormInitialTab = 'basic'\) => \{[\s\S]*activeTab\.value = id \? initialTab : 'basic'/,
  'route form open must accept an initial tab for existing routes while defaulting create flow to basic'
)

assertMatch(
  routeEditPage,
  /const initialTab = computed\(\(\) => \{[\s\S]*return 'flow'[\s\S]*await content\.open\('update', routeId\.value, initialTab\.value\)/,
  'route edit page must default update entry to flow graph tab'
)

console.log('mes-route-edit-default-flow-tab-static PASS')
