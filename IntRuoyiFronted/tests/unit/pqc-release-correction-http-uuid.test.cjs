const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')
const { ref, computed } = require('vue')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/Profile/components/PqcReleaseCorrection.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('correction.ts', script, ts.ScriptTarget.Latest, true)
const code = ts.transpileModule(ast.statements.filter(node => !ts.isImportDeclaration(node))
  .map(node => node.getText(ast)).join('\n') + '\nexports.form = { resubmit, key, task, correctionNote, submitting };', {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

function setup(crypto) {
  const writes = []
  const context = { exports: {}, ref, computed, ...loadUuid(crypto),
    useRoute: () => ({ query: {} }), useRouter: () => ({}), watch() {}, onBeforeUnmount() {},
    resubmitPqcReleaseCorrection: async payload => {
      writes.push(payload)
      throw new Error('网络中断')
    }
  }
  vm.runInNewContext(code, context)
  const form = context.exports.form
  form.task.value = { writable: true, workTaskId: '42', expectedVersion: 3,
    currentDossierRevision: 2, rejectedDossierRevision: 1 }
  form.correctionNote.value = '已补齐正式资料'
  return { ...form, writes }
}

test('HTTP correction generates UUID v4 and reuses the audit key after a failed submission', async () => {
  const httpCrypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }
  const form = setup(httpCrypto)
  await form.resubmit()
  await form.resubmit()
  assert.equal(form.writes.length, 2)
  assert.match(form.writes[0].idempotencyKey, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
  assert.equal(form.writes[1].idempotencyKey, form.writes[0].idempotencyKey)
  assert.equal(form.writes[0].workTaskId, '42')
  assert.equal(form.writes[0].expectedVersion, 3)
  assert.equal(form.writes[0].correctionNote, '已补齐正式资料')
  assert.equal(form.submitting.value, false)
})

test('missing secure random source fails before a correction request and leaves the key empty', async () => {
  const form = setup({})
  await assert.rejects(form.resubmit())
  assert.equal(form.writes.length, 0)
  assert.equal(form.key.value, '')
  assert.equal(form.submitting.value, false)
})
