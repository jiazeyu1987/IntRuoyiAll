<template>
  <section v-if="source" data-active-order-rework-source>
    <el-alert v-if="location.error" :title="location.error" type="error" :closable="false" show-icon />
    <el-descriptions v-else title="返工来源" :column="2" border size="small">
      <el-descriptions-item label="原生产周期">
        <router-link v-if="location.target" :to="location.target" data-active-order-rework-source-link>{{ source.sourceActiveOrderId }}</router-link>
        <span v-else data-active-order-rework-source-readonly>{{ source.sourceActiveOrderId }}（完整原周期批记录需历史追溯权限）</span>
      </el-descriptions-item>
      <el-descriptions-item label="原周期状态">已结束并转入返工</el-descriptions-item>
      <el-descriptions-item label="不合格评审">{{ source.reviewCode }}（{{ source.reviewId }}）</el-descriptions-item>
      <el-descriptions-item label="QA处置">返工</el-descriptions-item>
      <el-descriptions-item label="不合格原因">{{ source.nonconformanceReason }}</el-descriptions-item>
      <el-descriptions-item label="QA意见">{{ source.reviewOpinion }}</el-descriptions-item>
      <el-descriptions-item label="QA签署人">{{ source.qaSignature.signerName }}（账号 {{ source.qaUserId }}）</el-descriptions-item>
      <el-descriptions-item label="QA签名编号" data-active-order-rework-qa-signature>{{ source.qaSignature.signatureId }}</el-descriptions-item>
      <el-descriptions-item label="QA签署时间">{{ formatDateTimeValue(source.qaSignature.signedAt) }}</el-descriptions-item>
      <el-descriptions-item label="证据核验">{{ source.qaSignatureVerification.verificationStatus }}</el-descriptions-item>
      <el-descriptions-item label="签名内容哈希" :span="2">{{ source.qaSignatureEvidence.contentHash }}</el-descriptions-item>
      <el-descriptions-item label="签名证据哈希" :span="2">{{ source.qaSignatureEvidence.evidenceHash }}</el-descriptions-item>
    </el-descriptions>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/store/modules/user'
import type { TeamLeaderActiveOrderReworkSourceRespVO } from '@/api/mes/pro/processPool/teamLeader'
import { formatDateTimeValue } from '@/utils/formatTime'
import { buildReworkSourceDetailLocation } from './activeOrderReworkSourceLocation'

const props = defineProps<{ activeOrderId: number | string; source?: TeamLeaderActiveOrderReworkSourceRespVO }>()
const userStore = useUserStore()
const canOpenSourceHistory = computed(() => userStore.permissions.has('*:*:*') || userStore.permissions.has('mes:pro-edhr-batch-execution:query'))
const location = computed(() => {
  try {
    return { target: buildReworkSourceDetailLocation(props.activeOrderId, props.source, canOpenSourceHistory.value), error: '' }
  } catch (error) {
    return { target: undefined, error: error instanceof Error ? error.message : '返工来源读取失败' }
  }
})
</script>
