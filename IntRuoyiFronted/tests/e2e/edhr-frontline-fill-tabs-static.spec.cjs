const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const router = read('src/router/modules/remaining.ts')
const productionPagePath = 'src/views/mes/pro/edhr-batch/BatchProductionFillPage.vue'
const tabsPath = 'src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue'
const frontlinePanel = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

for (const pagePath of [productionPagePath, tabsPath]) {
  assert.ok(exists(pagePath), `${pagePath} must exist.`)
}

const productionPage = read(productionPagePath)
const tabs = read(tabsPath)

for (const tabName of ['批次执行', '批记录页面关系图']) {
  assert.match(tabs, new RegExp(tabName), `eDHR batch tabs must include ${tabName}.`)
}
assert.doesNotMatch(
  tabs,
  /<el-tab-pane\s+label="生产填写"\s+name="production"/,
  'production fill must be extracted from the eDHR batch internal tabs.'
)
assert.doesNotMatch(
  tabs,
  /production:\s*'\/mes\/pro\/feedback\/edhr-batch-production-fill'/,
  'internal tab navigation must not route to standalone frontline production.'
)

const route = {
  path: 'pro/feedback/edhr-batch-production-fill',
  name: 'MesProEdhrBatchProductionFill',
  component: 'BatchProductionFillPage.vue',
  title: '一线生产'
}
const routeIndex = router.indexOf(`path: '${route.path}'`)
assert.ok(routeIndex >= 0, `${route.path} route must exist.`)
const routeBlock = router.slice(routeIndex, router.indexOf('\n      {', routeIndex + route.path.length))
assert.match(routeBlock, new RegExp(`name: '${route.name}'`), `${route.path} route name must be stable.`)
assert.match(routeBlock, new RegExp(route.component), `${route.path} route component must be stable.`)
assert.match(routeBlock, new RegExp(`title: '${route.title}'`), `${route.path} route title must be visible.`)
assert.match(routeBlock, /permission:\s*\['mes:pro-edhr-batch-execution:query'\]/, `${route.path} must reuse eDHR batch permission.`)

assert.doesNotMatch(productionPage, /<EdhrBatchRecordTabs|active-tab="production"/, 'standalone frontline production page must not render shared internal tabs.')
assert.match(productionPage, /<FrontlineFixedTemplatePanel\s+mode="production"\s*\/>/, 'production fill page must directly lock production mode.')
assert.doesNotMatch(
  productionPage,
  /<ContentWrap>|data-edhr-frontline-production-page-title|按活跃订单、工序和设备填写一线生产记录/,
  'standalone production page must not render an extra admin title shell outside the approved 1920 prototype.'
)

assert.match(frontlinePanel, /defineProps<\{\s*mode\?:\s*'production'\s*\|\s*'pqc'/s, 'frontline panel must accept a fixed mode prop.')
assert.match(frontlinePanel, /templateModeMismatch/, 'frontline panel must expose template mismatch blocking state.')
const employeeSwitchStart = frontlinePanel.indexOf('const handleSelectEmployee')
const employeeSwitchEnd = frontlinePanel.indexOf('const handleValidate', employeeSwitchStart)
assert.ok(employeeSwitchStart >= 0 && employeeSwitchEnd > employeeSwitchStart, 'employee switch handler must exist.')
const employeeSwitchBlock = frontlinePanel.slice(employeeSwitchStart, employeeSwitchEnd)
assert.doesNotMatch(employeeSwitchBlock, /context\.templateCode\s*=\s*templateCode/, 'employee switch must not silently change the current page UI mode.')
assert.match(frontlinePanel, /visibleDeviceCards\s*=\s*computed\(\(\)\s*=>\s*configuredDeviceCards\.value\.slice\(0,\s*3\)\)/, 'production device cards must be limited to three devices.')
assert.match(frontlinePanel, /frontline-production-device-tabs/, 'production devices must use the compact three-device selector from the approved HTML.')
assert.doesNotMatch(frontlinePanel, /PREVIOUS_PROCESS_INPUT_QUANTITY|previousProcessInputQuantity|previousInputQuantity/, 'production payload must not include previous-process input quantity.')
assert.match(frontlinePanel, /const switchableProcessOptions = computed/, 'process picker must define unique process options.')
const processPickerBlock = frontlinePanel.match(/const pickerOptions = computed\([\s\S]*?\n}\)/)
assert.ok(processPickerBlock, 'process picker options block must exist.')
assert.match(processPickerBlock[0], /switchableProcessOptions\.value\.map/, 'process picker must use unique process options.')
assert.doesNotMatch(processPickerBlock[0], /deviceState\.processOptions\.map/, 'multiple devices must not duplicate process choices.')

const productionStart = frontlinePanel.indexOf('data-frontline-production-operator')
const productionEnd = frontlinePanel.indexOf('</footer>', productionStart)
assert.ok(productionStart >= 0 && productionEnd > productionStart, 'production operator block must exist.')
const productionBlockStart = frontlinePanel.lastIndexOf('<div', productionStart)
assert.ok(productionBlockStart >= 0, 'production operator wrapper must exist.')
const productionTemplate = frontlinePanel.slice(productionBlockStart, productionEnd)
for (const required of ['工序', '员工', '完成数量', '损耗数量', '不良明细', '填设备', '重填', '提交']) {
  assert.match(productionTemplate, new RegExp(required), `production UI must include ${required}.`)
}
assert.match(productionTemplate, /class="frontline-operator-screen screen"/, 'production UI must render the approved prototype screen class.')
assert.match(
  productionTemplate,
  /class="[^"]*\bfrontline-home-button\b[^"]*\bhome-btn\b[^"]*"[\s\S]*data-production-fullscreen-toggle[\s\S]*@click="handleProductionFullscreenToggle"[\s\S]*{{ productionFullscreenActionText }}/,
  'production UI must keep the reference Home button styling but use the explicit fullscreen toggle like PQC.'
)
assert.doesNotMatch(
  productionTemplate,
  /frontline-production-fullscreen-button|productionFullscreenButtonLabel/,
  'production UI must not keep the old removed fullscreen button class or label.'
)
assert.match(
  productionTemplate,
  /v-for="parameter in activeProductionDevice\.parameters"[\s\S]*parameter\.parameterName \|\| parameter\.parameterCode/,
  'production UI must render device parameters from team leader runtime configuration instead of hard-coded labels.'
)
assert.match(frontlinePanel, /configuredDefectReasons\s*=\s*computed[\s\S]*runtimeConfig\?\.defectReasons/, 'production defect reasons must come from team leader runtime configuration.')
assert.match(productionTemplate, /v-for="defect in configuredDefectReasons"/, 'production UI must render configured process defect reasons dynamically.')
for (const forbidden of ['生产工单', '工单', '生产订单', '上工序输入数量']) {
  assert.doesNotMatch(productionTemplate, new RegExp(forbidden), `production UI must not expose ${forbidden}.`)
}
assert.doesNotMatch(productionTemplate, />输出数量</, 'production UI must not expose the old output quantity wording.')
assert.doesNotMatch(frontlinePanel, /frontline-no-device/, 'production UI must not show a no-device placeholder panel.')

console.log('PASS: eDHR frontline fill tabs static contract')
