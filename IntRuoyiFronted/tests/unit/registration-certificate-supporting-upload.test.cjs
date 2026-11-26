const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const { File } = require('node:buffer')
const ts = require('typescript')
const { ref } = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const source = fs.readFileSync('src/views/dcc/registration-certificate/supporting-document/SupportingDocumentPanel.vue', 'utf8')
const script = compileScript(parse(source).descriptor, { id: 'supporting-upload' }).content
function setup(upload) {
  const state = { loading: ref(false), file: ref(new File(['evidence'], 'receipt.pdf')), input: ref({ value: 'receipt.pdf' }), documentType: ref('RENEWAL_ACCEPTANCE_RECEIPT') }
  const events = [], errors = []
  const submit = new Function(...Object.keys(state), 'props', 'uploadSupportingDocument', 'createSecureUuid', 'message', 'emit', 'FormData',
    ts.transpileModule(script.slice(script.indexOf('const submit ='), script.indexOf('const __returned__')), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText + '\nreturn submit')(
      ...Object.values(state), { certificateId: 42, versionId: 84 }, upload, () => 'key', { success() {}, error(value) { errors.push(value) } }, (...args) => events.push(args), FormData)
  return { ...state, submit, events, errors }
}
test('actual supporting upload submits its selected version and file, then refreshes detail', async () => {
  let call
  const state = setup(async (...args) => { call = args })
  await state.submit()
  assert.equal(call[0], 42)
  assert.equal(call[1].get('versionId'), '84')
  assert.equal(call[1].get('documentType'), 'RENEWAL_ACCEPTANCE_RECEIPT')
  assert.equal(await call[1].get('file').text(), 'evidence')
  assert.equal(call[2], 'key')
  assert.deepEqual(state.events, [['uploaded']])
  assert.equal(state.input.value.value, '')
  assert.equal(state.loading.value, false)
})
test('failed uploads retain the file and show the actual failure without a success event', async () => {
  const state = setup(async () => { throw new Error('scope denied') })
  await state.submit()
  assert.deepEqual(state.errors, ['scope denied'])
  assert.deepEqual(state.events, [])
  assert.equal(state.file.value.name, 'receipt.pdf')
  assert.equal(state.loading.value, false)
})
test('empty files and repeated clicks cannot send another upload', async () => {
  const state = setup(async () => assert.fail('must not upload'))
  state.file.value = new File([], 'empty.pdf')
  await state.submit()
  assert.equal(state.errors.length, 1)
  state.loading.value = true
  state.file.value = new File(['evidence'], 'receipt.pdf')
  await state.submit()
})
test('only domestic certificates show supporting materials and their light warning', () => {
  const condition = source.match(/<el-alert v-if="([^"]+)"/)[1]
  const shown = new Function('visualState', `return ${condition}`)
  assert.equal(shown('T_8'), true)
  for (const state of ['T_2', 'CE_T_12', 'CE_T_3', 'CLEARED', 'NONE']) assert.equal(shown(state), false)
  const cardCondition = source.match(/<el-card v-if="([^"]+)"/)[1]
  const cardShown = new Function('certificateType', `return ${cardCondition}`)
  assert.equal(cardShown('DOMESTIC'), true)
  for (const type of ['CE', 'FDA']) assert.equal(cardShown(type), false)
  assert.doesNotMatch(source, /CE_RENEWAL_|合同|报价单/)
})
