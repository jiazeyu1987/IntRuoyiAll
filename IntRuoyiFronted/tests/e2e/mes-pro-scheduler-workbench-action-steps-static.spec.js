const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduler-workbench/index.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/schedulerWorkbench/index.ts')

assert(fs.existsSync(pagePath), `排产员工作台页面必须存在：${pagePath}`)
assert(fs.existsSync(apiPath), `排产员工作台 API 合同必须存在：${apiPath}`)

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

for (const fragment of [
  'SchedulerWorkbenchStepVO',
  'steps: SchedulerWorkbenchStepVO[]',
  'primaryPath',
  'primaryMetricName',
  'primaryMetricValue'
]) {
  assert(apiSource.includes(fragment), `工作台 summary API 必须保留行动步骤合同：${fragment}`)
}

for (const fragment of [
  '行动链路',
  'scheduler-workbench__steps-panel',
  'summary.steps',
  'openWorkbenchStep',
  '工单同步',
  '排产入池',
  '配置体检',
  '发布复盘'
]) {
  assert(!pageSource.includes(fragment), `智能排产首页不应继续渲染行动步骤入口：${fragment}`)
}

assert.doesNotMatch(
  pageSource,
  /v-for="step in summary\.steps[\s\S]*:key="step\.sort/,
  '智能排产首页不应继续按 summary.steps 渲染行动链路。'
)

assert.doesNotMatch(
  pageSource,
  /@click="openWorkbenchStep\(step\)"/,
  '隐藏行动链路后不应保留 openWorkbenchStep(step) 点击跳转。'
)

assert.doesNotMatch(pageSource, /router\.push\(\{[\s\S]*path: step\.primaryPath/, '页面不应保留行动步骤路由跳转。')
assert.match(pageSource, /schedulerSettingsDialogVisible/, '排产设置必须改为弹框展示。')
assert.match(pageSource, />\s*排产设置\s*</, '首页必须保留排产设置按钮作为设置入口。')

assert(!pageSource.includes('catch {}'), '工作台不得用空 catch 吞掉行动步骤相关错误。')

console.log('PASS: MES scheduler workbench hides action steps static contract')
