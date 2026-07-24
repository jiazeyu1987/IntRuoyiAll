import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('company workbench hides the direct version center entry from the header', () => {
  const source = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')

  assert.doesNotMatch(source, /进入版本中心/)
  assert.doesNotMatch(source, /handleOpenVersionCenter/)
})

test('version center page resolves back navigation and refreshes selection from revision query', () => {
  const source = readText('src/views/showroom-admin/version-center/VersionCenterPage.vue')

  assert.match(source, /resolveRequestedRevisionId/)
  assert.match(source, /resolvePreferredHistoryRevisionId/)
  assert.match(source, /syncRevisionQuery/)
  assert.match(source, /ShowroomAdminCompany/)
  assert.match(source, /ShowroomAdminProduct/)
  assert.match(source, /revisionId: String\(revisionId\)/)
  assert.match(source, /watch\(/)
  assert.doesNotMatch(source, /await loadHistory\(\)/)
})

test('version center page clears stale detail and blocks republish interactions while switching revisions', () => {
  const pageSource = readText('src/views/showroom-admin/version-center/VersionCenterPage.vue')
  const diffSource = readText('src/views/showroom-admin/version-center/VersionDiffPanel.vue')

  assert.match(pageSource, /detailData\.value = null/)
  assert.match(pageSource, /detailLoading\.value = true/)
  assert.match(pageSource, /republishDialogVisible\.value = false/)
  assert.match(pageSource, /selectedRevisionId\.value = revisionId/)
  assert.match(
    pageSource,
    /historyLoading \|\| detailLoading \|\| !detailData\?\.selectedVersion/
  )
  assert.match(diffSource, /interactionsDisabled: boolean/)
  assert.match(diffSource, /版本切换中，当前暂不可执行发布操作/)
})

test('republish dialog and diff panel expose explicit blocker and global release messaging', () => {
  const diffSource = readText('src/views/showroom-admin/version-center/VersionDiffPanel.vue')
  const dialogSource = readText('src/views/showroom-admin/version-center/RepublishConfirmDialog.vue')

  assert.match(diffSource, /blocker\.scope/)
  assert.match(diffSource, /一步到位发布/)
  assert.match(diffSource, /全局 showroom release 重建/)
  assert.match(diffSource, /当前线上摘要/)
  assert.match(dialogSource, /这会复制所选历史版本为新的 published revision/)
  assert.match(dialogSource, /全局 showroom release 重建/)
  assert.match(dialogSource, /scope/)
  assert.match(dialogSource, /errorMessage/)
  assert.match(dialogSource, /GLOBAL_RELEASE/)
})

test('index page closes product detail before routing from dialog into version center', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /handleOpenProductVersionCenterFromDetail/)
  assert.match(source, /closeProductDetailDialog\(\)/)
  assert.match(source, /name: 'ShowroomAdminProductVersionCenter'/)
  assert.match(source, /query: \{ revisionId: String\(payload\.revisionId\) \}/)
})

test('product version center entry fails fast when display revision id is missing', () => {
  const listSource = readText('src/views/showroom-admin/components/ProductListTable.vue')
  const indexSource = readText('src/views/showroom-admin/index.vue')
  const versionCenterFn =
    indexSource.match(
      /const openProductVersionCenter = async[\s\S]*?(?=\nconst handleOpenProductVersionCenterFromDetail = async)/
    )?.[0] || ''

  assert.match(listSource, /displayRevisionId: string/)
  assert.match(listSource, /displayRevisionId: resolveStringField\(/)
  assert.doesNotMatch(versionCenterFn, /displayRevision\?\.revisionId/)
  assert.doesNotMatch(versionCenterFn, /currentRevisionId/)
  assert.match(versionCenterFn, /normalizeId\(payload\.displayRevisionId, 'payload\.displayRevisionId'\)/)
})
