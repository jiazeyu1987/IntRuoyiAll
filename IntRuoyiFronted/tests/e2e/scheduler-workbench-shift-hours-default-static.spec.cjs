const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const workbenchPage = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)

assert(
  workbenchPage.includes('const DEFAULT_SHIFT_HOURS = 10.5'),
  '排产员工作台必须集中声明默认班次小时 10.5。'
)
assert(
  /const shiftHoursForm = reactive\(\{\s*shiftHours: DEFAULT_SHIFT_HOURS\s*\}\)/s.test(workbenchPage),
  '班次小时表单初始化必须默认显示 10.5。'
)
assert(
  /shiftHours:\s*DEFAULT_SHIFT_HOURS/.test(workbenchPage),
  '班次小时设置快照缺省值必须是 10.5。'
)
assert(
  workbenchPage.includes('shiftHoursSetting.value.shiftHours ?? DEFAULT_SHIFT_HOURS'),
  '读取排产管理员设置缺少 shiftHours 时必须回填默认 10.5。'
)
assert(
  workbenchPage.includes('shiftHoursSetting.value.shiftHours ?? DEFAULT_SHIFT_HOURS'),
  '保存返回缺少 shiftHours 时表单必须继续保持默认 10.5。'
)
assert(
  workbenchPage.includes("callback(new Error('班次小时必须大于 0'))"),
  '班次小时校验必须禁止空值和非正数。'
)
assert(
  !workbenchPage.includes('shiftHours: undefined as number | undefined'),
  '班次小时表单不得再以空值初始化。'
)

console.log('scheduler-workbench-shift-hours-default-static.spec.cjs passed')
