const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const scheduleOrderPath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const scheduleOrderApiPath = path.join(root, 'src/api/mes/pro/scheduleorder/index.ts')

assert(fs.existsSync(scheduleOrderPath), `排产工单页面必须存在：${scheduleOrderPath}`)
assert(fs.existsSync(scheduleOrderApiPath), `排产工单 API 类型必须存在：${scheduleOrderApiPath}`)

const source = fs.readFileSync(scheduleOrderPath, 'utf8')
const apiSource = fs.readFileSync(scheduleOrderApiPath, 'utf8')

for (const token of [
  '查看',
  '工艺流程排产配置',
  '需要多少个',
  '做了多少个',
  'getProcessProgressStatus',
  'getProcessProgressStatusText',
  'getProcessProgressStatusTag',
  'getProcessProgressRowClass',
  'schedule-order-pool__process-row--finished',
  'schedule-order-pool__process-row--scheduled-not-started',
  'schedule-order-pool__process-row--unscheduled',
  'schedule-order-pool__process-row--in-progress'
]) {
  assert(source.includes(token), `排产工单查看工艺路线进度必须包含：${token}`)
}

assert(
  /<el-button[\s\S]*?@click="openProcessDialog\(row\)"[\s\S]*?>[\s\S]*?查看[\s\S]*?<\/el-button>/.test(source),
  '操作列必须提供直接打开工艺流程排产配置的查看按钮。'
)

assert(
  /effectiveCompletedQuantity[\s\S]*>=([\s\S]*?)plannedQuantity/.test(source) ||
    /plannedQuantity[\s\S]*<=([\s\S]*?)effectiveCompletedQuantity/.test(source),
  '已完成状态必须基于 effectiveCompletedQuantity 和 plannedQuantity 判断。'
)

assert(
  apiSource.includes('plannedQuantity: number') &&
    apiSource.includes('effectiveCompletedQuantity?: number') &&
    apiSource.includes('progressPercent?: number'),
  '排产工单工序 API 类型必须暴露需要数量、已完成数量和进度字段。'
)

console.log('PASS: MES schedule order route progress view static contract')
