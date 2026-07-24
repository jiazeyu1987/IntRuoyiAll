const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteProcessList.vue'),
  'utf8'
)

const mainTableSource = source.slice(
  source.indexOf('<el-table'),
  source.indexOf('<Dialog :title="formTitle"')
)

if (mainTableSource.includes('label="今日可用"')) {
  throw new Error('组成工序主表格不应再显示今日可用列。')
}

if (mainTableSource.includes('label="今日班次产能"')) {
  throw new Error('组成工序主表格不应再显示今日班次产能列。')
}

const standardResourceIndex = mainTableSource.indexOf('label="标准资源"')
if (standardResourceIndex === -1) {
  throw new Error('组成工序主表格必须保留标准资源列。')
}

const standardResourceSource = mainTableSource.slice(
  standardResourceIndex,
  mainTableSource.indexOf('</el-table-column>', standardResourceIndex)
)
if (!standardResourceSource.includes('openProcessCapacityDetail(scope.row)')) {
  throw new Error('标准资源列必须保留资源详情点击入口。')
}

if (!source.includes('todayShiftCapacityTotal')) {
  throw new Error('班次产能字段仍应保留给资源详情弹框使用。')
}
