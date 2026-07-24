const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/task/autoSchedule/index.ts')
const taskPagePath = path.join(repoRoot, 'src/views/mes/pro/task/index.vue')

function readUtf8(filePath) {
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const apiSource = readUtf8(apiPath)
const taskPageSource = readUtf8(taskPagePath)

const getDependenciesBlock = apiSource.match(/getDependencies:\s*async[\s\S]*?\n\s*}/)
assert.ok(getDependenciesBlock, 'auto schedule API must expose getDependencies')

assert.ok(
  getDependenciesBlock[0].includes('request.post<any[]>'),
  'dependency loading must use POST so large work-order scopes are sent in the request body'
)
assert.ok(
  getDependenciesBlock[0].includes("url: '/mes/pro/auto-schedule/dependencies'"),
  'dependency loading must keep the dependencies endpoint'
)
assert.ok(
  getDependenciesBlock[0].includes('data') && !getDependenciesBlock[0].includes('params'),
  'dependency loading must send workOrderIds/taskIds as JSON body data, not query params'
)
assert.ok(
  taskPageSource.includes('ProTaskAutoScheduleApi.getDependencies({ workOrderIds: scopeWorkOrderIds.value })'),
  'production scheduling page must load current gantt dependencies through the shared API contract'
)

console.log('PASS: MES auto schedule dependency POST static contract')
