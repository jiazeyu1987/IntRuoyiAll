<template>
  <section aria-label="受控版本变更" data-testid="dcc-revision-panel">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="实际变更类型">{{ revisionChangeLabel(facts.revisionChangeType) }}</el-descriptions-item>
      <el-descriptions-item label="来源受控版本">{{ facts.revisionSourceVersionNo || '未记录' }}</el-descriptions-item>
      <el-descriptions-item label="选中小版本">{{ facts.selectedIterationVersionNo || '未记录' }}</el-descriptions-item>
      <el-descriptions-item label="变更说明">{{ facts.changeDescription || '未记录' }}</el-descriptions-item>
    </el-descriptions>
    <el-alert v-if="checkedOutBy" :title="revisionCheckoutLabel(checkedOutBy, checkedOutByName, checkedOutTime)" type="info" :closable="false" />
    <el-alert v-if="lockedReason" :title="lockedReason" type="warning" :closable="false" />
    <el-form v-if="canPartial || canReplacement" label-width="110px" @submit.prevent="submit">
      <el-form-item label="来源受控版本">{{ baselineVersionNo }}</el-form-item>
      <el-form-item label="预设生效日期">{{ applicationFacts?.effectiveDate || '请先选择正文' }}</el-form-item>
      <el-form-item label="实际变更类型">
        <el-radio-group v-model="intent" :disabled="busy || !!lockedReason || checkedOutBy != null">
          <el-radio v-if="canPartial" value="PARTIAL">局部变更</el-radio>
          <el-radio v-if="canReplacement" value="REPLACEMENT">换版变更</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="正文小版本">
        <el-select v-model="selectedId" :disabled="busy || !!lockedReason || checkedOutBy != null" placeholder="选择本次送审正文">
          <el-option
          v-for="item in selectableIterations" :key="String(item.id)"
            :label="item.versionNo" :value="String(item.id)" />
        </el-select>
      </el-form-item>
      <slot v-if="selectedIteration?.canPreview" name="selected-body" :selected-iteration-id="selectedId" :baseline-id="baselineId"></slot>
      <el-form-item label="目标受控版本">{{ targetVersion || '当前不可分配' }}</el-form-item>
      <el-form-item label="变更说明">
        <el-input v-model="description" type="textarea" :disabled="busy || !!lockedReason || checkedOutBy != null" />
      </el-form-item>
      <p>本次审批使用所选小版本正文。检入仅保存小版本，正式变更另行提交。</p>
      <el-alert v-if="submissionError" :title="submissionError" type="error" :closable="false" />
      <el-button
          type="primary" :loading="busy" :disabled="applicationReady === false || !!lockedReason || checkedOutBy != null || !selectedId || !intent"
        data-testid="dcc-revision-submit" @click="submit">提交正式变更</el-button>
    </el-form>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { buildRevisionCommand, revisionChangeLabel, revisionCheckoutLabel } from './revision-model'
import type { RevisionApplicationFacts, RevisionCommand, RevisionFacts, RevisionId, RevisionIntent, RevisionTarget } from './revision-model'

const props = defineProps<{
  baselineId: RevisionId
  baselineVersionNo: string
  partialTarget: RevisionTarget
  replacementTarget: RevisionTarget
  idempotencyKey: string
  applicationFacts?: RevisionApplicationFacts
  applicationReady?: boolean
  initialSelectedIterationId?: string
  facts: RevisionFacts
  iterations: Array<{ id: RevisionId; versionNo: string; canPartial: boolean; canReplacement: boolean; canPreview: boolean }>
  canPartial: boolean
  canReplacement: boolean
  checkedOutBy?: RevisionId
  checkedOutByName?: string
  /** Parent formats the authoritative server checkout timestamp; no browser-generated time. */
  checkedOutTime?: string
  lockedReason?: string
  submitRevision: (command: RevisionCommand) => Promise<void>
}>()
const emit = defineEmits<{ submitted: [command: RevisionCommand]; selectedIterationChanged: [id: string]; intentChanged: [intent: RevisionIntent | undefined] }>()
const selectedId = ref(props.initialSelectedIterationId || '')
const intent = ref<RevisionIntent>()
const description = ref('')
const busy = ref(false)
const submissionError = ref('')
const selectableIterations = computed(() => props.iterations.filter(item => item.canPartial || item.canReplacement))
const selectedIteration = computed(() => selectableIterations.value.find(item => String(item.id) === selectedId.value))
const target = computed(() => intent.value === 'PARTIAL' ? props.partialTarget
  : intent.value === 'REPLACEMENT' ? props.replacementTarget : undefined)
const targetVersion = computed(() => target.value?.versionNo)
watch(() => props.baselineId, () => {
  selectedId.value = ''; intent.value = undefined; description.value = ''; submissionError.value = ''
})
watch(selectedId, (id) => emit('selectedIterationChanged', id), { flush: 'sync' })
watch(intent, (value) => emit('intentChanged', value), { flush: 'sync' })
const submit = async () => {
  if (busy.value) return
  submissionError.value = ''
  const requestBaselineId = props.baselineId
  const requestKey = props.idempotencyKey
  try {
    if (props.applicationReady === false || !props.applicationFacts) throw new Error('请先读取所选正文的已保存申请属性')
    if (props.checkedOutBy != null) throw new Error('该文件当前已检出，请先由检出人检入或撤销')
    if (props.lockedReason) throw new Error(props.lockedReason)
    if (!intent.value || (intent.value === 'PARTIAL' ? !props.canPartial : !props.canReplacement)) {
      throw new Error('当前无所选变更操作权限')
    }
    if (!selectedIteration.value || !(intent.value === 'PARTIAL' ? selectedIteration.value.canPartial : selectedIteration.value.canReplacement)) {
      throw new Error('请选择有权限的正文小版本')
    }
    if (!targetVersion.value || target.value?.unavailableReason) throw new Error('目标受控版本当前不可分配，请刷新正式变更选项')
    const command = buildRevisionCommand(props.baselineId, selectedId.value, intent.value, description.value, props.idempotencyKey, props.applicationFacts)
    busy.value = true
    await props.submitRevision(command)
    if (props.baselineId !== requestBaselineId || props.idempotencyKey !== requestKey) return
    emit('submitted', command)
  } catch (error) {
    if (props.baselineId === requestBaselineId && props.idempotencyKey === requestKey) {
      submissionError.value = error instanceof Error ? error.message : String(error)
    }
  } finally {
    busy.value = false
  }
}
</script>
