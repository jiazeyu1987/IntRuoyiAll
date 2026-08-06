const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const processPoolApi = readUtf8('src/api/mes/pro/processpool/index.ts')

const defaultColumnsStart = page.indexOf('const submissionDefaultColumns')
const defaultColumnsEnd = page.indexOf('const {', defaultColumnsStart)
assert.ok(defaultColumnsStart >= 0 && defaultColumnsEnd > defaultColumnsStart, 'team-leader submissionDefaultColumns must be declared')
const defaultColumns = page.slice(defaultColumnsStart, defaultColumnsEnd)

assert.doesNotMatch(defaultColumns, /key:\s*'workOrderCode'|label:\s*'生产工单'/, 'production report default columns must remove the red-box 生产工单 column')
assert.doesNotMatch(defaultColumns, /key:\s*'pqcResult'|label:\s*'PQC'/, 'production report default columns must remove the red-box PQC column')
assert.doesNotMatch(defaultColumns, /key:\s*'submissionContent'|label:\s*'提交内容'/, 'production report default columns must remove the red-box 提交内容 column')

assert.match(defaultColumns, /key:\s*'completionQuantity'[\s\S]*label:\s*'完成\/检验数量'/, 'report table must expose completion quantity')
assert.match(defaultColumns, /key:\s*'lossQuantity'[\s\S]*label:\s*'损耗数量'/, 'report table must expose loss quantity')
assert.match(defaultColumns, /key:\s*'lossBreakdown'[\s\S]*label:\s*'损耗明细'/, 'report table must expose loss reason quantity details')
assert.match(defaultColumns, /key:\s*'selectedDevice'[\s\S]*label:\s*'选用设备'/, 'report table must expose the selected device as a first-class column')
assert.match(defaultColumns, /key:\s*'deviceParameterReadings'[\s\S]*label:\s*'设备参数'/, 'report table must expose selected device parameter readings')

assert.match(page, /data-team-leader-loss-breakdown/, 'loss breakdown column needs a stable marker')
assert.match(page, /data-team-leader-selected-device/, 'selected device column needs a stable marker')
assert.match(page, /data-team-leader-device-parameter-readings/, 'device parameter readings column needs a stable marker')

assert.match(processPoolApi, /lossDetails\??:\s*ProcessPoolTimelineLossDetailVO\[\]/, 'timeline API VO must expose structured lossDetails')
assert.match(processPoolApi, /selectedDevice\??:\s*ProcessPoolTimelineSelectedDeviceVO/, 'timeline API VO must expose selectedDevice')
assert.match(processPoolApi, /deviceParameterReadings\??:\s*ProcessPoolTimelineDeviceParameterReadingVO\[\]/, 'timeline API VO must expose deviceParameterReadings')

console.log('PASS: production report table columns expose structured submission payload')
