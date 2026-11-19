const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/process/ProProcessForm.vue'), 'utf8')
const submit = source.slice(source.indexOf('const submitForm = async () => {'),
  source.indexOf('/** 重置表单 */'))
const plain = value => JSON.parse(JSON.stringify(value))

function setup(crypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }) {
  const opened = []
  const state = {
    ...loadUuid(crypto),
    formRef: { value: { validate: async () => true } },
    formType: { value: 'update' },
    formData: { value: { id: 81, name: '工序草稿', status: 0 } },
    sharedFingerprint: { value: 'formal-fingerprint' },
    processContents: { value: [{ id: 4, content: '步骤草稿', sort: 1 }] },
    sharedChangeDialog: { value: { open: async input => opened.push(input) } }
  }
  vm.createContext(state)
  vm.runInContext(`${submit}\nthis.submit = submitForm`, state)
  return { state, opened }
}

test('process edit opens shared approval with HTTP UUID v4 and unchanged fingerprint and snapshots', async () => {
  const { state, opened } = setup()
  await state.submit()
  assert.equal(opened.length, 1)
  const first = opened[0]
  assert.match(first.requestId, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
  assert.deepEqual(plain({ ...first, requestId: undefined }), {
    subjectType: 'PROCESS', action: 'UPDATE', sourceId: 81,
    expectedFingerprint: 'formal-fingerprint',
    process: { id: 81, name: '工序草稿', status: 0 },
    processContents: [{ id: 4, content: '步骤草稿', sort: 1 }], nodes: [], reason: ''
  })
  assert.notEqual(first.process, state.formData.value)
  assert.notEqual(first.processContents[0], state.processContents.value[0])
  await state.submit()
  assert.notEqual(opened[1].requestId, first.requestId)
})

test('missing or failed random source prevents opening shared approval', async () => {
  for (const crypto of [{}, { getRandomValues() { throw new Error('entropy failed') } }]) {
    const { state, opened } = setup(crypto)
    await assert.rejects(state.submit(), /不支持安全随机数|entropy failed/)
    assert.equal(opened.length, 0)
  }
})
