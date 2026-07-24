import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const sliceBetween = (source, startMarker, endMarker) => {
  const startIndex = source.indexOf(startMarker)
  assert.notEqual(startIndex, -1, `missing start marker: ${startMarker}`)
  const endIndex = source.indexOf(endMarker, startIndex)
  assert.notEqual(endIndex, -1, `missing end marker: ${endMarker}`)
  return source.slice(startIndex, endIndex)
}

test('showroom admin api exposes the manual release publish request', () => {
  const source = readText('src/api/showroom-admin/index.ts')
  const publishReleaseBlock = sliceBetween(
    source,
    'publishRelease: async (data: ShowroomReleasePublishReqVO): Promise<ShowroomReleasePublishRespVO> => {',
    'getCompanyHistory: async (params?: any) => {'
  )

  assert.match(source, /interface ShowroomReleasePublishRespVO/)
  assert.match(source, /interface ShowroomReleasePublishReqVO/)
  assert.match(source, /siteKey:\s*string/)
  assert.match(source, /stage:\s*'TEST'\s*\|\s*'PROD'/)
  assert.match(source, /const SHOWROOM_RELEASE_PUBLISH_REQUEST_TIMEOUT = 0/)
  assert.match(source, /publishRelease: async \(data: ShowroomReleasePublishReqVO\): Promise<ShowroomReleasePublishRespVO> =>/)
  assert.match(source, /url: '\/showroom\/release\/publish'/)
  assert.match(publishReleaseBlock, /data,/)
  assert.match(publishReleaseBlock, /timeout: SHOWROOM_RELEASE_PUBLISH_REQUEST_TIMEOUT/)
  assert.doesNotMatch(publishReleaseBlock, /timeout: 30000/)
})

test('company workbench renders the publicity-only manual release button to the right of edit company', () => {
  const companySource = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')
  const indexSource = readText('src/views/showroom-admin/index.vue')
  const publishHandler = sliceBetween(
    companySource,
    'const handlePublishShowroomRelease = async () => {',
    'const handleTranslateCompanyFields = async () => {'
  )

  assert.match(companySource, /手动发布展厅/)
  assert.match(companySource, /v-if="canPublishShowroomRelease"/)
  assert.match(companySource, /:loading="publishingShowroomRelease"/)
  assert.match(companySource, /@click="handlePublishShowroomRelease"/)
  assert.match(companySource, /const publishingShowroomRelease = ref\(false\)/)
  assert.match(companySource, /const SHOWROOM_PUBLICITY_ROLE_CODE = 'showroom_publicity'/)
  assert.match(companySource, /const SUPER_ADMIN_ROLE_CODE = 'super_admin'/)
  assert.match(companySource, /releaseScope\.siteKey/)
  assert.match(companySource, /releaseScope\.stage/)
  assert.match(companySource, /buildReleasePublishPayload/)
  assert.match(companySource, /展厅发布缺少 scope/)
  assert.match(companySource, /const canPublishShowroomRelease = computed\(\(\) =>/)
  assert.match(companySource, /roles\.includes\(SHOWROOM_PUBLICITY_ROLE_CODE\)/)
  assert.match(companySource, /roles\.includes\(SUPER_ADMIN_ROLE_CODE\)/)
  assert.match(companySource, /const handlePublishShowroomRelease = async \(\) =>/)
  assert.match(publishHandler, /await message\.confirm\('确认立即发布当前展厅内容吗？'/)
  assert.match(publishHandler, /await ShowroomAdminApi\.publishRelease\(buildReleasePublishPayload\(\)\)/)
  assert.match(publishHandler, /message\.alertSuccess\(`展厅发布成功：\$\{releaseId\}`\)/)
  assert.match(publishHandler, /formatShowroomStructuredError\(error,\s*'展厅发布'\)/)
  assert.match(publishHandler, /message\.alertError\(formatted\)/)
  assert.doesNotMatch(publishHandler, /message\.success\(`展厅已发布：\$\{releaseId\}`\)/)
  assert.doesNotMatch(publishHandler, /message\.error\(resolved\.message\)/)
  assert.ok(
    companySource.indexOf('@click="openEditDialog"') < companySource.indexOf('@click="handlePublishShowroomRelease"'),
    '手动发布展厅按钮必须位于编辑公司右侧'
  )
  assert.doesNotMatch(indexSource, /showroom-admin__toolbar/)
  assert.doesNotMatch(indexSource, /手动发布展厅/)
})
