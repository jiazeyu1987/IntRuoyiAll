const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const panel = readUtf8('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const context = readUtf8('src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts')
const feedbackApi = readUtf8('src/api/mes/pro/feedback/index.ts')

assert.match(
  feedbackApi,
  /getFrontlineRuntimeConfig\s*:\s*async/,
  'frontline feedback API must expose a runtime config reader.'
)
assert.match(
  feedbackApi,
  /\/mes\/pro\/feedback\/frontline\/device-account\/runtime-config/,
  'runtime config reader must use the formal frontline device-account runtime-config endpoint.'
)
assert.match(
  feedbackApi,
  /FrontlineRuntimeConfigVO/,
  'frontline API must type the runtime config response.'
)

assert.match(
  context,
  /runtimeConfig/,
  'frontline context state must store team-leader runtime config.'
)
assert.match(
  context,
  /getFrontlineRuntimeConfig/,
  'frontline context must load runtime config after selecting a process.'
)
assert.doesNotMatch(
  context,
  /getFrontlineEmployeeCandidates/,
  'employee options must come from team-leader runtime config, not the legacy workstation post source.'
)

assert.doesNotMatch(
  panel,
  /const\s+productionDefects\s*=/,
  'production defect reasons must not be hardcoded in the employee fill page.'
)
assert.doesNotMatch(
  panel,
  /type\s+ProductionDeviceParameterKey\s*=\s*'pressure'\s*\|\s*'time'/,
  'device parameter keys must be dynamic from runtime config, not fixed pressure/time.'
)
assert.doesNotMatch(
  panel,
  /frontlineProductionDevicePressure|frontlineProductionDeviceTime/,
  'device parameter controls must be rendered from configured parameter rules.'
)
assert.match(
  panel,
  /runtimeConfig\.defectReasons|configuredDefectReasons/,
  'defect reason cards must be rendered from runtime config.'
)
assert.match(
  panel,
  /runtimeConfig\.devices|configuredDeviceCards/,
  'device cards must be rendered from runtime config.'
)
assert.match(
  panel,
  /defaultValue/,
  'device parameter inputs must initialize from configured default values.'
)

console.log('PASS: frontline team runtime config static contract is wired')
