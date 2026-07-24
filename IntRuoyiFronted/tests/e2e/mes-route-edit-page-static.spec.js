const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (file) => fs.readFileSync(path.join(repoRoot, file), 'utf8')

const routeIndex = read('src/views/mes/pro/route/index.vue')
const routeForm = read('src/views/mes/pro/route/RouteForm.vue')
const routeFormContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const routeFlowConfigPanel = read('src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const remainingRouter = read('src/router/modules/remaining.ts')

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} must include: ${expected}`)
  }
}

const assertNotIncludes = (content, expected, label) => {
  if (content.includes(expected)) {
    throw new Error(`${label} must not include: ${expected}`)
  }
}

assertIncludes(routeIndex, '@click="openEditPage(scope.row.id)"', 'route list edit action opens route page')
assertIncludes(routeIndex, "name: 'MesProRouteEdit'", 'route list pushes hidden edit route')
assertNotIncludes(routeIndex, '@click="openForm(\'update\', scope.row.id)"', 'route list edit action no longer opens dialog')
assertNotIncludes(routeIndex, '@click="openForm(\'create\')"', 'route list no longer exposes the removed create action')
assertIncludes(routeIndex, "openForm('detail', Number(openId))", 'deep-link detail keeps existing dialog flow')

assertIncludes(routeForm, '<RouteFormContent', 'dialog shell reuses shared route form content')
assertIncludes(routeForm, "defineOptions({ name: 'RouteForm' })", 'dialog component remains available for create/detail')
assertNotIncludes(routeForm, '<el-tabs v-model="activeTab">', 'tabs moved out of dialog shell')

assertIncludes(routeFormContent, "defineOptions({ name: 'RouteFormContent' })", 'shared content component has stable name')
assertIncludes(routeFormContent, 'RouteProcessList', 'shared content keeps process tab')
assertIncludes(routeFormContent, 'RouteFlowGraphDesigner', 'shared content keeps flow graph tab')
assertIncludes(routeFormContent, 'RouteFlowConfigPanel', 'shared content keeps flow config tabs')
assertIncludes(routeFormContent, 'label="排产配置"', 'shared content exposes schedule config tab')
assertIncludes(routeFormContent, 'label="批记录配置"', 'shared content exposes batch record config tab')
assertIncludes(routeFormContent, 'RouteProductList', 'shared content keeps product tab')
assertIncludes(routeFormContent, 'submitForm', 'shared content owns save behavior')

assertIncludes(routeEditPage, "defineOptions({ name: 'MesProRouteEdit' })", 'edit page component name matches route')
assertIncludes(routeEditPage, '<RouteFormContent', 'edit page renders shared route form content')
assertIncludes(routeEditPage, 'mode="page"', 'edit page uses page mode')
assertIncludes(routeEditPage, "await content.open('update', routeId.value, initialTab.value)", 'edit page opens update form with resolved initial tab')
assertIncludes(routeEditPage, 'route.query.routeProcessId', 'edit page reads target route process query')
assertIncludes(routeEditPage, ':target-route-process-id="targetRouteProcessId"', 'edit page forwards target route process query')
assertIncludes(routeFormContent, ':target-route-process-id="targetRouteProcessId"', 'shared content forwards target route process query')
assertIncludes(routeFlowConfigPanel, 'highlight-current-row', 'schedule config panel highlights current row')
assertIncludes(routeFlowConfigPanel, 'setCurrentRow', 'schedule config panel selects target route process')
assertIncludes(routeFlowConfigPanel, 'scrollIntoView', 'schedule config panel scrolls target route process into view')

assertIncludes(remainingRouter, "name: 'MesProRouteEdit'", 'hidden edit route registered')
assertIncludes(
  remainingRouter,
  "component: () => import('@/views/mes/pro/route/RouteEditPage.vue')",
  'hidden edit route loads edit page'
)
assertIncludes(remainingRouter, "activeMenu: '/mes/pro/route'", 'hidden edit route keeps menu context')

console.log('mes-route-edit-page-static PASS')
