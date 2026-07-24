const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const workbenchPage = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)
const workbenchApi = fs.readFileSync(
  path.join(repoRoot, 'src/api/mes/pro/schedulerWorkbench/index.ts'),
  'utf8'
)
const workstationForm = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/md/workstation/WorkstationForm.vue'),
  'utf8'
)
const routeProcessList = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/route/RouteProcessList.vue'),
  'utf8'
)

assert(
  workbenchPage.includes('班时') && workbenchPage.includes('shiftHoursForm.shiftHours'),
  '排产员工作台必须提供班时表单。'
)
assert(
  workbenchApi.includes('/mes/pro/scheduler-workbench/shift-hours') &&
    workbenchApi.includes('saveShiftHoursSetting'),
  '排产员工作台 API 必须提供班次小时读写接口。'
)
assert(
  workstationForm.includes('label="班次小时"') &&
    workstationForm.includes('v-model="formData.shiftHours"'),
  '工作站表单必须允许维护班次小时。'
)
assert(
  workstationForm.includes('const data = { ...formData.value } as unknown as MdWorkstationVO') &&
    !workstationForm.includes('const data = { ...formData.value, shiftHours: undefined }'),
  '工作站表单提交不得清空 shiftHours。'
)
assert(
  !routeProcessList.includes('v-model="workerCapacityForm.shiftHours"'),
  '工艺路线人工产能弹窗不得继续编辑班次小时。'
)

console.log('mes-scheduler-workbench-shift-hours-static.spec.js passed')
