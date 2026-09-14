const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const page = fs.readFileSync(pagePath, 'utf8')

assert(
  /<el-table[\s\S]*:class="\{ 'team-leader-workbench__submission-table--single-line': activeLeaderTab === 'PQC' \}"/.test(
    page
  ),
  'PQC管理列表必须只在 PQC 组长上下文启用单行表格样式。'
)

assert(
  /\.team-leader-workbench__submission-table--single-line:deep\(\.el-table__body \.el-table__cell \.cell\)[\s\S]*white-space:\s*nowrap[\s\S]*overflow:\s*hidden[\s\S]*text-overflow:\s*ellipsis/.test(
    page
  ),
  'PQC管理列表单元格必须单行省略，不能撑高 row。'
)

assert(
  /\.team-leader-workbench__submission-table--single-line \.team-leader-workbench__structured-list[\s\S]*flex-wrap:\s*nowrap[\s\S]*overflow:\s*hidden/.test(
    page
  ),
  'PQC管理列表结构化标签必须保持同一行，超出内容裁剪。'
)

assert(
  /\.team-leader-workbench__submission-table--single-line \.team-leader-workbench__parameter-list[\s\S]*display:\s*flex[\s\S]*flex-wrap:\s*nowrap[\s\S]*overflow:\s*hidden/.test(
    page
  ),
  'PQC管理列表参数明细必须保持同一行，不能按项目换行。'
)

assert(
  !/team-leader-workbench__submission-table--single-line['"]?\s*:\s*true/.test(page),
  '单行样式不得作为表格全局常开配置。'
)

console.log('pqc-leader-management-single-line-static PASS')
