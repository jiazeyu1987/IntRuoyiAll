const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/index.vue')

assert(fs.existsSync(pagePath), `生产报工页面必须存在：${pagePath}`)

const pageSource = fs.readFileSync(pagePath, 'utf8')

assert(
  pageSource.includes(':cell-class-name="resolveImportRecordCellClassName"'),
  '待归属表必须按列声明单元格换行类，避免继续整表省略。'
)

assert(
  !/<el-table[\s\S]*?:data="importRecordList"[\s\S]*?show-overflow-tooltip/.test(pageSource),
  '待归属表不应继续开启整表 show-overflow-tooltip，否则长文本仍会统一省略。'
)

for (const fragment of [
  'label="工单" prop="workOrderCode" min-width="150"',
  'label="产品编码" prop="itemCode" min-width="140"',
  'label="产品名称" prop="itemName" min-width="160"',
  'label="规格" prop="specification" min-width="120"',
  'label="工序" min-width="180"',
  'label="归属结果" min-width="220"',
  '.feedback-import-table__cell--wrap',
  '.feedback-import-process',
  '.feedback-import-process__line'
]) {
  assert(pageSource.includes(fragment), `待归属表多行展示契约缺失：${fragment}`)
}

console.log('PASS: MES feedback pending table wrap static contract')
