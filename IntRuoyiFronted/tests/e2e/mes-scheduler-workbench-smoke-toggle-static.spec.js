const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(root, '..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readBackendText = (relativePath) => fs.readFileSync(path.join(workspaceRoot, 'ruoyi-vue-pro', relativePath), 'utf8')

const pageSource = readText('src/views/mes/pro/scheduler-workbench/index.vue')
const apiSource = readText('src/api/mes/pro/schedulerWorkbench/index.ts')
const backendControllerSource = readBackendText(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/schedulerworkbench/MesProSchedulerWorkbenchController.java'
)

for (const fragment of [
  'smokeTestStatus',
  'smokeTestOptions',
  'smokeTestRunning',
  'feedbackApprovalEnabled',
  'loadSmokeTestStatus',
  'toggleSmokeTest',
  'SchedulerWorkbenchApi.getSmokeTestStatus',
  'SchedulerWorkbenchApi.startSmokeTest',
  'SchedulerWorkbenchApi.stopSmokeTest',
  '开始测试',
  '结束测试',
  '报审',
  'scheduler-workbench__smoke-status',
  'scheduler-workbench__smoke-button',
  'scheduler-workbench__smoke-block',
  '班时策略测试',
  '测试未启',
  '测试已启',
  '测试已停',
  '冒烟'
]) {
  assert.ok(!pageSource.includes(fragment), `排产员工作台前端不得暴露冒烟测试内容: ${fragment}`)
}

assert.ok(pageSource.includes('schedulerSettingsDialogVisible'), '排产设置弹框必须继续存在。')
assert.ok(pageSource.includes('saveShiftHoursSetting'), '隐藏冒烟测试不得删除班时设置能力。')
assert.ok(pageSource.includes('savePolicySettings'), '隐藏冒烟测试不得删除策略设置能力。')
assert.ok(pageSource.includes('loadScheduleRules'), '隐藏冒烟测试不得删除排程规则加载能力。')
assert.doesNotMatch(pageSource, /scheduler-workbench__test-panel/, '冒烟测试不得单独显示为卡片。')
assert.doesNotMatch(pageSource, /canOperateSmokeTest/, '前端隐藏后不应再读取 smoke-test 权限控制可见入口。')
assert.doesNotMatch(pageSource, /\/smoke-test\//, '页面组件不得直接包含 smoke-test 接口路径。')
assert.doesNotMatch(pageSource, /catch\s*\(\s*\)\s*\{\s*\}/, '不得用空 catch 吞掉启停错误。')

for (const fragment of [
  'SchedulerWorkbenchSmokeTestStatusVO',
  'SchedulerWorkbenchSmokeTestStartReqVO',
  "status: 'IDLE' | 'RUNNING' | 'STOPPED' | 'FAILED'",
  'feedbackApprovalEnabled: boolean',
  'getSmokeTestStatus',
  "url: '/mes/pro/scheduler-workbench/smoke-test/status'",
  'startSmokeTest',
  "url: '/mes/pro/scheduler-workbench/smoke-test/start'",
  'data',
  'stopSmokeTest',
  "url: '/mes/pro/scheduler-workbench/smoke-test/stop'"
]) {
  assert.ok(apiSource.includes(fragment), `排产员工作台 API 缺少冒烟测试契约: ${fragment}`)
}

for (const fragment of [
  '@GetMapping("/smoke-test/status")',
  '@PostMapping("/smoke-test/start")',
  '@RequestBody(required = false)',
  'MesProSchedulerWorkbenchSmokeTestStartReqVO',
  '@PostMapping("/smoke-test/stop")',
  "mes:pro-scheduler-workbench:query"
]) {
  assert.ok(backendControllerSource.includes(fragment), `后端工作台控制器缺少冒烟测试端点: ${fragment}`)
}

console.log('mes-scheduler-workbench-smoke-toggle-static hidden-boundary: PASS')
