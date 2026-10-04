<template>
  <ContentWrap v-loading="loading" data-testid="dcc-detail-application-history">
    <p class="font-600">申请轮次与冻结证据</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <p v-if="lockPrimaryRound && !contextError">当前办理轮次：{{ primaryBpmRound }}；本面板展示该轮冻结证据。</p>
    <el-select :model-value="selectedKey" placeholder="选择申请轮次" :disabled="loading || lockPrimaryRound || Boolean(contextError)" @change="selectRound">
      <el-option
v-for="round in rounds" :key="roundKey(round)" :value="roundKey(round)"
        :label="`${applicationLabel(round.applicationType)} · 属性第 ${round.attributeRound} 轮 · BPM ${round.bpmRound}`" />
    </el-select>
    <el-empty v-if="!loading && !error && !rounds.length" description="没有已绑定的正式申请轮次" />
    <section v-if="evidence" v-loading="evidenceLoading" class="mt-12px">
      <p>文件 #{{ evidence.controlledFileId }} · {{ evidence.versionNo }} · {{ applicationLabel(evidence.applicationType) }} · BPM {{ evidence.bpmRound }} · 属性第 {{ evidence.attributeRound }} 轮</p>
      <template v-if="evidence.recorded && evidence.actualAttributes && evidence.defaultSource">
        <ProjectAttributesFields :model-value="evidence.actualAttributes" readonly />
        <el-collapse><el-collapse-item title="本轮默认来源（只读）" name="default-source">
          <ProjectAttributesFields :model-value="evidence.defaultSource" readonly />
        </el-collapse-item></el-collapse>
      </template>
      <el-alert v-else :title="evidence.unavailableReason === 'NOT_FROZEN' ? '本轮属性尚未提交冻结' : '历史申请属性未记录'" type="info" :closable="false" />
      <el-table :data="evidence.signatures" aria-label="所选申请轮次签名">
        <el-table-column prop="id" label="签名记录" min-width="100" />
        <el-table-column prop="actorNicknameSnapshot" label="签名人" min-width="100">
          <template #default="{ row }">{{ row.actorNicknameSnapshot || '未记录' }}</template>
        </el-table-column>
        <el-table-column prop="actionType" label="签名动作" min-width="120" />
        <el-table-column prop="actorDeptNameSnapshot" label="部门" min-width="100" />
        <el-table-column prop="signedAt" label="服务器签名时间" min-width="180">
          <template #default="{ row }">{{ formatControlledFileDateTime(row.signedAt) }}</template>
        </el-table-column>
        <el-table-column prop="processInstanceId" label="BPM 轮次" min-width="180" />
      </el-table>
    </section>
    <el-alert v-else-if="evidenceLoading" title="正在读取所选轮次证据" type="info" :closable="false" />
  </ContentWrap>
</template>
<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { getControlledFileApplicationRounds, getControlledFileApplicationEvidence } from '@/api/dcc/controlledFile/applicationRead'
import type { ControlledFileApplicationRound, ControlledFileApplicationEvidence, DccApplicationType } from '@/api/dcc/controlledFile/applicationRead'
import ProjectAttributesFields from '../project-attributes/ProjectAttributesFields.vue'
import { formatControlledFileDateTime } from './presentation'
import { validateApplicationRoundMappings } from './application-round-context'
const props = defineProps<{ fileId: string | number; primaryBpmRound?: string | null; lockPrimaryRound?: boolean; contextKey?: string; contextError?: string }>()
const rounds = ref<ControlledFileApplicationRound[]>([]), selectedKey = ref('')
const evidence = ref<ControlledFileApplicationEvidence>(), loading = ref(false), evidenceLoading = ref(false), error = ref('')
let generation = 0, selectionGeneration = 0
const roundKey = (round: ControlledFileApplicationRound) => JSON.stringify([round.controlledFileId, round.applicationType, round.bpmRound, round.attributeRound])
const applicationLabel = (type: DccApplicationType) => ({ UPLOAD: '初始上传', REVISION: '升版申请', OBSOLETE: '作废申请' })[type]
const selectRound = async (key: string) => {
  const token = ++selectionGeneration, context = generation
  selectedKey.value = key; evidence.value = undefined; error.value = ''; evidenceLoading.value = true
  try {
    const round = rounds.value.find(row => roundKey(row) === key)
    if (!round) throw new Error('所选申请轮次不存在，请刷新正式轮次列表')
    if (props.contextError) throw new Error(props.contextError)
    if (props.lockPrimaryRound && round.bpmRound !== props.primaryBpmRound) throw new Error('本面板只能显示当前办理轮次的申请证据')
    const result = await getControlledFileApplicationEvidence(round.controlledFileId, round.applicationType, round.bpmRound)
    if (context !== generation || token !== selectionGeneration) return
    if (String(result.controlledFileId) !== String(props.fileId) || result.applicationType !== round.applicationType || result.bpmRound !== round.bpmRound)
      throw new Error('申请证据与所选文件或实际办理轮次不一致')
    if (result.recorded && result.attributeRound !== round.attributeRound) throw new Error('申请证据与所选属性轮次不一致')
    evidence.value = result
  } catch (cause) {
    if (context === generation && token === selectionGeneration) { selectedKey.value = ''; error.value = cause instanceof Error ? cause.message : String(cause) }
  } finally {
    if (context === generation && token === selectionGeneration) evidenceLoading.value = false
  }
}
watch(() => [String(props.fileId), props.primaryBpmRound, props.contextKey, props.contextError, props.lockPrimaryRound], async () => {
  const token = ++generation; selectionGeneration++
  rounds.value = []; evidence.value = undefined; selectedKey.value = ''; error.value = ''; loading.value = true; evidenceLoading.value = false
  try {
    if (props.contextError) throw new Error(props.contextError)
    const result = await getControlledFileApplicationRounds(props.fileId)
    if (token === generation) {
      const validated = validateApplicationRoundMappings(props.fileId, result)
      if (props.primaryBpmRound) {
        const matching = validated.filter(row => row.bpmRound === props.primaryBpmRound)
        if (matching.length > 1) throw new Error('本版本 BPM 对应多个申请类型，正式映射不明确')
        if (matching.length !== 1) throw new Error('实际查看轮次的正式申请映射缺失，请核对本文件BPM记录')
        rounds.value = validated
        if (matching.length === 1) await selectRound(roundKey(matching[0]))
      } else if (props.lockPrimaryRound) throw new Error('当前办理轮次尚未核验，不能读取其他申请属性')
      else rounds.value = validated
    }
  } catch (cause) {
    if (token === generation) error.value = cause instanceof Error ? cause.message : String(cause)
  } finally { if (token === generation) loading.value = false }
}, { immediate: true, flush: 'sync' })
onBeforeUnmount(() => { generation++; selectionGeneration++ })
</script>
