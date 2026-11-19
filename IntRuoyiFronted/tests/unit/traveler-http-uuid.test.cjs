const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/edhr-traveler/TravelerPage.vue'), 'utf8')
const code = source.slice(source.indexOf('const openGenerateDialog = async () => {'),
  source.indexOf('const openDetailDrawer ='))

function setup(crypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }) {
  const writes = [], messages = []
  const state = {
    ...loadUuid(crypto),
    generateError: { value: '' }, generateSubmitting: { value: false },
    generateDialogVisible: { value: false }, generateForm: {},
    templateList: { value: [{ id: 4, status: 'ACTIVE' }] },
    activeTemplateOptions: { value: [{ id: 4, status: 'ACTIVE' }] },
    loadTemplateList: async () => {}, loadTravelerList: async () => {},
    generateFormRef: { value: { validate: async () => true } },
    message: { success: text => messages.push(text), error: text => messages.push(text) },
    resolveErrorMessage: error => error.message,
    generateEdhrTraveler: async data => {
      writes.push(data)
      if (state.fail) throw new Error('generate rejected')
    },
    fail: false
  }
  vm.createContext(state)
  vm.runInContext(`${code}\nthis.open = openGenerateDialog; this.submit = submitGenerate`, state)
  return { state, writes, messages }
}

test('traveler audit request uses HTTP UUID v4 and reuses dialog request on failure', async () => {
  const { state, writes } = setup()
  await state.open()
  const key = state.generateForm.requestId
  assert.match(key, /^traveler-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
  state.generateForm.batchExecutionId = 5
  state.generateForm.routeProcessId = 6
  state.fail = true
  await state.submit()
  assert.equal(state.generateDialogVisible.value, true)
  assert.equal(state.generateError.value, 'generate rejected')
  state.fail = false
  await state.submit()
  assert.equal(writes.length, 2)
  assert.equal(writes[0].requestId, key)
  assert.equal(writes[1].requestId, key)
  assert.equal(writes[1].batchExecutionId, 5)
  assert.equal(writes[1].routeProcessId, 6)
  await state.open()
  assert.notEqual(state.generateForm.requestId, key)
})

test('missing or failed random source does not open traveler dialog or send a write', async () => {
  for (const crypto of [{}, { getRandomValues() { throw new Error('entropy failed') } }]) {
    const { state, writes } = setup(crypto)
    await assert.rejects(state.open(), /不支持安全随机数|entropy failed/)
    assert.equal(state.generateDialogVisible.value, false)
    assert.equal(state.generateForm.requestId, undefined)
    assert.equal(writes.length, 0)
  }
})
