<template>
  <el-card shadow="never" class="mt-16px" header="变更生效通知与确认" v-loading="loading">
    <el-alert v-if="error" type="error" :closable="false" :title="error" />
    <el-table v-else :data="records" empty-text="暂无变更通知记录">
      <el-table-column prop="versionId" label="版本记录" />
      <el-table-column prop="recipientUserId" label="接收人账号ID" />
      <el-table-column prop="sentAt" label="发送时间" :formatter="dateTimeValueFormatter" />
      <el-table-column label="确认记录">
        <template #default="{ row }">
          <span v-if="row.confirmedAt">已确认：{{ formatDateTimeValue(row.confirmedAt) }}</span>
          <el-button v-else-if="messageId" type="primary" :loading="saving" @click="confirm">
            确认已知悉本次变更
          </el-button>
          <span v-else>待本人确认</span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { dateTimeValueFormatter, formatDateTimeValue } from '@/utils/formatTime'
import { listChangeConfirmations, getMyChangeConfirmation, confirmMyCertificateChange,
  type ChangeConfirmation } from '@/api/dcc/registrationCertificate/changeConfirmations'

const props = defineProps<{ certificateId?: number | string; messageId?: number | string }>()
const records = ref<ChangeConfirmation[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
let generation = 0
watch(() => [props.certificateId, props.messageId], async () => {
  const token = ++generation
  records.value = []
  error.value = ''
  loading.value = true
  try {
    if (Boolean(props.certificateId) === Boolean(props.messageId)) throw new Error('变更通知读取身份无效')
    const result = props.messageId
      ? [await getMyChangeConfirmation(props.messageId)]
      : await listChangeConfirmations(props.certificateId!)
    if (token === generation) records.value = result
  } catch (cause) {
    if (token === generation) error.value = cause instanceof Error ? cause.message : '读取变更通知记录失败'
  } finally { if (token === generation) loading.value = false }
}, { immediate: true })

const confirm = async () => {
  if (!props.messageId || saving.value || loading.value) return
  const token = generation
  saving.value = true
  error.value = ''
  try {
    const result = await confirmMyCertificateChange(props.messageId)
    if (token === generation) records.value = [result]
  } catch (cause) {
    if (token === generation) error.value = cause instanceof Error ? cause.message : '确认本次变更失败'
  } finally { saving.value = false }
}
</script>
