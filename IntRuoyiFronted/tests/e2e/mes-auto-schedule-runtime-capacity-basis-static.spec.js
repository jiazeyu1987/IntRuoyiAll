const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const autoScheduleApi = read('src/api/mes/pro/task/autoSchedule/index.ts')
const taskPage = read('src/views/mes/pro/task/index.vue')
const scheduleOrderPage = read('src/views/mes/pro/scheduleorder/index.vue')
const routeApi = read('src/api/mes/pro/route/index.ts')
const flowConfigApi = read('src/api/mes/pro/route/flowconfig.ts')

const previewReqStart = autoScheduleApi.indexOf('export interface ProTaskAutoSchedulePreviewReqVO')
assert.notEqual(previewReqStart, -1, 'auto schedule preview request type must exist')
const previewReqEnd = autoScheduleApi.indexOf('\n}', previewReqStart)
assert.ok(previewReqEnd > previewReqStart, 'auto schedule preview request type must be parseable')
const previewReqSource = autoScheduleApi.slice(previewReqStart, previewReqEnd)

assert.match(
  previewReqSource,
  /runtimeCapacityBasis:\s*['"]PLANNED['"]\s*\|\s*['"]ACTUAL['"]/,
  'auto schedule request must name PLANNED/ACTUAL as runtimeCapacityBasis'
)
assert.doesNotMatch(
  previewReqSource,
  /capacityMode:\s*['"]PLANNED['"]\s*\|\s*['"]ACTUAL['"]/,
  'auto schedule request must not reuse route capacityMode for PLANNED/ACTUAL'
)

assert.match(
  taskPage,
  /v-model=["']autoScheduleForm\.runtimeCapacityBasis["']/,
  'task auto schedule drawer must bind runtimeCapacityBasis'
)
assert.match(
  taskPage,
  /runtimeCapacityBasis:\s*autoScheduleForm\.runtimeCapacityBasis/,
  'task auto schedule request must send runtimeCapacityBasis'
)
assert.doesNotMatch(
  taskPage,
  /autoScheduleForm\.capacityMode|capacityMode:\s*autoScheduleForm\.capacityMode/,
  'task auto schedule drawer must not keep the old runtime capacityMode binding'
)

assert.match(
  scheduleOrderPage,
  /v-model=["']replanForm\.runtimeCapacityBasis["']/,
  'schedule order replan settings must bind runtimeCapacityBasis'
)
assert.match(
  scheduleOrderPage,
  /runtimeCapacityBasis:\s*replanForm\.runtimeCapacityBasis/,
  'schedule order replan request must send runtimeCapacityBasis'
)
assert.match(
  scheduleOrderPage,
  /capacityMode:\s*request\.runtimeCapacityBasis/,
  'schedule order preflight request may keep capacityMode but must derive it from runtimeCapacityBasis'
)
assert.doesNotMatch(
  scheduleOrderPage,
  /replanForm\.capacityMode|capacityMode:\s*request\.capacityMode/,
  'schedule order replan must not use the old runtime capacityMode field'
)

for (const [label, content] of [
  ['route API', routeApi],
  ['route flow config API', flowConfigApi]
]) {
  assert.match(
    content,
    /capacityMode\??:\s*['"]RESOURCE_CALCULATED['"]\s*\|\s*['"]MANUAL_OVERRIDE['"]/,
    `${label} must keep route strategy capacityMode`
  )
  assert.doesNotMatch(
    content,
    /runtimeCapacityBasis/,
    `${label} must not rename route strategy capacityMode`
  )
}

console.log('mes-auto-schedule-runtime-capacity-basis-static PASS')
