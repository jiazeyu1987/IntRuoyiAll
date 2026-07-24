const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'batchrecordformlist',
  'index.vue'
)
const page = fs.readFileSync(pagePath, 'utf8')

const extractFunction = (name) => {
  const start = page.indexOf(`const ${name} = async`)
  assert.notEqual(start, -1, `必须存在异步函数：${name}`)
  const nextConst = page.indexOf('\nconst ', start + 1)
  assert.notEqual(nextConst, -1, `函数 ${name} 后必须存在后续声明，便于静态截取。`)
  return page.slice(start, nextConst)
}

const getList = extractFunction('getList')

assert.match(
  getList,
  /await\s+BatchRecordReportApi\.getGeneratedReportPage\(/,
  '批记录表单首屏必须等待真实分页接口，先展示列表框架和当前页数据。'
)

for (const forbiddenBlockingCall of [
  'loadRecordFormPermissionRules',
  'loadSelectedReportTemplate',
  'selectReport',
  'handleTemplateActionQuery',
  'getCellRules',
  'getSignatureCellMarkers'
]) {
  assert.ok(
    !getList.includes(forbiddenBlockingCall),
    `首屏 getList 不得等待非首屏链路：${forbiddenBlockingCall}`
  )
}

assert.match(
  page,
  /const deferRecordFormSecondaryLoad = \([\s\S]*requestAnimationFrame[\s\S]*void loadRecordFormSecondaryData/,
  '填写人权限、默认预览和路由动作必须在列表首屏渲染后异步调度。'
)

const secondaryLoader = extractFunction('loadRecordFormSecondaryData')
for (const requiredDeferredCall of [
  'loadRecordFormPermissionRules',
  'loadSelectedReportTemplate',
  'handleTemplateActionQuery'
]) {
  assert.ok(
    secondaryLoader.includes(requiredDeferredCall),
    `辅助加载函数必须负责延后加载：${requiredDeferredCall}`
  )
}

assert.match(
  page,
  /const isStaleRecordFormListRequest = \(requestSerial: number\)[\s\S]*requestSerial !== recordFormListRequestSerial/,
  '批记录表单列表必须用请求序号阻止旧列表请求回写。'
)
assert.match(
  page,
  /templatePreviewRequestSerial[\s\S]*selectedReportId\.value !== row\.reportId/,
  '批记录表单预览必须阻止旧表单预览请求覆盖当前选中表单。'
)

console.log('PASS: batch record form first screen defers permissions and template preview.')
