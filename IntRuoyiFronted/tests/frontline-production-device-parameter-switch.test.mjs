import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import { runInNewContext } from 'node:vm'
import { parse, compileScript, compileTemplate } from 'vue/compiler-sfc'

const filename = 'FrontlineFixedTemplatePanel.vue'
const source = readFileSync(new URL('../src/views/mes/pro/feedback/' + filename, import.meta.url), 'utf8')
const between = (start, end) => {
  const a = source.indexOf(start), b = source.indexOf(end, a)
  assert.ok(a >= 0 && b > a)
  return source.slice(a, b)
}
const ref = value => ({ value })
const computed = fn => ({ get value() { return fn() } })
const harness = () => {
  const washer = { key: 'washer', deviceCode: 'B09031', deviceGroupKey: 'wash', selectionMode: 'MULTIPLE' }
  const dryer = { key: 'dryer', deviceCode: 'B04091', deviceGroupKey: 'dry', selectionMode: 'MULTIPLE' }
  const alternate = { key: 'alternate', deviceCode: 'B04041', deviceGroupKey: 'dry', selectionMode: 'SINGLE' }
  const runtime = {
    computed, payloadLoading: ref(false), selectedProductionDeviceKey: ref('washer'),
    selectedProductionDeviceKeys: ref(['washer', 'dryer']), visibleDeviceCards: ref([washer, dryer, alternate]),
    deviceParameterDraft: { washer: { temperature: 26, time: 30 }, dryer: { temperature: 60, time: 3 } },
    deviceMeteringValidityDraft: { washer: true, dryer: true },
    syncProductionDeviceParameterDraft() {}, syncProductionDeviceMeteringValidityDraft() {}
  }
  const script = between('const activeProductionDevice = computed(', 'const isProductionSelfCheckNarrativeDevice =') +
    '\nObject.assign(globalThis, { activeProductionDevice, showProductionDeviceParameters, toggleProductionDeviceSelection })'
  runInNewContext(stripTypeScriptTypes(script), runtime)
  return { runtime, washer, dryer, alternate }
}

test('switching to an already selected dryer preserves both devices and their entered parameters', () => {
  const { runtime: r, washer, dryer } = harness()
  const parameters = structuredClone(r.deviceParameterDraft), metering = structuredClone(r.deviceMeteringValidityDraft)
  assert.equal(r.activeProductionDevice.value, washer)
  r.showProductionDeviceParameters(dryer)
  assert.equal(r.activeProductionDevice.value, dryer)
  assert.deepEqual(r.selectedProductionDeviceKeys.value, ['washer', 'dryer'])
  assert.deepEqual(r.deviceParameterDraft, parameters)
  assert.deepEqual(r.deviceMeteringValidityDraft, metering)
  r.showProductionDeviceParameters(washer)
  assert.equal(r.activeProductionDevice.value, washer)
  assert.deepEqual(r.deviceParameterDraft, parameters)
  assert.deepEqual(r.deviceMeteringValidityDraft, metering)
})

test('the parameter action cannot select an unused device or change the active device during submission', () => {
  const { runtime: r, dryer, alternate } = harness()
  r.showProductionDeviceParameters(alternate)
  assert.equal(r.selectedProductionDeviceKey.value, 'washer')
  assert.deepEqual(r.selectedProductionDeviceKeys.value, ['washer', 'dryer'])
  r.payloadLoading.value = true
  r.showProductionDeviceParameters(dryer)
  assert.equal(r.selectedProductionDeviceKey.value, 'washer')
  assert.deepEqual(r.deviceParameterDraft.dryer, { temperature: 60, time: 3 })
})

test('explicitly deselecting a device still removes its draft and activates the remaining device', () => {
  const { runtime: r, dryer, washer } = harness()
  r.showProductionDeviceParameters(dryer)
  r.toggleProductionDeviceSelection(dryer)
  assert.deepEqual([...r.selectedProductionDeviceKeys.value], ['washer'])
  assert.equal(r.activeProductionDevice.value, washer)
  assert.equal(r.deviceParameterDraft.dryer, undefined)
  assert.equal(r.deviceMeteringValidityDraft.dryer, undefined)
  assert.deepEqual(r.deviceParameterDraft.washer, { temperature: 26, time: 30 })
})

test('single-choice device selection still replaces only its own device group', () => {
  const { runtime: r, alternate } = harness()
  r.toggleProductionDeviceSelection(alternate)
  assert.deepEqual([...r.selectedProductionDeviceKeys.value], ['washer', 'alternate'])
  assert.equal(r.activeProductionDevice.value, alternate)
})

test('the actual template exposes a separate guarded and accessible parameter button', () => {
  const { descriptor, errors } = parse(source, { filename })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: 'production-device-switch-test' })
  const template = compileTemplate({ source: descriptor.template.content, filename, id: 'production-device-switch-test', compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
  const button = descriptor.template.content.match(/<button\s[^>]*data-frontline-production-device-parameters[^>]*>/)?.[0]
  assert.ok(button)
  assert.match(button, /:disabled="payloadLoading \|\| !selectedProductionDeviceKeys.includes\(device.key\)"/)
  assert.match(button, /:aria-label="`\$\{device.label\}填写参数`"/)
  assert.match(button, /:aria-pressed="selectedProductionDeviceKey === device.key"/)
  assert.match(button, /@click="showProductionDeviceParameters\(device\)"/)
  assert.ok(!button.includes('toggleProductionDeviceSelection'))
})
