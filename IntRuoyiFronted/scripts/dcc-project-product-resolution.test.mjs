import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import ts from 'typescript'
import vm from 'node:vm'
import test from 'node:test'

const source = readFileSync(new URL('../src/views/dcc/controlled-file/upload/index.vue', import.meta.url), 'utf8')
const begin = source.indexOf('const applyDccProjectCodeProductNumber =')
const loader = source.slice(begin, source.indexOf('const loadBaseData =', begin))
test('product preview API uses the controlled-files controller route', () => {
  const api = readFileSync(new URL('../src/api/dcc/controlledFile/workflow.ts', import.meta.url), 'utf8')
  assert.match(api, /url: '\/dcc\/controlled-files\/project-product'/)
})
const setup = () => {
  const c = {
    formData: { dccProjectCodeId: 100, productCode: 'old', productMasterId: null },
    selectedProjectCode: { value: { projectCode: 'PROJECT-CODE' } },
    projectProductLoading: { value: false }, projectProductError: { value: '' },
    projectProductResolvedId: { value: null }, projectProductSource: { value: '' },
    resolveUploadErrorMessage: e => e.message
  }
  vm.createContext(c)
  vm.runInContext(ts.transpileModule(`let projectProductRequestSequence=0; ${loader}; globalThis.load=applyDccProjectCodeProductNumber`, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return c
}
test('bound product uses the server resolution, never the project label', async () => {
  const c = setup()
  c.previewControlledFileProjectProduct = async () => ({ projectCodeId: 100, productMasterId: 9, productCode: 'A1234567890123', source: 'PRODUCT_MASTER' })
  await c.load()
  assert.equal(c.formData.productCode, 'A1234567890123')
  assert.equal(c.formData.productMasterId, 9)
  assert.equal(c.projectProductResolvedId.value, 100)
})
test('unbound project never copies its project code into product fields', async () => {
  const c = setup()
  c.previewControlledFileProjectProduct = async () => ({
    projectCodeId: 100, productMasterId: null, productCode: null, productName: null, source: 'UNBOUND'
  })
  await c.load()
  assert.equal(c.formData.productCode, '')
  assert.equal(c.formData.productMasterId, null)
  assert.equal(c.projectProductSource.value, 'UNBOUND')
  assert.equal(c.projectProductResolvedId.value, 100)
})
test('a previous project result cannot replace the current product', async () => {
  const c = setup(); let finish
  c.previewControlledFileProjectProduct = async id => id === 100
    ? new Promise(resolve => { finish = resolve })
    : { projectCodeId: 200, productMasterId: 8, productCode: 'B1234567890123', source: 'PRODUCT_MASTER' }
  const first = c.load()
  c.formData.dccProjectCodeId = 200
  await c.load()
  assert.ok(finish)
  finish({ projectCodeId: 100, productCode: 'A1234567890123' }); await first
  assert.equal(c.formData.productCode, 'B1234567890123')
  assert.equal(c.projectProductResolvedId.value, 200)
})
test('failed product lookup keeps an explicit error and blocks resolution', async () => {
  const c = setup()
  c.previewControlledFileProjectProduct = async () => { throw new Error('Product disabled') }
  await c.load()
  assert.equal(c.formData.productCode, '')
  assert.equal(c.projectProductError.value, 'Product disabled')
  assert.equal(c.projectProductResolvedId.value, null)
  assert.match(source, /projectProductResolvedId\.value !== formData\.dccProjectCodeId/)
})
