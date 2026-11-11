<template>
  <el-card class="quantity-drafts" data-production-quantity-drafts>
    <template #header>
      <div class="quantity-drafts__header"><strong>超限填写待处理</strong><el-button :loading="loading" @click="load">刷新草稿</el-button></div>
    </template>
    <el-alert type="warning" :closable="false" title="这些填写未形成正式报工，不计入进度。组长先核对本次和历史归属；处理后仍须一线重新提交并通过最新数量校验。" />
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
        <template #default="{ row }">{{ row.status === 'BLOCKED' ? '等待组长核对' : '组长已处理，等待一线重提' }}<p v-if="row.reviewReason">{{ row.reviewReason }}</p></template>
      </el-table-column>
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-button v-if="leader && row.status === 'BLOCKED'" link type="primary" @click="openReview(row)">核对并处理</el-button>
          <el-button v-else-if="!leader" link type="primary" @click="$emit('restore', row)">恢复本次填写</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="reviewOpen" title="核对超限填写" width="650px" :close-on-click-modal="false">
      <template v-if="selected">
        <p>{{ summary(selected) }}</p>
        <p>此前累计 {{ selected.previousQuantity }}，本次 {{ selected.requestedQuantity }}，上限 {{ selected.upperQuantity }}。</p>
        <el-descriptions :column="1" border>
          <el-descriptions-item v-for="material in materials(selected)" :key="material.materialId" :label="material.materialName || `物料 ${material.materialId}`">完成 {{ material.outputQuantity }}，损耗 {{ material.lossQuantity }}</el-descriptions-item>
        </el-descriptions>
        <el-form label-position="top">
          <el-form-item label="核对结论">
            <el-radio-group v-model="decision"><el-radio value="CURRENT_ERROR">本次填写归属或数量有误，退回重填</el-radio><el-radio value="HISTORY_ERROR">历史归属有误，已完成重新分配</el-radio></el-radio-group>
          </el-form-item>
          <el-alert v-if="decision === 'HISTORY_ERROR'" type="info" :closable="false" title="请先在生产提交记录中完成全部或部分重新分配。系统会检查历史调整后是否已为本次填写腾出足够数量。" />
          <el-form-item label="处理说明（请明确正确订单、数量或历史分配调整情况）"><el-input v-model="reason" type="textarea" maxlength="1000" /></el-form-item>
          <el-form-item label="组长电子签名密码"><el-input v-model="password" type="password" autocomplete="off" /></el-form-item>
        </el-form>
      </template>
      <template #footer><el-button :disabled="saving" @click="reviewOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitReview">签署处理结果</el-button></template>
    </el-dialog>
  </el-card>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOwnQuantityDrafts, getLeaderQuantityDrafts, reviewQuantityDraft, type ProductionQuantityDraft } from '@/api/mes/pro/feedback/quantityDraft'
const props = defineProps<{ leader?: boolean }>()
defineEmits<{ restore: [draft: ProductionQuantityDraft] }>()
const rows = ref<ProductionQuantityDraft[]>([])
const loading = ref(false)
const loadError = ref('')
const saving = ref(false)
const reviewOpen = ref(false)
const selected = ref<ProductionQuantityDraft>()
const decision = ref('CURRENT_ERROR')
const reason = ref('')
const password = ref('')
const payload = (row: ProductionQuantityDraft) => JSON.parse(row.requestJson)
const summary = (row: ProductionQuantityDraft) => {
  const value = payload(row).feedbackPayload
  return [value.workOrderCode, value.batchCode, value.processName].filter(Boolean).join(' / ') || `工单 ${row.workOrderId} / 工序 ${row.processId}`
}
const materials = (row: ProductionQuantityDraft) => payload(row).materialDetails || []
const load = async () => {
  loading.value = true
  loadError.value = ''
  try { rows.value = await (props.leader ? getLeaderQuantityDrafts() : getOwnQuantityDrafts()) }
  catch (error) { loadError.value = `超限草稿读取失败：${error instanceof Error ? error.message : String(error)}` }
  finally { loading.value = false }
}
const openReview = (row: ProductionQuantityDraft) => {
  selected.value = row; decision.value = 'CURRENT_ERROR'; reason.value = ''; password.value = ''; reviewOpen.value = true
}
const submitReview = async () => {
  if (!selected.value || saving.value) return
  if (!reason.value.trim() || !password.value) { ElMessage.error('请填写处理说明和电子签名密码'); return }
  saving.value = true
  try {
    await reviewQuantityDraft({ id: selected.value.id, expectedVersion: selected.value.version, decision: decision.value, reason: reason.value.trim(), signaturePassword: password.value })
    reviewOpen.value = false; ElMessage.success('处理结果已签署，请由一线更正后重新提交'); await load()
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : String(error)) }
  finally { password.value = ''; saving.value = false }
}
onMounted(load)
defineExpose({ load })
</script>
<style scoped>
.quantity-drafts { margin: 12px 0; }
.quantity-drafts__header { display: flex; justify-content: space-between; align-items: center; }
</style>
