const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const scheduleOrderPath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')

assert(fs.existsSync(scheduleOrderPath), `排产工单页面必须存在：${scheduleOrderPath}`)

const source = fs.readFileSync(scheduleOrderPath, 'utf8')

assert(
  !source.includes('workOrderAdmissionReasonGroups'),
  '同步工单页签没有渲染阻断原因下拉时，不得维护未使用的原因分组选项状态。'
)
assert(
  !/v-for="group in workOrderAdmissionReasonGroups"/.test(source),
  '同步工单页签不得渲染已下线的阻断原因分组选项。'
)
assert(
  !/loadWorkOrderAdmissionReasonOptions/.test(source),
  '同步工单列表刷新不得扫描未渲染的阻断原因选项，否则会拖慢刷新时间。'
)
assert(
  !/pageSize:\s*200[\s\S]*reasonCode:\s*undefined/.test(source),
  '同步工单列表刷新不得为了原因选项发起 pageSize=200 的全量分页扫描。'
)

const admissionListLoaderStart = source.indexOf('const getWorkOrderAdmissionList = async () => {')
const admissionListLoaderEnd = source.indexOf('const handleWorkOrderAdmissionQuery', admissionListLoaderStart)
assert(
  admissionListLoaderStart >= 0 && admissionListLoaderEnd > admissionListLoaderStart,
  '同步工单列表加载函数必须存在。'
)
const admissionListLoaderSource = source.slice(admissionListLoaderStart, admissionListLoaderEnd)
assert(
  !admissionListLoaderSource.includes('Promise.all'),
  '同步工单主列表不得与其它辅助统计使用 Promise.all 并行等待，避免慢请求阻塞表格显示。'
)
const mainQueryIndex = admissionListLoaderSource.indexOf(
  'const data = await MesProScheduleOrderApi.getAdmissionDiff(workOrderAdmissionQueryParams)'
)
const listAssignIndex = admissionListLoaderSource.indexOf('workOrderAdmissionList.value = data.list')
assert(
  mainQueryIndex >= 0 && listAssignIndex > mainQueryIndex,
  '同步工单主列表接口返回后必须立即写入表格数据。'
)
assert(
  !admissionListLoaderSource.includes('await loadWorkOrderAdmissionReasonOptions()'),
  '同步工单主列表刷新不得等待阻断原因统计。'
)
assert.match(
  source,
  /let workOrderAdmissionRequestSerial = 0/,
  '同步工单列表刷新必须维护请求序号，防止旧慢请求覆盖新刷新结果。'
)
assert.match(
  admissionListLoaderSource,
  /const requestSerial = \+\+workOrderAdmissionRequestSerial[\s\S]*requestSerial !== workOrderAdmissionRequestSerial[\s\S]*return/,
  '同步工单列表刷新必须忽略过期请求响应。'
)
assert.match(
  admissionListLoaderSource,
  /message\.error\(`加载同步工单列表失败：/,
  '同步工单列表刷新失败必须给出可见错误提示。'
)

assert(
  !/v-for="group in admissionReasonGroups"/.test(source),
  '阻断原因下拉不能直接渲染静态全量原因分组。'
)

console.log('PASS: MES schedule order admission reason options static contract')
