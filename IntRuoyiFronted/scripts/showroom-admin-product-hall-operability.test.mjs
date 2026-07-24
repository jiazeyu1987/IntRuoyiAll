import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

test('showroom admin product workbench artifacts exist', () => {
  for (const relativePath of [
    'src/views/showroom-admin/product/index.ts',
    'src/views/showroom-admin/product/ProductDetailDialog.vue',
    'src/views/showroom-admin/product/ProductHistoryDrawer.vue',
    'src/views/showroom-admin/product/ProductWorkbench.vue'
  ]) {
    assert.ok(exists(relativePath), `${relativePath} must exist`)
  }
})

test('product detail workbench uses the real product detail and submit contract', () => {
  const source = readText('src/views/showroom-admin/product/ProductDetailDialog.vue')
  const contractsSource = readText('src/views/showroom-admin/product/contracts.ts')

  for (const token of [
    'ShowroomAdminApi.getProduct',
    'ShowroomAdminApi.saveProductDraft',
    'ShowroomAdminApi.submitProduct',
    'productId',
    'productCode',
    'currentRevisionId',
    'revisionId',
    'revisionNo',
    'status',
    'nameCn',
    'nameEn',
    'fields',
    'relatedProductIds',
    'discussionSummary',
    'narrations',
    'owner_company_id',
    'product_owner_type',
    'lifecycle_stage',
    'target_market',
    'pipeline_layout',
    'indication_content',
    'core_selling_points',
    'model_specification',
    'cover_image',
    'registration_certificate',
    'clinical_effect',
    'fim_status'
  ]) {
    assert.match(`${source}\n${contractsSource}`, new RegExp(token))
  }

  assert.match(source, /注册证/)
  assert.match(source, /临床效果/)
  assert.match(source, /FIM状态/)
  assert.doesNotMatch(source, /<el-form-item label="中文名称"/)
  assert.doesNotMatch(source, /<el-form-item label="英文名称"/)
  assert.doesNotMatch(source, /<el-form-item label="产品归属\/类型"/)
  assert.doesNotMatch(source, /<el-form-item label="目标市场"/)
  assert.match(source, /保存草稿/)
  assert.match(source, /提交审批/)
  assert.doesNotMatch(source, /mock/i)
  assert.doesNotMatch(source, /demo/i)
  assert.doesNotMatch(source, /fallback/i)
})

test('product history drawer renders grouped revision diff metadata', () => {
  const source = readText('src/views/showroom-admin/product/ProductHistoryDrawer.vue')

  for (const token of [
    'ShowroomAdminApi.getProductHistory',
    'revisionId',
    'revisionNo',
    'status',
    'diffItems',
    'fieldCode',
    'label',
    'oldValue',
    'newValue',
    'operatorId',
    'operatorAction',
    'createdAt'
  ]) {
    assert.match(source, new RegExp(token))
  }

  assert.match(source, /版本历史/)
  assert.match(source, /差异/)
  assert.doesNotMatch(source, /flat/i)
  assert.doesNotMatch(source, /mock/i)
})

test('product workbench links detail and history entry actions', () => {
  const source = readText('src/views/showroom-admin/product/ProductWorkbench.vue')

  assert.match(source, /ProductDetailDialog/)
  assert.match(source, /ProductHistoryDrawer/)
  assert.match(source, /productId/)
  assert.match(source, /编辑详情/)
  assert.match(source, /查看历史/)
  assert.doesNotMatch(source, /mock/i)
})

test('showroom admin hall workbench artifacts exist', () => {
  for (const relativePath of [
    'src/views/showroom-admin/hall/index.ts',
    'src/views/showroom-admin/hall/HallEditorDialog.vue',
    'src/views/showroom-admin/hall/HallWorkbench.vue',
    'src/views/showroom-admin/components/HallProductMappingDialog.vue'
  ]) {
    assert.ok(exists(relativePath), `${relativePath} must exist`)
  }
})

test('hall editor and mapping dialog use the real hall mapping contract', () => {
  const hallEditorSource = readText('src/views/showroom-admin/hall/HallEditorDialog.vue')
  const mappingSource = readText('src/views/showroom-admin/components/HallProductMappingDialog.vue')
  const contractsSource = readText('src/views/showroom-admin/hall/contracts.ts')

  for (const token of [
    'ShowroomAdminApi.createHall',
    'ShowroomAdminApi.updateHall',
    'hallId',
    'hallCode',
    'name',
    'description'
  ]) {
    assert.match(hallEditorSource, new RegExp(token))
  }

  for (const token of [
    'ShowroomAdminApi.updateHallProductMapping',
    'hallId',
    'productMappings',
    'productId',
    'productCode',
    'mapping'
  ]) {
    assert.match(`${mappingSource}\n${contractsSource}`, new RegExp(token))
  }

  assert.match(contractsSource, /displayOrder/)
  assert.match(mappingSource, /维护展项/)
  assert.match(mappingSource, /选择当前展柜包含的展项|已选展项/)
  assert.match(mappingSource, /multiple/)
  assert.match(mappingSource, /ShowroomAdminApi\.getHallItemOptions/)
  assert.doesNotMatch(mappingSource, /ShowroomAdminApi\.getProductPage/)
  assert.doesNotMatch(mappingSource, /productPage\.total/)
  assert.doesNotMatch(mappingSource, /productPage\.list/)
  assert.doesNotMatch(mappingSource, /pageNo/)
  assert.doesNotMatch(mappingSource, /pageSize:\s*20/)
  assert.match(hallEditorSource, /新增展柜|编辑展柜/)
  assert.doesNotMatch(mappingSource, /<el-table-column label="displayOrder"/)
  assert.doesNotMatch(mappingSource, /el-input-number/)
  assert.doesNotMatch(mappingSource, /mock/i)
  assert.doesNotMatch(mappingSource, /fallback/i)
})

test('hall editors submit backend-required bilingual hall fields', () => {
  const workspaceSource = readText('src/views/showroom-admin/index.vue')
  const hallEditorSource = readText('src/views/showroom-admin/hall/HallEditorDialog.vue')
  const contractsSource = readText('src/views/showroom-admin/hall/contracts.ts')

  assert.match(workspaceSource, /<el-form-item label="英文名称" required>/)
  assert.match(workspaceSource, /v-model="hallForm\.nameEn"/)
  assert.match(workspaceSource, /<el-form-item label="英文描述">/)
  assert.match(workspaceSource, /v-model="hallForm\.descriptionEn"/)
  assert.match(workspaceSource, /nameEn:\s*hallForm\.nameEn\.trim\(\)/)
  assert.match(workspaceSource, /descriptionEn:\s*hallForm\.descriptionEn\.trim\(\)/)
  assert.match(workspaceSource, /!payload\.hallCode \|\| !payload\.name \|\| !payload\.nameEn/)
  assert.match(workspaceSource, /nameEn:\s*payload\.nameEn/)
  assert.match(workspaceSource, /descriptionEn:\s*payload\.descriptionEn/)
  assert.doesNotMatch(workspaceSource, /nameEn:\s*payload\.name\b/)

  assert.match(hallEditorSource, /<el-form-item label="英文名称">/)
  assert.match(hallEditorSource, /v-model="form\.nameEn"/)
  assert.match(hallEditorSource, /<el-form-item label="英文描述">/)
  assert.match(hallEditorSource, /v-model="form\.descriptionEn"/)

  assert.match(contractsSource, /nameEn:\s*string/)
  assert.match(contractsSource, /descriptionEn:\s*string/)
  assert.match(contractsSource, /nameEn:\s*expectString\(record\.nameEn, 'nameEn'\)/)
  assert.match(contractsSource, /descriptionEn:\s*expectString\(record\.descriptionEn, 'descriptionEn', true\)/)
  assert.match(contractsSource, /nameEn:\s*form\.nameEn\.trim\(\)/)
  assert.match(contractsSource, /descriptionEn:\s*form\.descriptionEn\.trim\(\)/)
  assert.match(contractsSource, /!payload\.hallCode \|\| !payload\.name \|\| !payload\.nameEn/)
  assert.match(contractsSource, /nameEn:\s*payload\.nameEn/)
  assert.match(contractsSource, /descriptionEn:\s*payload\.descriptionEn/)
  assert.doesNotMatch(contractsSource, /nameEn:\s*payload\.name\b/)
})

test('hall workbench links editor and mapping entry actions', () => {
  const source = readText('src/views/showroom-admin/hall/HallWorkbench.vue')

  assert.match(source, /HallEditorDialog/)
  assert.match(source, /HallProductMappingDialog/)
  assert.match(source, /hall/)
  assert.match(source, /products/)
  assert.match(source, /编辑展柜/)
  assert.match(source, /维护展项/)
  assert.doesNotMatch(source, /mock/i)
})

test('hall list uses item wording for mixed product and award mappings', () => {
  const source = readText('src/views/showroom-admin/components/HallListTable.vue')

  assert.match(source, /展项数量/)
  assert.match(source, /维护展项/)
  assert.match(source, /itemMappings/)
  assert.match(source, /productCount:\s*productMappings\.length/)
  assert.doesNotMatch(source, /产品数量/)
  assert.doesNotMatch(source, /维护产品/)
})
