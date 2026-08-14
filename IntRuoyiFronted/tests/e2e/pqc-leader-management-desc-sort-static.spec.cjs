const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const readUtf8 = (absolutePath) => fs.readFileSync(absolutePath, 'utf8').replace(/\r\n/g, '\n')

const page = readUtf8(path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
))
const timelineMapper = readUtf8(path.join(
  workspaceRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml'
))

const buildSubmissionParamsBlock = page.match(
  /const buildSubmissionParams = \(\): TeamLeaderSubmissionPageReqVO => \{[\s\S]*?\n\}/
)?.[0] || ''

assert.match(
  page,
  /const showPqcManagementModule = computed\([\s\S]*activeLeaderTab\.value === 'PQC'[\s\S]*activePqcModuleTab\.value === 'management'[\s\S]*\)/,
  'PQC管理必须继续使用正式管理列表区域加载提交分页数据。'
)
assert.doesNotMatch(
  buildSubmissionParamsBlock,
  /sortField|sortOrder/,
  'PQC管理默认倒序必须来自后端正式分页排序，不能通过前端当前页排序参数临时绕过。'
)
assert.match(
  page,
  /submissionList\.value\s*=\s*data\.list \|\| \[\]/,
  '前端必须直接使用正式分页返回的顺序。'
)
assert.doesNotMatch(
  page,
  /submissionList\.value\s*=\s*\(?data\.list \|\| \[\]\)?\.sort|\.sort\(\s*\([^)]*submittedAt/,
  '前端不得只对当前页数组按提交时间排序，否则跨页最近提交优先会失真。'
)
assert.match(
  timelineMapper,
  /ORDER BY\s+pool_event\.server_submit_time\s+DESC,\s*pool_event\.id\s+DESC\s+LIMIT\s+#\{offset\},\s*#\{reqVO\.pageSize\}/,
  '后端 Mapper 必须按服务端提交时间倒序分页；同一提交时间按事件 ID 倒序稳定排列。'
)
assert.doesNotMatch(
  timelineMapper,
  /ORDER BY\s+pool_event\.server_submit_time\s+ASC,\s*pool_event\.id\s+ASC/,
  '后端 Mapper 不得继续按提交时间升序返回 PQC管理分页。'
)

console.log('PASS: PQC管理列表按服务端提交时间倒序加载')
