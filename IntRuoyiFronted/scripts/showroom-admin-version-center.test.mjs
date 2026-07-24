import assert from 'node:assert/strict'
import fs from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const loadVersionCenterContracts = async () => {
  const filePath = path.join(
    root,
    'src/views/showroom-admin/version-center/contracts.ts'
  )
  const source = fs.readFileSync(filePath, 'utf8')
  const transformed = stripTypeScriptTypes(source, {
    mode: 'transform'
  })
  const moduleUrl = `data:text/javascript;base64,${Buffer.from(transformed).toString('base64')}`

  return await import(moduleUrl)
}

test('showroom version center artifacts exist', () => {
  for (const relativePath of [
    'src/views/showroom-admin/version-center/contracts.ts',
    'src/views/showroom-admin/version-center/VersionCenterPage.vue',
    'src/views/showroom-admin/version-center/VersionCenterHeader.vue',
    'src/views/showroom-admin/version-center/VersionHistoryList.vue',
    'src/views/showroom-admin/version-center/VersionSnapshotPreview.vue',
    'src/views/showroom-admin/version-center/VersionDiffPanel.vue',
    'src/views/showroom-admin/version-center/RepublishConfirmDialog.vue',
    'src/api/showroom-admin/version-center.ts'
  ]) {
    assert.ok(exists(relativePath), `${relativePath} must exist`)
  }
})

test('showroom router defines hidden static company and product version center routes', () => {
  const source = readText('src/router/modules/showroom.ts')

  assert.match(
    source,
    /VersionCenterPage\.vue|showroomVersionCenterView/
  )
  assert.match(source, /path: 'company\/version-center\/:companyId\(\\\\d\+\)'/)
  assert.match(source, /name: 'ShowroomAdminCompanyVersionCenter'/)
  assert.match(source, /activeMenu: '\/showroom\/company'/)
  assert.match(source, /versionTargetType: 'COMPANY'/)
  assert.match(source, /path: 'product\/version-center\/:productId\(\\\\d\+\)'/)
  assert.match(source, /name: 'ShowroomAdminProductVersionCenter'/)
  assert.match(source, /activeMenu: '\/showroom\/product'/)
  assert.match(source, /versionTargetType: 'PRODUCT'/)
  assert.match(source, /hidden: true/)
  assert.match(source, /noCache: true/)
})

test('product entry exposes version center action while company header keeps it hidden', () => {
  const companySource = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')
  const productListSource = readText('src/views/showroom-admin/components/ProductListTable.vue')

  assert.doesNotMatch(companySource, /进入版本中心/)
  assert.doesNotMatch(companySource, /handleOpenVersionCenter/)
  assert.match(productListSource, /版本中心/)
  assert.match(productListSource, /emit\('version-center', \{ productId: row\.productId, displayRevisionId: row\.displayRevisionId \}\)/)
  assert.match(productListSource, /'version-center': \[payload: \{ productId: string; displayRevisionId: string \}\]/)
})

test('version center page consumes the final history/detail/republish contracts explicitly', () => {
  const pageSource = readText('src/views/showroom-admin/version-center/VersionCenterPage.vue')
  const contractsSource = readText('src/views/showroom-admin/version-center/contracts.ts')
  const apiSource = readText('src/api/showroom-admin/version-center.ts')
  const diffSource = readText('src/views/showroom-admin/version-center/VersionDiffPanel.vue')

  for (const token of [
    'getVersionCenterHistory',
    'getVersionCenterDetail',
    'republishVersionCenter',
    'currentContentVersion',
    'currentPublicVersion',
    'currentRelease',
    'republishReadiness',
    'republishError',
    'blockers',
    'VersionCenterHeader',
    'VersionHistoryList',
    'VersionSnapshotPreview',
    'VersionDiffPanel',
    'RepublishConfirmDialog',
    'resolvePreferredHistoryRevisionId',
    'historyRequestToken',
    'detailRequestToken'
  ]) {
    assert.match(`${pageSource}\n${contractsSource}\n${apiSource}`, new RegExp(token))
  }

  assert.match(diffSource, /interactionsDisabled/)
  assert.match(apiSource, /import request from '@\/config\/axios'/)
  assert.doesNotMatch(apiSource, /axios\.request/)
  assert.doesNotMatch(`${pageSource}\n${contractsSource}\n${apiSource}`, /mock/i)
  assert.doesNotMatch(`${pageSource}\n${contractsSource}\n${apiSource}`, /fallback/i)
})

test('version center history detail and republish require explicit release scope', () => {
  const pageSource = readText('src/views/showroom-admin/version-center/VersionCenterPage.vue')
  const companyVersionSource = readText(
    'src/views/showroom-admin/company-version/CompanyVersionWorkbench.vue'
  )
  const apiSource = readText('src/api/showroom-admin/version-center.ts')

  assert.match(apiSource, /export type VersionCenterReleaseStage = 'TEST' \| 'PROD'/)
  assert.match(apiSource, /export interface VersionCenterReleaseScope/)
  assert.match(apiSource, /siteKey:\s*string/)
  assert.match(apiSource, /stage:\s*VersionCenterReleaseStage/)
  assert.match(apiSource, /assertVersionCenterReleaseScope/)
  assert.match(apiSource, /版本中心缺少 scope：siteKey\/stage/)
  assert.match(apiSource, /getVersionCenterHistory = async \(params: VersionCenterHistoryQuery\)/)
  assert.match(apiSource, /getVersionCenterDetail = async \(params: VersionCenterDetailQuery\)/)
  assert.match(apiSource, /republishVersionCenter = async \(data: VersionCenterRepublishReqVO\)/)
  assert.match(apiSource, /assertVersionCenterReleaseScope\(params\)/)
  assert.match(apiSource, /assertVersionCenterReleaseScope\(data\)/)

  assert.match(pageSource, /versionCenterReleaseScope/)
  assert.match(pageSource, /siteKey:\s*'yingtai-showroom'/)
  assert.match(pageSource, /stage:\s*'TEST' as const/)
  assert.match(pageSource, /resolveVersionCenterReleaseScope/)
  assert.match(pageSource, /\.\.\.resolveVersionCenterReleaseScope\(\)/)
  assert.match(pageSource, /版本中心缺少 scope：siteKey\/stage/)

  for (const source of [pageSource, companyVersionSource]) {
    assert.doesNotMatch(
      source,
      /getVersionCenter(?:History|Detail)\(\{\s*targetType:[\s\S]*?(?:revisionId[\s\S]*?)?\}\s*\)/,
      'version center requests must not be built without release scope'
    )
  }
  assert.doesNotMatch(
    pageSource,
    /republishVersionCenter\(\{\s*targetType:[\s\S]*?sourceRevisionId:[\s\S]*?\}\s*\)/,
    'republish request must not be built without release scope'
  )
})

test('version center contracts normalize explicit backend shapes and honor selection priority', async () => {
  const {
    normalizeVersionCenterHistoryResponse,
    normalizeVersionCenterDetailResponse,
    normalizeVersionCenterRepublishResponse,
    resolvePreferredHistoryRevisionId
  } = await loadVersionCenterContracts()

  const history = normalizeVersionCenterHistoryResponse({
    targetType: 'PRODUCT',
    targetId: 22,
    currentContentRevisionId: 103,
    currentPublicRevisionId: 102,
    currentReleaseId: 'release-9',
    items: [
      {
        revisionId: 101,
        revisionNo: 1,
        publishedAt: '2026-05-23T10:00:00Z',
        publishedBy: 9001,
        copiedFromRevisionId: null,
        currentContent: false,
        currentPublic: false,
        selectable: true,
        previewSummaryImageUrl: 'https://example.com/preview-101.png',
        diffSummary: ['在售国家更新']
      },
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
        diffSummary: ['英文在售国家更新']
      }
    ]
  })

  assert.equal(history.items.length, 2)
  assert.equal(resolvePreferredHistoryRevisionId(history, 999), 102)
  assert.equal(resolvePreferredHistoryRevisionId(history, 101), 101)

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
      fields: [
        {
          fieldCode: 'target_market',
          label: '在售国家',
          labelEn: 'Countries on Sale',
          order: 10,
          valueZh: '中国、欧盟',
          valueEn: 'China, European Union'
        }
      ],
      image: {
        contentImage: {
          source: 'PRODUCT_REVISION_COVER_IMAGE',
          url: 'https://example.com/content.png',
          alt: '导管鞘封面',
          versionId: null,
          fileId: null
        },
        releasePreviewAsset: {
          source: 'PRODUCT_PREVIEW_ASSET_VERSION',
          url: 'https://example.com/public.png',
          alt: '导管鞘公开预览',
          versionId: 7001,
          fileId: 8001,
          sourceRevisionId: 102
        }
      },
      narrations: [
        {
          language: 'ZH',
          versionId: 501,
          scriptText: '中文讲解',
          audioUrl: 'https://example.com/zh.mp3',
          duration: 12,
          voice: 'zh-voice'
        },
        {
          language: 'EN',
          versionId: 502,
          scriptText: 'English script',
          audioUrl: 'https://example.com/en.mp3',
          duration: 14,
          voice: 'en-voice'
        }
      ]
    },
    currentContentVersion: {
      revisionId: 103,
      revisionNo: 3,
      publishedAt: null,
      publishedBy: null,
      copiedFromRevisionId: 102,
      currentContent: true,
      currentPublic: false,
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
    currentPublicVersion: null,
    currentRelease: {
      releaseId: 'release-9',
      manifestHash: 'abc123',
      publishedAt: '2026-05-23T11:05:00Z',
      companyRevisionId: 88,
      productInCurrentRelease: true,
      productCurrentReleaseRevisionId: 102
    },
    fieldDiffs: [
      {
        fieldCode: 'target_market',
        label: '在售国家',
        labelEn: 'Countries on Sale',
        order: 10,
        selectedValueZh: '中国、欧盟',
        selectedValueEn: 'China, European Union',
        currentContentValueZh: '中国',
        currentContentValueEn: 'China',
        changed: true
      }
    ],
    permissions: {
      canRepublish: true,
      republishDisabledReason: null
    },
    republishReadiness: {
      ready: false,
      blockers: [
        {
          blockerCode: 'SHOWROOM_VERSION_REPUBLISH_GLOBAL_RELEASE_BLOCKED',
          message: '全局 release 被阻断',
          affectedRevisionIds: [102, 201],
          scope: 'GLOBAL_RELEASE'
        }
      ]
    }
  })

  assert.equal(detail.selectedVersion.fields[0].label, '在售国家')
  assert.equal(detail.selectedVersion.fields[0].labelEn, 'Countries on Sale')
  assert.equal(detail.selectedVersion.image.releasePreviewAsset?.versionId, 7001)
  assert.equal(detail.republishReadiness.blockers[0].scope, 'GLOBAL_RELEASE')

  const republish = normalizeVersionCenterRepublishResponse({
    targetType: 'PRODUCT',
    targetId: 22,
    sourceRevisionId: 102,
    newRevisionId: 104,
    newRevisionNo: 4,
    releaseId: 'release-10',
    manifestHash: 'def456',
    publishedAt: '2026-05-23T11:10:00Z'
  })

  assert.equal(republish.newRevisionId, 104)
  assert.equal(republish.releaseId, 'release-10')
})
