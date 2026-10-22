<template>
  <el-dialog :model-value="modelValue" title="核对类型存储" width="720px" :close-on-click-modal="false"
    :close-on-press-escape="!submitting" :show-close="!submitting" :before-close="beforeClose">
    <el-alert title="核对已保存类型与项目文件夹的存储位置。确认后保留原类型、正文、审批矩阵和历史证明，追加本次恢复记录。" type="info" :closable="false" />
    <el-alert v-if="error" data-testid="dcc-working-type-storage-recovery-error" :title="error" type="error" :closable="false" class="mt-12px" />
    <div v-if="preview" data-testid="dcc-working-type-storage-recovery-facts" class="mt-12px">
      <p>文件编号：{{ preview.fileNumber }}</p>
      <p>已保存类型：{{ preview.currentTypePath }}</p>
      <p>项目 / 文件夹：{{ preview.projectId }} / {{ preview.projectFolderId }}</p>
      <p>原证明：{{ preview.originalProofId }}</p>
      <p>原存储目录：{{ preview.oldDirectoryId }}；目标基础目录：{{ preview.targetBaseDirectoryId }}</p>
      <p>目标存储目录：{{ preview.targetDirectoryId || '确认时按正式配置创建' }}</p>
      <p>正文摘要：{{ preview.sourceSha256 }}</p>
      <p>冻结矩阵摘要：{{ preview.matrixDigest }}</p>
      <p v-if="preview.recovered" data-testid="dcc-working-type-storage-recovery-receipt">已恢复，恢复记录：{{ preview.recoveryEvidenceId }}</p>
      <el-form v-else label-width="90px" class="mt-12px">
        <el-form-item label="恢复原因" required>
          <el-input v-model="reason" data-testid="dcc-working-type-storage-recovery-reason" type="textarea" maxlength="512" :disabled="busy || !!command" />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button data-testid="dcc-working-type-storage-recovery-cancel" :disabled="submitting" @click="cancel">关闭</el-button>
      <el-button data-testid="dcc-working-type-storage-recovery-preview" :loading="busy && !submitting" :disabled="submitting" @click="loadPreview">重新核对</el-button>
      <el-button v-if="!preview?.recovered" data-testid="dcc-working-type-storage-recovery-confirm" type="primary" :loading="submitting"
        :disabled="busy || !preview || !reason.trim()" @click="confirm">确认恢复存储位置</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { generateUUID } from '@/utils'
import { previewWorkingTypeStorageRecovery, confirmWorkingTypeStorageRecovery,
  type WorkingTypeStorageRecoveryPreview, type WorkingTypeStorageRecoveryCommand } from '@/api/dcc/controlledFile/workingTypeStorageRecovery'

const props = defineProps<{ modelValue: boolean; fileId?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void; (e: 'recovered', fileId: string): void }>()
const preview = ref<WorkingTypeStorageRecoveryPreview>(), error = ref(''), reason = ref(''), busy = ref(false), submitting = ref(false)
const command = ref<WorkingTypeStorageRecoveryCommand>()
let generation = 0
const message = (cause: unknown) => cause instanceof Error ? cause.message : String(cause)
const cancel = () => { if (submitting.value) return; generation++; busy.value = false; emit('update:modelValue', false) }
const beforeClose = () => cancel()
const loadPreview = async () => {
  if (submitting.value || !props.modelValue || !props.fileId) return
  const token = ++generation, fileId = props.fileId
  preview.value = undefined; command.value = undefined; reason.value = ''; error.value = ''; busy.value = true
  try {
    const result = await previewWorkingTypeStorageRecovery(fileId)
    if (token !== generation || !props.modelValue || props.fileId !== fileId) return
    if (result.controlledFileId !== fileId) throw new Error('类型存储恢复文件身份不一致')
    preview.value = result
  } catch (cause) { if (token === generation) error.value = message(cause) }
  finally { if (token === generation) busy.value = false }
}
const confirm = async () => {
  const selected = preview.value, fileId = props.fileId
  if (busy.value || !props.modelValue || !fileId || !selected || selected.recovered || !reason.value.trim()) return
  const token = generation
  command.value ??= { expectedOriginalProofId: selected.originalProofId, expectedOriginalProofSha256: selected.originalProofSha256,
    expectedObservedSha256: selected.observedSha256, requestId: generateUUID(), changeReason: reason.value.trim(), confirmed: true }
  busy.value = true; submitting.value = true; error.value = ''
  try {
    const result = await confirmWorkingTypeStorageRecovery(fileId, command.value)
    if (token !== generation || !props.modelValue || props.fileId !== fileId) return
    if (result.controlledFileId !== fileId || result.masterId !== selected.masterId || !result.recovered || !result.recoveryEvidenceId
      || result.originalProofId !== selected.originalProofId || result.originalProofSha256 !== selected.originalProofSha256)
      throw new Error('类型存储恢复回执与原对象不一致，请重新核对')
    preview.value = result
    emit('recovered', fileId); emit('update:modelValue', false)
  } catch (cause) { if (token === generation) error.value = message(cause) }
  finally { if (token === generation) { busy.value = false; submitting.value = false } }
}
watch(() => [props.modelValue, props.fileId] as const, () => {
  generation++; preview.value = undefined; command.value = undefined; reason.value = ''; error.value = ''; busy.value = false; submitting.value = false
  if (props.modelValue && props.fileId) void loadPreview()
}, { immediate: true })
</script>
