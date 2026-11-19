const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/

test('HTTP random source generates distinct UUID v4 values without randomUUID', () => {
  const crypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }
  const { createSecureUuid } = loadUuid(crypto)
  const values = Array.from({ length: 1000 }, createSecureUuid)
  assert.equal(new Set(values).size, values.length)
  for (const value of values) assert.match(value, uuidPattern)
})

test('UUID generation uses exactly 16 random bytes and fixes only version and variant bits', () => {
  const { createSecureUuid } = loadUuid({
    randomUUID() { throw new Error('must not call randomUUID') },
    getRandomValues(bytes) {
      assert.equal(bytes.length, 16)
      return bytes.fill(255)
    }
  })
  assert.equal(createSecureUuid(), 'ffffffff-ffff-4fff-bfff-ffffffffffff')
  assert.equal(loadUuid({ getRandomValues: bytes => bytes.fill(0) }).createSecureUuid(),
    '00000000-0000-4000-8000-000000000000')
})

test('missing random source fails and entropy failures retain their original error', () => {
  for (const crypto of [null, {}, { randomUUID: () => 'must not be used' }]) {
    assert.throws(() => loadUuid(crypto).createSecureUuid(), /不支持安全随机数/)
  }
  const cause = new Error('entropy unavailable')
  assert.throws(() => loadUuid({ getRandomValues() { throw cause } }).createSecureUuid(),
    error => error === cause)
})

test('legacy generateUUID delegates to the shared UUID implementation with explicit failure', () => {
  const source = fs.readFileSync(path.resolve(__dirname, '../../src/utils/index.ts'), 'utf8')
  const code = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  let calls = 0
  const exports = {}
  vm.runInNewContext(code, { exports, require: id => {
    if (id === 'lodash-es') return {}
    assert.equal(id, './uuid')
    return { createSecureUuid() { calls++; return 'shared-value' } }
  } })
  assert.equal(exports.generateUUID(), 'shared-value')
  assert.equal(calls, 1)
  assert.doesNotMatch(source.slice(source.indexOf('export const generateUUID'),
    source.indexOf('export const generateUUID') + 90), /Math\.random|randomUUID/)
})
