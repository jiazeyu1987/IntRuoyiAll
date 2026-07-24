const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteProcessList.vue'),
  'utf8'
)

const mainTableSource = source.slice(
  source.indexOf('<el-table v-loading="loading"'),
  source.indexOf('<Dialog :title="formTitle"')
)

const formDialogSource = source.slice(
  source.indexOf('<Dialog :title="formTitle"'),
  source.indexOf('<Dialog :title="machineryListDialogTitle"')
)

for (const label of ['label="等待时间"', 'label="甘特图颜色"']) {
  if (mainTableSource.includes(label)) {
    throw new Error(`组成工序主表格不应显示 ${label} 列。`)
  }
  if (!formDialogSource.includes(label)) {
    throw new Error(`编辑工序弹框应保留 ${label} 字段。`)
  }
}

for (const prop of ['prop="waitTime"', 'prop="colorCode"']) {
  if (!formDialogSource.includes(prop)) {
    throw new Error(`编辑工序弹框应保留 ${prop} 绑定。`)
  }
}
