const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const router = read('src/router/modules/remaining.ts')
const productionPagePath = 'src/views/mes/pro/edhr-batch/BatchProductionFillPage.vue'
const pqcPagePath = 'src/views/mes/pro/edhr-batch/BatchPqcFillPage.vue'
const tabsPath = 'src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue'
const frontlinePanel = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

for (const pagePath of [productionPagePath, pqcPagePath, tabsPath]) {
  assert.ok(exists(pagePath), `${pagePath} must exist.`)
}

const productionPage = read(productionPagePath)
const pqcPage = read(pqcPagePath)
const tabs = read(tabsPath)

for (const tabName of ['批次执行', '历史批记录', '生产填写', 'PQC填写']) {
  assert.match(tabs, new RegExp(tabName), `eDHR batch tabs must include ${tabName}.`)
}
assert.match(
  tabs,
  /query:\s*\{\s*\.\.\.route\.query\s*\}/,
  'eDHR batch tabs must preserve report-work query context when switching tabs.'
)

for (const route of [
  {
    path: 'pro/feedback/edhr-batch-production-fill',
    name: 'MesProEdhrBatchProductionFill',
    component: 'BatchProductionFillPage.vue',
    title: '生产填写'
  },
  {
    path: 'pro/feedback/edhr-batch-pqc-fill',
    name: 'MesProEdhrBatchPqcFill',
    component: 'BatchPqcFillPage.vue',
    title: 'PQC填写'
  }
]) {
  const routeIndex = router.indexOf(`path: '${route.path}'`)
  assert.ok(routeIndex >= 0, `${route.path} route must exist.`)
  const routeBlock = router.slice(routeIndex, router.indexOf('\n      {', routeIndex + route.path.length))
  assert.match(routeBlock, new RegExp(`name: '${route.name}'`), `${route.path} route name must be stable.`)
  assert.match(routeBlock, new RegExp(route.component), `${route.path} route component must be stable.`)
  assert.match(routeBlock, new RegExp(`title: '${route.title}'`), `${route.path} route title must be visible.`)
  assert.match(routeBlock, /permission:\s*\['mes:pro-edhr-batch-execution:query'\]/, `${route.path} must reuse eDHR batch permission.`)
}

assert.match(productionPage, /<EdhrBatchRecordTabs\s+active-tab="production"/, 'production fill page must render shared tabs.')
assert.match(productionPage, /<FrontlineFixedTemplatePanel\s+mode="production"/, 'production fill page must lock production mode.')
assert.match(pqcPage, /<EdhrBatchRecordTabs\s+active-tab="pqc"/, 'PQC fill page must render shared tabs.')
assert.match(pqcPage, /<FrontlineFixedTemplatePanel\s+mode="pqc"/, 'PQC fill page must lock PQC mode.')

assert.match(frontlinePanel, /defineProps<\{\s*mode\?:\s*'production'\s*\|\s*'pqc'/s, 'frontline panel must accept a fixed mode prop.')
assert.match(frontlinePanel, /templateModeMismatch/, 'frontline panel must expose template mismatch blocking state.')
const employeeSwitchBlock = frontlinePanel.match(/const handleSelectEmployee[\s\S]*?\n}\n\nconst handleValidate/)
assert.ok(employeeSwitchBlock, 'employee switch handler must exist.')
assert.doesNotMatch(employeeSwitchBlock[0], /context\.templateCode\s*=\s*templateCode/, 'employee switch must not silently change the current page UI mode.')
assert.match(frontlinePanel, /selectedDeviceCards\.value\.slice\(0,\s*3\)/, 'production device cards must be limited to three devices.')
assert.doesNotMatch(frontlinePanel, /el-tabs[\s\S]*设备/, 'production devices must not use tab layout.')

const productionTemplate = frontlinePanel.match(/v-else[\s\S]*data-frontline-production-operator[\s\S]*?<footer class="frontline-submit-bar">/)
assert.ok(productionTemplate, 'production operator block must exist.')
for (const forbidden of ['生产工单', '工单', '生产订单']) {
  assert.doesNotMatch(productionTemplate[0], new RegExp(forbidden), `production UI must not expose ${forbidden}.`)
}

const pqcTemplate = frontlinePanel.match(/data-frontline-pqc-operator[\s\S]*?<footer class="frontline-submit-bar">/)
assert.ok(pqcTemplate, 'PQC operator block must exist.')
for (const required of ['生产订单', '工序', '员工', '主页', '长度', '厘米', '外观', '密封', '压力', 'MPa', '检验数量', '损耗数量']) {
  assert.match(pqcTemplate[0], new RegExp(required), `PQC UI must include ${required}.`)
}
for (const required of ['首检', '巡检', '末检']) {
  assert.match(frontlinePanel, new RegExp(required), `PQC inspection options must include ${required}.`)
}
for (const forbidden of ['检验方法', '成功', '失败', '巡检摘要']) {
  assert.doesNotMatch(pqcTemplate[0], new RegExp(forbidden), `PQC UI must not show ${forbidden}.`)
}

console.log('PASS: eDHR frontline fill tabs static contract')
