<template>
  <section v-if="canQuery" data-release-outcome-recovery>
    <el-divider content-position="left">待补发的放行结果通知</el-divider>
    <el-button :loading="loading" @click="load">刷新补发列表</el-button>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-table v-if="rows.length" :data="rows" row-key="id" border>
      <el-table-column prop="workOrderCode" label="原生产订单" min-width="250" />
      <el-table-column prop="batchCode" label="原批号" min-width="180" />
      <el-table-column label="通知内容" width="150">
        <template #default="{ row }">{{ labels[row.eventType] }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <ReleaseTaskNotificationEntry :work-task-id="row.workTaskId">查看回执并补发</ReleaseTaskNotificationEntry>
        </template>
      </el-table-column>
    </el-table>
    <p v-else-if="!loading && !error">暂无待补发的放行结果通知</p>
  </section>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useUserStore } from '@/store/modules/user'
import { listOwnPendingReleaseOutcomes, type ReleaseOutcomeRecovery } from '@/api/mes/pro/productionRelease/notification'
import ReleaseTaskNotificationEntry from './ReleaseTaskNotificationEntry.vue'

const props = defineProps<{ active: boolean }>()
const userStore = useUserStore()
const canQuery = computed(() => userStore.permissions.has('*:*:*') || userStore.permissions.has('mes:pro-edhr-work-task:query'))
const rows = ref<ReleaseOutcomeRecovery[]>([])
const loading = ref(false)
const error = ref('')
const labels = { PQC_APPROVED: 'PQC生产放行通过', PQC_REJECTED: 'PQC生产放行驳回',
  MARKET_APPROVED: '上市放行通过', MARKET_REJECTED: '上市放行驳回', MARKET_WITHDRAWN: '上市放行撤回' }
let generation = 0
const load = async () => {
  const token = ++generation
  rows.value = []
  error.value = ''
  if (!props.active || !canQuery.value) { loading.value = false; return }
  loading.value = true
  try {
    const result = await listOwnPendingReleaseOutcomes()
    if (token !== generation) return
    if (!Array.isArray(result) || result.some(row => !/^[1-9][0-9]*$/.test(row.id) ||
      !/^[1-9][0-9]*$/.test(row.workTaskId) || !row.workOrderCode || !row.batchCode || !(row.eventType in labels))) {
      throw new Error('放行结果通知身份或内容无效，请核对原通知记录')
    }
    rows.value = result
  } catch (failure) {
    if (token === generation) error.value = (failure as Error)?.message || '放行结果通知查询失败'
  } finally {
    if (token === generation) loading.value = false
  }
}
watch(() => [props.active, canQuery.value, userStore.getUser.id], load, { immediate: true })
onBeforeUnmount(() => { generation += 1 })
</script>
