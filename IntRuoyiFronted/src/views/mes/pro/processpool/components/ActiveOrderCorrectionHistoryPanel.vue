<template>
  <section class="correction-history" aria-label="正式补正历史">
    <el-button :disabled="!context" data-active-order-correction-history-entry @click="history.open">补正历史</el-button>
    <el-dialog v-model="history.state.visible" title="正式补正历史" width="900px" :teleported="false" @closed="history.close">
      <el-alert v-if="history.state.error" :title="history.state.error" type="error" :closable="false" show-icon role="alert" />
      <el-skeleton v-else-if="history.state.loading" :rows="6" animated />
      <template v-else-if="history.state.timeline">
        <el-empty v-if="!history.state.timeline.corrections.length" description="该正式周期没有补正修订" />
        <article v-for="row in history.state.timeline.corrections" :key="row.revisionId" class="correction-history__revision" data-active-order-correction-revision>
          <h3>{{ row.eventType === 'PRODUCTION_SUBMIT' ? '生产报工' : 'PQC检验' }} · {{ row.processName }}</h3>
          <dl class="correction-history__summary">
            <dt>更正人</dt><dd data-correction-signer-name>{{ row.signerName }}</dd>
            <dt>更正时间</dt><dd>{{ formatDateTimeValue(row.revisedAt) }}</dd>
            <dt>签名时间</dt><dd>{{ formatDateTimeValue(row.signedAt) }}</dd>
            <dt>签名验证</dt>
            <dd>
              <el-tag :type="row.verificationStatus === 'VALID' ? 'success' : 'danger'">
                {{ row.verificationStatus === 'VALID' ? '验证通过' : '验证状态异常' }}
              </el-tag>
            </dd>
          </dl>
          <p>更正原因：<span data-correction-reason>{{ row.reason }}</span></p>
          <el-table :data="row.changes" aria-label="正式修订字段差异">
            <el-table-column prop="fieldName" label="修改字段" />
            <el-table-column label="本次修改前">
              <template #default="{ row: change }"><BusinessEvidenceDetails title="修改前" :field="change.fieldCode" :source="JSON.stringify(change.beforeValue)" /></template>
            </el-table-column>
            <el-table-column label="本次修改后">
              <template #default="{ row: change }"><BusinessEvidenceDetails title="修改后" :field="change.fieldCode" :source="JSON.stringify(change.afterValue)" /></template>
            </el-table-column>
          </el-table>
          <details class="correction-history__references">
            <summary>查看关联编号与原始状态</summary>
            <dl class="correction-history__summary">
              <dt>原事件编号</dt><dd data-correction-event-id>{{ row.eventId }}</dd>
              <dt>正式修订编号</dt><dd data-correction-revision-id>{{ row.revisionId }}</dd>
              <dt>原退回轮次编号</dt><dd data-correction-rejected-review-id>{{ row.supersededReviewId ?? '未记录' }}</dd>
              <dt>签名人编号</dt><dd data-correction-signer-id>{{ row.signerUserId }}</dd>
              <dt>签名记录编号</dt><dd data-correction-signature-id>{{ row.signatureId }}</dd>
              <dt>原始业务类型</dt><dd data-correction-event-type>{{ row.eventType }}</dd>
              <dt>原始验证状态</dt><dd data-correction-verification>{{ row.verificationStatus }}</dd>
            </dl>
          </details>
          <el-button data-active-order-correction-signature :disabled="row.verificationStatus !== 'VALID'" @click="$emit('signature', row.signatureId)">
            查看 {{ row.signerName }} 的补正签名证据
          </el-button>
        </article>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import BusinessEvidenceDetails from './BusinessEvidenceDetails.vue'
import { watch, onBeforeUnmount } from 'vue'
import { formatDateTimeValue } from '@/utils/formatTime'
import { getActiveOrderCorrections } from '@/api/mes/pro/edhr/activeOrderCorrection'
import type { SignatureBusinessContext } from './activeOrderSignatureEvidenceViewer'
import { createActiveOrderCorrectionHistory } from './activeOrderCorrectionHistory'
const props = defineProps<{ context?: SignatureBusinessContext }>()
defineEmits<{ signature: [id: number | string] }>()
const history = createActiveOrderCorrectionHistory(() => props.context,
  source => getActiveOrderCorrections(source.scope, source.identity))
watch(() => props.context, () => history.close(), { deep: true })
onBeforeUnmount(() => history.close())
</script>

<style scoped>
.correction-history { margin: 16px 0; }
.correction-history__revision { border-bottom: 1px solid #e2e8f0; padding: 16px 0; }
.correction-history__revision p { overflow-wrap: anywhere; }
.correction-history__revision .el-button { margin-top: 12px; }
.correction-history__summary { display: grid; grid-template-columns: 120px minmax(0, 1fr); gap: 8px 16px; }
.correction-history__summary dt { color: var(--el-text-color-secondary); }
.correction-history__summary dd { margin: 0; overflow-wrap: anywhere; }
.correction-history__references { margin-top: 16px; }
.correction-history__references summary { cursor: pointer; color: var(--el-color-primary); }
:deep(.el-dialog) { max-width: calc(100vw - 32px); }
</style>
