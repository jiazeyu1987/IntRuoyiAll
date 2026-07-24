const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/index.vue')
assert(fs.existsSync(pagePath), `生产报工页面必须存在：${pagePath}`)

const source = fs.readFileSync(pagePath, 'utf8')
const start = source.indexOf('const handleAttributionSuccess = async')
const end = source.indexOf('const handleDelete = async', start)
assert(start >= 0 && end > start, '必须存在归属成功后的列表刷新处理。')

const handler = source.slice(start, end)
assert(
  !handler.includes("importQueryParams.attributionStatus = 'PENDING'"),
  '归属保存后不能强制切回 PENDING，否则刚保存的已归属记录会从当前列表消失。'
)
assert(
  handler.includes('importQueryParams.feedbackId = undefined'),
  '归属一条记录后不得用正式报工编号过滤待归属列表。'
)
assert(
  !handler.includes("importQueryParams.attributionStatus = 'ATTRIBUTED'"),
  '归属一条记录后也不能切到仅 ATTRIBUTED 筛选，否则会破坏混合展示。'
)
assert(
  !handler.includes('importQueryParams.feedbackId = successPayload?.feedbackId'),
  '归属一条记录后不能只显示当前正式报工编号对应记录。'
)
assert(
  handler.includes('await getImportRecordList()'),
  '归属保存后必须刷新待归属列表，显示最新的行内状态。'
)
assert(
  !handler.includes('await getList()'),
  '归属保存后不应在待归属页直接刷新正式报工列表，否则会触发与当前页无关的额外权限请求或重复提示。'
)

console.log('PASS: MES feedback attribution continuation static contract')
