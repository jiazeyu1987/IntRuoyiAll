<template>
  <div class="w-full">
    <UserSelectV2 :model-value="modelValue" :user-options="candidates" :candidate-loading="loading"
      :candidate-error="error" :disabled="disabled || loading || !candidates.length" placeholder="请选择偏差管理员"
      @update:model-value="emit('update:modelValue', $event)" @change="emit('change', $event)" />
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-alert v-else-if="loaded && !candidates.length" title="当前租户暂无可接收偏差的启用管理员，请联系管理员分配偏差管理员角色及权限。" type="warning" :closable="false" show-icon />
    <el-button v-if="!loading" link type="primary" @click="load">刷新偏差管理员</el-button>
  </div>
</template>
<script setup lang="ts">
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import { getDeviationReceiverCandidates, type DeviationAdministratorOption } from '@/api/mes/pro/edhr/deviation'
import { useApprovalCenterInvalidationStore } from '@/store/modules/approvalCenterInvalidation'
const props = defineProps<{ modelValue?: number | string; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: number | string | undefined]; change: [value: any] }>()
const session = useApprovalCenterInvalidationStore()
const candidates = ref<DeviationAdministratorOption[]>([])
const loading = ref(false)
const loaded = ref(false)
const error = ref('')
let generation = 0
const load = async () => {
  const current = ++generation
  const sessionKey = session.syncSession().key
  const isCurrent = () => current === generation && session.isCurrent(sessionKey)
  loading.value = true; loaded.value = false; error.value = ''; candidates.value = []
  try {
    const result = await getDeviationReceiverCandidates()
    if (!isCurrent()) return
    if (!Array.isArray(result)) throw new Error('偏差管理员候选返回格式无效')
    candidates.value = result; loaded.value = true
    if (props.modelValue != null && !result.some(row => String(row.id) === String(props.modelValue))) {
      emit('update:modelValue', undefined); emit('change', undefined)
    }
  } catch (cause: any) {
    if (!isCurrent()) return
    error.value = cause?.response?.data?.msg || cause?.message || '偏差管理员读取失败，请重试'
    emit('update:modelValue', undefined); emit('change', undefined)
  } finally { if (isCurrent()) loading.value = false }
}
watch(() => session.syncSession().key, () => { generation++; candidates.value = []; emit('update:modelValue', undefined); void load() })
onBeforeUnmount(() => generation++)
onMounted(load)
</script>
