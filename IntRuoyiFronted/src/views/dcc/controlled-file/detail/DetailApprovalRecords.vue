<template>
  <section ref="recordsElement" class="mt-16px" data-testid="dcc-application-node-records">
    <p class="font-600">审批记录</p>
    <p v-if="!records.length">暂无前序审批或电子签名记录。</p>
    <el-table v-else :data="records" border data-user-table-column-explicit>
      <el-table-column label="审批环节" min-width="160">
        <template #default="{ row }">{{ approvalNodeLabel(row) }}</template>
      </el-table-column>
      <el-table-column label="办理人" min-width="110">
        <template #default="{ row }">
          <div class="approval-record-primary"><span>{{ row.actorNicknameSnapshot || row.actorUsernameSnapshot || '姓名未记录' }}</span></div>
          <div v-if="row.assignmentTarget" class="approval-record-secondary">被指派会签人：{{ row.assignmentTarget.nickname || '姓名未记录' }}</div>
          <div v-else-if="row.confirmedFields.some(field => field.field === 'signoffAssignee')" class="approval-record-secondary">被指派会签人：{{ row.confirmedFields.find(field => field.field === 'signoffAssignee')?.value || '姓名未记录' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="所属部门" min-width="120">
        <template #default="{ row }">
          <div class="approval-record-primary"><span>{{ row.actorDeptNameSnapshot || '部门未记录' }}</span></div>
          <div v-if="row.assignmentTarget" class="approval-record-secondary">会签义务部门：{{ row.assignmentTarget.obligationDepartmentName || '部门未记录' }}</div>
          <div v-else-if="row.confirmedFields.some(field => field.field === 'obligationDepartment')" class="approval-record-secondary">会签义务部门：{{ row.confirmedFields.find(field => field.field === 'obligationDepartment')?.value || '部门未记录' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="处理结果" min-width="110">
        <template #default="{ row }">{{ approvalResultLabel(row) }}</template>
      </el-table-column>
      <el-table-column label="审批意见" min-width="200">
        <template #default="{ row }">
          <div>{{ getDccSignatureOpinionLabel(row.comment) }}</div>
          <p v-for="field in row.confirmedFields.filter(field => !['signoffAssignee', 'obligationDepartment'].includes(field.field))" :key="field.field" class="approval-record-secondary">{{ field.label }}：{{ field.value || '未记录' }}</p>
          <DetailRelationStageEvidence
            v-if="evidence.relationEvidence && row.relationStages.some(stage => stage.snapshot.members.some(member => member.assigneeUserId != null))"
            :evidence="evidence.relationEvidence" :tenant-id="evidence.relationEvidence.tenantId"
            :file-id="String(evidence.controlledFileId)" :application-type="evidence.applicationType"
            :bpm-round="evidence.bpmRound" :attribute-round="evidence.relationEvidence.attributeRound"
            :context-key="row.recordId" :signature-id="row.signatureId" :task-id="row.taskId"
            remediation-only :signature-facts="evidence.signatures" :node-records="evidence.nodeRecords" />
        </template>
      </el-table-column>
      <el-table-column label="处理时间" min-width="175">
        <template #default="{ row }">{{ formatControlledFileDateTime(row.occurredAt) }}</template>
      </el-table-column>
      <el-table-column label="电子签名" min-width="110">
        <template #default="{ row }">{{ row.signatureId ? approvalSignatureStatus(row.signatureEvidenceStatus) : '未签名' }}</template>
      </el-table-column>

    </el-table>
  </section>
</template>
<script setup lang="ts">
import { computed, ref, onMounted, onUpdated, onBeforeUnmount } from 'vue'
import type { ControlledFileApplicationEvidence } from '@/api/dcc/controlledFile/applicationRead'
import { buildApprovalRecords, approvalNodeLabel, approvalResultLabel, approvalSignatureStatus } from './approval-records'
import { formatControlledFileDateTime } from './presentation'
import { getDccSignatureOpinionLabel } from '../shared/signature-evidence'
import DetailRelationStageEvidence from './DetailRelationStageEvidence.vue'
const props = defineProps<{ evidence: ControlledFileApplicationEvidence }>()
const records = computed(() => buildApprovalRecords(props.evidence))
const recordsElement = ref<HTMLElement>()
let alignmentObserver: ResizeObserver | undefined
// Names and departments may wrap independently; reserve the taller primary height in both cells.
const alignAssignmentDetails = () => {
  recordsElement.value?.querySelectorAll('tbody tr').forEach(row => {
    const primaryCells = Array.from(row.querySelectorAll<HTMLElement>('.approval-record-primary'))
    const height = Math.max(0, ...primaryCells.map(cell => cell.firstElementChild!.getBoundingClientRect().height))
    primaryCells.forEach(cell => { cell.style.minHeight = `${height}px` })
  })
}
const observePrimaryCells = () => {
  alignmentObserver?.disconnect()
  recordsElement.value?.querySelectorAll('.approval-record-primary > span').forEach(cell => alignmentObserver?.observe(cell))
  alignAssignmentDetails()
}
onMounted(() => {
  alignmentObserver = new ResizeObserver(alignAssignmentDetails)
  observePrimaryCells()
})
onUpdated(observePrimaryCells)
onBeforeUnmount(() => alignmentObserver?.disconnect())
</script>

<style scoped>
:deep(.el-table__cell) { vertical-align: top; }
.approval-record-primary > span { display: block; }
.approval-record-secondary { margin: 4px 0 0; color: var(--el-text-color-regular); font-size: 13px; line-height: 1.5; }
</style>
