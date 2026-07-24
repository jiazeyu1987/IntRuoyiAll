const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/TrackingPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

assert.ok(
  source.includes('class="edhr-tracking__execution-link"') &&
    source.includes('@click="openTrackingDetail(row)"'),
  '追踪页必须由执行编号链接承担执行追踪入口。'
)

assert.equal(
  (source.match(/@click="openTrackingDetail\(row\)"/g) || []).length,
  1,
  '追踪页执行追踪入口只能保留在执行编号链接上。'
)

assert.doesNotMatch(
  source,
  /<el-table-column label="操作"[\s\S]*?>[\s\S]*?查看[\s\S]*?<\/el-table-column>/,
  '追踪页不得保留只包含“查看”的操作列。'
)

assert.ok(
  source.includes('canOpenDetail(row)') &&
    source.includes('edhr-tracking__execution-link--disabled') &&
    source.includes('缺少执行记录 ID，无法打开执行追踪。'),
  '缺少 executionId 时必须展示不可点击执行编号并说明原因。'
)

assert.ok(
  source.includes("path: '/mes/pro/feedback/edhr-execution/form'") &&
    source.includes("viewMode: 'tracking'"),
  '追踪页执行编号必须通过执行表单路由进入只读 tracking 视图。'
)

console.log('PASS: EDHR tracking execution entry static contract')
