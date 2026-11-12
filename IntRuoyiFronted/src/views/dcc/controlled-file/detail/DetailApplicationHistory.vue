<template>
  <ContentWrap class="dcc-detail-surface" v-loading="loading" data-testid="dcc-detail-application-history">
    <p class="font-600">申请信息</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-select
      v-if="!lockPrimaryRound && rounds.length > 1"
      :model-value="selectedKey"
      placeholder="查看历史申请"
      :disabled="loading || lockPrimaryRound || Boolean(contextError)"
      @change="selectRound"
    >
      <el-option
        v-for="round in rounds"
        :key="roundKey(round)"
        :value="roundKey(round)"
        :label="`${applicationLabel(round.applicationType, round.revisionChangeType)} · ${formatControlledFileDateTime(round.applicationTime)}`"
      />
    </el-select>
    <p v-if="!loading && !error && !rounds.length">暂无申请信息</p>
    <section v-if="evidence" v-loading="evidenceLoading" class="mt-12px">
      <el-descriptions :column="3" border class="application-information">
        <el-descriptions-item label="申请类型">{{ applicationLabel(evidence.applicationType, selectedRound?.revisionChangeType) }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatControlledFileDateTime(selectedRound?.applicationTime) }}</el-descriptions-item>
        <template v-if="evidence.recorded && evidence.actualAttributes">
          <el-descriptions-item v-if="evidence.actualAttributes.libraryScope" label="适用范围">{{ evidence.actualAttributes.libraryScope === 'QMS' ? 'QMS 文件库' : '通用文件库' }}，项目属性不适用</el-descriptions-item>
          <template v-else>
            <el-descriptions-item label="目标市场">{{ evidence.actualAttributes.targetMarkets.map(market => marketLabels[market]).join('、') }}</el-descriptions-item>
            <el-descriptions-item label="注册人身份">{{ identityLabels[evidence.actualAttributes.licenseHolder || ''] || '未记录' }}</el-descriptions-item>
            <el-descriptions-item label="生产方身份">{{ identityLabels[evidence.actualAttributes.actualManufacturer || ''] || '未记录' }}</el-descriptions-item>
            <el-descriptions-item label="文件转移">{{ identityLabels[evidence.actualAttributes.documentTransfer || ''] || '未记录' }}</el-descriptions-item>
            <el-descriptions-item v-if="evidence.actualAttributes.otherMarket" label="其他市场说明" :span="3">{{ evidence.actualAttributes.otherMarket }}</el-descriptions-item>
            <el-descriptions-item v-if="evidence.actualAttributes.documentTransfer === 'Y'" label="转移至" :span="3">{{ evidence.actualAttributes.transferTo }}</el-descriptions-item>
          </template>
        </template>
        <template v-if="evidence.applicationType === 'REVISION'">
          <el-descriptions-item label="变更类型">{{ applicationLabel('REVISION', selectedRound?.revisionChangeType) }}</el-descriptions-item>
          <el-descriptions-item label="来源文件版本">{{ getDccVersionDisplayText(selectedRound?.revisionSourceVersionNo) }}</el-descriptions-item>
          <el-descriptions-item label="变更说明" :span="3">{{ selectedRound?.changeDescription || '未记录' }}</el-descriptions-item>
        </template>
      </el-descriptions>
      <el-alert
        v-if="!evidence.recorded || !evidence.actualAttributes"
        :title="
          evidence.unavailableReason === 'NOT_FROZEN'
            ? '本轮属性尚未提交冻结'
            : '历史申请属性未记录'
        "
        type="info"
        :closable="false"
      />
      <DetailApprovalRecords v-if="showNodeRecords" :evidence="evidence" />
    </section>
    <el-alert
      v-else-if="evidenceLoading"
      title="正在读取所选轮次证据"
      type="info"
      :closable="false"
    />
  </ContentWrap>
</template>
<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { serializeDccBodyReadContext, type DccBodyReadContext } from '@/api/dcc/controlledFile/bodyReadContext'
import {
  getControlledFileApplicationRounds,
  getControlledFileApplicationEvidence
} from '@/api/dcc/controlledFile/applicationRead'
import type {
  ControlledFileApplicationRound,
  ControlledFileApplicationEvidence,
  DccApplicationType
} from '@/api/dcc/controlledFile/applicationRead'
import DetailApprovalRecords from './DetailApprovalRecords.vue'
import { formatControlledFileDateTime } from './presentation'
import { validateApplicationRoundMappings } from './application-round-context'
import { validateApplicationNodeRecords } from './application-node-records'
import { detailIdentity } from './relation-contract'
import type { DetailRelationApplicationSelection } from './relation-contract'
import { getDccVersionDisplayText } from '../shared/version-display'
const props = defineProps<{
  bodyReadContext?: DccBodyReadContext
  fileId: string | number
  expectedVersionNo?: string
  primaryApplicationType?: DccApplicationType
  primaryBpmRound?: string | null
  lockPrimaryRound?: boolean
  contextKey?: string
  contextError?: string
  showNodeRecords?: boolean
  readAuthority?: () => string
}>()
const emit = defineEmits<{
  'selection-change': [selection: DetailRelationApplicationSelection | null]
}>()
const rounds = ref<ControlledFileApplicationRound[]>([]),
  selectedKey = ref('')
const evidence = ref<ControlledFileApplicationEvidence>(),
  loading = ref(false),
  evidenceLoading = ref(false),
  error = ref('')
const loadedAuthority = ref<string | undefined>()
let generation = 0,
  selectionGeneration = 0
const roundKey = (round: ControlledFileApplicationRound) =>
  JSON.stringify([
    round.controlledFileId,
    round.applicationType,
    round.bpmRound,
    round.attributeRound
  ])
const selectedRound = computed(() => rounds.value.find(round => roundKey(round) === selectedKey.value))
const marketLabels: Record<string, string> = { NMPA: 'NMPA 国内', CE: 'CE 欧盟', FDA: 'FDA', MADSAP: 'MDSAP', OTHER: '其他', NA: '不适用' }
const identityLabels: Record<string, string> = { Y: '是', N: '否', NA: '不适用' }
const applicationLabel = (type: DccApplicationType, revisionChangeType?: string | null) =>
  type === 'REVISION'
    ? (revisionChangeType === 'PARTIAL' ? '局部变更' : revisionChangeType === 'REPLACEMENT' ? '换版变更' : '变更类型未记录')
    : ({ UPLOAD: '初始上传', OBSOLETE: '作废申请' })[type]
const selectRound = async (key: string) => {
  const token = ++selectionGeneration,
    context = generation,
    authority = props.readAuthority?.()
  const isCurrent = () => context === generation && token === selectionGeneration && authority === props.readAuthority?.()
  emit('selection-change', null)
  if (loadedAuthority.value !== authority) {
    rounds.value = []
    evidence.value = undefined
    selectedKey.value = ''
    loadedAuthority.value = undefined
    error.value = '申请权限上下文已变化，请刷新当前详情后重试'
    return
  }
  selectedKey.value = key
  evidence.value = undefined
  error.value = ''
  evidenceLoading.value = true
  try {
    const round = rounds.value.find((row) => roundKey(row) === key)
    if (!round) throw new Error('所选申请轮次不存在，请刷新正式轮次列表')
    if (props.contextError) throw new Error(props.contextError)
    if (props.lockPrimaryRound && round.bpmRound !== props.primaryBpmRound)
      throw new Error('本面板只能显示当前办理轮次的申请证据')
    if (props.lockPrimaryRound && props.primaryApplicationType && round.applicationType !== props.primaryApplicationType)
      throw new Error('所选申请类型与来源指定的正式申请不一致')
    const result = await getControlledFileApplicationEvidence(
      round.controlledFileId,
      round.applicationType,
      round.bpmRound,
      props.bodyReadContext ? serializeDccBodyReadContext(props.fileId, props.bodyReadContext) : undefined
    )
    if (!isCurrent()) return
    if (
      detailIdentity(result.controlledFileId) !== detailIdentity(props.fileId) ||
      result.applicationType !== round.applicationType ||
      result.bpmRound !== round.bpmRound
    )
      throw new Error('申请证据与所选文件或实际办理轮次不一致')
    if (props.expectedVersionNo && result.versionNo !== props.expectedVersionNo)
      throw new Error('申请证据与所选原始版本不一致')
    if (
      !Array.isArray(result.signatures) ||
      result.signatures.some((signature) => signature.processInstanceId !== round.bpmRound)
    )
      throw new Error('申请签名证据属于其他流程轮次')
    if (props.showNodeRecords) validateApplicationNodeRecords(result)
    if (result.recorded === false && result.attributeRound === null) {
      if (
        !['NOT_RECORDED', 'NOT_FROZEN'].includes(result.unavailableReason || '') ||
        result.actualAttributes != null ||
        result.defaultSource != null
      )
        throw new Error('申请证据缺少正式未记录事实')
      evidence.value = result
      return
    }
    if (result.attributeRound !== round.attributeRound)
      throw new Error('申请证据与所选属性轮次不一致')
    evidence.value = result
    emit('selection-change', {
      controlledFileId: detailIdentity(round.controlledFileId),
      applicationType: round.applicationType,
      bpmRound: round.bpmRound,
      attributeRound: round.attributeRound
    })
  } catch (cause) {
    if (isCurrent()) {
      selectedKey.value = ''
      evidence.value = undefined
      emit('selection-change', null)
      error.value = cause instanceof Error ? cause.message : String(cause)
    }
  } finally {
    if (isCurrent()) evidenceLoading.value = false
  }
}
watch(
  () => [
    String(props.fileId),
    JSON.stringify(props.bodyReadContext),
    props.primaryBpmRound,
    props.contextKey,
    props.contextError,
    props.lockPrimaryRound,
    props.expectedVersionNo,
    props.primaryApplicationType,
    props.readAuthority?.()
  ],
  async () => {
    const token = ++generation
    const authority = props.readAuthority?.()
    const isCurrent = () => token === generation && authority === props.readAuthority?.()
    selectionGeneration++
    emit('selection-change', null)
    loadedAuthority.value = undefined
    rounds.value = []
    evidence.value = undefined
    selectedKey.value = ''
    error.value = ''
    loading.value = true
    evidenceLoading.value = false
    try {
      if (props.contextError) throw new Error(props.contextError)
      const result = await getControlledFileApplicationRounds(props.fileId,
        props.bodyReadContext ? serializeDccBodyReadContext(props.fileId, props.bodyReadContext) : undefined)
      if (isCurrent()) {
        const validated = validateApplicationRoundMappings(props.fileId, result)
        loadedAuthority.value = authority
        if (props.primaryBpmRound) {
          const matching = validated.filter((row) => row.bpmRound === props.primaryBpmRound)
          if (matching.length > 1) throw new Error('本版本 BPM 对应多个申请类型，正式映射不明确')
          if (matching.length !== 1)
            throw new Error('实际查看轮次的正式申请映射缺失，请核对本文件BPM记录')
          rounds.value = validated
          if (matching.length === 1) await selectRound(roundKey(matching[0]))
        } else if (props.lockPrimaryRound)
          throw new Error('当前办理轮次尚未核验，不能读取其他申请属性')
        else {
          rounds.value = validated
          if (validated.length === 1) await selectRound(roundKey(validated[0]))
        }
      }
    } catch (cause) {
      if (isCurrent()) error.value = cause instanceof Error ? cause.message : String(cause)
    } finally {
      if (isCurrent()) loading.value = false
    }
  },
  { immediate: true, flush: 'sync' }
)
onBeforeUnmount(() => {
  loadedAuthority.value = undefined
  generation++
  selectionGeneration++
  emit('selection-change', null)
})
</script>
<style scoped>
.application-information :deep(.el-descriptions__table) {
  table-layout: fixed;
}

.application-information :deep(.el-descriptions__label) {
  width: 11.5%;
}

.application-information :deep(.el-descriptions__content) {
  overflow-wrap: anywhere;
}
</style>
