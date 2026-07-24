import assert from 'node:assert/strict'
import fs from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const loadTsModule = async (relativePath) => {
  const source = readText(relativePath)
  const transformed = stripTypeScriptTypes(source, { mode: 'transform' })
  const moduleUrl = `data:text/javascript;base64,${Buffer.from(transformed).toString('base64')}`
  return await import(moduleUrl)
}

test('showroom audio controls consume backend audioUrl without infra file metadata URL fallback', async () => {
  const narrationContractsSource = readText('src/views/showroom-admin/narration/contracts.ts')
  const workspaceSource = readText('src/views/showroom-admin/narration/NarrationWorkspace.vue')
  const adminSource = readText('src/views/showroom-admin/index.vue')

  assert.doesNotMatch(narrationContractsSource, /\/admin-api\/infra\/file\/get\?id=/)
  assert.match(narrationContractsSource, /audioUrl:\s*string\s*\|\s*null/)
  assert.match(narrationContractsSource, /resolveNarrationAudioUrl/)
  assert.match(workspaceSource, /resolveNarrationAudioUrl\(currentVersion\.value\)/)
  assert.match(adminSource, /zhAudioUrl:\s*string/)
  assert.match(adminSource, /enAudioUrl:\s*string/)
  assert.match(adminSource, /zhAudioUrl:\s*''/)
  assert.match(adminSource, /enAudioUrl:\s*''/)
  assert.match(adminSource, /productNarrationDraft\.zhAudioUrl = version\.audioUrl \|\| ''/)
  assert.match(adminSource, /productNarrationDraft\.enAudioUrl = version\.audioUrl \|\| ''/)
  assert.match(adminSource, /draftProductZhAudioUrl = computed\(\(\) => productNarrationDraft\.zhAudioUrl\)/)
  assert.doesNotMatch(adminSource, /draftProductZhAudioUrl = computed\(\(\) => buildAudioUrl/)

  const { normalizeNarrationVersion, resolveNarrationAudioUrl } = await loadTsModule(
    'src/views/showroom-admin/narration/contracts.ts'
  )
  const version = normalizeNarrationVersion({
    id: 7,
    key: { targetType: 'PRODUCT', targetId: 11, audienceType: 'PUBLIC', language: 'ZH' },
    sourceRevisionId: 101,
    versionNo: 3,
    scriptText: '中文讲解',
    audioFileId: 9001,
    audioUrl: '/showroom/assets/product-audio-zh/hash-1',
    audioDurationSeconds: 12,
    voice: 'zh-voice',
    generationStatus: 'AUDIO_GENERATED',
    status: 'DRAFT',
    generatedByAi: false,
    generatedAt: null,
    publishedAt: null,
    live: false
  })
  assert.equal(resolveNarrationAudioUrl(version), '/showroom/assets/product-audio-zh/hash-1')
  assert.throws(
    () =>
      resolveNarrationAudioUrl({
        ...version,
        audioUrl: ''
      }),
    /缺少真实 audioUrl/
  )
})

test('product cover generation is a draft-only form fill and field publish is not blocked by audio', () => {
  const adminSource = readText('src/views/showroom-admin/index.vue')

  assert.match(adminSource, /requireGeneratedCoverImageUrl/)
  assert.match(adminSource, /AI封面已生成，已回填表单，尚未保存草稿或发布/)
  assert.doesNotMatch(adminSource, /syncGeneratedProductCoverBaseline\(productForm\.coverImage\)/)
  assert.match(adminSource, /保存草稿/)
  assert.match(adminSource, /materialBlockers/)
  assert.doesNotMatch(adminSource, /const narrationPair = await loadCurrentRevisionProductNarrationPair\(productDetail\)/)
})

test('release publish requires explicit site and stage scope before calling backend', () => {
  const apiSource = readText('src/api/showroom-admin/index.ts')
  const companySource = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')

  assert.match(apiSource, /interface ShowroomReleasePublishReqVO/)
  assert.match(apiSource, /siteKey:\s*string/)
  assert.match(apiSource, /stage:\s*'TEST'\s*\|\s*'PROD'/)
  assert.match(apiSource, /publishRelease:\s*async\s*\(data:\s*ShowroomReleasePublishReqVO\)/)
  assert.match(companySource, /releaseScope\.siteKey/)
  assert.match(companySource, /releaseScope\.stage/)
  assert.match(companySource, /stage:\s*'TEST'/)
  assert.match(companySource, /buildReleasePublishPayload/)
  assert.match(companySource, /展厅发布缺少 scope/)
  assert.match(companySource, /ShowroomAdminApi\.publishRelease\(buildReleasePublishPayload\(\)\)/)
})

test('company save keeps backend-current publish before narration publish and exposes blockers', () => {
  const companySource = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')

  assert.match(companySource, /showroom-company-workbench__error-panel/)
  assert.match(companySource, /releasePublishError/)
  assert.match(companySource, /formatShowroomStructuredError\(error,\s*'展厅发布'\)/)
  assert.match(companySource, /formatShowroomStructuredError\(error,\s*'公司保存'\)/)
  assert.doesNotMatch(companySource, /公司语音已保存，但公司信息保存失败/)

  const publishCompanyIndex = companySource.indexOf('ShowroomAdminApi.publishCompany(')
  const publishNarrationIndex = companySource.indexOf('publishNarrationDraftIfNeeded()')
  assert.ok(publishCompanyIndex >= 0, 'company publish call must exist')
  assert.ok(publishNarrationIndex >= 0, 'narration publish call must exist')
  assert.ok(
    publishCompanyIndex < publishNarrationIndex,
    'company current publish must happen before narration publish to avoid saved-audio/failed-company partial success'
  )
})

test('product and company AI translation do not overwrite Chinese source text', () => {
  const adminSource = readText('src/views/showroom-admin/index.vue')
  const companySource = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')

  assert.match(adminSource, /translateProductFieldsToEn/)
  assert.match(adminSource, /productForm\.nameEn =/)
  assert.doesNotMatch(adminSource, /productForm\.nameCn = resolveStringValue\(translation/)
  assert.match(adminSource, /英文内容已翻译，可继续微调后再生成语音/)

  assert.match(companySource, /translateCompanyFieldsToEn/)
  assert.doesNotMatch(companySource, /\.fields,\s*\.\.\.buildCompanyTranslationSourceFields/)
  assert.match(companySource, /英文卡片和英文介绍已按当前中文内容回填，可继续手工修改/)
})

test('structured showroom errors expose target language resource ids and backend code', async () => {
  const sharedSource = readText('src/views/showroom-admin/shared/structuredError.ts')
  const companySource = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')
  const adminSource = readText('src/views/showroom-admin/index.vue')
  const axiosSource = readText('src/config/axios/service.ts')

  assert.match(sharedSource, /formatShowroomStructuredError/)
  assert.match(sharedSource, /backendErrorCode/)
  assert.match(sharedSource, /missingFields/)
  assert.match(sharedSource, /contentHash/)
  assert.match(companySource, /showroom-company-workbench__error-panel/)
  assert.match(companySource, /formatShowroomStructuredError/)
  assert.match(adminSource, /formatShowroomStructuredError/)
  assert.match(axiosSource, /details/)
  assert.match(axiosSource, /backendErrorCode/)

  const { formatShowroomStructuredError } = await loadTsModule(
    'src/views/showroom-admin/shared/structuredError.ts'
  )
  const formatted = formatShowroomStructuredError(
    {
      message: 'SHOWROOM_RELEASE_MATERIAL_MISSING: company ZH narration audio is required',
      details: {
        backendErrorCode: 'SHOWROOM_RELEASE_MATERIAL_MISSING',
        operation: 'release.publish',
        targetType: 'COMPANY',
        targetId: 1,
        targetCode: 'MAIN',
        revisionId: 10,
        language: 'ZH',
        missingFields: ['audioFileId'],
        fileId: null,
        assetId: 'company-audio-zh',
        contentHash: null,
        endpoint: '/admin-api/showroom/release/publish'
      }
    },
    '展厅发布'
  )
  assert.match(formatted, /展厅发布失败：SHOWROOM_RELEASE_MATERIAL_MISSING/)
  assert.match(formatted, /目标：COMPANY #1 MAIN，版本：10，语言：ZH/)
  assert.match(formatted, /缺失字段：audioFileId/)
  assert.match(formatted, /assetId=company-audio-zh/)
  assert.match(formatted, /contentHash=未生成/)
})

test('version center renders field diffs with material blockers instead of whole-page material failure', async () => {
  const contractsSource = readText('src/views/showroom-admin/version-center/contracts.ts')
  const historySource = readText('src/views/showroom-admin/version-center/VersionHistoryList.vue')
  const diffSource = readText('src/views/showroom-admin/version-center/VersionDiffPanel.vue')
  const pageSource = readText('src/views/showroom-admin/version-center/VersionCenterPage.vue')

  assert.match(contractsSource, /blockers:\s*VersionCenterBlocker\[\]/)
  assert.match(contractsSource, /targetType\?:\s*VersionCenterTargetType/)
  assert.match(contractsSource, /language\?:\s*'ZH'\s*\|\s*'EN'/)
  assert.match(contractsSource, /fileId\?:\s*number\s*\|\s*null/)
  assert.match(contractsSource, /assetId\?:\s*string\s*\|\s*null/)
  assert.match(contractsSource, /contentHash\?:\s*string\s*\|\s*null/)
  assert.match(historySource, /item\.blockers/)
  assert.match(historySource, /formatVersionCenterBlocker/)
  assert.match(diffSource, /formatVersionCenterBlocker/)
  assert.match(pageSource, /:republish-readiness="detailData\?\.republishReadiness \|\| emptyReadiness"/)

  const {
    normalizeVersionCenterHistoryResponse,
    normalizeVersionCenterDetailResponse,
    formatVersionCenterBlocker
  } = await loadTsModule('src/views/showroom-admin/version-center/contracts.ts')

  const history = normalizeVersionCenterHistoryResponse({
    targetType: 'PRODUCT',
    targetId: 22,
    currentContentRevisionId: 103,
    currentPublicRevisionId: 102,
    currentReleaseId: 'release-9',
    items: [
      {
        revisionId: 102,
        revisionNo: 2,
        publishedAt: '2026-05-23T11:00:00Z',
        publishedBy: 9002,
        copiedFromRevisionId: 101,
        currentContent: false,
        currentPublic: true,
        selectable: true,
        previewSummaryImageUrl: null,
        diffSummary: ['英文卖点更新'],
        blockers: [
          {
            blockerCode: 'PRODUCT_NARRATION_ZH_MISSING',
            message: '缺少中文音频',
            affectedRevisionIds: [102],
            scope: 'SELECTED_VERSION',
            targetType: 'PRODUCT',
            targetId: 22,
            language: 'ZH',
            missingFields: ['audioFileId'],
            fileId: null,
            assetId: 'product-audio-zh',
            contentHash: null
          }
        ]
      }
    ]
  })
  assert.equal(history.items[0].blockers[0].language, 'ZH')
  assert.match(formatVersionCenterBlocker(history.items[0].blockers[0]), /PRODUCT #22/)
  assert.match(formatVersionCenterBlocker(history.items[0].blockers[0]), /language=ZH/)

  const detail = normalizeVersionCenterDetailResponse({
    targetSummary: {
      targetType: 'PRODUCT',
      targetId: 22,
      title: '导管鞘',
      titleEn: 'Sheath',
      currentContentRevisionId: 103,
      currentPublicRevisionId: 102
    },
    selectedVersion: {
      revisionId: 102,
      revisionNo: 2,
      publishedAt: '2026-05-23T11:00:00Z',
      publishedBy: 9002,
      copiedFromRevisionId: 101,
      currentContent: false,
      currentPublic: true,
      title: '导管鞘',
      titleEn: 'Sheath',
      companyType: null,
      fields: [],
      image: {
        contentImage: {
          source: 'PRODUCT_REVISION_COVER_IMAGE',
          url: null,
          alt: null,
          versionId: null,
          fileId: null
        },
        releasePreviewAsset: null
      },
      narrations: []
    },
    currentContentVersion: null,
    currentPublicVersion: null,
    currentRelease: null,
    fieldDiffs: [
      {
        fieldCode: 'cover_image',
        label: '封面',
        labelEn: 'Cover Image',
        order: 10,
        selectedValueZh: null,
        selectedValueEn: null,
        currentContentValueZh: '/showroom/assets/new-cover/hash',
        currentContentValueEn: null,
        changed: true
      }
    ],
    permissions: { canRepublish: false, republishDisabledReason: '缺物料' },
    republishReadiness: {
      ready: false,
      blockers: [
        {
          blockerCode: 'PRODUCT_COVER_MISSING',
          message: '缺少封面',
          affectedRevisionIds: [102],
          scope: 'SELECTED_VERSION',
          targetType: 'PRODUCT',
          targetId: 22,
          missingFields: ['cover_image'],
          assetId: 'product-cover',
          contentHash: null
        }
      ]
    }
  })
  assert.equal(detail.fieldDiffs[0].fieldCode, 'cover_image')
  assert.equal(detail.republishReadiness.blockers[0].missingFields?.[0], 'cover_image')
})

test('hall mapping fail-fast keeps at least one product and uses mapping endpoint contract', () => {
  const dialogSource = readText('src/views/showroom-admin/components/HallProductMappingDialog.vue')
  const contractsSource = readText('src/views/showroom-admin/hall/contracts.ts')
  const apiSource = readText('src/api/showroom-admin/index.ts')

  assert.match(contractsSource, /至少需要一条产品映射/)
  assert.match(dialogSource, /selectedProductIds\.length <= 1/)
  assert.match(dialogSource, /至少保留一条产品映射/)
  assert.match(dialogSource, /ShowroomAdminApi\.updateHallProductMapping\(payload\)/)
  assert.match(apiSource, /url: '\/showroom\/hall\/update-product-mapping'/)
})
