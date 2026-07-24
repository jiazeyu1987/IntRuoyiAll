import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('showroom product ownership UI is fixed to 瑛泰医疗 in the basic dialog', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /瑛泰医疗/)
  assert.doesNotMatch(source, /v-for="option in productCompanyOptions"/)
  assert.doesNotMatch(source, /placeholder="请选择公司"/)
  assert.doesNotMatch(source, /clearable/)
})

test('showroom product list ownership display is fixed to 瑛泰医疗', () => {
  const source = readText('src/views/showroom-admin/components/ProductListTable.vue')
  const contractsSource = readText('src/views/showroom-admin/product/contracts.ts')

  assert.match(source, /瑛泰医疗/)
  assert.doesNotMatch(source, /companyOptions/)
  assert.doesNotMatch(source, /companyNameById/)
  assert.match(contractsSource, /瑛泰医疗/)
})
