import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.resolve(root, relativePath), 'utf8')

test('MDM product master frontend contract is wired through API, page, DCC, and showroom', () => {
  const apiSource = readText('src/api/mdm/product/index.ts')
  const pageSource = readText('src/views/mdm/product/index.vue')
  const workflowSource = readText('src/api/dcc/controlledFile/workflow.ts')
  const uploadSource = readText('src/views/dcc/controlled-file/upload/index.vue')
  const metadataSource = readText('src/views/dcc/controlled-file/shared/ControlledFileMetadataDialog.vue')
  const showroomApiSource = readText('src/api/showroom-admin/index.ts')
  const showroomProductContractsSource = readText('src/views/showroom-admin/product/contracts.ts')
  const showroomHallContractsSource = readText('src/views/showroom-admin/hall/contracts.ts')

  for (const endpoint of [
    '/mdm/product/page',
    '/mdm/product/simple-list',
    '/mdm/product/import-preview',
    '/mdm/product/import-confirm',
    '/mdm/product/references',
    '/showroom/product/mdm-mapping-preview',
    '/showroom/product/mdm-mapping-confirm'
  ]) {
    assert.match(apiSource, new RegExp(endpoint.replaceAll('/', '\\/')))
  }

  for (const token of [
    '基础数据',
    '产品主数据',
    'mdm:product:create',
    'mdm:product:map-showroom',
    'handleImportPreview',
    'handleImportConfirm',
    'handleReferences',
    'handleShowroomMappingPreview',
    'handleShowroomMappingConfirm',
    '展厅映射'
  ]) {
    assert.match(pageSource, new RegExp(token))
  }

  assert.match(workflowSource, /productMasterId:\s*number/)
  assert.match(workflowSource, /assertRequiredNumber\(payload,\s*'productMasterId'/)
  assert.match(workflowSource, /\/dcc\/controlled-files\/product-options/)
  assert.match(uploadSource, /getDccProductOptions/)
  assert.doesNotMatch(uploadSource, /getProductSimpleList/)
  assert.match(uploadSource, /productMasterId/)
  assert.match(metadataSource, /getDccProductOptions/)
  assert.doesNotMatch(metadataSource, /getProductSimpleList/)
  assert.match(fs.readFileSync(path.resolve(root, 'src/views/dcc/controlled-file/external-review/index.vue'), 'utf8'), /getDccProductOptions/)
  assert.match(metadataSource, /productMasterId/)
  assert.match(showroomApiSource, /productMasterId\?:\s*number/)
  assert.match(showroomProductContractsSource, /productMasterId:\s*number\s*\|\s*null/)
  assert.match(showroomHallContractsSource, /productMasterId:\s*number\s*\|\s*null/)
})
