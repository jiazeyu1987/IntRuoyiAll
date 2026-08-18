const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const workbenchSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)
const scheduleOrderApiSource = fs.readFileSync(
  path.join(repoRoot, 'src/api/mes/pro/scheduleorder/index.ts'),
  'utf8'
)
const packageJson = JSON.parse(fs.readFileSync(path.join(repoRoot, 'package.json'), 'utf8'))

assert.equal(
  packageJson.scripts?.['e2e:mes:scheduler-workbench-process-capacity-edit:static'],
  'node tests/e2e/scheduler-workbench-process-capacity-edit-static.spec.js',
  'package.json must expose the scheduler workbench process capacity edit static gate'
)

assert.match(
  scheduleOrderApiSource,
  /interface\s+MesProScheduleOrderProcessWipSettingsReqVO[\s\S]*shiftCapacityTotal\?:\s*number/,
  'process WIP settings payload must carry the edited shift capacity'
)

assert.match(
  workbenchSource,
  /const\s+processWipShiftCapacityDrafts\s*=\s*reactive<Record<string,\s*number\s*\|\s*undefined>>/,
  'scheduler workbench must keep per-row editable shift capacity drafts'
)

assert.match(
  workbenchSource,
  /v-model="processWipShiftCapacityDrafts\[getProcessWipRowKey\(row\)\]"[\s\S]*@change="handleProcessWipShiftCapacityChange\(row,\s*\$event\)"/,
  'process WIP shift capacity column must be directly editable and save on change'
)

assert.match(
  workbenchSource,
  /if\s*\(\s*overrides\.shiftCapacityTotal\s*!==\s*undefined\s*\)\s*\{[\s\S]*payload\.shiftCapacityTotal\s*=\s*overrides\.shiftCapacityTotal[\s\S]*\}/,
  'process WIP settings payload must submit shift capacity only when the user edits capacity'
)

const payloadBuilderStart = workbenchSource.indexOf('const buildProcessWipSettingsPayload = (')
const payloadBuilderEnd = workbenchSource.indexOf('const saveProcessWipSettings = async', payloadBuilderStart)
assert(payloadBuilderStart >= 0 && payloadBuilderEnd > payloadBuilderStart, 'process WIP settings payload builder must exist')
const payloadBuilderSource = workbenchSource.slice(payloadBuilderStart, payloadBuilderEnd)

assert.match(
  payloadBuilderSource,
  /hasProcessWipSettingOverride\(overrides,\s*'nightShiftEnabled'\)[\s\S]*payload\.nightShiftEnabled\s*=/,
  'process WIP settings payload must submit night shift only when the user edits night shift'
)

assert.match(
  payloadBuilderSource,
  /hasProcessWipSettingOverride\(overrides,\s*'plannedStartDate'\)[\s\S]*payload\.plannedStartDate\s*=/,
  'process WIP settings payload must submit planned start date only when the user edits planned start date'
)

assert.doesNotMatch(
  payloadBuilderSource,
  /nightShiftEnabled:\s*overrides\.nightShiftEnabled\s*===\s*undefined/,
  'capacity-only saves must not carry the current night shift value and mutate route schedule config'
)

assert.doesNotMatch(
  workbenchSource,
  /overrides\.shiftCapacityTotal\s*===\s*undefined\s*\?\s*row\.shiftCapacityTotal\s*:\s*overrides\.shiftCapacityTotal/,
  'night shift or planned date saves must not resend the current row capacity and accidentally convert resource capacity to manual override'
)

assert.match(
  workbenchSource,
  /const\s+handleProcessWipShiftCapacityChange\s*=\s*async\s*\([\s\S]*saveProcessWipSettings\(row,\s*\{\s*shiftCapacityTotal/,
  'scheduler workbench must save shift capacity through the formal process WIP settings API'
)

assert.match(
  workbenchSource,
  /const saveShiftHoursSetting = async \(\) => \{[\s\S]*await SchedulerWorkbenchApi\.saveShiftHoursSetting[\s\S]*await Promise\.all\(\[loadSummary\(\), loadProcessWipStatistics\(\)\]\)/,
  'saving shift hours must refresh process WIP so edited hourly capacity is re-scaled for the new shift hours'
)

console.log('scheduler-workbench-process-capacity-edit-static PASS')
