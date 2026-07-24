const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')

const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const assertFile = (relativePath, label) => {
  const fullPath = path.join(root, relativePath)
  assert.ok(fs.existsSync(fullPath), `${label} 组件文件缺失：${relativePath}`)
  return fs.readFileSync(fullPath, 'utf8')
}

const scheduleOrderPagePath = 'src/views/mes/pro/scheduleorder/index.vue'
const workbenchPagePath = 'src/views/mes/pro/scheduler-workbench/index.vue'
const scheduleOrderPage = read(scheduleOrderPagePath)
const workbenchPage = read(workbenchPagePath)

const scheduleOrderComponents = {
  list: 'src/views/mes/pro/scheduleorder/components/ScheduleOrderMainList.vue',
  processDetail: 'src/views/mes/pro/scheduleorder/components/ScheduleOrderProcessDetail.vue',
  replanDrawer: 'src/views/mes/pro/scheduleorder/components/ScheduleOrderReplanDrawer.vue'
}
const workbenchComponents = {
  processWipTable: 'src/views/mes/pro/scheduler-workbench/components/ProcessWipTable.vue'
}

const splitSources = [scheduleOrderPage, workbenchPage]
for (const [label, relativePath] of Object.entries(scheduleOrderComponents)) {
  const source = assertFile(relativePath, `排产工单 ${label}`)
  splitSources.push(source)
  const componentName = path.basename(relativePath, '.vue')
  const directImportPattern = new RegExp(
    `import\\s+${componentName}\\s+from\\s+['\"]\\./components/${componentName}\\.vue['\"]`
  )
  const typedAliasImportPattern = new RegExp(
    `import\\s+Base${componentName}\\s+from\\s+['\"]\\./components/${componentName}\\.vue['\"][\\s\\S]*const\\s+${componentName}\\s*=\\s*Base${componentName}\\s+as`
  )
  assert.doesNotMatch(source, /<slot\b[^>]*\/>/, `${componentName} 不得使用自闭合 slot，避免 Vite 运行态 lint 阻断`)
  assert.ok(
    directImportPattern.test(scheduleOrderPage) || typedAliasImportPattern.test(scheduleOrderPage),
    `排产工单页面必须显式导入 ${componentName}`
  )
  assert.match(scheduleOrderPage, new RegExp(`<${componentName}(\\s|>)`), `排产工单页面模板必须使用 ${componentName}`)
}
for (const [label, relativePath] of Object.entries(workbenchComponents)) {
  const source = assertFile(relativePath, `排产工作台 ${label}`)
  splitSources.push(source)
  const componentName = path.basename(relativePath, '.vue')
  assert.doesNotMatch(source, /<slot\b[^>]*\/>/, `${componentName} 不得使用自闭合 slot，避免 Vite 运行态 lint 阻断`)
  assert.match(workbenchPage, new RegExp(`import\\s+${componentName}\\s+from\\s+['\"]\\./components/${componentName}\\.vue['\"]`), `排产工作台页面必须显式导入 ${componentName}`)
  assert.match(workbenchPage, new RegExp(`<${componentName}(\\s|>)`), `排产工作台页面模板必须使用 ${componentName}`)
}

const combined = splitSources.join('\n')

for (const apiName of [
  'getScheduleOrderPage',
  'getProcessList',
  'getOperationLog',
  'preflightScheduleOrders',
  'replanPreview',
  'replanApply',
  'getProcessWipStatistics',
  'saveProcessWipSettings'
]) {
  assert.ok(combined.includes(apiName), `拆分后仍必须保留目标 API 调用：${apiName}`)
}

for (const permission of [
  "'mes:pro-schedule-order:export'",
  "'mes:pro-auto-schedule:replan'",
  "'mes:pro-scheduler-workbench:update'"
]) {
  assert.ok(combined.includes(permission), `拆分后仍必须保留权限口径：${permission}`)
}
assert.ok(
  !combined.includes("'mes:pro-scheduler-workbench:smoke-test'"),
  '前端拆分后不得继续暴露排产工作台冒烟测试权限入口'
)

assert.match(combined, /row-key="id"/, '排产工单主表必须继续使用 id 作为行键')
assert.match(combined, /:row-key="getProcessWipRowKey"/, '工作台在制表必须继续使用路线工序行键')
assert.match(combined, /routeVersionId == null \|\| row\.routeProcessId == null/, '工作台在制行键必须 fail-fast 检查路线版本和路线工序')
assert.match(combined, /return `\$\{row\.routeVersionId\}:\$\{row\.routeProcessId\}`/, '工作台在制行键必须由 routeVersionId + routeProcessId 组成')
assert.doesNotMatch(combined, /return `\$\{row\.processId\}`/, '工作台在制行键不得退回 processId')

for (const message of [
  '重排预览失败，请查看接口返回信息',
  '应用重排失败，请查看接口返回信息',
  '工序在制数据缺少路线工序标识'
]) {
  assert.ok(combined.includes(message), `拆分后错误提示必须继续可见：${message}`)
}
assert.doesNotMatch(combined, /catch\s*\([^)]*\)\s*\{\s*\}/, '拆分后不得出现空 catch 吞掉真实错误')

console.log('PASS: MES scheduling stage5 frontend split static contract')
