const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')
const routerPath = path.join(repoRoot, 'src/router/modules/remaining.ts')
const router = fs.readFileSync(routerPath, 'utf8')

const assertRouteExists = (route) => {
  assert.match(router, new RegExp(`path:\\s*'${route}'`), `必须保留正式路由 ${route}`)
}

const assertRouteAbsent = (route) => {
  assert.doesNotMatch(router, new RegExp(`path:\\s*'${route}'`), `必须移除冗余隐藏别名路由 ${route}`)
}

const assertSingleComponentRoute = (component) => {
  const matches = router.match(new RegExp(component.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g')) || []
  assert.strictEqual(matches.length, 1, `${component} 只允许保留一个前端路由入口`)
}

assertRouteExists('pro/feedback/edhr-work-task')
assertRouteExists('pro/feedback/edhr-signatures')
assertRouteExists('pro/feedback/edhr-label')
assertRouteExists('pro/feedback/edhr-form')

assertRouteAbsent('pro/edhr-work-task')
assertRouteAbsent('pro/feedback/edhr-signature')
assertRouteAbsent('pro/edhr-recordbook')
assertRouteAbsent('pro/feedback/edhr-recordbook')
assertRouteAbsent('pro/feedback/edhr-print-task')
assertRouteAbsent('pro/feedback/edhr-form-template')
assertRouteAbsent('pro/feedback/edhr-form-instance')

assertSingleComponentRoute('@/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue')
assertSingleComponentRoute('@/views/mes/pro/edhr/SignaturePage.vue')
assertSingleComponentRoute('@/views/mes/pro/edhr-label-print/LabelPrintQueuePage.vue')
assertSingleComponentRoute('@/views/mes/pro/edhr-form/FormPage.vue')

console.log('EDHR_REDUNDANT_ROUTE_ALIAS_STATIC_PASS=1')
