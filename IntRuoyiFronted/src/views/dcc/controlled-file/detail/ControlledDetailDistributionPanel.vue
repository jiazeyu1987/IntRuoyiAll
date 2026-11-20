<template>
  <section class="controlled-detail-distribution" data-testid="dcc-detail-distribution-panel">
    <section class="controlled-detail-section">
      <h3>分发与签收</h3>
      <p class="controlled-detail-distribution__summary">
        <el-tag :type="file.distributionCompleted === true ? 'success' : 'info'">{{ file.distributionCompleted === true ? '分发节点已完成' : file.distributionCompleted === false ? '分发节点尚未完成' : '分发节点结果未记录' }}</el-tag>
        <span>{{ hasDepartmentReceipt ? '已有部门分发表回执' : '暂无部门分发表回执' }}</span>
      </p>
      <el-alert v-if="receiptError" :title="receiptError" type="error" :closable="false" />
      <el-table data-user-table-column-explicit v-loading="loading" :data="rows" row-key="key" empty-text="暂无获授权的分发与签收记录">
        <el-table-column label="接收部门" min-width="170"><template #default="{ row }">{{ row.departmentName || departmentNames.get(Number(row.departmentId)) || '未记录部门名称' }}</template></el-table-column>
        <el-table-column label="接收人" min-width="170"><template #default="{ row }">{{ getControlledDetailPersonText(row.userName || userNames.get(Number(row.userId))) || '未记录接收人姓名' }}</template></el-table-column>
        <el-table-column label="下发渠道" min-width="140"><template #default="{ row }">{{ getDistributionMediumLabel(row.medium) }}<span v-if="row.messageStatus === 'FAILED'" class="controlled-detail-muted"> · 发送失败{{ row.errorMessage ? `：${row.errorMessage}` : '' }}</span></template></el-table-column>
        <el-table-column label="阅读时间" min-width="180"><template #default="{ row }">{{ formatControlledFileDateTime(row.readAt) }}</template></el-table-column>
        <el-table-column label="接收人签收" min-width="240"><template #default="{ row }">
          <el-tag :type="row.acknowledgedAt ? 'success' : 'info'">{{ row.acknowledgedAt ? '已签收' : getDistributionStatusLabel(row.status, file) }}</el-tag>
          <span class="controlled-detail-distribution__time">{{ row.acknowledgedAt ? formatControlledFileDateTime(row.acknowledgedAt) : '' }}</span>
          <div v-if="row.recoveredAt" class="controlled-detail-muted">已回收 {{ formatControlledFileDateTime(row.recoveredAt) }}</div>
        </template></el-table-column>
      </el-table>
      <el-button v-if="canReadReceipts" class="controlled-detail-distribution__refresh" link type="primary" :loading="loading" @click="loadReceipts">刷新签收记录</el-button>
      <el-collapse v-model="expanded" class="controlled-detail-distribution__original">
        <el-collapse-item title="查看原分发记录" name="original">
          <p class="controlled-detail-muted">原分发摘要独立保留；接收人后续签收以上表的实际记录为准。</p>
          <el-table data-user-table-column-explicit :data="file.distributionStatuses || []" empty-text="暂无原分发摘要">
            <el-table-column label="接收部门" min-width="160"><template #default="{ row }">{{ departmentNames.get(row.departmentId) || '未记录部门名称' }}</template></el-table-column>
            <el-table-column label="下发渠道" min-width="120"><template #default="{ row }">{{ getDistributionMediumLabel(row.distributionMedium) }}</template></el-table-column>
            <el-table-column label="原分发摘要" min-width="160"><template #default="{ row }">{{ getDistributionStatusLabel(row.status, file) }}</template></el-table-column>
            <el-table-column label="部门回执时间" min-width="180"><template #default="{ row }">{{ formatControlledFileDateTime(row.acknowledgedAt) }}</template></el-table-column>
            <el-table-column label="回执人" min-width="140"><template #default="{ row }">{{ getControlledDetailPersonText(userNames.get(row.acknowledgedBy)) || '—' }}</template></el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </section>
    <section class="controlled-detail-section">
      <h3>培训</h3>
      <dl class="controlled-detail-facts controlled-detail-facts--labels">
        <div><dt>培训要求</dt><dd>{{ file.needTraining === true ? '需要培训' : file.needTraining === false ? '无需培训' : '未记录培训要求' }}</dd></div>
        <div><dt>培训对象</dt><dd><template v-if="trainingTargets.length"><span v-for="(target, index) in trainingTargets" :key="target.id">{{ index ? '；' : '' }}{{ departmentNames.get(target.departmentId) || '未记录部门名称' }}：{{ target.names.join('、') || '未记录培训人员' }}</span></template><span v-else>暂无培训对象记录</span></dd></div>
        <div><dt>线下培训</dt><dd>
          <span>{{ file.trainingRecordAvailable === true ? '记录已上传' : file.trainingRecordAvailable === false ? '尚无已上传记录' : '未返回记录状态' }}</span>
          <el-button v-if="trainingAttachments.length" link type="primary" @click="emit('locate-training-attachment', trainingAttachments[0].attachmentId)">在附件中查看培训记录单</el-button>
          <span v-else-if="file.trainingRecordAvailable === true" class="controlled-detail-muted"> · 当前附件清单无可定位的培训记录单</span>
        </dd></div>
      </dl>
      <el-table data-user-table-column-explicit v-if="trainingRows.length" :data="trainingRows" empty-text="暂无培训确认记录">
        <el-table-column label="部门" min-width="170"><template #default="{ row }">{{ departmentNames.get(row.departmentId) || '未记录部门名称' }}</template></el-table-column>
        <el-table-column label="培训对象" min-width="170"><template #default="{ row }">{{ getControlledDetailPersonText(userNames.get(row.userId)) || '未记录人员姓名' }}</template></el-table-column>
        <el-table-column label="培训状态" min-width="150"><template #default="{ row }">{{ getTrainingStatusLabel(row.status) }}</template></el-table-column>
        <el-table-column label="确认时间" min-width="180"><template #default="{ row }">{{ formatControlledFileDateTime(row.acknowledgedAt) }}</template></el-table-column>
      </el-table>
    </section>
  </section>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import type { ControlledFileVO } from '@/api/dcc/controlledFile/workflow'
import { getDistributionRecipientReceipts } from '@/api/dcc/controlledFile/distribution'
import { validateDistributionFacts, type DistributionRecipientFact } from '../workflow/distribution-facts'
import { checkPermi } from '@/utils/permission'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { useUserStoreWithOut } from '@/store/modules/user'
import { buildControlledDetailDistributionRows, canReadControlledDistributionReceipts, getControlledDetailTrainingAttachments } from './controlled-detail-distribution'
import { getControlledDetailPersonText } from './controlled-detail-people'
import { formatControlledFileDateTime, getDistributionMediumLabel, getDistributionStatusLabel, getTrainingStatusLabel } from './presentation'

const props = defineProps<{
  file: ControlledFileVO
  userNames: Map<number, string>
  departmentNames: Map<number, string>
  readAuthority?: () => string
}>()
const emit = defineEmits<{ 'locate-training-attachment': [attachmentId: number | string] }>()
const userStore = useUserStoreWithOut()
const canReadReceipts = computed(() => canReadControlledDistributionReceipts(props.file,
  checkPermi(['dcc:controlled-file:distribute', 'dcc:controlled-file:edit'])))
const contextKey = computed(() => JSON.stringify([
  props.file.id, props.file.versionNo, getTenantId(), getVisitTenantId(), userStore.getUser.id,
  canReadReceipts.value, props.readAuthority?.()
]))
const receipts = ref<DistributionRecipientFact[]>([]), receiptError = ref(''), loading = ref(false)
const expanded = ref<string[]>([])
let generation = 0
const loadReceipts = async () => {
  const token = ++generation, context = contextKey.value, file = props.file
  receipts.value = []; receiptError.value = ''; loading.value = false
  if (!canReadReceipts.value) return
  loading.value = true
  try {
    const result = await getDistributionRecipientReceipts(String(file.id))
    if (token === generation && context === contextKey.value)
      receipts.value = validateDistributionFacts(result, file.id, file.versionNo)
  } catch (error) {
    if (token === generation && context === contextKey.value)
      receiptError.value = error instanceof Error ? error.message : '签收记录读取失败，请刷新后重试'
  } finally {
    if (token === generation && context === contextKey.value) loading.value = false
  }
}
const rows = computed(() => buildControlledDetailDistributionRows(props.file, receipts.value))
const hasDepartmentReceipt = computed(() => (props.file.distributionStatuses || []).some(row => row.acknowledgedAt != null))
const trainingTargets = computed(() => (props.file.trainingStatuses || []).map(row => ({
  id: row.id, departmentId: row.departmentId,
  names: row.assignments.map(assignment => getControlledDetailPersonText(props.userNames.get(assignment.userId)) || '未记录人员姓名')
})))
const trainingRows = computed(() => (props.file.trainingStatuses || []).flatMap(row =>
  row.assignments.map(assignment => ({ ...assignment, departmentId: row.departmentId }))))
const trainingAttachments = computed(() => getControlledDetailTrainingAttachments(props.file))
watch([contextKey, () => props.file], () => { expanded.value = []; void loadReceipts() }, { immediate: true, flush: 'sync' })
onBeforeUnmount(() => { generation++; receipts.value = [] })
</script>
<style scoped>
@import './controlled-detail-panels.css';
.controlled-detail-distribution { display: grid; gap: 14px; min-width: 0; }
.controlled-detail-distribution__summary { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin: 0 0 12px; font-size: 13px; line-height: 20px; }
.controlled-detail-distribution__time { margin-left: 10px; }
.controlled-detail-distribution__refresh { margin-top: 12px; }
.controlled-detail-distribution__original { margin-top: 12px; }
.controlled-detail-distribution__original :deep(.el-collapse-item__content) { padding-left: 0; padding-right: 0; }
</style>
