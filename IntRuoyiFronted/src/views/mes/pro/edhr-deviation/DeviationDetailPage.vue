<template>
  <ContentWrap>
    <div class="mb-16px flex items-center justify-between">
      <h2>偏差详情</h2>
      <el-button @click="router.push('/user/profile')">返回个人工作台</el-button>
    </div>
    <el-alert v-if="!deviationId" title="偏差编号无效，请从本人待办重新进入" type="error" :closable="false" />
    <el-alert v-else-if="taskNavigationError" :title="taskNavigationError" type="error" :closable="false" />
    <div v-else-if="taskNavigationLoading" v-loading="true" class="min-h-100px"></div>
    <DeviationDetail v-else :key="route.fullPath" :id="deviationId" :initial-tab="initialTab" />
  </ContentWrap>
</template>
<script setup lang="ts">
import { getDeviationTaskNavigation } from '@/api/mes/pro/edhr/deviationTaskNavigation'
import { resolveDeviationTaskTab } from '@/utils/edhrWorkTaskNavigation'
import { requireExactJavaLong } from '@/utils/exactJavaLong'
import { useApprovalCenterInvalidationStore } from '@/store/modules/approvalCenterInvalidation'
import DeviationDetail from './DeviationDetail.vue'
const route = useRoute()
const router = useRouter()
const session = useApprovalCenterInvalidationStore()
const deviationId = computed(() => {
  const id = route.query.deviationId
  if (typeof id !== 'string' || !/^[1-9]\d*$/.test(id) || !Number.isSafeInteger(Number(id))) return undefined
  return Number(id)
})
const taskNavigationLoading = ref(false)
const taskNavigationError = ref('')
const initialTab = ref<'corrective' | 'disposition'>()
let sequence = 0
const resolveTaskContext = async () => {
  const currentSequence = ++sequence
  const routeKey = route.fullPath
  const sessionKey = session.syncSession().key
  const isCurrent = () => currentSequence === sequence && route.fullPath === routeKey && session.isCurrent(sessionKey)
  initialTab.value = undefined
  taskNavigationError.value = ''
  taskNavigationLoading.value = false
  if (route.query.workTaskId === undefined || !deviationId.value) return
  taskNavigationLoading.value = true
  try {
    if (typeof route.query.workTaskId !== 'string') throw new Error('工作任务编号无效，请从本人待办重新进入')
    const taskId = requireExactJavaLong(route.query.workTaskId, '工作任务编号')
    const id = String(deviationId.value)
    const task = await getDeviationTaskNavigation(taskId)
    if (!isCurrent()) return
    initialTab.value = resolveDeviationTaskTab(task, taskId, id)
  } catch (error: any) {
    if (isCurrent()) taskNavigationError.value = error?.response?.data?.msg || error?.message || '工作任务校验失败，请从本人待办重新进入'
  } finally {
    if (isCurrent()) taskNavigationLoading.value = false
  }
}
watch(() => [route.fullPath, session.syncSession().key], resolveTaskContext, { immediate: true })
onBeforeUnmount(() => sequence++)
</script>
