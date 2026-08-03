const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routeFormContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} missing: ${expected}`)
  }
}

const assertMatch = (content, pattern, label) => {
  if (!pattern.test(content)) {
    throw new Error(`${label} missing`)
  }
}

const indexOfOrThrow = (content, needle, label) => {
  const index = content.indexOf(needle)
  if (index < 0) {
    throw new Error(`${label} missing: ${needle}`)
  }
  return index
}

const indexOfTabOrThrow = (content, label, name) => {
  const pattern = new RegExp(`<el-tab-pane[^>]*label="${label}"[^>]*name="${name}"`)
  const match = pattern.exec(content)
  if (!match) {
    throw new Error(`${label} tab missing with name ${name}`)
  }
  return match.index
}

assertMatch(
  routeFormContent,
  /type RouteFormInitialTab =[\s\S]*\| 'basic'[\s\S]*\| 'flow'[\s\S]*\| 'product'/,
  'route form tab union must include basic, flow, and product tabs'
)
assertIncludes(
  routeFormContent,
  "const activeTab = ref<RouteFormInitialTab>('basic')",
  'create flow defaults to basic tab'
)
indexOfTabOrThrow(routeFormContent, '基础信息', 'basic')
indexOfTabOrThrow(routeFormContent, '流转关系图', 'flow')
indexOfTabOrThrow(routeFormContent, '关联产品', 'product')
assertIncludes(routeFormContent, 'v-if="formData.id"', 'dependent route tabs remain guarded by route id')

const basicTabIndex = indexOfTabOrThrow(routeFormContent, '基础信息', 'basic')
const flowTabIndex = indexOfTabOrThrow(routeFormContent, '流转关系图', 'flow')
const productTabIndex = indexOfTabOrThrow(routeFormContent, '关联产品', 'product')

if (!(basicTabIndex < flowTabIndex && flowTabIndex < productTabIndex)) {
  throw new Error('route tabs must keep basic -> flow -> product')
}

const formStartIndex = indexOfOrThrow(routeFormContent, '<el-form', 'form start')
const tabsMatch = /<el-tabs[\s\S]*?v-model="activeTab"/.exec(routeFormContent)
if (!tabsMatch) {
  throw new Error('tabs start missing with activeTab model')
}
const tabsIndex = tabsMatch.index
const contentBeforeTabs = routeFormContent.slice(formStartIndex, tabsIndex)

for (const label of ['label="编码"', 'label="名称"', 'label="负责人"', 'label="说明"', 'label="备注"']) {
  if (contentBeforeTabs.includes(label)) {
    throw new Error(`${label} must render inside the basic info tab, not above the tabs`)
  }
}

assertMatch(
  routeFormContent,
  /<el-tab-pane[^>]*label="基础信息"[^>]*name="basic"[^>]*>[\s\S]*label="编码"[\s\S]*label="名称"[\s\S]*label="负责人"[\s\S]*label="说明"[\s\S]*label="备注"[\s\S]*<\/el-tab-pane>/,
  'basic tab must contain all basic route fields'
)
assertIncludes(routeFormContent, 'await formRef.value.validate()', 'top save validates the same form data')
assertIncludes(routeFormContent, 'ProRouteApi.updateRoute(data)', 'top save continues updating the same form data')
assertIncludes(
  routeEditPage,
  "return 'flow'",
  'edit page keeps default flow graph tab'
)
assertIncludes(
  routeEditPage,
  "await content.open('update', routeId.value, initialTab.value)",
  'edit page opens update form with resolved initial tab'
)

console.log('mes-route-basic-info-tab-static PASS')
