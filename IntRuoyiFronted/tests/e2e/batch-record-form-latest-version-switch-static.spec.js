const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const projectRoot = path.resolve(repoRoot, '..')

const read = (relativePath) => fs.readFileSync(path.resolve(projectRoot, relativePath), 'utf8')

const pageSource = read('IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue')
const apiSource = read('IntRuoyiFronted/src/api/mes/pro/batchrecordreport/index.ts')
const reqVoSource = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecordreport/vo/BatchRecordReportPageReqVO.java'
)
const serviceSource = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportServiceImpl.java'
)

assert(
  pageSource.includes('class="batch-record-form-toolbar__latest-version-switch"'),
  '批记录表单工具栏必须用专属容器放置最新版本开关。'
)
assert(pageSource.includes('<el-switch'), '批记录表单工具栏必须使用 Element Plus switch。')
assert(
  pageSource.includes('v-model="queryParams.latestVersionOnly"'),
  '最新版本开关必须绑定到查询状态。'
)
assert(
  pageSource.includes('@change="handleLatestVersionOnlyChange"'),
  '切换最新版本开关后必须立即重新查询列表。'
)
assert(
  pageSource.includes('latestVersionOnly: false'),
  '查询参数必须显式初始化 latestVersionOnly，避免未定义状态。'
)
assert(
  pageSource.includes('latestVersionOnly: queryParams.latestVersionOnly || undefined'),
  '开启最新版本开关时必须把 latestVersionOnly 传给分页接口，关闭时不发送该条件。'
)
assert(
  !pageSource.includes('@click="handleBatchDelete"'),
  '截图位置的批量删除按钮必须被最新版本开关替换，不得继续绑定批量删除按钮。'
)
assert(
  !pageSource.includes('批量删除'),
  '截图位置不得继续渲染“批量删除”按钮文字。'
)
assert(
  !pageSource.includes('handleBatchDelete'),
  '删除批量删除按钮后不得保留废弃 handleBatchDelete 处理函数。'
)

assert(
  apiSource.includes('latestVersionOnly?: boolean'),
  '前端批记录表单分页请求类型必须声明 latestVersionOnly。'
)
assert(
  reqVoSource.includes('private Boolean latestVersionOnly;'),
  '后端分页 Request VO 必须声明 latestVersionOnly。'
)
assert(
  serviceSource.includes('filterLatestBatchRecordVersions(baseReports)'),
  '后端分页服务必须在分页前按最新批记录版本过滤。'
)

console.log('PASS: batch-record form latest version switch static contract')
