const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')

const page = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = read('src/api/mes/pro/processpool/teamLeader.ts')

const managementTabCount = (page.match(/data-pqc-leader-module-tab-management(?=[\s/>])/g) || []).length
const taskTabCount = (page.match(/data-pqc-leader-module-tab-task(?=[\s/>])/g) || []).length
assert.ok(managementTabCount > 0, 'PQC管理页签必须继续存在。')
assert.equal(taskTabCount, managementTabCount, '每一组 PQC 模块页签都必须同步增加 PQC任务。')

assert.match(
  page,
  /<el-tab-pane\s+label="PQC任务"\s+name="task"\s+data-pqc-leader-module-tab-task\s*\/>/,
  'PQC任务必须是独立的页面内部功能页签。'
)
assert.match(
  page,
  /const activePqcModuleTab = ref<'personnel' \| 'management' \| 'task' \| 'detail' \| 'history'>\('management'\)/,
  'PQC任务必须进入页签状态类型，同时保持 PQC管理为默认页签。'
)
assert.match(
  page,
  /const showPqcTaskModule = computed\([\s\S]*activeLeaderTab\.value === 'PQC'[\s\S]*activePqcModuleTab\.value === 'task'[\s\S]*\)/,
  'PQC任务必须有独立显示 gate。'
)
assert.match(
  page,
  /<ContentWrap\s+v-if="showPqcTaskModule"[\s\S]*data-pqc-leader-active-task-list[\s\S]*:data="pqcActiveTaskList"/,
  'PQC任务页签必须渲染正式任务列表。'
)
for (const label of ['任务状态', '当前订单', 'QA版本', '工艺路线版本', 'QA工序', '检验类型', '业务日期', '计划/完成数量']) {
  assert.match(page, new RegExp(`label="${label}"`), `PQC任务列表必须显示“${label}”列。`)
}
assert.match(
  page,
  /const loadPqcActiveTasks = async \(\) => \{[\s\S]*getPqcLeaderActiveTaskList\(\)[\s\S]*pqcActiveTaskList\.value = \[\][\s\S]*PQC任务加载失败/,
  'PQC任务加载失败必须清空列表并显示明确错误，不能降级到其它数据源。'
)
assert.match(
  page,
  /watch\(activePqcModuleTab, async \(tab\) => \{[\s\S]*tab === 'task'[\s\S]*await loadPqcActiveTasks\(\)/,
  '切换到 PQC任务时必须加载正式任务列表。'
)

assert.match(api, /export interface PqcLeaderActiveTaskRespVO \{[\s\S]*qaVersionNo: string[\s\S]*routeVersionNo: string/)
assert.match(
  api,
  /export const getPqcLeaderActiveTaskList = async \(\) => \{[\s\S]*url: '\/mes\/pro\/process-pool\/team-leader\/pqc-task\/active-list'/,
  '前端必须调用 PQC组长专用活跃任务接口。'
)
assert.doesNotMatch(
  page,
  /showPqcTaskModule[\s\S]{0,1200}getFrontlinePqcActiveOrders/,
  'PQC任务列表不得使用一线待检订单接口拼接任务。'
)

console.log('PASS: PQC组长活跃任务列表静态合同')
