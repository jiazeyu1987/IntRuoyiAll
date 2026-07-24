const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'
)
const source = fs.readFileSync(pagePath, 'utf8')

const operationColumnMatch = source.match(
  /<el-table-column v-if="isEdhrBatchExecutionColumnVisible\('operation'\)[\s\S]*?<\/el-table-column>/
)

assert(operationColumnMatch, '批次执行列表必须保留操作列。')

const operationColumn = operationColumnMatch[0]

assert.match(operationColumn, />填写<\/el-button>/, '操作列必须保留填写入口。')
assert.match(operationColumn, />模板<\/el-button>/, '操作列必须保留模板入口。')
assert.match(operationColumn, />作废<\/el-button>/, '操作列必须显示作废入口。')
assert.doesNotMatch(operationColumn, />\s*打印\s*<\/el-button>/, '操作列不得继续显示打印入口。')
assert.match(operationColumn, /@click="openVoidDialog\(row\)"/, '作废入口必须打开作废流程弹窗。')
assert.doesNotMatch(operationColumn, />追溯<\/el-button>/, '操作列不得继续显示追溯入口。')
assert.doesNotMatch(
  operationColumn,
  /@click="openTraceActionDialog\(row\)"/,
  '操作列不得继续把作废位置绑定到追溯弹窗。'
)
assert.doesNotMatch(
  operationColumn,
  /@click="handleDownloadArchive\(row\)"/,
  '操作列不得继续绑定行内打印/下载归档入口。'
)

assert.match(source, /const openVoidDialog = \(row: EdhrBatchExecutionRespVO\) =>/, '页面必须声明作废弹窗处理函数。')
assert.match(source, /voidDialogVisible\.value = true/, '作废入口必须打开作废弹窗。')
assert.doesNotMatch(source, /const handleDownloadArchive = async/, '页面不应保留未使用的行内打印处理函数。')

console.log('edhr batch list void button static contract passed')
