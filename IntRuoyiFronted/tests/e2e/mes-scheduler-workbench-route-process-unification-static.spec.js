const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const apiSource = fs.readFileSync(path.join(root, 'src/api/mes/pro/scheduleorder/index.ts'), 'utf8')
const pageSource = fs.readFileSync(path.join(root, 'src/views/mes/pro/scheduler-workbench/index.vue'), 'utf8')

for (const field of [
  'routeId: number',
  'routeCode: string',
  'routeName: string',
  'routeVersionId: number',
  'routeProcessId: number'
]) {
  assert.match(apiSource, new RegExp(field.replace(':', '\\s*:\\s*')), `WIP API 缺少路线字段：${field}`)
}

assert.match(
  apiSource,
  /interface MesProScheduleOrderProcessWipSettingsReqVO[\s\S]*routeVersionId:\s*number[\s\S]*routeProcessId:\s*number/,
  '工作台保存请求必须使用路线版本和路线工序作为唯一键'
)
assert.doesNotMatch(apiSource, /nightShiftMixed/, '夜班状态必须来自唯一路线配置，不再暴露混合状态')

for (const label of ['工艺路线编码', '工艺路线名称']) {
  assert.match(pageSource, new RegExp(`label="${label}"`), `工作台缺少路线列：${label}`)
}

assert.match(pageSource, /:row-key="getProcessWipRowKey"/, '工作台表格必须使用路线工序稳定行键')
assert.match(pageSource, /routeVersionId:\s*row\.routeVersionId/, '保存请求必须提交 routeVersionId')
assert.match(pageSource, /routeProcessId:\s*row\.routeProcessId/, '保存请求必须提交 routeProcessId')
assert.doesNotMatch(pageSource, /nightShiftMixed/, '工作台不得保留夜班混合状态禁用逻辑')

console.log('PASS: scheduler workbench route-process unification static contract')
