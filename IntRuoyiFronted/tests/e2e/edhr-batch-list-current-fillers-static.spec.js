const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'
)
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/edhr/batchExecution.ts')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert(
  pageSource.includes('label="当前工序"') &&
    pageSource.includes('label="当前填写人"'),
  '批次执行列表必须在当前工序旁新增当前填写人列。'
)

const currentFillersColumn = pageSource.match(
  /<el-table-column[\s\S]*?label="当前填写人"[\s\S]*?<\/el-table-column>/
)?.[0] || ''

assert(currentFillersColumn, '批次执行列表必须保留当前填写人列。')

assert(
  !/[生设质][产备量]：/.test(currentFillersColumn),
  '当前填写人列只展示人名，不得继续展示生产、设备、质量分类前缀。'
)

assert(
  currentFillersColumn.includes('{{ resolveCurrentProcessFillerNames(row) }}'),
  '当前填写人列必须通过统一函数把生产、设备、质量填写人合并为姓名展示。'
)

assert(
  pageSource.includes('const resolveCurrentProcessFillerNames') &&
    pageSource.includes("return names.length ? names.join('、') : '--'"),
  '当前填写人列必须集中格式化人员名称，缺失配置时显示 --。'
)

assert(
  pageSource.includes('new Set<string>()'),
  '当前填写人列合并多类填写人时必须去重，避免同一人因多个来源重复展示。'
)

assert(
  !pageSource.includes('currentProcessCode ||') &&
    !pageSource.includes('candidateSourceIds') &&
    !pageSource.includes('userId ||'),
  '当前填写人列不得使用工序编码、候选源 ID 或用户 ID 兜底冒充人员名称。'
)

assert(
  apiSource.includes('EdhrBatchExecutionCurrentProcessFillerRespVO') &&
    apiSource.includes('currentProcessProductionFillers?: EdhrBatchExecutionCurrentProcessFillerRespVO[]') &&
    apiSource.includes('currentProcessEquipmentFillers?: EdhrBatchExecutionCurrentProcessFillerRespVO[]') &&
    apiSource.includes('currentProcessQualityFillers?: EdhrBatchExecutionCurrentProcessFillerRespVO[]'),
  '前端 API 类型必须声明当前工序生产、设备、质量填写人字段。'
)
