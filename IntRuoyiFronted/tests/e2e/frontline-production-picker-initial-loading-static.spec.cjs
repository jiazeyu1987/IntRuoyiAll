const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)

const productionPickerStart = panel.indexOf('v-if="activePicker"')
const productionPickerEnd = panel.indexOf('<main class="frontline-operator-main', productionPickerStart)
assert.ok(productionPickerStart >= 0 && productionPickerEnd > productionPickerStart)
const productionPicker = panel.slice(productionPickerStart, productionPickerEnd)

assert.match(
  productionPicker,
  /v-if="pickerStatusText"[\s\S]*class="frontline-picker__empty"[\s\S]*role="status"[\s\S]*aria-live="polite"[\s\S]*\{\{ pickerStatusText \}\}/,
  'production process and employee pickers must expose loading, prerequisite, empty, and error state text.'
)

const pickerStatusStart = panel.indexOf('const pickerStatusText = computed')
const pickerStatusEnd = panel.indexOf('\nconst frontlineContextKey', pickerStatusStart)
assert.ok(pickerStatusStart >= 0 && pickerStatusEnd > pickerStatusStart)
const pickerStatusBlock = panel.slice(pickerStatusStart, pickerStatusEnd)

for (const requiredState of [
  'deviceState.lastError',
  'deviceState.loadingProcesses',
  'deviceState.selectedProcess',
  'deviceState.loadingEmployees',
  'pickerOptions.value.length'
]) {
  assert.ok(
    pickerStatusBlock.includes(requiredState),
    `picker status must use ${requiredState}.`
  )
}
assert.match(pickerStatusBlock, /工序加载中/)
assert.match(pickerStatusBlock, /请先选择工序/)
assert.match(pickerStatusBlock, /员工加载中/)
assert.match(pickerStatusBlock, /暂无可用工序/)
assert.match(pickerStatusBlock, /当前工序暂无可选员工/)

const productionInitializationStart = panel.indexOf('const initializeProductionSelection = async')
const productionInitializationEnd = panel.indexOf('\nconst resolveErrorMessage', productionInitializationStart)
assert.ok(productionInitializationStart >= 0 && productionInitializationEnd > productionInitializationStart)
const productionInitialization = panel.slice(
  productionInitializationStart,
  productionInitializationEnd
)
assert.match(
  productionInitialization,
  /await loadFrontlineDeviceProcesses\(deviceState\)[\s\S]*findInitialProcess\(processes\)[\s\S]*await handleSelectProcess\(initialProcess\)/,
  'production initialization must load and select the formal process context.'
)

const mountedStart = panel.indexOf('onMounted(async () => {')
const mountedEnd = panel.indexOf('\nonUnmounted(', mountedStart)
assert.ok(mountedStart >= 0 && mountedEnd > mountedStart)
const mountedBlock = panel.slice(mountedStart, mountedEnd)
assert.match(
  mountedBlock,
  /const catalogRequest = FrontlineTemplateApi\.getCatalog\(\)[\s\S]*const \[loadedCatalog\] = await Promise\.all\(\[[\s\S]*catalogRequest,[\s\S]*initializeProductionSelection\(\)[\s\S]*\]\)[\s\S]*catalog\.value = loadedCatalog/,
  'production process selection must initialize concurrently with the template catalog request.'
)

assert.match(
  panel,
  /\.frontline-picker__empty\s*\{[\s\S]*grid-column:\s*1\s*\/\s*-1;/,
  'picker state text must span the option grid.'
)

console.log('frontline production picker initial loading static contract passed')
