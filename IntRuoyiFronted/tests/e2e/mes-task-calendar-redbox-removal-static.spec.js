const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const calendarPage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/task/calendar/index.vue'),
  'utf8'
)

const template = calendarPage.slice(0, calendarPage.indexOf('</template>'))

for (const fragment of [
  '>模拟 +1 天<',
  '模拟 +{{ simulationAdvanceDays }} 天',
  '>重置模拟日<',
  '>打开重排<',
  '<label>当前正式排程</label>',
  '<label>状态</label>',
  '@click="reloadCurrentView"'
]) {
  assert.equal(
    template.includes(fragment),
    false,
    `排程日历必须删除截图红框内容：${fragment}`
  )
}

for (const fragment of [
  '<label>任务</label>',
  '<label>工单</label>',
  '<label>短缺</label>',
  '<label>任务总数</label>',
  '<label>最近更新时间</label>'
]) {
  assert.equal(
    template.includes(fragment),
    true,
    `排程日历必须保留红框外统计内容：${fragment}`
  )
}

for (const fragment of [
  'const simulationLoading = ref(false)',
  'const simulationAdvanceDays = ref(30)',
  'const currentScheduleStatusText = computed',
  'const currentScheduleStatusLabel = computed',
  'async function advanceSimulationDay',
  'async function advanceSimulationDays',
  'async function resetSimulation',
  'function openScheduleOrderReplan'
]) {
  assert.equal(
    calendarPage.includes(fragment),
    false,
    `排程日历必须清理红框内容的废弃引用：${fragment}`
  )
}

console.log('PASS: MES task calendar red-box content is removed')
