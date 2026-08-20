const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const apiSource = fs.readFileSync(
  path.resolve(__dirname, '../../../../api/mes/pro/feedback/index.ts'),
  'utf8'
).replace(/\r\n/g, '\n')
const contextSource = fs.readFileSync(
  path.join(__dirname, 'frontlineDeviceEmployeeContext.ts'),
  'utf8'
).replace(/\r\n/g, '\n')
const panelSource = fs.readFileSync(
  path.join(__dirname, 'FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')

const sliceBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `${label} missing end marker`)
  return source.slice(start, end)
}

assert.match(
  apiSource,
  /getFrontlineProductionActiveOrderProcesses:[\s\S]*active-order\/processes[\s\S]*params:\s*\{\s*activeOrderId\s*\}/,
  'Production must expose an active-order frozen process API.'
)

const runtimeConfigApi = sliceBetween(
  apiSource,
  'getFrontlineRuntimeConfig: async (params:',
  '// 获取 PQC 员工 + PQC 组长',
  'runtime config API'
)
assert.match(
  runtimeConfigApi,
  /activeOrderId:\s*number/,
  'Production runtime-config API params must require activeOrderId.'
)
assert.doesNotMatch(
  runtimeConfigApi,
  /activeOrderId\?:/,
  'Production runtime-config API params must not allow requests without activeOrderId.'
)
assert.match(
  runtimeConfigApi,
  /if\s*\(!params\.activeOrderId\)\s*\{[\s\S]*当前工序缺少活跃订单身份/,
  'Production runtime-config API must fail fast before sending a request without activeOrderId.'
)

const productionOrderSelection = sliceBetween(
  contextSource,
  'export const selectFrontlineProductionActiveOrder',
  'export const selectFrontlinePqcActiveOrder',
  'production active-order selection'
)
assert.match(
  productionOrderSelection,
  /getFrontlineProductionActiveOrderProcesses\(\s*activeOrder\.activeOrderId\s*\)/,
  'Selecting a production active order must load that order frozen processes.'
)
assert.doesNotMatch(
  productionOrderSelection,
  /getFrontlineDeviceAccountProcesses\(/,
  'Production active-order selection must not read the current route process list.'
)
assert.match(
  productionOrderSelection,
  /selectedProcess\s*=\s*undefined[\s\S]*selectedEmployee\s*=\s*undefined[\s\S]*runtimeConfig\s*=\s*undefined[\s\S]*template\s*=\s*undefined/,
  'Switching active orders must clear process, employee, runtime config, and template state.'
)

const productionProcessSelection = sliceBetween(
  contextSource,
  'export const selectFrontlineProcess',
  'const toEmployeeCandidate',
  'production process selection'
)
assert.match(
  productionProcessSelection,
  /if\s*\(!process\.activeOrderId\)\s*\{[\s\S]*当前工序缺少活跃订单身份/,
  'Selecting a production process must fail fast before runtime-config when activeOrderId is missing.'
)
assert.match(
  contextSource,
  /getFrontlineRuntimeConfig\(\{[\s\S]*activeOrderId:\s*process\.activeOrderId[\s\S]*routeProcessId:\s*process\.routeProcessId/,
  'Runtime-config requests must carry the selected active order identity.'
)
assert.match(
  contextSource,
  /FrontlineSwitchActualEmployeeReqVO[\s\S]*activeOrderId/,
  'Employee switching must preserve active-order identity.'
)

const runtimeCachePreload = sliceBetween(
  contextSource,
  'export const preloadFrontlineProductionRuntimeCache',
  '  state.preloadingRuntimeCache = true',
  'production runtime cache preload'
)
assert.match(
  runtimeCachePreload,
  /createFrontlineProcessRuntimeCacheKey\(item\) === createFrontlineProcessRuntimeCacheKey\(process\)/,
  'Fullscreen runtime preloading must dedupe by activeOrderId + route/process identity.'
)
assert.doesNotMatch(
  runtimeCachePreload,
  /createFrontlineBaseProcessKey\(item\) === createFrontlineBaseProcessKey\(process\)/,
  'Fullscreen runtime preloading must not collapse different active orders with the same route/process identity.'
)
assert.match(
  runtimeCachePreload,
  /if\s*\(uniqueProcesses\.some\(\(process\) => !process\.activeOrderId\)\)\s*\{[\s\S]*当前工序缺少活跃订单身份/,
  'Fullscreen runtime preloading must fail fast before runtime-config when any process lacks activeOrderId.'
)

const applyOrderContext = sliceBetween(
  panelSource,
  'const applyActiveOrderToContext',
  'const applyProcessToContext',
  'active-order context reset'
)
assert.match(
  applyOrderContext,
  /context\.routeProcessId\s*=\s*undefined[\s\S]*context\.processId\s*=\s*undefined[\s\S]*context\.actualEmployeeId\s*=\s*undefined/,
  'Changing active orders must clear the previous process and employee context.'
)

const switchableProcessOptions = sliceBetween(
  panelSource,
  'const switchableProcessOptions = computed',
  'const normalizeActiveOrderKeyword',
  'switchable production process options'
)
assert.match(
  switchableProcessOptions,
  /`MES-\$\{process\.activeOrderId\}-\$\{process\.routeId\}-\$\{process\.routeProcessId\}-\$\{process\.processId\}`/,
  'Switchable production process options must keep activeOrderId in their identity key.'
)

const pickerOptions = sliceBetween(
  panelSource,
  'const pickerOptions = computed<FrontlinePickerOption[]>',
  '  if (activePicker.value === \'employee\')',
  'picker production process options'
)
assert.match(
  pickerOptions,
  /`MES-\$\{process\.activeOrderId\}-\$\{process\.routeId\}-\$\{process\.routeProcessId\}-\$\{process\.processId\}`/,
  'Production process picker keys must keep activeOrderId in their identity key.'
)

const sameProcess = sliceBetween(
  panelSource,
  'const isSameProcess =',
  'const selectedProductionProcessIndex',
  'same process comparison'
)
assert.match(
  sameProcess,
  /left\.activeOrderId === right\.activeOrderId/,
  'Production process comparison must include activeOrderId.'
)

console.log('PASS: frontline production uses active-order frozen processes')
