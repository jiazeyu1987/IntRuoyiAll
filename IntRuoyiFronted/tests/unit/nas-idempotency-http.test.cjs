const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')

const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(path.join(root, 'src/views/system/nas/index.vue'), 'utf8')
const script = ts.createSourceFile('nas.ts', page.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1], ts.ScriptTarget.Latest, true)
const names = ['createNasOriginalPathSyncIdempotencyKey', 'createNasUncontrolledImportIdempotencyKey', 'handleSyncNasOriginalPathOne']
const statements = script.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(script))))
assert.equal(statements.length, names.length)

function setup(crypto) {
  const writes = []
  const context = vm.createContext({ crypto, exports: {}, controlAuditFiles: { syncingOne: false, errorMessage: '' },
    handleSyncNasOriginalPathFiles: async (payload) => writes.push(payload) })
  const transpile = source => ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  vm.runInContext(`(() => { ${transpile(fs.readFileSync(path.join(root, 'src/utils/uuid.ts'), 'utf8'))} })()`, context)
  vm.runInContext(`const createSecureUuid = exports.createSecureUuid;\n${transpile(statements.map(statement => statement.getText(script)).join('\n'))}\nexports.handlers = { ${names.join(', ')} };`, context)
  return { ...context.exports.handlers, writes, state: context.controlAuditFiles }
}

test('NAS sync and import generate distinct UUID v4 keys with HTTP crypto capabilities', async () => {
  assert.match(page, /import \{ createSecureUuid \} from '@\/utils\/uuid'/)
  const h = setup({ getRandomValues: bytes => webcrypto.getRandomValues(bytes) })
  const sync = h.createNasOriginalPathSyncIdempotencyKey()
  const imported = h.createNasUncontrolledImportIdempotencyKey()
  const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/
  assert.match(sync, uuid)
  assert.match(imported, uuid)
  assert.notEqual(sync, imported)
  await h.handleSyncNasOriginalPathOne()
  assert.equal(h.writes.length, 1)
  assert.equal(h.writes[0].selectionScope, 'FIRST_UNSYNCED')
  assert.match(h.writes[0].idempotencyKey, uuid)
  assert.equal(h.state.syncingOne, false)
})

test('NAS entropy failures stop sync before its API and remain visible through its existing error boundary', async () => {
  for (const crypto of [undefined, {}, { getRandomValues() { throw new Error('entropy unavailable') } }]) {
    const h = setup(crypto)
    assert.throws(() => h.createNasUncontrolledImportIdempotencyKey())
    await h.handleSyncNasOriginalPathOne()
    assert.equal(h.writes.length, 0)
    assert.match(h.state.errorMessage, /不支持安全随机数|entropy unavailable/)
    assert.equal(h.state.syncingOne, false)
  }
})
