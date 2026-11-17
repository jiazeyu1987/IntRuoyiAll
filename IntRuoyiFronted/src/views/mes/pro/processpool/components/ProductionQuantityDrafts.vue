<template>
  <el-card class="quantity-drafts" data-production-quantity-drafts>
    <template #header>
      <div class="quantity-drafts__header"><strong>历史超限草稿</strong><el-button :loading="loading" @click="load">刷新草稿</el-button></div>
    </template>
    <el-alert type="warning" :closable="false" title="此处仅保留旧规则产生、尚未正式提交的草稿。历史核对不代表正式报工已修改；新正式记录由组长决定驳回或直接修改，超限仅提示。" />
    <el-alert v-if="loadError" type="error" :closable="false" :title="loadError" />
    <el-table v-else v-loading="loading" :data="rows" empty-text="没有待处理的超限填写">
      <el-table-column prop="employeeId" label="填写员工编号" width="155" />
      <el-table-column label="订单 / 批次 / 工序" min-width="220">
        <template #default="{ row }">{{ summary(row) }}</template>
      </el-table-column>
      <el-table-column prop="previousQuantity" label="此前累计" width="110" />
      <el-table-column prop="requestedQuantity" label="本次填写" width="110" />
      <el-table-column prop="upperQuantity" label="数量上限" width="110" />
      <el-table-column label="处理状态" min-width="200">
        <template #default="{ row }">{{ row.status === 'BLOCKED' ? '历史未提交草稿，可恢复提交' : '历史已核对草稿，尚未正式提交' }}<p v-if="row.reviewReason">历史核对意见（仅供追溯）：{{ row.reviewReason }}</p></template>
      </el-table-column>
      <el-table-column v-if="!leader" label="操作" width="130">
        <template #default="{ row }">
          <el-button link type="primary" @click="$emit('restore', row)">恢复本次填写</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOwnQuantityDrafts, getLeaderQuantityDrafts, type ProductionQuantityDraft } from '@/api/mes/pro/feedback/quantityDraft'
const props = defineProps<{ leader?: boolean }>()
defineEmits<{ restore: [draft: ProductionQuantityDraft] }>()
const rows = ref<ProductionQuantityDraft[]>([])
const loading = ref(false)
const loadError = ref('')
const payload = (row: ProductionQuantityDraft) => JSON.parse(row.requestJson)
const summary = (row: ProductionQuantityDraft) => {
  const value = payload(row).feedbackPayload
  return [value.workOrderCode, value.batchCode, value.processName].filter(Boolean).join(' / ') || `工单 ${row.workOrderId} / 工序 ${row.processId}`
}
const load = async () => {
  loading.value = true
  loadError.value = ''
  try { rows.value = await (props.leader ? getLeaderQuantityDrafts() : getOwnQuantityDrafts()) }
  catch (error) { loadError.value = `超限草稿读取失败：${error instanceof Error ? error.message : String(error)}` }
  finally { loading.value = false }
}
onMounted(load)
defineExpose({ load })
</script>
<style scoped>
.quantity-drafts { margin: 12px 0; }
.quantity-drafts__header { display: flex; justify-content: space-between; align-items: center; }
</style>
