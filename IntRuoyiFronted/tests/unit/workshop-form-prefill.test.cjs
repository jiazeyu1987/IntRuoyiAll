const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const Vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/views/mes/md/workstation/workshop/WorkshopForm.vue')
const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'))
assert.deepEqual(errors, [])
const ast = ts.createSourceFile('workshop.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const script = ast.statements.filter(statement => !ts.isImportDeclaration(statement))
  .map(statement => statement.getText(ast)).join('\n')
const compiled = ts.transpileModule(script + '\nreturn { open, formData, formLoading, formReady };', {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText

function createForm({ getWorkshop } = {}) {
  const context = {
    ref: Vue.ref, computed: Vue.computed, reactive: Vue.reactive,
    defineOptions() {}, defineExpose() {}, defineEmits: () => () => {},
    useI18n: () => ({ t: value => value }), useMessage: () => ({}),
    CommonStatusEnum: { ENABLE: 0 },
    MdWorkshopApi: { getWorkshop }
  }
  return Function(...Object.keys(context), compiled)(...Object.values(context))
}

const workshop = { id: 12, code: 'WS-20260513-02', name: '车间2', area: 100, chargeUserId: 8, status: 0, remark: '原备注' }

test('edit loads existing workshop fields and prevents saving while detail is pending', async () => {
  let releaseDetail
  const detail = new Promise(resolve => { releaseDetail = resolve })
  const form = createForm({
    getWorkshop: async id => { assert.equal(id, workshop.id); return detail }
  })
  const opening = form.open('update', workshop.id)
  assert.equal(form.formLoading.value, true)
  assert.equal(form.formReady.value, false)
  releaseDetail({ ...workshop })
  await opening
  assert.deepEqual(form.formData.value, workshop)
  assert.equal(form.formLoading.value, false)
  assert.equal(form.formReady.value, true)
})

test('detail request failure is propagated without displaying previous workshop data', async () => {
  const error = new Error('详情加载失败')
  const form = createForm({ getWorkshop: async () => { throw error } })
  form.formData.value = { ...workshop }
  await assert.rejects(form.open('update', workshop.id), error)
  assert.equal(form.formData.value.code, undefined)
  assert.equal(form.formReady.value, false)
  assert.equal(form.formLoading.value, false)
})

test('create resets edited values and detail also fills the existing code', async () => {
  const form = createForm({ getWorkshop: async () => ({ ...workshop }) })
  await form.open('detail', workshop.id)
  assert.equal(form.formData.value.code, workshop.code)
  await form.open('create')
  assert.equal(form.formData.value.code, undefined)
  assert.equal(form.formReady.value, true)
})

test('template compiles and save remains disabled until initialization succeeds', () => {
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'workshop-prefill' }).errors, [])
  assert.match(descriptor.template.content, /:disabled="formLoading \|\| !formReady"/)
})
