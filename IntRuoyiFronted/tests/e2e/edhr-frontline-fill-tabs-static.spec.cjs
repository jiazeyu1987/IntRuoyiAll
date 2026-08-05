const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
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

for (const tabName of ['批次执行', 'PQC填写', '批记录页面关系图']) {
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

for (const route of [
  {
    path: 'pro/feedback/edhr-batch-production-fill',
    name: 'MesProEdhrBatchProductionFill',
    component: 'BatchProductionFillPage.vue',
    title: '一线生产'
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

assert.doesNotMatch(productionPage, /<EdhrBatchRecordTabs|active-tab="production"/, 'standalone frontline production page must not render shared internal tabs.')
assert.match(productionPage, /<FrontlineFixedTemplatePanel\s+mode="production"/, 'production fill page must lock production mode.')
assert.match(pqcPage, /<EdhrBatchRecordTabs\s+active-tab="pqc"/, 'PQC fill page must render shared tabs.')
assert.match(pqcPage, /<FrontlineFixedTemplatePanel\s+mode="pqc"/, 'PQC fill page must lock PQC mode.')

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
const productionTemplate = frontlinePanel.slice(productionStart, productionEnd)
for (const required of ['工序', '员工', 'productionFullscreenButtonLabel', '完成数量', '损耗数量', '不良明细', '填设备', '重填', '提交']) {
  assert.match(productionTemplate, new RegExp(required), `production UI must include ${required}.`)
}
assert.match(productionTemplate, /frontline-production-fullscreen-button/, 'production UI must expose the fullscreen toggle button.')
assert.doesNotMatch(productionTemplate, /@click="handleHome"[\s\S]*主页/, 'production UI must not expose the old Home route button.')
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

const pqcTemplate = frontlinePanel.match(/data-frontline-pqc-operator[\s\S]*?<footer class="frontline-pqc-submit-bar">/)
assert.ok(pqcTemplate, 'PQC operator block must exist.')
for (const required of ['生产订单', '工序', '员工', '检验内容', '首检', '巡检', '末检', '检验数量', '损耗数量', '签名编号', '全部合格', '全部不良', '逐件选择']) {
  assert.match(pqcTemplate[0], new RegExp(required), `PQC UI must include ${required}.`)
}
assert.match(
  pqcTemplate[0],
  /data-pqc-fullscreen-toggle[\s\S]*pqcFullscreenActionText/,
  'PQC UI top action must default to 最大化 and switch to 主页 through fullscreen state.'
)
assert.match(
  frontlinePanel,
  /const pqcFullscreenActionText = computed\(\(\) =>\s*isPqcFullscreen\.value \? '主页' : '最大化'/,
  'PQC fullscreen action text must be 最大化 before fullscreen and 主页 while fullscreen.'
)
assert.doesNotMatch(
  pqcTemplate[0],
  /@click="handleHome">主页<\/button>/,
  'PQC UI must not show the old hard-coded home button before fullscreen.'
)
assert.match(
  pqcTemplate[0],
  /v-for="item in pqcInspectionItems"[\s\S]*:data-pqc-inspection-entry="item\.key"/,
  'PQC UI must render inspection entries from the formal QA/PQC task snapshot.'
)
assert.match(
  pqcTemplate[0],
  /:data-pqc-inspection-group="item\.key"/,
  'PQC choice layout must preserve stable data attributes for every dynamic inspection item.'
)
assert.match(
  pqcTemplate[0],
  /{{ item\.label }}/,
  'PQC UI must display the formal inspection item label instead of fixed labels.'
)
assert.match(
  frontlinePanel,
  /const pqcInspectionItems = computed<PqcInspectionItem\[\]>\(\(\) =>\s*\(deviceState\.selectedProcess\?\.inspectionItems \|\| \[\]\)\.map/,
  'PQC inspection items must come from selectedProcess.inspectionItems.'
)
assert.match(
  frontlinePanel,
  /key: item\.itemCode[\s\S]*label: item\.itemName \|\| item\.itemCode[\s\S]*type: isPqcNumericResultType\(item\.resultType\) \? 'number' : 'choice'/,
  'PQC inspection item labels and types must use the formal QA snapshot fields.'
)
assert.match(
  frontlinePanel,
  /hasPqcTaskSnapshot[\s\S]*process\?\.inspectionItems\?\.length/,
  'PQC mode must fail fast when the formal QA/PQC inspection item snapshot is missing.'
)
for (const forbidden of ['成功', '失败', '巡检摘要']) {
  assert.doesNotMatch(pqcTemplate[0], new RegExp(forbidden), `PQC UI must not show ${forbidden}.`)
}

console.log('PASS: eDHR frontline fill tabs static contract')
