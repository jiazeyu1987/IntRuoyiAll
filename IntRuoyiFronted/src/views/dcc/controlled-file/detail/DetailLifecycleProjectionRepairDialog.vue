<template>
  <el-dialog :model-value="modelValue" title="校验生命周期投影" width="900px" destroy-on-close @close="close" data-testid="dcc-lifecycle-projection-repair-dialog">
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mb-12px" />
    <div v-loading="loading">
      <template v-if="preview">
        <el-alert title="本次维护仅修正共享生命周期投影；原文件、审批历史和电子签名保持原有事实。" type="info" :closable="false" class="mb-12px" />
        <el-descriptions :column="2" border>
          <el-descriptions-item label="文件 / Master / 共享投影">{{ preview.fileId }} / {{ preview.masterId ?? '未记录' }} / {{ preview.versionRefId ?? '未记录' }}</el-descriptions-item>
          <el-descriptions-item label="租户 / 版本">{{ preview.tenantId }} / {{ preview.versionNo }}</el-descriptions-item>
          <el-descriptions-item label="原 DCC 状态">{{ preview.dccStatus }}</el-descriptions-item>
          <el-descriptions-item label="原共享状态">{{ preview.canonicalStatus ?? '未记录' }} / {{ preview.domainStatus ?? '未记录' }}</el-descriptions-item>
          <el-descriptions-item label="原 BPM">{{ preview.processInstanceId ?? '未记录' }}</el-descriptions-item>
          <el-descriptions-item label="原签名记录">{{ preview.signatureIds.join('、') || '未记录' }}</el-descriptions-item>
          <el-descriptions-item label="受控 / 实际生效">{{ preview.controlledTime ?? '未记录' }} / {{ preview.activatedTime ?? '未记录' }}</el-descriptions-item>
          <el-descriptions-item label="批准 / 发布">{{ preview.approvedTime ?? '未记录' }} / {{ preview.publishedTime ?? '未记录' }}</el-descriptions-item>
          <el-descriptions-item label="预设生效日期">{{ preview.effectiveDate ?? '未记录' }}</el-descriptions-item>
          <el-descriptions-item label="发布 / 盖章文件">{{ preview.publishedFileId ?? '未记录' }} / {{ preview.stampedFileId ?? '未记录' }}</el-descriptions-item>
          <el-descriptions-item label="最新受控 / 当前执行文件">{{ preview.latestControlledFileId ?? '未记录' }} / {{ preview.currentActiveControlledFileId ?? '未记录' }}</el-descriptions-item>
          <el-descriptions-item label="拟修正状态 / 正式动作">{{ preview.expectedTargetStatus ?? '未记录' }} / {{ preview.expectedActions.join('、') || '无' }}</el-descriptions-item>
          <el-descriptions-item label="原事实摘要" :span="2"><span class="break-all">{{ preview.sourceFactsHash }}</span></el-descriptions-item>
          <el-descriptions-item label="原投影预像摘要" :span="2"><span class="break-all">{{ preview.preimageHash }}</span></el-descriptions-item>
        </el-descriptions>
        <el-alert v-if="!preview.canRepair" title="当前正式校验结果不允许维护；本窗口只读展示。" type="warning" :closable="false" class="mt-12px" />
        <el-form v-else class="mt-12px" label-width="100px">
          <el-form-item label="修复原因" required><el-input v-model="reason" type="textarea" :maxlength="500" :disabled="saving" show-word-limit data-testid="dcc-lifecycle-projection-repair-reason" /></el-form-item>
        </el-form>
      </template>
    </div>
    <template #footer>
      <el-button :disabled="saving" @click="load">重新校验</el-button>
      <el-button :disabled="saving" @click="close">关闭</el-button>
      <el-button v-if="preview?.canRepair" type="primary" :loading="saving" :disabled="!canSubmit" @click="submit" data-testid="dcc-lifecycle-projection-repair-submit">确认维护共享投影</el-button>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { generateUUID } from '@/utils'
import { getLifecycleProjectionRepairPreview, repairLifecycleProjection, type LifecycleProjectionRepairPreview } from '@/api/dcc/controlledFile/lifecycleProjectionRepair'
const props = defineProps<{ modelValue: boolean; fileId: string; allowed: boolean; contextKey: string; readContextKey: () => string }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; repaired: [context: { fileId: string; contextKey: string }] }>()
const preview = ref<LifecycleProjectionRepairPreview>(), reason = ref(''), error = ref(''), loading = ref(false), saving = ref(false)
let sequence = 0, loadedContext = '', idempotencyKey = ''
const context = () => JSON.stringify([props.fileId, props.contextKey, props.readContextKey(), props.allowed, props.modelValue])
const canSubmit = computed(() => props.allowed && props.modelValue && preview.value?.canRepair === true && preview.value.fileId === props.fileId && loadedContext === context() && Boolean(reason.value.trim()) && reason.value.trim().length <= 500 && !loading.value && !saving.value)
const close = () => { sequence++; preview.value = undefined; loadedContext = ''; emit('update:modelValue', false) }
const load = async () => {
  const token = ++sequence, captured = context(), id = props.fileId
  preview.value = undefined; loadedContext = ''; reason.value = ''; error.value = ''; loading.value = false
  if (!props.allowed || !props.modelValue || !/^[1-9]\d*$/.test(id)) return
  loading.value = true
  try {
    const result = await getLifecycleProjectionRepairPreview(id)
    if (token !== sequence || captured !== context()) return
    preview.value = result; loadedContext = captured; idempotencyKey = generateUUID()
  } catch (cause) { if (token === sequence && captured === context()) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (token === sequence && captured === context()) loading.value = false }
}
const submit = async () => {
  if (!canSubmit.value || !preview.value) return
  const facts = preview.value, captured = context(), token = sequence, actualReason = reason.value.trim()
  if (!facts.masterId || !facts.versionRefId || !facts.processInstanceId || facts.canonicalStatus !== 'FINALIZING') { error.value = '正式维护事实不完整，请重新校验'; return }
  saving.value = true; error.value = ''
  try {
    try { await ElMessageBox.confirm('确认按以上原事实，仅维护共享生命周期投影？原文件、历史和签名保持不变。', '二次确认', { type: 'warning' }) }
    catch (cause) { if (cause === 'cancel' || cause === 'close') return; throw cause }
    if (token !== sequence || captured !== context() || reason.value.trim() !== actualReason || preview.value !== facts) return
    const result = await repairLifecycleProjection(props.fileId, { masterId: facts.masterId, versionRefId: facts.versionRefId, processInstanceId: facts.processInstanceId, expectedCanonicalStatus: 'FINALIZING', sourceFactsHash: facts.sourceFactsHash, preimageHash: facts.preimageHash, reason: actualReason, idempotencyKey })
    if (token !== sequence || captured !== context()) return
    if (result.canonicalStatus !== facts.expectedTargetStatus || result.domainStatus !== facts.expectedTargetStatus) throw new Error('生命周期投影维护结果与校验目标不一致，请重新校验')
    close(); emit('repaired', { fileId: facts.fileId, contextKey: props.contextKey })
  } catch (cause) { if (token === sequence && captured === context()) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (token === sequence || !props.modelValue) saving.value = false }
}
watch(() => [props.modelValue, props.fileId, props.allowed, props.contextKey], () => { sequence++; preview.value = undefined; loadedContext = ''; loading.value = false; saving.value = false; if (props.modelValue) void load() }, { immediate: true, flush: 'sync' })
onBeforeUnmount(() => { sequence++; preview.value = undefined; loadedContext = '' })
</script>
