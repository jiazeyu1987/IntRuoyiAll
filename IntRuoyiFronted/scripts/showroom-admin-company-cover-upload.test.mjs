import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('company contracts preserve cover image and displayNameEn through current state and save payload', () => {
  const source = readText('src/views/showroom-admin/company/contracts.ts')

  assert.match(source, /displayNameEn: string/)
  assert.match(source, /coverImage: string/)
  assert.match(source, /displayNameEn: expectString\(record\.displayNameEn/)
  assert.match(source, /coverImage:\s*resolveOptionalString\(fieldsRecord\.cover_image\)/)
  assert.match(source, /displayNameEn:\s*current\.displayNameEn/)
  assert.match(source, /coverImage:\s*current\.coverImage/)
  assert.match(source, /displayNameEn:\s*normalizeCompanyFieldValue\(form\.displayNameEn\)/)
  assert.match(source, /\['cover_image',\s*normalizeCompanyFieldValue\(form\.coverImage\)\]/)
  assert.match(source, /normalizeCompanyFieldValue\(form\.coverImage\)\s*!==\s*normalizeCompanyFieldValue\(current\.coverImage\)/)
})

test('company profile form exposes cover uploader without mutating props directly', () => {
  const source = readText('src/views/showroom-admin/company/CompanyProfileForm.vue')

  assert.match(source, /公司封面/)
  assert.match(source, /UploadImg/)
  assert.match(source, /@update:model-value="updateCoverImage"/)
  assert.match(source, /const updateCoverImage = \(value: string\)/)
  assert.doesNotMatch(source, /v-model="form\.coverImage"/)
})

test('company workbench renders saved cover preview and explicit empty state', () => {
  const source = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')

  assert.match(source, /公司封面/)
  assert.match(source, /current\.coverImage/)
  assert.match(source, /未上传封面/)
  assert.match(source, /preview-src-list/)
  assert.match(source, /companyCoverPreviewList/)
})
