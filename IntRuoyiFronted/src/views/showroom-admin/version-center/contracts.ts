export type VersionCenterTargetType = 'COMPANY' | 'PRODUCT'
export type VersionCenterBlockerScope =
  | 'SELECTED_VERSION'
  | 'CURRENT_CONTENT'
  | 'CURRENT_RELEASE'
  | 'GLOBAL_RELEASE'

export interface VersionCenterHistoryItem {
  revisionId: number
  revisionNo: number
  publishedAt: string | null
  publishedBy: number | null
  copiedFromRevisionId: number | null
  currentContent: boolean
  currentPublic: boolean
  selectable: boolean
  previewSummaryImageUrl: string | null
  diffSummary: string[]
  blockers: VersionCenterBlocker[]
}

export interface VersionCenterHistoryRespVO {
  targetType: VersionCenterTargetType
  targetId: number
  currentContentRevisionId: number | null
  currentPublicRevisionId: number | null
  currentReleaseId: string | null
  items: VersionCenterHistoryItem[]
}

export interface VersionCenterFieldValueVO {
  fieldCode: string
  label: string
  labelEn: string
  order: number
  valueZh: string | null
  valueEn: string | null
}

export interface VersionCenterImageAssetVO {
  source:
    | 'COMPANY_REVISION_COVER_IMAGE'
    | 'PRODUCT_REVISION_COVER_IMAGE'
    | 'COMPANY_PREVIEW_ASSET_VERSION'
    | 'PRODUCT_PREVIEW_ASSET_VERSION'
  url: string | null
  alt: string | null
  versionId: number | null
  fileId: number | null
  sourceRevisionId?: number | null
}

export interface VersionCenterNarrationVO {
  language: 'ZH' | 'EN'
  versionId: number | null
  scriptText: string | null
  audioUrl: string | null
  duration: number | null
  voice: string | null
}

export interface VersionCenterSnapshotVO {
  revisionId: number
  revisionNo: number
  publishedAt: string | null
  publishedBy: number | null
  copiedFromRevisionId: number | null
  currentContent: boolean
  currentPublic: boolean
  title: string
  titleEn: string | null
  companyType: string | null
  fields: VersionCenterFieldValueVO[]
  image: {
    contentImage: VersionCenterImageAssetVO
    releasePreviewAsset: VersionCenterImageAssetVO | null
  }
  narrations: VersionCenterNarrationVO[]
}

export interface VersionCenterFieldDiffVO {
  fieldCode: string
  label: string
  labelEn: string
  order: number
  selectedValueZh: string | null
  selectedValueEn: string | null
  currentContentValueZh: string | null
  currentContentValueEn: string | null
  changed: boolean
}

export interface VersionCenterReleaseSummary {
  releaseId: string
  manifestHash: string
  publishedAt: string
  companyRevisionId: number
  productInCurrentRelease: boolean | null
  productCurrentReleaseRevisionId: number | null
}

export interface VersionCenterPermissionVO {
  canRepublish: boolean
  republishDisabledReason: string | null
}

export interface VersionCenterBlocker {
  blockerCode: string
  message: string
  affectedRevisionIds: number[]
  scope: VersionCenterBlockerScope
  targetType?: VersionCenterTargetType
  targetId?: number | null
  language?: 'ZH' | 'EN'
  missingFields?: string[]
  fileId?: number | null
  assetId?: string | null
  contentHash?: string | null
}

export interface VersionCenterRepublishReadiness {
  ready: boolean
  blockers: VersionCenterBlocker[]
}

export interface VersionCenterTargetSummary {
  targetType: VersionCenterTargetType
  targetId: number
  title: string
  titleEn: string | null
  currentContentRevisionId: number | null
  currentPublicRevisionId: number | null
}

export interface VersionCenterDetailRespVO {
  targetSummary: VersionCenterTargetSummary
  selectedVersion: VersionCenterSnapshotVO
  currentContentVersion: VersionCenterSnapshotVO | null
  currentPublicVersion: VersionCenterSnapshotVO | null
  currentRelease: VersionCenterReleaseSummary | null
  fieldDiffs: VersionCenterFieldDiffVO[]
  permissions: VersionCenterPermissionVO
  republishReadiness: VersionCenterRepublishReadiness
}

export interface VersionCenterRepublishRespVO {
  targetType: VersionCenterTargetType
  targetId: number
  sourceRevisionId: number
  newRevisionId: number
  newRevisionNo: number
  releaseId: string
  manifestHash: string
  publishedAt: string
}

const expectRecord = (value: unknown, fieldName: string): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`版本中心缺少对象字段：${fieldName}`)
  }
  return value as Record<string, unknown>
}

const expectNumber = (value: unknown, fieldName: string) => {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`版本中心缺少数值字段：${fieldName}`)
  }
  return value
}

const expectBoolean = (value: unknown, fieldName: string) => {
  if (typeof value !== 'boolean') {
    throw new Error(`版本中心缺少布尔字段：${fieldName}`)
  }
  return value
}

const expectString = (value: unknown, fieldName: string, allowEmpty = false) => {
  if (typeof value !== 'string' || (!allowEmpty && value.trim().length === 0)) {
    throw new Error(`版本中心缺少字符串字段：${fieldName}`)
  }
  return value
}

const expectNullableString = (value: unknown, fieldName: string) => {
  if (value === null) {
    return null
  }
  return expectString(value, fieldName, true)
}

const expectNullableNumber = (value: unknown, fieldName: string) => {
  if (value === null) {
    return null
  }
  return expectNumber(value, fieldName)
}

const expectArray = (value: unknown, fieldName: string) => {
  if (!Array.isArray(value)) {
    throw new Error(`版本中心缺少数组字段：${fieldName}`)
  }
  return value
}

const expectTargetType = (value: unknown, fieldName: string): VersionCenterTargetType => {
  const targetType = expectString(value, fieldName)
  if (targetType !== 'COMPANY' && targetType !== 'PRODUCT') {
    throw new Error(`版本中心字段无效：${fieldName}`)
  }
  return targetType
}

const expectBlockerScope = (value: unknown, fieldName: string): VersionCenterBlockerScope => {
  const scope = expectString(value, fieldName)
  if (
    scope !== 'SELECTED_VERSION' &&
    scope !== 'CURRENT_CONTENT' &&
    scope !== 'CURRENT_RELEASE' &&
    scope !== 'GLOBAL_RELEASE'
  ) {
    throw new Error(`版本中心字段无效：${fieldName}`)
  }
  return scope
}

const expectNarrationLanguage = (value: unknown, fieldName: string): 'ZH' | 'EN' => {
  const language = expectString(value, fieldName)
  if (language !== 'ZH' && language !== 'EN') {
    throw new Error(`版本中心字段无效：${fieldName}`)
  }
  return language
}

const expectOptionalTargetType = (
  value: unknown,
  fieldName: string
): VersionCenterTargetType | undefined => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  return expectTargetType(value, fieldName)
}

const expectOptionalNarrationLanguage = (
  value: unknown,
  fieldName: string
): 'ZH' | 'EN' | undefined => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  return expectNarrationLanguage(value, fieldName)
}

const expectOptionalNullableNumber = (value: unknown, fieldName: string) => {
  if (value === undefined) {
    return undefined
  }
  return expectNullableNumber(value, fieldName)
}

const expectOptionalNullableString = (value: unknown, fieldName: string) => {
  if (value === undefined) {
    return undefined
  }
  return expectNullableString(value, fieldName)
}

const normalizeOptionalStringArray = (value: unknown, fieldName: string): string[] | undefined => {
  if (value === undefined || value === null) {
    return undefined
  }
  return expectArray(value, fieldName).map((item, index) =>
    expectString(item, `${fieldName}[${index}]`)
  )
}

const normalizeImageAsset = (
  value: unknown,
  fieldName: string
): VersionCenterImageAssetVO => {
  const record = expectRecord(value, fieldName)
  return {
    source: expectString(record.source, `${fieldName}.source`) as VersionCenterImageAssetVO['source'],
    url: expectNullableString(record.url, `${fieldName}.url`),
    alt: expectNullableString(record.alt, `${fieldName}.alt`),
    versionId: expectNullableNumber(record.versionId, `${fieldName}.versionId`),
    fileId: expectNullableNumber(record.fileId, `${fieldName}.fileId`),
    sourceRevisionId:
      record.sourceRevisionId === undefined
        ? null
        : expectNullableNumber(record.sourceRevisionId, `${fieldName}.sourceRevisionId`)
  }
}

const normalizeFieldValues = (value: unknown, fieldName: string): VersionCenterFieldValueVO[] => {
  return expectArray(value, fieldName).map((item, index) => {
    const record = expectRecord(item, `${fieldName}[${index}]`)
    return {
      fieldCode: expectString(record.fieldCode, `${fieldName}[${index}].fieldCode`),
      label: expectString(record.label, `${fieldName}[${index}].label`),
      labelEn: expectString(record.labelEn, `${fieldName}[${index}].labelEn`),
      order: expectNumber(record.order, `${fieldName}[${index}].order`),
      valueZh: expectNullableString(record.valueZh, `${fieldName}[${index}].valueZh`),
      valueEn: expectNullableString(record.valueEn, `${fieldName}[${index}].valueEn`)
    }
  })
}

const normalizeNarrations = (value: unknown, fieldName: string): VersionCenterNarrationVO[] => {
  return expectArray(value, fieldName).map((item, index) => {
    const record = expectRecord(item, `${fieldName}[${index}]`)
    return {
      language: expectNarrationLanguage(record.language, `${fieldName}[${index}].language`),
      versionId: expectNullableNumber(record.versionId, `${fieldName}[${index}].versionId`),
      scriptText: expectNullableString(record.scriptText, `${fieldName}[${index}].scriptText`),
      audioUrl: expectNullableString(record.audioUrl, `${fieldName}[${index}].audioUrl`),
      duration: expectNullableNumber(record.duration, `${fieldName}[${index}].duration`),
      voice: expectNullableString(record.voice, `${fieldName}[${index}].voice`)
    }
  })
}

const normalizeSnapshot = (value: unknown, fieldName: string): VersionCenterSnapshotVO => {
  const record = expectRecord(value, fieldName)
  const image = expectRecord(record.image, `${fieldName}.image`)
  return {
    revisionId: expectNumber(record.revisionId, `${fieldName}.revisionId`),
    revisionNo: expectNumber(record.revisionNo, `${fieldName}.revisionNo`),
    publishedAt: expectNullableString(record.publishedAt, `${fieldName}.publishedAt`),
    publishedBy: expectNullableNumber(record.publishedBy, `${fieldName}.publishedBy`),
    copiedFromRevisionId: expectNullableNumber(
      record.copiedFromRevisionId,
      `${fieldName}.copiedFromRevisionId`
    ),
    currentContent: expectBoolean(record.currentContent, `${fieldName}.currentContent`),
    currentPublic: expectBoolean(record.currentPublic, `${fieldName}.currentPublic`),
    title: expectString(record.title, `${fieldName}.title`),
    titleEn: expectNullableString(record.titleEn, `${fieldName}.titleEn`),
    companyType: expectNullableString(record.companyType, `${fieldName}.companyType`),
    fields: normalizeFieldValues(record.fields, `${fieldName}.fields`),
    image: {
      contentImage: normalizeImageAsset(image.contentImage, `${fieldName}.image.contentImage`),
      releasePreviewAsset:
        image.releasePreviewAsset === null
          ? null
          : normalizeImageAsset(
              image.releasePreviewAsset,
              `${fieldName}.image.releasePreviewAsset`
            )
    },
    narrations: normalizeNarrations(record.narrations, `${fieldName}.narrations`)
  }
}

const normalizeFieldDiffs = (value: unknown, fieldName: string): VersionCenterFieldDiffVO[] => {
  return expectArray(value, fieldName).map((item, index) => {
    const record = expectRecord(item, `${fieldName}[${index}]`)
    return {
      fieldCode: expectString(record.fieldCode, `${fieldName}[${index}].fieldCode`),
      label: expectString(record.label, `${fieldName}[${index}].label`),
      labelEn: expectString(record.labelEn, `${fieldName}[${index}].labelEn`),
      order: expectNumber(record.order, `${fieldName}[${index}].order`),
      selectedValueZh: expectNullableString(
        record.selectedValueZh,
        `${fieldName}[${index}].selectedValueZh`
      ),
      selectedValueEn: expectNullableString(
        record.selectedValueEn,
        `${fieldName}[${index}].selectedValueEn`
      ),
      currentContentValueZh: expectNullableString(
        record.currentContentValueZh,
        `${fieldName}[${index}].currentContentValueZh`
      ),
      currentContentValueEn: expectNullableString(
        record.currentContentValueEn,
        `${fieldName}[${index}].currentContentValueEn`
      ),
      changed: expectBoolean(record.changed, `${fieldName}[${index}].changed`)
    }
  })
}

const normalizeBlockers = (value: unknown, fieldName: string): VersionCenterBlocker[] => {
  return expectArray(value, fieldName).map((item, index) => {
    const record = expectRecord(item, `${fieldName}[${index}]`)
    return {
      blockerCode: expectString(record.blockerCode, `${fieldName}[${index}].blockerCode`),
      message: expectString(record.message, `${fieldName}[${index}].message`),
      affectedRevisionIds: expectArray(
        record.affectedRevisionIds,
        `${fieldName}[${index}].affectedRevisionIds`
      ).map((revisionId, revisionIndex) =>
        expectNumber(revisionId, `${fieldName}[${index}].affectedRevisionIds[${revisionIndex}]`)
      ),
      scope: expectBlockerScope(record.scope, `${fieldName}[${index}].scope`),
      targetType: expectOptionalTargetType(record.targetType, `${fieldName}[${index}].targetType`),
      targetId: expectOptionalNullableNumber(record.targetId, `${fieldName}[${index}].targetId`),
      language: expectOptionalNarrationLanguage(record.language, `${fieldName}[${index}].language`),
      missingFields: normalizeOptionalStringArray(
        record.missingFields,
        `${fieldName}[${index}].missingFields`
      ),
      fileId: expectOptionalNullableNumber(record.fileId, `${fieldName}[${index}].fileId`),
      assetId: expectOptionalNullableString(record.assetId, `${fieldName}[${index}].assetId`),
      contentHash: expectOptionalNullableString(
        record.contentHash,
        `${fieldName}[${index}].contentHash`
      )
    }
  })
}

export const normalizeVersionCenterBlockers = (value: unknown, fieldName: string) => {
  return normalizeBlockers(value, fieldName)
}

export const normalizeVersionCenterHistoryResponse = (
  value: unknown
): VersionCenterHistoryRespVO => {
  const record = expectRecord(value, 'history')
  return {
    targetType: expectTargetType(record.targetType, 'history.targetType'),
    targetId: expectNumber(record.targetId, 'history.targetId'),
    currentContentRevisionId: expectNullableNumber(
      record.currentContentRevisionId,
      'history.currentContentRevisionId'
    ),
    currentPublicRevisionId: expectNullableNumber(
      record.currentPublicRevisionId,
      'history.currentPublicRevisionId'
    ),
    currentReleaseId: expectNullableString(record.currentReleaseId, 'history.currentReleaseId'),
    items: expectArray(record.items, 'history.items').map((item, index) => {
      const row = expectRecord(item, `history.items[${index}]`)
      return {
        revisionId: expectNumber(row.revisionId, `history.items[${index}].revisionId`),
        revisionNo: expectNumber(row.revisionNo, `history.items[${index}].revisionNo`),
        publishedAt: expectNullableString(
          row.publishedAt,
          `history.items[${index}].publishedAt`
        ),
        publishedBy: expectNullableNumber(
          row.publishedBy,
          `history.items[${index}].publishedBy`
        ),
        copiedFromRevisionId: expectNullableNumber(
          row.copiedFromRevisionId,
          `history.items[${index}].copiedFromRevisionId`
        ),
        currentContent: expectBoolean(
          row.currentContent,
          `history.items[${index}].currentContent`
        ),
        currentPublic: expectBoolean(
          row.currentPublic,
          `history.items[${index}].currentPublic`
        ),
        selectable: expectBoolean(row.selectable, `history.items[${index}].selectable`),
        previewSummaryImageUrl: expectNullableString(
          row.previewSummaryImageUrl,
          `history.items[${index}].previewSummaryImageUrl`
        ),
        diffSummary: expectArray(row.diffSummary, `history.items[${index}].diffSummary`).map(
          (summary, summaryIndex) =>
            expectString(summary, `history.items[${index}].diffSummary[${summaryIndex}]`)
        ),
        blockers: normalizeBlockers(row.blockers || [], `history.items[${index}].blockers`)
      }
    })
  }
}

export const normalizeVersionCenterDetailResponse = (
  value: unknown
): VersionCenterDetailRespVO => {
  const record = expectRecord(value, 'detail')
  const targetSummary = expectRecord(record.targetSummary, 'detail.targetSummary')
  const permissions = expectRecord(record.permissions, 'detail.permissions')
  const republishReadiness = expectRecord(
    record.republishReadiness,
    'detail.republishReadiness'
  )

  return {
    targetSummary: {
      targetType: expectTargetType(targetSummary.targetType, 'detail.targetSummary.targetType'),
      targetId: expectNumber(targetSummary.targetId, 'detail.targetSummary.targetId'),
      title: expectString(targetSummary.title, 'detail.targetSummary.title'),
      titleEn: expectNullableString(targetSummary.titleEn, 'detail.targetSummary.titleEn'),
      currentContentRevisionId: expectNullableNumber(
        targetSummary.currentContentRevisionId,
        'detail.targetSummary.currentContentRevisionId'
      ),
      currentPublicRevisionId: expectNullableNumber(
        targetSummary.currentPublicRevisionId,
        'detail.targetSummary.currentPublicRevisionId'
      )
    },
    selectedVersion: normalizeSnapshot(record.selectedVersion, 'detail.selectedVersion'),
    currentContentVersion:
      record.currentContentVersion === null
        ? null
        : normalizeSnapshot(record.currentContentVersion, 'detail.currentContentVersion'),
    currentPublicVersion:
      record.currentPublicVersion === null
        ? null
        : normalizeSnapshot(record.currentPublicVersion, 'detail.currentPublicVersion'),
    currentRelease:
      record.currentRelease === null
        ? null
        : (() => {
            const release = expectRecord(record.currentRelease, 'detail.currentRelease')
            return {
              releaseId: expectString(release.releaseId, 'detail.currentRelease.releaseId'),
              manifestHash: expectString(
                release.manifestHash,
                'detail.currentRelease.manifestHash'
              ),
              publishedAt: expectString(
                release.publishedAt,
                'detail.currentRelease.publishedAt'
              ),
              companyRevisionId: expectNumber(
                release.companyRevisionId,
                'detail.currentRelease.companyRevisionId'
              ),
              productInCurrentRelease:
                release.productInCurrentRelease === null
                  ? null
                  : expectBoolean(
                      release.productInCurrentRelease,
                      'detail.currentRelease.productInCurrentRelease'
                    ),
              productCurrentReleaseRevisionId: expectNullableNumber(
                release.productCurrentReleaseRevisionId,
                'detail.currentRelease.productCurrentReleaseRevisionId'
              )
            }
          })(),
    fieldDiffs: normalizeFieldDiffs(record.fieldDiffs, 'detail.fieldDiffs'),
    permissions: {
      canRepublish: expectBoolean(permissions.canRepublish, 'detail.permissions.canRepublish'),
      republishDisabledReason: expectNullableString(
        permissions.republishDisabledReason,
        'detail.permissions.republishDisabledReason'
      )
    },
    republishReadiness: {
      ready: expectBoolean(republishReadiness.ready, 'detail.republishReadiness.ready'),
      blockers: normalizeBlockers(republishReadiness.blockers, 'detail.republishReadiness.blockers')
    }
  }
}

export const normalizeVersionCenterRepublishResponse = (
  value: unknown
): VersionCenterRepublishRespVO => {
  const record = expectRecord(value, 'republish')
  return {
    targetType: expectTargetType(record.targetType, 'republish.targetType'),
    targetId: expectNumber(record.targetId, 'republish.targetId'),
    sourceRevisionId: expectNumber(record.sourceRevisionId, 'republish.sourceRevisionId'),
    newRevisionId: expectNumber(record.newRevisionId, 'republish.newRevisionId'),
    newRevisionNo: expectNumber(record.newRevisionNo, 'republish.newRevisionNo'),
    releaseId: expectString(record.releaseId, 'republish.releaseId'),
    manifestHash: expectString(record.manifestHash, 'republish.manifestHash'),
    publishedAt: expectString(record.publishedAt, 'republish.publishedAt')
  }
}

export const resolvePreferredHistoryRevisionId = (
  history: VersionCenterHistoryRespVO,
  requestedRevisionId?: number | null
) => {
  const availableIds = new Set(history.items.map((item) => item.revisionId))
  if (requestedRevisionId && availableIds.has(requestedRevisionId)) {
    return requestedRevisionId
  }
  if (history.currentPublicRevisionId && availableIds.has(history.currentPublicRevisionId)) {
    return history.currentPublicRevisionId
  }
  if (history.currentContentRevisionId && availableIds.has(history.currentContentRevisionId)) {
    return history.currentContentRevisionId
  }
  return history.items[0]?.revisionId ?? null
}

export const resolveVersionCenterNarrationLabel = (language: 'ZH' | 'EN') => {
  return language === 'ZH' ? '中文讲解' : 'English Narration'
}

const blockerValue = (value: unknown) => {
  if (value === undefined || value === null || value === '') {
    return '未生成'
  }
  return String(value)
}

export const formatVersionCenterBlocker = (blocker: VersionCenterBlocker) => {
  const target = [
    blocker.targetType,
    blocker.targetId !== undefined && blocker.targetId !== null ? `#${blocker.targetId}` : ''
  ]
    .filter(Boolean)
    .join(' ')
  const parts = [
    `[${blocker.scope}] ${blocker.blockerCode}`,
    blocker.message,
    target,
    blocker.language ? `language=${blocker.language}` : '',
    blocker.missingFields?.length ? `missing=${blocker.missingFields.join(',')}` : '',
    `fileId=${blockerValue(blocker.fileId)}`,
    `assetId=${blockerValue(blocker.assetId)}`,
    `contentHash=${blockerValue(blocker.contentHash)}`
  ].filter(Boolean)
  return parts.join(' · ')
}
