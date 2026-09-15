import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import ts from 'typescript'
import vm from 'node:vm'
import test from 'node:test'

const source = readFileSync(fileURLToPath(new URL('../src/views/dcc/controlled-file/upload/index.vue', import.meta.url)), 'utf8')
const begin = source.indexOf('const loadRelatedFileOptions =')
const end = source.indexOf('const handleFileTypeTaxonomyChange =', begin)
const loader = source.slice(begin, end)
const setup = () => {
  const c = {
    formData: { dccProjectCodeId: 100, relatedControlledFileIds: [1] },
    relatedFileOptions: { value: [{ id: 1, fileNumber: 'chosen' }] }, relatedFileOptionsLoading: { value: false },
    relatedFileOptionsError: { value: '' }, relatedFilePageNo: { value: 1 }, relatedFileTotal: { value: 0 },
    relatedFileKeyword: { value: 'target' }, message: { error: () => {} }, resolveUploadErrorMessage: e => e.message
  }
  vm.createContext(c)
  vm.runInContext(ts.transpileModule(`let relatedFileRequestSequence=0; ${loader}; globalThis.load=loadRelatedFileOptions`, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return c
}
test('later related-file pages use server keyword and preserve selected version labels', async () => {
  const c = setup(); let query
  c.getProjectCodeControlledFilesPage = async (_id, params) => { query = params; return { total: 250, list: [{ id: 201, status: 'ACTIVE', businessSourceType: 'DCC_CONTROLLED_FILE' }] } }
  await c.load(100, 2)
  assert.equal(query.pageNo, 2); assert.equal(query.keyword, 'target')
  assert.equal(c.relatedFileTotal.value, 250)
  assert.ok(c.relatedFileOptions.value.some(x => x.id === 1))
  assert.ok(c.relatedFileOptions.value.some(x => x.id === 201))
})
test('old keyword results cannot replace newer results within the same project', async () => {
  const c = setup(); let finish
  c.getProjectCodeControlledFilesPage = async (_id, params) => {
    if (params.keyword === 'target') return new Promise(resolve => { finish = resolve })
    return { total: 1, list: [{ id: 3, status: 'ACTIVE', businessSourceType: 'DCC_CONTROLLED_FILE' }] }
  }
  const first = c.load(100)
  c.relatedFileKeyword.value = 'new'
  await c.load(100)
  assert.ok(finish, 'initial keyword sent to server')
  finish({ total: 1, list: [{ id: 2, status: 'ACTIVE', businessSourceType: 'DCC_CONTROLLED_FILE' }] }); await first
  assert.ok(c.relatedFileOptions.value.some(x => x.id === 3))
  assert.ok(!c.relatedFileOptions.value.some(x => x.id === 2))
})
