import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

test('showroom admin company workbench artifacts exist', () => {
  for (const relativePath of [
    'src/views/showroom-admin/company/index.ts',
    'src/views/showroom-admin/company/contracts.ts',
    'src/views/showroom-admin/company/CompanyProfileForm.vue',
    'src/views/showroom-admin/company/CompanyWorkbench.vue'
  ]) {
    assert.ok(exists(relativePath), `${relativePath} must exist`)
  }
})

test('company workbench uses the real company current and save contracts without publicity-role gating', () => {
  const source = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')
  const contractsSource = readText('src/views/showroom-admin/company/contracts.ts')
  const apiSource = readText('src/api/showroom-admin/index.ts')
  const publishBlock =
    source.match(/const handlePublish = async \(\) => \{[\s\S]*?\n\}/)?.[0] || ''

  for (const token of [
    'ShowroomAdminApi.getCompanyCurrent',
    'ShowroomAdminApi.publishCompany',
    'ShowroomAdminApi.translateCompanyFieldsToEn',
    'ShowroomAdminApi.generateCompanyNarrationAudio',
    'ShowroomAdminApi.publishCompanyNarration',
    'status',
    'fields',
    'displayName',
    'displayNameEn',
    'live',
    'companyFieldDefinitions',
    'fetchDisplayNarrationByLanguage'
  ]) {
    assert.match(source, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  for (const token of [
    'normalizeCompanyCurrent',
    'createCompanyDraftForm',
    'buildCompanyDraftPayload',
    'hasCompanyDraftChanges'
  ]) {
    assert.match(`${source}\n${contractsSource}`, new RegExp(token))
  }

  assert.match(source, /公司信息/)
  assert.match(source, /编辑公司/)
  assert.match(source, /保存/)
  assert.match(source, /当前版本/)
  assert.match(source, /语音介绍/)
  assert.match(source, /生成中文音频/)
  assert.match(source, /Generate English Audio/)
  assert.match(source, /handleTranslateCompanyFields/)
  assert.match(source, /translatingCompanyFields/)
  assert.match(source, /activeDisplayCompanyName/)
  assert.match(source, /activeDisplayCompanyNameClass/)
  assert.match(source, /displayLanguageTabs/)
  assert.match(source, /activeDisplayLanguageTab/)
  assert.match(source, /editorLanguageTabs/)
  assert.match(source, /activeEditorLanguageTab/)
  assert.match(source, /Company Content/)
  assert.match(source, /Narration/)
  assert.match(source, /AI生成中文介绍/)
  assert.match(source, /中文 tab 只负责中文语音介绍文本和中文音频/)
  assert.match(
    source,
    /The English tab only manages the English narration text and English audio/
  )
  assert.match(source, /englishTranslationStale/)
  assert.match(source, /zhNarrationDraft/)
  assert.match(source, /enNarrationDraft/)
  assert.match(source, /introTextZh/)
  assert.match(source, /<audio/)
  assert.match(source, /controls/)
  assert.match(source, /已发布/)
  assert.match(source, /el-dialog/)
  assert.match(source, /hasCompanyDraftChanges\(current\.value, form\.value\)\s*\|\|\s*canSaveNarration\.value/)
  assert.match(publishBlock, /publishCompanyNarration|publishNarrationDraftIfNeeded/)
  assert.doesNotMatch(`${source}\n${contractsSource}`, /showroom_publicity|SHOWROOM_COMPANY_EDITOR_ROLE/)
  assert.doesNotMatch(source, /保存语音/)
  assert.doesNotMatch(source, /确认无误再保存语音/)
  assert.doesNotMatch(source, /播放中文/)
  assert.doesNotMatch(source, /播放英文/)
  assert.doesNotMatch(source, /playAudio/)
  assert.doesNotMatch(source, /直接发布/)
  assert.doesNotMatch(source, /企宣角色/)
  assert.doesNotMatch(source, /Revision ID/)
  assert.doesNotMatch(source, /结构化字段差异预览/)
  assert.doesNotMatch(source, /提交审批/)
  assert.doesNotMatch(source, /待提交字段/)
  assert.doesNotMatch(source, /当前版本与历史版本直接在公司信息页可见/)
  assert.doesNotMatch(source, /查看版本/)
  assert.doesNotMatch(source, /复制为最新版本/)
  assert.doesNotMatch(source, /historyPreviewVisible/)
  assert.doesNotMatch(source, /mock/i)
  assert.doesNotMatch(source, /fallback/i)
  assert.match(apiSource, /translateCompanyFieldsToEn/)
})

test('company profile form emits updates instead of mutating props directly', () => {
  const source = readText('src/views/showroom-admin/company/CompanyProfileForm.vue')
  const workbenchSource = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')
  const contractsSource = readText('src/views/showroom-admin/company/contracts.ts')

  assert.match(source, /defineEmits/)
  assert.match(source, /update:form/)
  assert.match(source, /translate-english/)
  assert.match(source, /language: 'zh' \| 'en'/)
  assert.match(source, /@update:model-value/)
  assert.match(source, /const props = defineProps/)
  assert.match(source, /displayNameEn/)
  assert.match(source, /definition\.labelEn/)
  assert.match(source, /resolveCompanyEnglishFieldKey/)
  assert.match(source, /updateDisplayNameEn/)
  assert.match(source, /updateEnglishField/)
  assert.match(source, /Translate English Content/)
  assert.match(source, /Company Name/)
  assert.doesNotMatch(source, /v-model="form\./)
  assert.doesNotMatch(source, /<el-tabs/)
  assert.doesNotMatch(source, /<el-tab-pane/)
  assert.match(workbenchSource, /@update:form="handleFormChange"/)
  assert.match(workbenchSource, /@translate-english="handleTranslateCompanyFields"/)
  assert.match(workbenchSource, /const handleFormChange = \(nextForm: CompanyDraftForm\)/)
  assert.match(contractsSource, /labelEn/)
  assert.match(contractsSource, /resolveCompanyEnglishFieldKey/)
  assert.match(contractsSource, /_en/)
})

test('showroom admin history workbench artifacts exist', () => {
  for (const relativePath of [
    'src/views/showroom-admin/history/index.ts',
    'src/views/showroom-admin/history/contracts.ts',
    'src/views/showroom-admin/history/VersionDiffDrawer.vue',
    'src/views/showroom-admin/history/CompanyHistoryWorkbench.vue'
  ]) {
    assert.ok(exists(relativePath), `${relativePath} must exist`)
  }
})

test('company version workbench artifacts exist', () => {
  for (const relativePath of [
    'src/views/showroom-admin/company-version/index.ts',
    'src/views/showroom-admin/company-version/CompanyVersionWorkbench.vue'
  ]) {
    assert.ok(exists(relativePath), `${relativePath} must exist`)
  }
})

test('company version workbench uses the real company history contract', () => {
  const source = readText('src/views/showroom-admin/company-version/CompanyVersionWorkbench.vue')

  for (const token of [
    'ShowroomAdminApi.getCompanyCurrent',
    'ShowroomAdminApi.getCompanyHistory',
    'ShowroomAdminApi.getCompany(',
    'ShowroomAdminApi.restoreCompanyRevision',
    'revisionId',
    'revisionNo',
    'status',
    'diffItems'
  ]) {
    assert.match(source, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  for (const copyToken of ['公司版本', '版本历史', '查看版本', '复制为最新版本', '历史版本详情']) {
    assert.match(source, new RegExp(copyToken))
  }

  assert.doesNotMatch(source, /mock/i)
  assert.doesNotMatch(source, /fallback/i)
})

test('company history workbench uses the real company history contract', () => {
  const source = readText('src/views/showroom-admin/history/CompanyHistoryWorkbench.vue')
  const drawerSource = readText('src/views/showroom-admin/history/VersionDiffDrawer.vue')
  const contractsSource = readText('src/views/showroom-admin/history/contracts.ts')

  for (const token of [
    'ShowroomAdminApi.getCompanyCurrent',
    'ShowroomAdminApi.getCompanyHistory',
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

  for (const token of [
    'normalizeCompanyHistory',
    'resolveCompanyHistoryStatusText',
    'resolveCompanyHistoryStatusTagType',
    'expectNullableString',
    'expectStringish'
  ]) {
    assert.match(contractsSource, new RegExp(token))
  }

  assert.match(source, /版本历史/)
  assert.match(source, /查看差异/)
  assert.match(drawerSource, /字段差异/)
  assert.doesNotMatch(source, /mock/i)
  assert.doesNotMatch(source, /fallback/i)
})

test('showroom admin dashboard workbench artifacts exist', () => {
  for (const relativePath of [
    'src/views/showroom-admin/dashboard/index.ts',
    'src/views/showroom-admin/dashboard/contracts.ts',
    'src/views/showroom-admin/dashboard/ShowroomDashboardCards.vue',
    'src/views/showroom-admin/dashboard/ShowroomDashboardWorkbench.vue'
  ]) {
    assert.ok(exists(relativePath), `${relativePath} must exist`)
  }
})

test('dashboard workbench uses real paged contracts and exposes stale-audio blocker explicitly', () => {
  const source = readText('src/views/showroom-admin/dashboard/ShowroomDashboardWorkbench.vue')
  const contractsSource = readText('src/views/showroom-admin/dashboard/contracts.ts')

  for (const token of [
    'ShowroomAdminApi.getHallPage',
    'ShowroomAdminApi.getProductPage',
    'ShowroomAdminApi.getApprovalPage',
    "request.get({ url: '/showroom/assignment/page'",
    'pageNo',
    'pageSize',
    'fetchPagedTotal',
    'pendingAssignmentCount',
    'incompleteProductCount',
    'pendingApprovalCount',
    'liveHallCount',
    'productCount'
  ]) {
    assert.match(source + '\n' + contractsSource, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  assert.match(source, /Dashboard/)
  assert.match(source, /讲解音频陈旧统计待后端契约补齐/)
  assert.match(source, /统计暂不可用/)
  assert.doesNotMatch(source, /mock/i)
  assert.doesNotMatch(source, /fallback/i)
})
