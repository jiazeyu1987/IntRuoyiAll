import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'
import test from 'node:test'

const text = path => readFileSync(fileURLToPath(new URL('../' + path, import.meta.url)), 'utf8')
test('template editor retains invalid items and their precise repair reason', () => {
  const source = text('src/views/dcc/controlled-file/basic-data/components/ProjectFileTemplateEditor.vue')
  const start = source.indexOf('const toEditorRows =')
  const end = source.indexOf('const open =', start)
  const declaration = source.slice(start, end)
    .replace(': DccProjectFileTemplateRespVO', '').replace(': EditorRow[]', '')
  const context = vm.createContext({})
  vm.runInContext(`let rowKeySequence=0; ${declaration}; globalThis.convert=toEditorRows`, context)
  const rows = context.convert({ items: [{ fileTypeTaxonomyId: 103, fileName: 'drawing', valid: false, validationMessage: 'Category disabled' }] })
  assert.equal(rows.length, 1)
  assert.equal(rows[0].fileTypeTaxonomyId, 103)
  assert.equal(rows[0].invalidReason, 'Category disabled')
})
test('upload candidates exclude invalid template items', async () => {
  const source = text('src/views/dcc/controlled-file/upload/index.vue')
  const body = source.match(/const loadProjectFileTemplate = async \(projectCodeId: number\) => \{([\s\S]*?)\n\}/)?.[1]
  assert.ok(body)
  const context = {
    projectCodeId: 100, formData: { dccProjectCodeId: 100 },
    projectFileTemplateLoading: { value: false }, projectFileTemplateError: { value: '' },
    projectFileTemplateItems: { value: [] }, fileTypeTaxonomies: { value: [] },
    getProjectCodeFileTemplate: async () => ({ taxonomyOptions: [], items: [{ id: 1, valid: true }, { id: 2, valid: false }] }),
    message: { error: () => {} }, resolveUploadErrorMessage: e => e.message
  }
  await vm.runInNewContext(`(async () => {${body}})()`, context)
  assert.deepEqual(Array.from(context.projectFileTemplateItems.value, item => item.id), [1])
})
