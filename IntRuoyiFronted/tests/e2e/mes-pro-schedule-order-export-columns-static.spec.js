const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const pageSource = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/scheduleorder/index.vue'),
  'utf8'
)
const apiSource = fs.readFileSync(
  path.join(root, 'src/api/mes/pro/scheduleorder/index.ts'),
  'utf8'
)

const defaultColumns = [
  'erpWorkOrderCode',
  'productCode',
  'productName',
  'productSpecification',
  'quantityProgress',
  'promiseDate',
  'latestStartTime',
  'plannedStartTime',
  'plannedEndTime',
  'priorityNo',
  'productionMaterialListSummary',
  'currentProcessName',
  'createTime'
]

assert(
  pageSource.includes("v-hasPermi=\"['mes:pro-schedule-order:export']\""),
  '导出按钮必须受 mes:pro-schedule-order:export 权限保护。'
)
assert(
  pageSource.includes('openScheduleOrderExportDialog') &&
    pageSource.includes('scheduleOrderExportVisible'),
  '排产工单页必须提供导出列选择弹窗入口。'
)
assert(
  pageSource.includes('scheduleOrderExportColumns') &&
    pageSource.includes('defaultScheduleOrderExportColumns'),
  '导出列必须有默认可见列配置。'
)
for (const column of defaultColumns) {
  assert(
    pageSource.includes(`key: '${column}'`) ||
      pageSource.includes(`'${column}'`),
    `默认导出列必须包含 ${column}。`
  )
}
assert(
  pageSource.includes("message.warning('请至少选择一个导出列')"),
  '取消全部列后必须在前端阻止导出并明确提示。'
)
assert(
  pageSource.includes('exportColumns: scheduleOrderExportColumns.value'),
  '确认导出必须把当前选中列传给后端。'
)
assert(
  apiSource.includes('/mes/pro/schedule-order/export-excel'),
  'API 必须暴露排产工单导出接口。'
)
assert(
  apiSource.includes('exportScheduleOrderExcel') &&
    apiSource.includes('request.download'),
  'API 导出方法必须走 blob 下载请求。'
)
assert(
  pageSource.includes("download.excel(data, '排产工单.xls')"),
  '导出成功后必须使用既有 Excel 下载工具。'
)

console.log('PASS mes-pro-schedule-order-export-columns-static')
