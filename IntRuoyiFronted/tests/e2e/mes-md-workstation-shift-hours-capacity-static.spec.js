const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const workstationPage = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/md/workstation/index.vue'),
  'utf8'
)
const workstationForm = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/md/workstation/WorkstationForm.vue'),
  'utf8'
)

assert(
  workstationPage.includes('label="班次小时"') && workstationPage.includes("key: 'shiftHours'"),
  '工作站列表必须展示班次小时列。'
)
assert(
  workstationPage.includes('label="班次产能"') && workstationPage.includes("key: 'todayCapacity'"),
  '工作站列表必须将 todayCapacity 展示为班次产能列。'
)
assert(
  workstationPage.includes('const formatWorkstationIntegerCapacity =') &&
    workstationPage.includes('Number.isFinite(parsed)') &&
    workstationPage.includes('Math.round(parsed).toString()'),
  '工作站列表必须提供整数产能展示函数，避免直接显示小数尾差。'
)
assert(
  workstationPage.includes('{{ formatWorkstationIntegerCapacity(scope.row.machineryStandardHourlyCapacity) }}') &&
    workstationPage.includes('{{ formatWorkstationIntegerCapacity(scope.row.todayCapacity) }}'),
  '设备标准小时产能和班次产能必须通过整数展示函数渲染。'
)
assert(
  !workstationPage.includes('{{ scope.row.machineryStandardHourlyCapacity ?? 0 }}') &&
    !workstationPage.includes('{{ scope.row.todayCapacity ?? 0 }}'),
  '工作站列表不得继续直接输出设备标准小时产能或班次产能原始小数。'
)
assert(
  workstationForm.includes('label="班次小时"') && workstationForm.includes('v-model="formData.shiftHours"'),
  '工作站表单必须允许维护班次小时。'
)
assert(
  !workstationForm.includes('shiftHours: undefined }') &&
    !workstationForm.includes('shiftHours: undefined } as unknown as MdWorkstationVO'),
  '工作站提交时不得清空 shiftHours。'
)
assert(
  !workstationPage.includes('当日有效工时') &&
    !workstationPage.includes('effectiveHours') &&
    !workstationPage.includes('handleEffectiveHoursChange'),
  '工作站列表班次产能不得继续由页面默认 8 小时或手填有效工时驱动，必须使用工作站班次小时。'
)

console.log('mes-md-workstation-shift-hours-capacity-static.spec.js passed')
