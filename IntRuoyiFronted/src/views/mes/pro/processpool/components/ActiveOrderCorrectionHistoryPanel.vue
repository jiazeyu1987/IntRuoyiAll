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
          <p>更正时间：{{ formatDateTimeValue(row.revisedAt) }} · 更正人：{{ row.signerName }}</p>
          <p>更正原因：{{ row.reason }}</p>
          <el-table :data="row.changes" aria-label="正式修订字段差异">
            <el-table-column prop="fieldName" label="修改字段" />
            <el-table-column prop="beforeValue" label="本次修改前" />
            <el-table-column prop="afterValue" label="本次修改后" />
          </el-table>
          <el-button data-active-order-correction-signature :disabled="row.verificationStatus !== 'VALID'" @click="$emit('signature', row.signatureId)">
            查看 {{ row.signerName }} 的补正签名证据
          </el-button>
        </article>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
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
</style>
