const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const loadUuid = require('../helpers/load-secure-uuid.cjs')
const httpCrypto = { getRandomValues: bytes => require('node:crypto').webcrypto.getRandomValues(bytes) }

function setup(crypto = httpCrypto) {
  const filename = path.resolve(__dirname, '../../src/views/mes/dv/machinery/MachineryForm.vue')
  const { descriptor } = parse(fs.readFileSync(filename, 'utf8'), { filename })
  const script = compileScript(descriptor, { id: filename })
  const code = ts.transpileModule(script.content, {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
  }).outputText
  const module = { exports: {} }, opened = [], directWrites = [], events = []
  const context = { ...vue, module, exports: module.exports,
    useI18n: () => ({ t: value => value }), useMessage: () => ({ success() {} }),
    require: name => {
      if (name === 'vue') return vue
      if (name === '@/utils/uuid') return loadUuid(crypto)
      if (name === '@/api/mes/dv/machinery') return { DvMachineryApi: {
        createMachinery: async data => directWrites.push(data), updateMachinery: async data => directWrites.push(data)
      } }
      if (name === '@/views/mes/utils/constants') return { MesDvMachineryStatusEnum: { STOP: 'STOP' } }
      return {}
    }
  }
  vm.runInNewContext(code, context)
  const state = module.exports.default.setup({}, { expose() {}, emit: (...args) => events.push(args) })
  state.formRef.value = { validate: async () => true }
  state.sharedChangeDialog.value = { open: async request => opened.push(JSON.parse(JSON.stringify(request))) }
  state.formType.value = 'update'
  state.formData.value = { id: 83, code: 'DEVICE-83', name: '设备原始草稿', status: 'STOP' }
  state.sharedFingerprint.value = 'source-fingerprint'
  return { state, opened, directWrites, events }
}

test('HTTP device update sends UUID v4 to shared confirmation and preserves fingerprint and draft', async () => {
  assert.equal(httpCrypto.randomUUID, undefined)
  const { state, opened, directWrites, events } = setup()
  await state.submitForm()
  assert.equal(opened.length, 1)
  assert.match(opened[0].requestId, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
  assert.deepEqual({ ...opened[0], requestId: undefined }, {
    subjectType: 'DEVICE', action: 'UPDATE', sourceId: 83, expectedFingerprint: 'source-fingerprint',
    requestId: undefined, device: { id: 83, code: 'DEVICE-83', name: '设备原始草稿', status: 'STOP' }, reason: '', nodes: []
  })
  assert.equal(directWrites.length, 0)
  assert.equal(events.length, 0)
  state.formData.value.name = '后续草稿'
  assert.equal(opened[0].device.name, '设备原始草稿')
  await state.submitForm()
  assert.notEqual(opened[0].requestId, opened[1].requestId)
})

test('missing or failed random source stops before shared confirmation or device writes', async () => {
  for (const crypto of [{}, { getRandomValues() { throw new Error('entropy failed') } }]) {
    const { state, opened, directWrites, events } = setup(crypto)
    await assert.rejects(state.submitForm(), /安全随机数|entropy failed/)
    assert.equal(opened.length, 0)
    assert.equal(directWrites.length, 0)
    assert.equal(events.length, 0)
    assert.equal(state.formLoading.value, false)
  }
})

test('validation failure occurs before random generation or shared confirmation', async () => {
  const { state, opened } = setup({ getRandomValues() { throw new Error('must not generate') } })
  state.formRef.value = { validate: async () => { throw new Error('validation failed') } }
  await assert.rejects(state.submitForm(), /validation failed/)
  assert.equal(opened.length, 0)
})
