import assert from 'node:assert/strict'
import fs from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()

const loadTsModule = async (relativePath) => {
  const source = fs.readFileSync(path.join(root, relativePath), 'utf8')
  const transformed = stripTypeScriptTypes(source, { mode: 'transform' })
  const moduleUrl = `data:text/javascript;base64,${Buffer.from(transformed).toString('base64')}`
  return await import(moduleUrl)
}

const createCandidate = (index, overrides = {}) => ({
  productId: index + 1,
  productCode: `product_${String(index + 1).padStart(3, '0')}`,
  nameCn: `候选产品 ${index + 1}`,
  revisionNo: 1,
  incomplete: index % 2 === 0,
  hallIds: [],
  ...overrides
})

test('hall product candidates preserve explicit empty Chinese names', async () => {
  const { normalizeHallProductCandidateOptions } = await loadTsModule(
    'src/views/showroom-admin/hall/contracts.ts'
  )
  const products = Array.from({ length: 10 }, (_, index) => createCandidate(index))
  products[9] = createCandidate(9, { nameCn: '' })

  const normalized = normalizeHallProductCandidateOptions(products)

  assert.equal(normalized[9].productCode, 'product_010')
  assert.equal(normalized[9].nameCn, '')
  assert.equal(normalized[9].revisionNo, 1)
})

test('selected hall products preserve explicit empty Chinese names from revisions', async () => {
  const { normalizeProductOptions } = await loadTsModule(
    'src/views/showroom-admin/hall/contracts.ts'
  )
  const products = Array.from({ length: 10 }, (_, index) => ({
    productId: index + 1,
    productCode: `product_${String(index + 1).padStart(3, '0')}`,
    revision: {
      nameCn: `已选产品 ${index + 1}`,
      revisionNo: 1
    },
    incomplete: false
  }))
  products[9].revision.nameCn = ''

  const normalized = normalizeProductOptions(products)

  assert.equal(normalized[9].productCode, 'product_010')
  assert.equal(normalized[9].nameCn, '')
  assert.equal(normalized[9].revisionNo, 1)
})

test('hall product candidates still fail fast when Chinese name field is absent', async () => {
  const { normalizeHallProductCandidateOptions } = await loadTsModule(
    'src/views/showroom-admin/hall/contracts.ts'
  )
  const products = Array.from({ length: 10 }, (_, index) => createCandidate(index))
  delete products[9].nameCn

  assert.throws(
    () => normalizeHallProductCandidateOptions(products),
    /展柜工作台缺少字符串字段：products\[9\]\.nameCn/
  )
})
