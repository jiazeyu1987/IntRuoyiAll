<template>
  <el-dialog v-model="visible" title="签名原始证据与核验诊断" width="850px" @close="loader.clear()">
    <el-alert v-if="state.error" :title="state.error" type="error" :closable="false" show-icon />
    <el-skeleton v-else-if="state.loading" :rows="8" animated />
    <template v-else-if="state.selected">
      <el-alert title="此处展示原始签名的本地核验诊断；业务完整追溯及外部可信时间须分别核验。" type="info" :closable="false" />
      <el-descriptions :column="2" border class="evidence-fields">
        <el-descriptions-item label="全局编号">{{ state.selected.globalId }}</el-descriptions-item>
        <el-descriptions-item label="签名编号">{{ state.selected.evidence.id }}</el-descriptions-item>
        <el-descriptions-item label="原签时姓名">{{ state.selected.evidence.actorDisplayName || '未记录' }}</el-descriptions-item>
        <el-descriptions-item label="签署账号编号">{{ state.selected.evidence.actorId }}</el-descriptions-item>
        <el-descriptions-item label="业务动作">{{ state.selected.evidence.actionCode }}</el-descriptions-item>
        <el-descriptions-item label="签名含义">{{ state.selected.evidence.meaningLabel || '未记录' }}</el-descriptions-item>
        <el-descriptions-item label="签署时间">{{ signatureRecordSignedAt(state.selected.evidence.signedAt) }}</el-descriptions-item>
        <el-descriptions-item label="证据格式">{{ state.selected.verification.evidenceFormatVersion }}</el-descriptions-item>
        <el-descriptions-item label="签署原因" :span="2">{{ state.selected.evidence.reason || '未记录' }}</el-descriptions-item>
        <el-descriptions-item label="核验状态">{{ signatureRecordVerificationDisplay(state.selected.verification).status }}</el-descriptions-item>
        <el-descriptions-item label="本地摘要核验">{{ evidenceDimensionLabel(state.selected.verification.digestStatus) }}</el-descriptions-item>
        <el-descriptions-item label="时间值核验">{{ evidenceDimensionLabel(state.selected.verification.timeConsistencyStatus) }}</el-descriptions-item>
        <el-descriptions-item label="创建审计绑定">{{ evidenceDimensionLabel(state.selected.verification.auditBindingStatus) }}</el-descriptions-item>
        <el-descriptions-item label="时间来源">{{ evidenceDimensionLabel(state.selected.verification.timeSource) }}</el-descriptions-item>
        <el-descriptions-item label="时间信任">{{ evidenceDimensionLabel(state.selected.verification.timeTrustStatus) }}</el-descriptions-item>
        <el-descriptions-item label="核验范围" :span="2">{{ signatureRecordVerificationDisplay(state.selected.verification).scope }}</el-descriptions-item>
        <el-descriptions-item label="核验原因" :span="2">{{ signatureRecordVerificationDisplay(state.selected.verification).reasons }}</el-descriptions-item>
        <el-descriptions-item label="业务主题" :span="2">{{ state.selected.evidence.subjectType }} / {{ state.selected.evidence.subjectId }}</el-descriptions-item>
        <el-descriptions-item label="内容摘要" :span="2">{{ state.selected.evidence.contentHash || '未记录' }}</el-descriptions-item>
        <el-descriptions-item label="证据摘要" :span="2">{{ state.selected.evidence.evidenceHash || '未记录' }}</el-descriptions-item>
      </el-descriptions>
      <ReadableData class="evidence-fields" title="签名内容明细" :source="state.selected.evidence.canonicalContentJson" />
    </template>
    <template #footer><el-button @click="visible = false">关闭</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import ReadableData from '@/components/BusinessData/ReadableData.vue'
import { onBeforeUnmount, reactive, ref } from 'vue'
import { getSignatureGovernanceRecordEvidence, type SignatureGovernanceEvidenceDetail } from '@/api/signature-governance/records'
import { createSignatureRecordEvidenceLoader, evidenceDimensionLabel, signatureRecordVerificationDisplay, signatureRecordSignedAt } from './signatureRecordEvidence'
const visible = ref(false)
const state = reactive<{ loading: boolean; error: string; selected?: SignatureGovernanceEvidenceDetail }>({ loading: false, error: '' })
const loader = createSignatureRecordEvidenceLoader(getSignatureGovernanceRecordEvidence, state)
const open = (globalId: string) => { visible.value = true; void loader.open(globalId) }
defineExpose({ open })
onBeforeUnmount(loader.clear)
</script>

<style scoped>
.evidence-fields { margin-top: 16px; overflow-wrap: anywhere; }
pre { white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
