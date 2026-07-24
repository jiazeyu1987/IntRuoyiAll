import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

test('showroom admin exposes a visible company-version tab between company and product', () => {
  const source = readText('src/router/modules/showroom.ts')
  const companyIndex = source.indexOf("name: 'ShowroomAdminCompany'")
  const companyVersionIndex = source.indexOf("name: 'ShowroomAdminCompanyVersion'")
  const productIndex = source.indexOf("name: 'ShowroomAdminProduct'")

  assert.notEqual(companyIndex, -1, 'company route must exist')
  assert.notEqual(companyVersionIndex, -1, 'company-version route must exist')
  assert.notEqual(productIndex, -1, 'product route must exist')
  assert.ok(
    companyIndex < companyVersionIndex && companyVersionIndex < productIndex,
    'company-version tab must be ordered between company and product'
  )
  assert.match(source, /title: '公司版本'/)
})

test('showroom admin shell renders the dedicated company-version workbench and maps the new section', () => {
  const source = readText('src/views/showroom-admin/index.vue')

  assert.match(source, /CompanyVersionWorkbench/)
  assert.match(source, /activeSection === 'companyVersion'/)
  assert.match(source, /routeName: 'ShowroomAdminCompanyVersion'/)

  const companyIndex = source.indexOf("{ name: 'company', routeName: 'ShowroomAdminCompany' }")
  const companyVersionIndex = source.indexOf(
    "{ name: 'companyVersion', routeName: 'ShowroomAdminCompanyVersion' }"
  )
  const productIndex = source.indexOf("{ name: 'product', routeName: 'ShowroomAdminProduct' }")

  assert.ok(companyIndex !== -1 && companyVersionIndex !== -1 && productIndex !== -1)
  assert.ok(
    companyIndex < companyVersionIndex && companyVersionIndex < productIndex,
    'shell section order must keep company-version between company and product'
  )
})

test('company workbench no longer embeds the version-history card', () => {
  const source = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')

  assert.doesNotMatch(source, /当前版本与历史版本直接在公司信息页可见/)
  assert.doesNotMatch(source, /查看版本/)
  assert.doesNotMatch(source, /复制为最新版本/)
  assert.doesNotMatch(source, /historyPreviewVisible/)
  assert.doesNotMatch(source, /handleViewHistoryRevision/)
})

test('dedicated company-version workbench loads company history preview from version-center detail and renders bilingual audio', () => {
  assert.ok(
    exists('src/views/showroom-admin/company-version/CompanyVersionWorkbench.vue'),
    'company-version workbench must exist'
  )
  assert.ok(
    exists('src/views/showroom-admin/company-version/index.ts'),
    'company-version index export must exist'
  )

  const source = readText('src/views/showroom-admin/company-version/CompanyVersionWorkbench.vue')

  for (const token of [
    'ShowroomAdminApi.getCompanyCurrent',
    'ShowroomAdminApi.getCompanyHistory',
    'ShowroomAdminApi.restoreCompanyRevision',
    'getVersionCenterDetail',
    "targetType: 'COMPANY'",
    'normalizeVersionCenterDetailResponse',
    '版本历史',
    '查看版本',
    '复制为最新版本',
    '语音版本：',
    'Voice：',
    '未生成音频',
    'No audio generated',
    'preload="none"'
  ]) {
    assert.match(source, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  assert.doesNotMatch(source, /ShowroomAdminApi\.getCompany\(/)
  assert.match(source, /<audio[\s\S]*controls[\s\S]*preload="none"/)
})
