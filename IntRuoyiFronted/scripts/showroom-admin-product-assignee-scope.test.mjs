import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('showroom product list separates management actions from row editability', () => {
  const source = readText('src/views/showroom-admin/components/ProductListTable.vue')

  assert.match(source, /manageProducts\?: boolean/)
  assert.match(source, /const manageProducts = computed\(\(\) => Boolean\(props\.manageProducts\)\)/)
  assert.match(source, /editable: boolean/)
  assert.match(source, /<el-button v-if="manageProducts" type="primary" @click="emit\('create'\)">/)
  assert.match(source, /<el-button\s+v-if="manageProducts"[\s\S]*@click="emit\('generate', row\.raw\)"/)
  assert.match(source, /<el-button v-if="manageProducts" link type="primary" @click="emit\('assign', row\.raw\)">/)
  assert.match(source, /<el-button v-if="row\.editable" link type="primary" @click="emit\('edit', row\.raw\)">基础信息<\/el-button>/)
  assert.match(source, /<el-button link type="primary" @click="emit\('detail', row\.raw\)">详细信息<\/el-button>/)
})

test('showroom admin workspace passes explicit publicity management state to product list', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /const isShowroomPublicity = computed\(\(\) =>/)
  assert.match(source, /roles\.includes\(SHOWROOM_PUBLICITY_ROLE_CODE\)/)
  assert.match(source, /:manage-products="isShowroomPublicity"/)
})

test('showroom product detail contract and dialog respect backend editable capability', () => {
  const contractSource = readText('src/views/showroom-admin/product/contracts.ts')
  const dialogSource = readText('src/views/showroom-admin/product/ProductDetailDialog.vue')

  assert.match(contractSource, /editable: boolean/)
  assert.match(contractSource, /editable: expectBoolean\(record\.editable, 'editable'\)/)
  assert.match(dialogSource, /const readonly = computed\(\(\) => Boolean\(detail\.value\) && !detail\.value\.editable\)/)
  assert.match(dialogSource, /<el-input v-model="form\.fields\.registration_certificate" :disabled="readonly" \/>/)
  assert.match(dialogSource, /<el-button v-if="!readonly" :disabled="!detail \|\| !form" :loading="saving" @click="handleSaveDraft">/)
  assert.match(dialogSource, /return !readonly\.value && \(changedFieldCodes\.value\.length > 0 \|\| status === 'DRAFT' \|\| status === 'REJECTED'\)/)
})

test('showroom assigned lifecycle wording remains 指派中 for IN_FILLING', () => {
  const listSource = readText('src/views/showroom-admin/components/ProductListTable.vue')
  const contractSource = readText('src/views/showroom-admin/product/contracts.ts')

  assert.match(listSource, /label="指派中" value="IN_FILLING"/)
  assert.match(listSource, /IN_FILLING: \{ text: '指派中', tagType: 'warning' \}/)
  assert.match(contractSource, /IN_FILLING: '指派中'/)
})
