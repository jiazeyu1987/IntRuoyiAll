<template>
  <div class="w-full">
    <UserSelectV2 :model-value="modelValue" :user-options="candidates" :candidate-loading="loading"
      :candidate-error="error" :disabled="disabled || loading || !candidates.length" :placeholder="placeholder || '请选择签署责任人'"
      @update:model-value="emit('update:modelValue', $event)" />
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-alert v-else-if="selectedUnavailable" title="已选人员当前不具备本节点办理资格，请重新选择合格人员。" type="warning" :closable="false" show-icon />
    <el-alert v-else-if="loaded && !candidates.length" title="当前职责暂无合格候选人，请配置对应节点权限及工作任务查询权限。" type="warning" :closable="false" show-icon />
    <el-button v-if="!disabled && !loading" link type="primary" @click="load">刷新候选人</el-button>
  </div>
</template>
<script setup lang="ts">
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import { getDeviationReviewerCandidates, type DeviationReviewerOption } from '@/api/mes/pro/edhr/deviationReview'
import { useApprovalCenterInvalidationStore } from '@/store/modules/approvalCenterInvalidation'

const props = defineProps<{ modelValue?: number | string; node: string; disabled?: boolean; placeholder?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: number | string | undefined] }>()
const session = useApprovalCenterInvalidationStore()
const candidates = ref<DeviationReviewerOption[]>([])
const loading = ref(false)
const loaded = ref(false)
const error = ref('')
const selectedUnavailable = computed(() => loaded.value && props.modelValue != null
  && !candidates.value.some(row => String(row.id) === String(props.modelValue)))
let generation = 0
const load = async () => {
  const current = ++generation
  const node = props.node
  const sessionKey = session.syncSession().key
  const isCurrent = () => current === generation && node === props.node && session.isCurrent(sessionKey)
  loading.value = true
  loaded.value = false
  error.value = ''
  candidates.value = []
  try {
    const result = await getDeviationReviewerCandidates(node)
    if (!isCurrent()) return
    if (!Array.isArray(result)) throw new Error('签署候选人返回格式无效')
    candidates.value = result
    loaded.value = true
  } catch (cause: any) {
    if (!isCurrent()) return
    error.value = cause?.response?.data?.msg || cause?.message || '签署候选人读取失败，请重试'
  } finally {
    if (isCurrent()) loading.value = false
  }
}
watch(() => [props.node, session.syncSession().key], () => { void load() }, { immediate: true })
onBeforeUnmount(() => generation++)
</script>
