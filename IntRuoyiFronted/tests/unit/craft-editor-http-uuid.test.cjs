const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/craft/index.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('craft.ts', script, ts.ScriptTarget.Latest, true)
const functions = new Set(['openEditor', 'saveOrPreview', 'confirmAll'])
const code = ts.transpileModule(ast.statements.filter(node =>
  ts.isFunctionDeclaration(node) && functions.has(node.name.text)
).map(node => node.getText(ast)).join('\n'), {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText
const plain = value => JSON.parse(JSON.stringify(value))
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/
const craft = { id: 5, code: 'CRAFT-5', name: '工艺', processId: 9, fingerprint: 'version-1', parametersJson: '[{"parameterCode":"P1"}]' }

function setup(crypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }) {
  const writes = [], previews = []
  let fail = true
  const context = {
    ...loadUuid(crypto), form: {},
    editorCode: { value: '' }, readOnly: { value: false }, editorError: { value: '' },
    editorVisible: { value: false }, saving: { value: false }, impact: { value: undefined },
    impactError: { value: '' }, impactVisible: { value: false },
    explain: error => error.message, message: { success() {} }, load: async () => {},
    CraftApi: {
      preview: async value => { previews.push(plain(value)); return { impactToken: 'preview-token' } },
      updateAll: async value => {
        writes.push(plain(value))
        if (fail) throw new Error('failed write')
        return { status: 'APPLIED' }
      }
    }
  }
  vm.createContext(context)
  vm.runInContext(code, context)
  return { context, writes, previews, allowWrite: () => { fail = false } }
}

test('HTTP craft editor keeps UUID v4 and fingerprint across preview and failed-write retry', async () => {
  const { context: c, writes, previews, allowWrite } = setup()
  c.openEditor(craft)
  const key = c.form.requestId
  assert.match(key, uuidPattern)
  c.form.reason = '修改参数'
  await c.saveOrPreview()
  await c.confirmAll()
  assert.equal(c.impactError.value, 'failed write')
  assert.equal(c.editorVisible.value, true)
  allowWrite()
  await c.confirmAll()
  for (const command of [...previews, ...writes]) {
    assert.equal(command.requestId, key)
    assert.equal(command.expectedFingerprint, craft.fingerprint)
    assert.deepEqual(command.parameters, [{ parameterCode: 'P1' }])
  }
  assert.equal(writes.length, 2)
  assert.equal(writes[1].impactToken, 'preview-token')
  c.openEditor(craft)
  assert.notEqual(c.form.requestId, key)
})

test('missing or failing secure random source stops editor opening without changing draft or requesting writes', () => {
  for (const crypto of [{}, { getRandomValues() { throw new Error('entropy failed') } }]) {
    const { context: c, writes, previews } = setup(crypto)
    c.form.name = '原草稿'
    assert.throws(() => c.openEditor(craft), /不支持安全随机数|entropy failed/)
    assert.deepEqual(plain(c.form), { name: '原草稿' })
    assert.equal(c.editorVisible.value, false)
    assert.equal(writes.length, 0)
    assert.equal(previews.length, 0)
  }
})
