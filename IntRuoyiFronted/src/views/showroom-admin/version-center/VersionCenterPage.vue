<template>
  <ContentWrap class="showroom-version-center-page">
    <VersionCenterHeader
      :current-content-version="detailData?.currentContentVersion || null"
      :current-public-version="detailData?.currentPublicVersion || null"
      :current-release="detailData?.currentRelease || null"
      :target-type="resolvedTargetType"
      :title="detailData?.targetSummary.title || pageTitle"
      :title-en="detailData?.targetSummary.titleEn || null"
      @back="handleBack"
    />

    <el-alert
      v-if="historyLoadError"
      :closable="false"
      class="showroom-version-center-page__alert"
      show-icon
      type="error"
      :title="historyLoadError"
    />

    <template v-else>
      <el-alert
        v-if="detailLoadError"
        :closable="false"
        class="showroom-version-center-page__alert"
        show-icon
        type="error"
        :title="detailLoadError"
      />

      <div class="showroom-version-center-page__layout">
        <VersionHistoryList
          :items="historyItems"
          :selected-revision-id="selectedRevisionId"
          @select="handleSelectRevision"
        />
        <div v-loading="detailLoading" class="showroom-version-center-page__middle">
          <VersionSnapshotPreview :selected-version="detailData?.selectedVersion || null" />
        </div>
        <div v-loading="detailLoading" class="showroom-version-center-page__right">
          <VersionDiffPanel
            :current-content-version="detailData?.currentContentVersion || null"
            :current-public-version="detailData?.currentPublicVersion || null"
            :current-release="detailData?.currentRelease || null"
            :field-diffs="detailData?.fieldDiffs || []"
            :permissions="detailData?.permissions || emptyPermissions"
            :republish-readiness="detailData?.republishReadiness || emptyReadiness"
            :republishing="republishing"
            :interactions-disabled="historyLoading || detailLoading || !detailData?.selectedVersion"
            @republish="openRepublishDialog"
          />
        </div>
      </div>
    </template>

    <RepublishConfirmDialog
      v-model="republishDialogVisible"
      :loading="republishing"
      :permissions="detailData?.permissions || emptyPermissions"
      :republish-readiness="detailData?.republishReadiness || emptyReadiness"
      :selected-version="detailData?.selectedVersion || null"
      :target-label="detailData?.targetSummary.title || pageTitle"
      :error-message="republishError"
      :error-blockers="republishErrorBlockers"
      @confirm="handleRepublish"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  getVersionCenterDetail,
  getVersionCenterHistory,
  republishVersionCenter,
  VersionCenterApiError
} from '@/api/showroom-admin/version-center'
import RepublishConfirmDialog from './RepublishConfirmDialog.vue'
import VersionCenterHeader from './VersionCenterHeader.vue'
import VersionDiffPanel from './VersionDiffPanel.vue'
import VersionHistoryList from './VersionHistoryList.vue'
import VersionSnapshotPreview from './VersionSnapshotPreview.vue'
import {
  normalizeVersionCenterBlockers,
  normalizeVersionCenterDetailResponse,
  normalizeVersionCenterHistoryResponse,
  normalizeVersionCenterRepublishResponse,
  resolvePreferredHistoryRevisionId,
  type VersionCenterBlocker,
  type VersionCenterDetailRespVO,
  type VersionCenterHistoryRespVO,
  type VersionCenterPermissionVO,
  type VersionCenterRepublishReadiness,
  type VersionCenterTargetType
} from './contracts'

defineOptions({ name: 'VersionCenterPage' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

const emptyPermissions: VersionCenterPermissionVO = {
  canRepublish: false,
  republishDisabledReason: '详情尚未加载完成'
}

const emptyReadiness: VersionCenterRepublishReadiness = {
  ready: false,
  blockers: []
}

const historyLoading = ref(false)
const detailLoading = ref(false)
const republishing = ref(false)
const republishDialogVisible = ref(false)
const historyLoadError = ref('')
const detailLoadError = ref('')
const republishError = ref('')
const historyData = ref<VersionCenterHistoryRespVO | null>(null)
const detailData = ref<VersionCenterDetailRespVO | null>(null)
const selectedRevisionId = ref<number | null>(null)
const republishErrorBlockers = ref<VersionCenterBlocker[]>([])
const historyRequestToken = ref(0)
const detailRequestToken = ref(0)

const versionCenterReleaseScope = {
  siteKey: 'yingtai-showroom',
  stage: 'TEST' as const
}

const resolveVersionCenterReleaseScope = () => {
  if (!versionCenterReleaseScope.siteKey || !versionCenterReleaseScope.stage) {
    throw new Error('版本中心缺少 scope：siteKey/stage')
  }
  return {
    siteKey: versionCenterReleaseScope.siteKey,
    stage: versionCenterReleaseScope.stage
  }
}

const resolvedTargetType = computed<VersionCenterTargetType>(() => {
  const routeTargetType = route.meta.versionTargetType
  if (routeTargetType === 'COMPANY' || routeTargetType === 'PRODUCT') {
    return routeTargetType
  }
  throw new Error('版本中心路由缺少 versionTargetType')
})

const resolvedTargetId = computed(() => {
  const rawValue =
    resolvedTargetType.value === 'COMPANY' ? route.params.companyId : route.params.productId
  const targetId = Number(rawValue)
  if (!Number.isFinite(targetId) || targetId <= 0) {
    throw new Error('版本中心路由缺少有效 targetId')
  }
  return targetId
})

const pageTitle = computed(() =>
  resolvedTargetType.value === 'COMPANY' ? '公司版本中心' : '产品版本中心'
)

const historyItems = computed(() => historyData.value?.items || [])

const clearRepublishError = () => {
  republishError.value = ''
  republishErrorBlockers.value = []
}

const resolveRequestedRevisionId = () => {
  const rawRevisionId = Array.isArray(route.query.revisionId)
    ? route.query.revisionId[0]
    : route.query.revisionId
  if (!rawRevisionId) {
    return null
  }
  const revisionId = Number(rawRevisionId)
  return Number.isFinite(revisionId) && revisionId > 0 ? revisionId : null
}

const syncRevisionQuery = async (revisionId: number) => {
  const currentRevisionId = resolveRequestedRevisionId()
  if (currentRevisionId === revisionId) {
    return
  }
  await router.replace({
    name: String(route.name),
    params: route.params,
    query: {
      ...route.query,
      revisionId: String(revisionId)
    }
  })
}

const loadDetail = async (revisionId: number) => {
  const requestToken = detailRequestToken.value + 1
  detailRequestToken.value = requestToken
  detailLoading.value = true
  detailLoadError.value = ''
  try {
    const detail = normalizeVersionCenterDetailResponse(
      await getVersionCenterDetail({
        ...resolveVersionCenterReleaseScope(),
        targetType: resolvedTargetType.value,
        targetId: resolvedTargetId.value,
        revisionId
      })
    )
    if (detailRequestToken.value !== requestToken) {
      return
    }
    detailData.value = detail
  } catch (error) {
    if (detailRequestToken.value !== requestToken) {
      return
    }
    detailData.value = null
    const resolved = error instanceof Error ? error : new Error(String(error))
    detailLoadError.value = resolved.message
  } finally {
    if (detailRequestToken.value === requestToken) {
      detailLoading.value = false
    }
  }
}

const loadHistory = async () => {
  const requestToken = historyRequestToken.value + 1
  historyRequestToken.value = requestToken
  historyLoading.value = true
  historyLoadError.value = ''
  clearRepublishError()
  try {
    const history = normalizeVersionCenterHistoryResponse(
      await getVersionCenterHistory({
        ...resolveVersionCenterReleaseScope(),
        targetType: resolvedTargetType.value,
        targetId: resolvedTargetId.value
      })
    )
    if (historyRequestToken.value !== requestToken) {
      return
    }
    historyData.value = history
    const nextRevisionId = resolvePreferredHistoryRevisionId(history, resolveRequestedRevisionId())
    selectedRevisionId.value = nextRevisionId
    if (nextRevisionId) {
      await loadDetail(nextRevisionId)
    } else {
      detailData.value = null
      detailLoadError.value = ''
    }
  } catch (error) {
    if (historyRequestToken.value !== requestToken) {
      return
    }
    historyData.value = null
    detailData.value = null
    selectedRevisionId.value = null
    const resolved = error instanceof Error ? error : new Error(String(error))
    historyLoadError.value = resolved.message
  } finally {
    if (historyRequestToken.value === requestToken) {
      historyLoading.value = false
    }
  }
}

const handleSelectRevision = async (revisionId: number) => {
  if (selectedRevisionId.value === revisionId) {
    return
  }
  clearRepublishError()
  republishDialogVisible.value = false
  detailData.value = null
  detailLoadError.value = ''
  detailLoading.value = true
  selectedRevisionId.value = revisionId
  await syncRevisionQuery(revisionId)
}

const normalizeRepublishErrorBlockers = (details: unknown) => {
  if (!details || typeof details !== 'object' || Array.isArray(details)) {
    return []
  }
  const detailRecord = details as Record<string, unknown>
  if (Array.isArray(detailRecord.blockers)) {
    return normalizeVersionCenterBlockers(detailRecord.blockers, 'republishError.blockers')
  }
  const nestedDetails =
    detailRecord.details &&
    typeof detailRecord.details === 'object' &&
    !Array.isArray(detailRecord.details)
      ? (detailRecord.details as Record<string, unknown>)
      : null
  if (Array.isArray(nestedDetails?.blockers)) {
    return normalizeVersionCenterBlockers(
      nestedDetails.blockers,
      'republishError.details.blockers'
    )
  }
  return []
}

const openRepublishDialog = () => {
  if (historyLoading.value || detailLoading.value || !detailData.value?.selectedVersion) {
    return
  }
  clearRepublishError()
  republishDialogVisible.value = true
}

const handleRepublish = async () => {
  if (!detailData.value?.selectedVersion) {
    throw new Error('当前没有选中的历史版本')
  }
  republishing.value = true
  clearRepublishError()
  try {
    const result = normalizeVersionCenterRepublishResponse(
      await republishVersionCenter({
        ...resolveVersionCenterReleaseScope(),
        targetType: resolvedTargetType.value,
        targetId: resolvedTargetId.value,
        sourceRevisionId: detailData.value.selectedVersion.revisionId
      })
    )
    republishDialogVisible.value = false
    await router.replace({
      name: String(route.name),
      params: route.params,
      query: {
        ...route.query,
        revisionId: String(result.newRevisionId)
      }
    })
    message.success(
      `已复制为新版本 V${result.newRevisionNo}，并刷新当前 release ${result.releaseId}`
    )
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    republishError.value = resolved.message
    republishErrorBlockers.value =
      error instanceof VersionCenterApiError ? normalizeRepublishErrorBlockers(error.details) : []
    message.error(resolved.message)
    throw resolved
  } finally {
    republishing.value = false
  }
}

const handleBack = async () => {
  await router.push({
    name: resolvedTargetType.value === 'COMPANY' ? 'ShowroomAdminCompany' : 'ShowroomAdminProduct'
  })
}

watch(
  () =>
    [
      route.name,
      route.params.companyId,
      route.params.productId,
      route.query.revisionId
    ] as const,
  () => {
    void loadHistory()
  },
  { immediate: true }
)
</script>

<style scoped>
.showroom-version-center-page {
  min-width: 1280px;
}

.showroom-version-center-page__alert {
  margin-top: 12px;
}

.showroom-version-center-page__layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr) 360px;
  gap: 12px;
  margin-top: 12px;
  align-items: start;
}

.showroom-version-center-page__middle,
.showroom-version-center-page__right {
  min-width: 0;
}

@media (max-width: 1400px) {
  .showroom-version-center-page {
    min-width: 0;
  }

  .showroom-version-center-page__layout {
    grid-template-columns: 1fr;
  }
}
</style>
