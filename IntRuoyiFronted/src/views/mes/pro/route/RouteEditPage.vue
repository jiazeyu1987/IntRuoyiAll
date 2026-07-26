<!-- MES 工艺路线编辑页面 -->
<template>
  <ContentWrap class="route-edit-page">
    <el-alert
      v-if="routeEditBlockingError"
      class="route-edit-page__error"
      type="error"
      :title="routeEditBlockingError"
      :closable="false"
      show-icon
    >
      <template #default>
        <div class="route-edit-page__error-content">
          <span>请从工艺流程列表选择有效路线后再进入编辑。</span>
          <el-button type="primary" link @click="handleBackToList">返回列表</el-button>
        </div>
      </template>
    </el-alert>
    <RouteFormContent
      v-else
      ref="contentRef"
      mode="page"
      :basic-readonly="true"
      :target-route-process-id="targetRouteProcessId"
      :route-version-edit-context="routeVersionEditContext"
      :route-version-action-loading="routeVersionActionLoading"
      @back="handleBackToList"
      @request-candidate-edit="handleEditProductionConfig"
      @request-route-version-submit="handleSubmitRouteCandidateVersion"
      @success="handleSaved"
    />
    <div
      v-if="!routeEditBlockingError && !['flow', 'basic', 'product'].includes(activeRouteTab)"
      class="route-edit-page__actions"
    >
      <el-button
        type="primary"
        :disabled="contentRef?.formLoading"
        @click="contentRef?.submitForm()"
      >
        保 存
      </el-button>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import {
  ProRouteApi,
  type ProRouteVersionVO,
  type ProRouteVersionLifecycleStatus,
  type RouteVersionEditContext
} from '@/api/mes/pro/route'
import RouteFormContent from './RouteFormContent.vue'
import {
  buildRouteCandidateEditQuery,
  ensureSameSourceDraftCandidateForProductionConfig
} from './routeCandidateEntry'
import { isRouteConfirmCancel, resolveRouteOperationErrorMessage } from './routeError'

defineOptions({ name: 'MesProRouteEdit' })

const route = useRoute()
const router = useRouter()
const message = useMessage()
const contentRef = ref<InstanceType<typeof RouteFormContent>>()
const loadedRouteRequestKey = ref<string>()
const routeVersionActionLoading = ref(false)
const routeLoadError = ref('')
const LIST_EDIT_ROUTE_DRAFT_ORIGIN = 'list-edit'
const LIST_EDIT_DISCARD_ON_UNSAVED_EXIT = '1'
const INVALID_ROUTE_EDIT_MESSAGE = '编辑工艺路线失败：缺少有效路线编号'

const routeId = computed(() => Number(route.params.id || route.query.id))
const isCurrentRouteEditPage = computed(() => route.name === 'MesProRouteEdit')
const routeIdInvalidError = computed(() =>
  isCurrentRouteEditPage.value && (!Number.isFinite(routeId.value) || routeId.value <= 0)
    ? INVALID_ROUTE_EDIT_MESSAGE
    : ''
)
const routeEditBlockingError = computed(() => routeIdInvalidError.value || routeLoadError.value)
const normalizeRouteQueryText = (value: unknown) => {
  if (Array.isArray(value)) return value[0] ? String(value[0]) : ''
  return value ? String(value) : ''
}
const routeDraftOrigin = computed(() => normalizeRouteQueryText(route.query.routeDraftOrigin))
const discardOnUnsavedExit = computed(() => normalizeRouteQueryText(route.query.discardOnUnsavedExit))
const targetRouteProcessId = computed(() => {
  const value = Number(route.query.routeProcessId)
  return Number.isFinite(value) && value > 0 ? value : undefined
})
const routeVersionEditContext = computed<RouteVersionEditContext | undefined>(() => {
  const routeVersionId = Number(normalizeRouteQueryText(route.query.routeVersionId))
  if (!Number.isFinite(routeVersionId) || routeVersionId <= 0) {
    return undefined
  }
  const versionNo = normalizeRouteQueryText(route.query.routeVersionNo)
  const lifecycleStatus = normalizeRouteQueryText(
    route.query.routeVersionStatus
  ) as ProRouteVersionLifecycleStatus
  if (!versionNo || !lifecycleStatus) {
    throw new Error('编辑候选版本失败：缺少路线版本上下文')
  }
  return { routeVersionId, versionNo, lifecycleStatus }
})
const initialTab = computed(() => {
  const tab = String(route.query.tab || '')
  if (['basic', 'flow', 'product'].includes(tab)) {
    return tab as 'basic' | 'flow' | 'product'
  }
  return 'flow'
})
const activeRouteTab = computed(() => contentRef.value?.getActiveTab() ?? initialTab.value)
const routeVersionEditContextKey = computed(() => {
  const context = routeVersionEditContext.value
  return context
    ? `${context.routeVersionId}:${context.versionNo}:${context.lifecycleStatus}`
    : 'active'
})
const listEditCandidateDraftSaved = ref(false)
const listEditCandidateDraftDiscarded = ref(false)
const routeLeaveAlreadyConfirmed = ref(false)
const routeCandidateDraftKey = computed(
  () => `${routeId.value}:${routeVersionEditContextKey.value}:${routeDraftOrigin.value}:${discardOnUnsavedExit.value}`
)
const isListEditCandidateDraftExitGuardEnabled = computed(
  () =>
    routeDraftOrigin.value === LIST_EDIT_ROUTE_DRAFT_ORIGIN &&
    discardOnUnsavedExit.value === LIST_EDIT_DISCARD_ON_UNSAVED_EXIT
)
const hasRouteCandidateDraftChanges = () =>
  Boolean(contentRef.value?.hasRouteCandidateDraftChanges?.())
const shouldPromptUnsavedCandidateDraftBeforeExit = computed(() => {
  const context = routeVersionEditContext.value
  return (
    context?.lifecycleStatus === 'DRAFT' &&
    ((isListEditCandidateDraftExitGuardEnabled.value &&
      !listEditCandidateDraftSaved.value &&
      !listEditCandidateDraftDiscarded.value) ||
      hasRouteCandidateDraftChanges())
  )
})
const buildRouteRequestKey = () =>
  `${routeId.value}:${initialTab.value}:${routeVersionEditContextKey.value}`

type SubmitRouteCandidateVersionOptions = {
  confirmMessage?: string
  confirmTitle?: string
}

const resolveRoutePublishSuccessMessage = (version: ProRouteVersionVO | undefined) => {
  if (version?.lifecycleStatus === 'PENDING_APPROVAL') {
    return '候选版本已提交发布，等待审批'
  }
  if (version?.lifecycleStatus === 'ACTIVE') {
    return '候选版本已提交并发布生效'
  }
  return '候选版本已提交，发布策略已执行'
}

const buildPostSubmitPublishQuery = (version: ProRouteVersionVO) => {
  const nextQuery = { ...route.query }
  delete nextQuery.routeDraftOrigin
  delete nextQuery.discardOnUnsavedExit
  if (version.lifecycleStatus === 'ACTIVE') {
    delete nextQuery.routeVersionId
    delete nextQuery.routeVersionNo
    delete nextQuery.routeVersionStatus
    return nextQuery
  }
  nextQuery.routeVersionId = String(version.id)
  nextQuery.routeVersionNo = version.versionNo
  nextQuery.routeVersionStatus = version.lifecycleStatus
  return nextQuery
}

const syncRouteQueryAfterLatestSubmitStatus = async (latestVersion: ProRouteVersionVO) => {
  await router.replace({
    name: 'MesProRouteEdit',
    params: { id: routeId.value },
    query: buildPostSubmitPublishQuery(latestVersion)
  })
}

const loadRoute = async () => {
  if (!isCurrentRouteEditPage.value) {
    routeLoadError.value = ''
    return
  }
  if (routeIdInvalidError.value) {
    routeLoadError.value = ''
    loadedRouteRequestKey.value = undefined
    return
  }
  const nextRouteRequestKey = buildRouteRequestKey()
  if (loadedRouteRequestKey.value === nextRouteRequestKey) {
    return
  }
  routeLoadError.value = ''
  await nextTick()
  const content = contentRef.value
  if (!content) {
    routeLoadError.value = '编辑工艺路线失败：表单组件未加载'
    loadedRouteRequestKey.value = undefined
    return
  }
  try {
    await content.open('update', routeId.value, initialTab.value)
    loadedRouteRequestKey.value = nextRouteRequestKey
  } catch (error) {
    const errorMessage = resolveRouteOperationErrorMessage(
      error,
      '加载工艺流程失败，请查看后端返回错误'
    )
    routeLoadError.value = errorMessage
    loadedRouteRequestKey.value = undefined
    message.error(errorMessage)
  }
}

const submitRouteCandidateVersion = async (options: SubmitRouteCandidateVersionOptions = {}) => {
  const context = routeVersionEditContext.value
  if (!context || context.lifecycleStatus !== 'DRAFT') {
    message.error('提交候选版本失败：只有草稿候选版本允许提交发布。')
    return
  }
  routeVersionActionLoading.value = true
  try {
    const latestVersion = await ProRouteApi.getRouteVersion(context.routeVersionId)
    if (latestVersion.lifecycleStatus !== 'DRAFT') {
      if (latestVersion.active || latestVersion.lifecycleStatus === 'ACTIVE') {
        message.success('候选版本已发布生效，无需重复提交')
      } else {
        message.warning(
          `提交候选版本已取消：当前版本状态为${latestVersion.lifecycleStatus}，只有草稿候选版本允许提交发布。`
        )
      }
      await syncRouteQueryAfterLatestSubmitStatus(latestVersion)
      return
    }
    await message.confirm(
      options.confirmMessage ||
        '提交后当前候选版本将进入审批阶段；审批通过后自动发布生效，本次候选版本中的多项生产配置会统一发布。是否继续？',
      options.confirmTitle || '提交发布'
    )
    const nextVersion = await ProRouteApi.submitAndPublishRouteCandidateVersion({
      id: latestVersion.id
    })
    message.success(resolveRoutePublishSuccessMessage(nextVersion))
    await router.replace({
      name: 'MesProRouteEdit',
      params: { id: routeId.value },
      query: buildPostSubmitPublishQuery(nextVersion)
    })
  } catch (error) {
    if (isRouteConfirmCancel(error)) return
    message.error(resolveRouteOperationErrorMessage(error, '提交候选版本失败，请查看后端返回错误'))
  } finally {
    routeVersionActionLoading.value = false
  }
}

const markListEditCandidateDraftSaved = () => {
  if (routeDraftOrigin.value === LIST_EDIT_ROUTE_DRAFT_ORIGIN) {
    listEditCandidateDraftSaved.value = true
  }
}

const clearListEditDraftExitQuery = async () => {
  if (
    routeDraftOrigin.value !== LIST_EDIT_ROUTE_DRAFT_ORIGIN &&
    discardOnUnsavedExit.value !== LIST_EDIT_DISCARD_ON_UNSAVED_EXIT
  ) {
    return
  }
  const nextQuery = { ...route.query }
  delete nextQuery.routeDraftOrigin
  delete nextQuery.discardOnUnsavedExit
  await router.replace({
    name: 'MesProRouteEdit',
    params: { id: routeId.value },
    query: nextQuery
  })
}

const handleSaved = async () => {
  markListEditCandidateDraftSaved()
  await clearListEditDraftExitQuery()
}

const handleEditProductionConfig = async () => {
  if (!Number.isFinite(routeId.value) || routeId.value <= 0) {
    message.error('进入候选版本失败：缺少有效路线编号')
    return
  }
  routeVersionActionLoading.value = true
  try {
    const candidateResult = await ensureSameSourceDraftCandidateForProductionConfig({
      routeId: routeId.value,
      actionName: '编辑生产配置',
      changeReason: '生产配置编辑创建候选版本',
      confirm: (content, title) => message.confirm(content, title),
      success: (content) => message.success(content),
      existingConfirmMessage:
        '当前路线已有草稿候选版本。确认后进入该候选版本，后续修改会合并到同一个候选版本，发布前不影响生效版本。是否继续？',
      existingConfirmTitle: '进入候选版本',
      createConfirmMessage:
        '生效版本为只读。确认后创建候选版本，后续生产配置修改会在候选版本中连续编辑，发布前不影响生效版本。是否继续？',
      createConfirmTitle: '创建候选版本',
      existingSuccessMessage: '正在进入候选版本编辑',
      createdSuccessMessage: '候选版本已创建，正在进入编辑'
    })
    if (!candidateResult) return
    await router.push({
      name: 'MesProRouteEdit',
      params: { id: routeId.value },
      query: buildRouteCandidateEditQuery(candidateResult.candidate, {
        ...route.query,
        tab: activeRouteTab.value || initialTab.value || 'flow'
      })
    })
  } catch (error) {
    message.error(resolveRouteOperationErrorMessage(error, '进入候选版本失败，请查看后端返回错误'))
  } finally {
    routeVersionActionLoading.value = false
  }
}

const handleSubmitRouteCandidateVersion = async () => {
  await submitRouteCandidateVersion({
    confirmMessage:
      '提交后当前候选版本将进入审批阶段；审批通过后自动发布生效，本次候选版本中的多项生产配置会统一发布。是否继续？',
    confirmTitle: '提交发布'
  })
}

const confirmUnsavedCandidateDraftBeforeExit = async () => {
  if (!shouldPromptUnsavedCandidateDraftBeforeExit.value) return undefined
  const context = routeVersionEditContext.value
  const content = contentRef.value
  if (!context) {
    message.error('退出候选版本失败：缺少路线版本上下文')
    return false
  }
  if (!content?.submitForm) {
    message.error('保存候选草稿失败：表单组件未加载')
    return false
  }
  try {
    await ElMessageBox.confirm(
      isListEditCandidateDraftExitGuardEnabled.value
        ? '当前候选版本还没有保存，是否保存草稿？选择“不保存草稿”会自动丢弃本次列表编辑创建的候选版本。'
        : '当前草稿有未保存修改，是否保存草稿？选择“不保存草稿”会放弃当前页面未保存的修改，已有草稿会保留。',
      '退出确认',
      {
        confirmButtonText: '保存草稿',
        cancelButtonText: '不保存草稿',
        distinguishCancelAndClose: true,
        type: 'warning'
      }
    )
    await content.submitForm()
    markListEditCandidateDraftSaved()
    return true
  } catch (error) {
    if (error === 'cancel') {
      if (isListEditCandidateDraftExitGuardEnabled.value) {
        routeVersionActionLoading.value = true
        try {
          await ProRouteApi.cancelRouteCandidateVersion(context.routeVersionId)
          listEditCandidateDraftDiscarded.value = true
          message.success('未保存的候选版本草稿已丢弃')
          return true
        } catch (cancelError) {
          message.error(resolveRouteOperationErrorMessage(cancelError, '丢弃候选版本草稿失败，请查看后端返回错误'))
          return false
        } finally {
          routeVersionActionLoading.value = false
        }
      } else {
        content.discardRouteCandidateDraftChanges?.()
        message.success('未保存的草稿修改已放弃')
        return true
      }
    }
    if (error === 'close') {
      return false
    }
    message.error(resolveRouteOperationErrorMessage(error, '保存候选草稿失败，请查看后端返回错误'))
    return false
  }
}

const confirmRouteEditPageLeave = async () => {
  const unsavedCandidateResult = await confirmUnsavedCandidateDraftBeforeExit()
  if (unsavedCandidateResult !== undefined) {
    return unsavedCandidateResult
  }
  const content = contentRef.value
  if (!content?.confirmFlowGraphDraftSaveBeforeExit) return true
  return await content.confirmFlowGraphDraftSaveBeforeExit()
}

const handleBackToList = async () => {
  const canLeave = await confirmRouteEditPageLeave()
  if (canLeave === false) return
  routeLeaveAlreadyConfirmed.value = true
  try {
    await router.push('/mes/pro/route')
  } finally {
    routeLeaveAlreadyConfirmed.value = false
  }
}

onBeforeRouteLeave(async () => {
  if (routeLeaveAlreadyConfirmed.value) {
    routeLeaveAlreadyConfirmed.value = false
    return true
  }
  return await confirmRouteEditPageLeave()
})

watch(
  routeCandidateDraftKey,
  () => {
    listEditCandidateDraftSaved.value = false
    listEditCandidateDraftDiscarded.value = false
  },
  { immediate: true }
)

watch(
  () => [isCurrentRouteEditPage.value, routeId.value, initialTab.value, routeVersionEditContextKey.value],
  async () => {
    await loadRoute()
  },
  { immediate: true }
)
</script>

<style scoped lang="scss">
.route-edit-page {
  min-height: calc(100vh - 120px);
}

.route-edit-page__error {
  margin: 8px 0;
}

.route-edit-page__error-content {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #4b5563;
}

.route-edit-page__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 10px 0;
}
</style>
