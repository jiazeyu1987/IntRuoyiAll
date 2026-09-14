const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const page = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = read('src/api/mes/pro/processpool/teamLeader.ts')

const processConfigMarker = page.indexOf('v-if="showProductionProcessConfigModule"')
const blockStart = page.lastIndexOf('<ContentWrap', processConfigMarker)
const nextModuleMarker = page.indexOf('v-if="showProductionConfigModule"', processConfigMarker)
const blockEnd = page.lastIndexOf('<ContentWrap', nextModuleMarker)
assert.notEqual(blockStart, -1, '必须找到生产组长工序配置模块。')
assert.notEqual(blockEnd, -1, '工序配置模块必须有稳定结束边界。')
const processConfigBlock = page.slice(blockStart, blockEnd)

assert.match(
  page,
  /import TableMultiFilter from '@\/components\/TableMultiFilter\/index\.vue'/,
  '工序配置必须复用标准条件 Tab 查询控件。'
)
assert.match(
  processConfigBlock,
  /<TableMultiFilter[\s\S]*table-key="mes\.processPool\.teamLeader\.processConfig"[\s\S]*:filter-definitions="processConfigFilterDefinitions"[\s\S]*:state="processConfigFilterState"/,
  '红框查询区必须以稳定 table key 接入五字段条件 Tab。'
)
for (const eventBinding of [
  '@update:state="updateProcessConfigFilterState"',
  '@query="applyProcessConfigFilter"',
  '@reset="resetProcessConfigFilter"',
  '@remove="removeProcessConfigFilterCondition"'
]) {
  assert.ok(processConfigBlock.includes(eventBinding), `工序配置查询控件缺少 ${eventBinding}。`)
}
assert.doesNotMatch(
  processConfigBlock,
  /team-leader-workbench__section-title">工序配置|以路线工序串联损耗原因/,
  '红框内旧标题和说明必须被标准过滤查询控件替换。'
)
assert.match(
  processConfigBlock,
  /data-team-leader-process-config-create-entry[\s\S]*@click="openCreateProcessConfigDataDialog"/,
  '右侧新增按钮及正式处理器必须保留。'
)
assert.match(
  processConfigBlock,
  /<el-table[\s\S]*:data="processConfigDisplayRows"[\s\S]*data-team-leader-process-config-table/,
  '表格必须使用独立后端查询结果，不得直接覆盖未过滤候选基线。'
)
assert.doesNotMatch(processConfigBlock, /<UnifiedListTemplate/, '工序配置不得扩展为完整标准列表模板。')

for (const [key, label, queryParamKey] of [
  ['route', '工艺路线', 'routeKeyword'],
  ['process', '工序', 'processKeyword'],
  ['lossReason', '损耗原因', 'lossReasonKeyword'],
  ['device', '映射设备', 'deviceKeyword'],
  ['parameter', '设备参数标准', 'parameterKeyword']
]) {
  const pattern = new RegExp(
    `key:\\s*'${key}'[\\s\\S]*?label:\\s*'${label}'[\\s\\S]*?queryParamKey:\\s*'${queryParamKey}'[\\s\\S]*?operators:\\s*\\['contains'\\]`
  )
  assert.match(page, pattern, `${label}必须以 contains 映射正式参数 ${queryParamKey}。`)
}

assert.match(page, /const processConfigRows = ref<TeamLeaderProcessConfigRowRespVO\[\]>\(\[\]\)/)
assert.match(page, /const processConfigDisplayRows = ref<TeamLeaderProcessConfigRowRespVO\[\]>\(\[\]\)/)
assert.match(
  page,
  /useTableMultiFilter\([\s\S]*PROCESS_CONFIG_TABLE_KEY[\s\S]*processConfigFilterDefinitions[\s\S]*processConfigQuery[\s\S]*queryProcessConfigRows[\s\S]*\)/,
  '查询状态必须由标准多条件 hook 管理。'
)
assert.doesNotMatch(page, /processConfigFilter[^\n]*\.setCondition\(/, '条件 Tab 默认必须为空，不得预置隐藏筛选。')
assert.match(
  page,
  /\.team-leader-workbench__process-config-filter[\s\S]*flex:\s*1 1 auto;[\s\S]*min-width:\s*0;/,
  '查询控件必须保留可收缩宽度，不能被新增按钮挤压为零。'
)

assert.match(api, /export interface TeamLeaderProcessConfigListReqVO\s*\{[\s\S]*routeKeyword\?: string[\s\S]*processKeyword\?: string[\s\S]*lossReasonKeyword\?: string[\s\S]*deviceKeyword\?: string[\s\S]*parameterKeyword\?: string/)
assert.match(
  api,
  /getTeamLeaderProcessConfigList\s*=\s*async\s*\(params:\s*TeamLeaderProcessConfigListReqVO\s*=\s*\{\}\)[\s\S]*url:\s*'\/mes\/pro\/process-pool\/team-leader\/process-config\/list'[\s\S]*params/,
  '工序配置 API 必须发送正式可选查询参数。'
)

console.log('team-leader-process-config-filter-query-static PASS')
