const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/index.vue')

assert(fs.existsSync(pagePath), `生产报工页面必须存在：${pagePath}`)

const pageSource = fs.readFileSync(pagePath, 'utf8')

for (const forbiddenFragment of [
  '本次导入批次',
  '一键查看本次待归属',
  '导入批次已定位到待归属列表',
  '<el-table-column label="记录编号"',
  '<el-table-column label="归属状态"',
  '<el-table-column label="来源文件"',
  '<el-table-column label="工作表/行"',
  '<el-table-column label="派工单号"',
  '<el-table-column label="报工人" prop="feedbackUserName"',
  '<el-table-column label="导入行指定审批人"',
  '<el-table-column label="候选数"'
]) {
  assert(
    !pageSource.includes(forbiddenFragment),
    `待归属页不应渲染截图红框内导入诊断信息：${forbiddenFragment}`
  )
}

for (const requiredFragment of [
  'label="正式报工编号"',
  'label="工单" prop="workOrderCode"',
  'label="产品编码" prop="itemCode"',
  'label="产品名称" prop="itemName"',
  'label="工序"',
  'label="报工数量" prop="feedbackQuantity"',
  'label="报工时间"',
  'label="归属结果"',
  'label="报工人"',
  'label="当前审批人"',
  'label="备注"',
  '确认报工',
  '选择归属',
  '修改归属',
  'openAttribution(scope.row)',
  'feedback-import-batch-summary'
]) {
  assert(pageSource.includes(requiredFragment), `待归属页必须保留归属业务入口：${requiredFragment}`)
}

assert(!pageSource.includes('catch {}'), '生产报工页不得用空 catch 吞掉错误。')

console.log('PASS: MES feedback import diagnostics hidden static contract')
