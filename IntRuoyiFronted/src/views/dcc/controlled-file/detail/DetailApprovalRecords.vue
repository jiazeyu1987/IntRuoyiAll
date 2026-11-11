<template>
  <section class="mt-16px" data-testid="dcc-application-node-records">
    <p class="font-600">审批记录</p>
    <p v-if="!records.length">暂无前序审批或电子签名记录。</p>
    <el-table v-else :data="records" border data-user-table-column-explicit>
      <el-table-column label="审批环节" min-width="160">
        <template #default="{ row }">{{ row.nodeName || '审批环节名称未记录' }}</template>
      </el-table-column>
      <el-table-column label="办理人" min-width="110">
        <template #default="{ row }">{{ row.actorNicknameSnapshot || row.actorUsernameSnapshot || '姓名未记录' }}</template>
      </el-table-column>
      <el-table-column label="所属部门" min-width="120">
        <template #default="{ row }">{{ row.actorDeptNameSnapshot || '部门未记录' }}</template>
      </el-table-column>
      <el-table-column label="处理结果" min-width="110">
        <template #default="{ row }">{{ approvalResultLabel(row) }}</template>
      </el-table-column>
      <el-table-column label="审批意见" min-width="200">
        <template #default="{ row }">{{ getDccSignatureOpinionLabel(row.comment) }}</template>
      </el-table-column>
      <el-table-column label="处理时间" min-width="175">
        <template #default="{ row }">{{ formatControlledFileDateTime(row.occurredAt) }}</template>
      </el-table-column>
      <el-table-column label="电子签名" min-width="110">
        <template #default="{ row }">{{ row.signatureId ? approvalSignatureStatus(row.signatureEvidenceStatus) : '未签名' }}</template>
      </el-table-column>
      <el-table-column type="expand">
        <template #default="{ row }">
          <template v-if="row.signatureId">
            <p>签名人：{{ row.signature?.actorNicknameSnapshot || '姓名未记录' }}</p>
            <p>签名时间：{{ formatControlledFileDateTime(row.signature?.signedAt) }}</p>
            <p>有效状态：{{ approvalSignatureStatus(row.signatureEvidenceStatus) }}</p>
          </template>
          <p v-for="field in row.confirmedFields" :key="field.field">{{ field.label }}：{{ field.value || '未记录' }}</p>
          <p v-if="row.assignmentTarget && !row.confirmedFields.some(field => field.field === 'signoffAssignee')">
            指派会签人：{{ row.assignmentTarget.nickname || '姓名未记录' }}；会签部门：{{ row.assignmentTarget.obligationDepartmentName || '部门未记录' }}
          </p>
          <DetailRelationStageEvidence
            v-if="evidence.relationEvidence && row.relationStages.some(stage => stage.snapshot.members.some(member => member.assigneeUserId != null))"
            :evidence="evidence.relationEvidence"
            :tenant-id="evidence.relationEvidence.tenantId"
            :file-id="String(evidence.controlledFileId)"
            :application-type="evidence.applicationType"
            :bpm-round="evidence.bpmRound"
            :attribute-round="evidence.relationEvidence.attributeRound"
            :context-key="row.recordId"
            :signature-id="row.signatureId"
            :task-id="row.taskId"
            remediation-only
            :signature-facts="evidence.signatures"
            :node-records="evidence.nodeRecords"
          />
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import type { ControlledFileApplicationEvidence } from '@/api/dcc/controlledFile/applicationRead'
import { buildApprovalRecords, approvalResultLabel, approvalSignatureStatus } from './approval-records'
import { formatControlledFileDateTime } from './presentation'
import { getDccSignatureOpinionLabel } from '../shared/signature-evidence'
import DetailRelationStageEvidence from './DetailRelationStageEvidence.vue'
const props = defineProps<{ evidence: ControlledFileApplicationEvidence }>()
const records = computed(() => buildApprovalRecords(props.evidence))
</script>
