<template>
  <ContentWrap v-loading="loading" data-testid="dcc-detail-application-history">
    <p class="font-600">{{ lockPrimaryRound ? '本次申请信息' : '申请轮次与冻结证据' }}</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <p v-if="lockPrimaryRound && !contextError"
      >当前办理轮次：{{ primaryBpmRound }}；本面板展示该轮冻结证据。</p
    >
    <el-select
      :model-value="selectedKey"
      placeholder="选择申请轮次"
      :disabled="loading || lockPrimaryRound || Boolean(contextError)"
      @change="selectRound"
    >
      <el-option
        v-for="round in rounds"
        :key="roundKey(round)"
        :value="roundKey(round)"
        :label="`${applicationLabel(round.applicationType)} · 属性第 ${round.attributeRound} 轮 · BPM ${round.bpmRound}`"
      />
    </el-select>
    <el-empty v-if="!loading && !error && !rounds.length" description="没有已绑定的正式申请轮次" />
    <section v-if="evidence" v-loading="evidenceLoading" class="mt-12px">
      <p
        >文件 #{{ evidence.controlledFileId }} ·
        {{ getDccVersionDisplayText(evidence.versionNo) }} ·
        {{ applicationLabel(evidence.applicationType) }} · BPM {{ evidence.bpmRound }} · 属性第
        {{ evidence.attributeRound }} 轮</p
      >
      <template v-if="evidence.recorded && evidence.actualAttributes && evidence.defaultSource">
        <ProjectAttributesFields :model-value="evidence.actualAttributes" readonly />
        <el-collapse
          ><el-collapse-item title="本轮默认来源（只读）" name="default-source">
            <ProjectAttributesFields
              :model-value="evidence.defaultSource"
              readonly
            /> </el-collapse-item
        ></el-collapse>
      </template>
      <el-alert
        v-else
        :title="
          evidence.unavailableReason === 'NOT_FROZEN'
            ? '本轮属性尚未提交冻结'
            : '历史申请属性未记录'
        "
        type="info"
        :closable="false"
      />
      <section v-if="showNodeRecords" class="mt-16px" data-testid="dcc-application-node-records">
        <p class="font-600">前序节点记录</p>
        <el-table :data="evidence.nodeRecords" empty-text="本轮暂无已发生的节点记录" border>
          <el-table-column label="节点" min-width="150"
            ><template #default="{ row }">{{
              row.nodeName || row.stageCode
            }}</template></el-table-column
          >
          <el-table-column label="办理人" min-width="150"
            ><template #default="{ row }">{{
              row.actorNicknameSnapshot ||
              row.actorUsernameSnapshot ||
              (row.actorId ? `账号 #${row.actorId}` : '未记录')
            }}</template></el-table-column
          >
          <el-table-column label="部门" min-width="140"
            ><template #default="{ row }">{{
              row.actorDeptNameSnapshot || '未记录'
            }}</template></el-table-column
          >
          <el-table-column label="动作及结果" min-width="160"
            ><template #default="{ row }"
              >{{
                row.actionType === 'TASK_COMPLETED'
                  ? '任务完成'
                  : getDccSignatureTaskActionLabel(row.actionType)
              }}
              · {{ row.result || '结果未记录' }}</template
            ></el-table-column
          >
          <el-table-column label="意见" min-width="200"
            ><template #default="{ row }">{{ row.comment || '未记录' }}</template></el-table-column
          >
          <el-table-column label="时间" min-width="175"
            ><template #default="{ row }">{{
              formatControlledFileDateTime(row.occurredAt)
            }}</template></el-table-column
          >
          <el-table-column label="电子签名" min-width="150"
            ><template #default="{ row }">{{
              row.signatureId
                ? `#${row.signatureId} · ${row.signatureEvidenceStatus || '状态未记录'}`
                : '未记录'
            }}</template></el-table-column
          >
          <el-table-column label="指派对象" min-width="160"
            ><template #default="{ row }">{{
              row.assignmentTarget
                ? `${row.assignmentTarget.nickname || `账号 #${row.assignmentTarget.userId}`} · 部门义务：${row.assignmentTarget.obligationDepartmentName || '未记录'}`
                : '不适用'
            }}</template></el-table-column
          >
          <el-table-column label="已确认业务内容" min-width="240"
            ><template #default="{ row }"
              ><p v-for="field in row.confirmedFields" :key="field.field"
                >{{ field.label }}：{{ field.value ?? '未记录' }}</p
              ><span v-if="!row.confirmedFields.length">未记录</span></template
            ></el-table-column
          >
        </el-table>
      </section>
      <el-table :data="evidence.signatures" aria-label="所选申请轮次签名">
        <el-table-column prop="id" label="签名记录" min-width="100" />
        <el-table-column prop="actorNicknameSnapshot" label="签名人" min-width="100">
          <template #default="{ row }">{{ row.actorNicknameSnapshot || '未记录' }}</template>
        </el-table-column>
        <el-table-column label="签名动作" min-width="120"
          ><template #default="{ row }">{{
            getDccSignatureTaskActionLabel(row.actionType)
          }}</template></el-table-column
        >
        <el-table-column prop="actorDeptNameSnapshot" label="部门" min-width="100" />
        <el-table-column prop="signedAt" label="服务器签名时间" min-width="180">
          <template #default="{ row }">{{ formatControlledFileDateTime(row.signedAt) }}</template>
        </el-table-column>
        <el-table-column prop="processInstanceId" label="BPM 轮次" min-width="180" />
      </el-table>
      <DetailRelationStageEvidence
        v-if="evidence.relationEvidence"
        :evidence="evidence.relationEvidence"
        :tenant-id="evidence.relationEvidence.tenantId"
        :file-id="String(evidence.controlledFileId)"
        :application-type="evidence.applicationType"
        :bpm-round="evidence.bpmRound"
        :attribute-round="evidence.relationEvidence.attributeRound"
        :context-key="selectedKey"
        :signature-facts="evidence.signatures"
      />
      <p v-else>本申请轮次的关联阶段证据未记录。</p>
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
import { ref, watch, onBeforeUnmount } from 'vue'
import {
  getControlledFileApplicationRounds,
  getControlledFileApplicationEvidence
} from '@/api/dcc/controlledFile/applicationRead'
import type {
  ControlledFileApplicationRound,
  ControlledFileApplicationEvidence,
  DccApplicationType
} from '@/api/dcc/controlledFile/applicationRead'
import ProjectAttributesFields from '../project-attributes/ProjectAttributesFields.vue'
import DetailRelationStageEvidence from './DetailRelationStageEvidence.vue'
import { formatControlledFileDateTime } from './presentation'
import { validateApplicationRoundMappings } from './application-round-context'
import { getDccSignatureTaskActionLabel } from '../shared/signature-evidence'
import { validateApplicationNodeRecords } from './application-node-records'
import { detailIdentity } from './relation-contract'
import type { DetailRelationApplicationSelection } from './relation-contract'
import { getDccVersionDisplayText } from '../shared/version-display'
const props = defineProps<{
  fileId: string | number
  primaryBpmRound?: string | null
  lockPrimaryRound?: boolean
  contextKey?: string
  contextError?: string
  showNodeRecords?: boolean
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
let generation = 0,
  selectionGeneration = 0
const roundKey = (round: ControlledFileApplicationRound) =>
  JSON.stringify([
    round.controlledFileId,
    round.applicationType,
    round.bpmRound,
    round.attributeRound
  ])
const applicationLabel = (type: DccApplicationType) =>
  ({ UPLOAD: '初始上传', REVISION: '升版申请', OBSOLETE: '作废申请' })[type]
const selectRound = async (key: string) => {
  const token = ++selectionGeneration,
    context = generation
  emit('selection-change', null)
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
    const result = await getControlledFileApplicationEvidence(
      round.controlledFileId,
      round.applicationType,
      round.bpmRound
    )
    if (context !== generation || token !== selectionGeneration) return
    if (
      detailIdentity(result.controlledFileId) !== detailIdentity(props.fileId) ||
      result.applicationType !== round.applicationType ||
      result.bpmRound !== round.bpmRound
    )
      throw new Error('申请证据与所选文件或实际办理轮次不一致')
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
    if (context === generation && token === selectionGeneration) {
      selectedKey.value = ''
      evidence.value = undefined
      emit('selection-change', null)
      error.value = cause instanceof Error ? cause.message : String(cause)
    }
  } finally {
    if (context === generation && token === selectionGeneration) evidenceLoading.value = false
  }
}
watch(
  () => [
    String(props.fileId),
    props.primaryBpmRound,
    props.contextKey,
    props.contextError,
    props.lockPrimaryRound
  ],
  async () => {
    const token = ++generation
    selectionGeneration++
    emit('selection-change', null)
    rounds.value = []
    evidence.value = undefined
    selectedKey.value = ''
    error.value = ''
    loading.value = true
    evidenceLoading.value = false
    try {
      if (props.contextError) throw new Error(props.contextError)
      const result = await getControlledFileApplicationRounds(props.fileId)
      if (token === generation) {
        const validated = validateApplicationRoundMappings(props.fileId, result)
        if (props.primaryBpmRound) {
          const matching = validated.filter((row) => row.bpmRound === props.primaryBpmRound)
          if (matching.length > 1) throw new Error('本版本 BPM 对应多个申请类型，正式映射不明确')
          if (matching.length !== 1)
            throw new Error('实际查看轮次的正式申请映射缺失，请核对本文件BPM记录')
          rounds.value = validated
          if (matching.length === 1) await selectRound(roundKey(matching[0]))
        } else if (props.lockPrimaryRound)
          throw new Error('当前办理轮次尚未核验，不能读取其他申请属性')
        else rounds.value = validated
      }
    } catch (cause) {
      if (token === generation) error.value = cause instanceof Error ? cause.message : String(cause)
    } finally {
      if (token === generation) loading.value = false
    }
  },
  { immediate: true, flush: 'sync' }
)
onBeforeUnmount(() => {
  generation++
  selectionGeneration++
  emit('selection-change', null)
})
</script>
