const assert = require('node:assert/strict')
const { existsSync, readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const repoRoot = resolve(__dirname, '../..')
const pagePath = resolve(repoRoot, 'src/views/mes/pro/task/index.vue')

assert.ok(existsSync(pagePath), 'missing MES production scheduling page')

const page = readFileSync(pagePath, 'utf8')
const template = page.match(/<template>([\s\S]*?)<script setup lang="ts">/)?.[1] || ''
const script = page.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)?.[1] || ''

assert.match(page, /defineOptions\(\{\s*name:\s*'MesProTask'\s*\}\)/, 'component name must stay MesProTask')
assert.match(template, /<ContentWrap[^>]*title="当前排产甘特图"/, 'page must keep the current schedule gantt card')
assert.match(template, /<GanttChart[\s\S]*:readonly="true"[\s\S]*:height="ganttHeight"/, 'gantt must fill through dynamic height')

for (const removedText of [
  'doc-alert',
  '工单编码',
  '工单名称',
  '需求日期',
  '搜索',
  '重置',
  '自动排产',
  '甘特图编辑',
  '待排产工单',
  '<el-table',
  '<Pagination',
  '<el-drawer',
  '<WorkOrderForm2'
]) {
  assert.doesNotMatch(template, new RegExp(removedText), `template must remove ${removedText}`)
}

for (const removedCode of [
  'ProWorkOrderApi',
  'WorkOrderForm2',
  'MdItemSelect',
  'MdClientSelect',
  'TreeExpandActions',
  'useTreeTableExpand',
  'openAutoScheduleDrawer',
  'openGanttEdit',
  'getWorkOrderList',
  'handleQuery',
  'resetQuery'
]) {
  assert.doesNotMatch(script, new RegExp(removedCode), `script must remove unused ${removedCode}`)
}

assert.match(script, /const ganttHeight = ref\(/, 'gantt height must be reactive')
assert.match(script, /window\.addEventListener\('resize', updateGanttHeight\)/, 'page must react to viewport resize')
assert.match(script, /ProTaskApi\.getGanttTaskList\(\{\}\)/, 'gantt data must load without hidden filter controls')
assert.match(script, /ProTaskAutoScheduleApi\.getDependencies\(\{\s*taskIds\s*\}\)/, 'dependency links must derive from visible gantt tasks')

console.log('mes-pro-task gantt-only static contract passed')
