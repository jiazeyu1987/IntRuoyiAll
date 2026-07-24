const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const pageSource = fs.readFileSync(pagePath, 'utf8')

const configTableStart = pageSource.indexOf('class="route/flow-config-table"')
assert(configTableStart >= 0, '排产用途配置表格必须存在。')

const keyColumnStart = pageSource.indexOf('label="关键工序"', configTableStart)
const qualityColumnStart = pageSource.indexOf('label="质检确认"', keyColumnStart)
const capacityModeColumnStart = pageSource.indexOf('label="产能模式"', keyColumnStart)
const keyColumnEndCandidates = [qualityColumnStart, capacityModeColumnStart].filter((index) => index > keyColumnStart)
const keyColumnEnd = Math.min(...keyColumnEndCandidates)

assert(keyColumnStart >= 0, '排产用途配置表格必须显示“关键工序”列。')
assert(keyColumnEnd > keyColumnStart, '关键工序列必须位于后续配置列之前。')

const keyColumnSource = pageSource.slice(keyColumnStart, keyColumnEnd)

assert(keyColumnSource.includes('<el-switch'), '关键工序列必须使用 el-switch 直接切换。')
assert(keyColumnSource.includes(':model-value="Boolean(scope.row.keyFlag)"'), '关键工序开关必须读取行级 keyFlag。')
assert(keyColumnSource.includes('@change="(enabled) => handleKeyProcessSwitch(scope.row, Boolean(enabled))"'), '关键工序开关必须调用专用切换方法。')
assert(keyColumnSource.includes(':loading="keyProcessSavingRouteProcessId === scope.row.routeProcessId"'), '关键工序开关必须有行级保存状态。')
assert(keyColumnSource.includes(':disabled="isKeyProcessSwitchDisabled(scope.row)"'), '关键工序开关必须按权限、路线状态和数据完整性禁用。')

assert(pageSource.includes('const keyProcessSavingRouteProcessId = ref<number>()'), '页面必须维护关键工序行级保存状态。')
assert(pageSource.includes('routeProcess?.keyFlag'), '排产配置加载必须把路线工序主数据 keyFlag 合并到配置表。')
assert(pageSource.includes('checkPermi([KEY_PROCESS_UPDATE_PERMISSION])'), '关键工序开关必须校验 mes:pro-route:update 权限。')
assert(pageSource.includes('CommonStatusEnum.ENABLE'), '关键工序开关必须识别已启用路线状态。')
assert(pageSource.includes('已启用工艺路线不允许修改关键工序，请先停用路线'), '已启用路线必须给出明确提示。')
assert(pageSource.includes('findCurrentKeyProcessRow'), '打开新关键工序时必须查找当前关键工序。')
assert.match(
  pageSource,
  /await updateRouteProcessKeyFlag\(currentKeyProcess,\s*false\)[\s\S]*await updateRouteProcessKeyFlag\(row,\s*true\)/,
  '打开新关键工序时必须先关闭旧关键工序，再打开目标工序。'
)
assert(pageSource.includes('await loadUseConfig(row.routeProcessId)'), '关键工序保存失败后必须重新加载配置保持前后端一致。')

console.log('PASS: MES route flow key process switch static contract')
