<template>
  <section data-testid="dcc-relation-stage-evidence">
    <p class="font-600">本轮关联阶段证据</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-alert
      v-if="!error && (!projection || !projection.recorded)"
      title="本轮签名关联阶段未记录"
      type="info"
      :closable="false"
    />
    <article v-for="stage in projection?.stages || []" :key="stage.snapshot.header.id">
      <p class="font-600"
        >{{ stageLabel(stage.snapshot.header.stageCode) }} · 义务
        {{ stage.snapshot.header.obligationId }}</p
      >
      <p
        >原义务部门：{{ obligationDepartmentName(stage.snapshot.header) }} · 当时签名人：{{
          signatureFact(stage.snapshot.header.signatureId)?.actorNicknameSnapshot?.trim() ||
          '未记录'
        }}
        · 签名人当时部门：{{
          signatureFact(stage.snapshot.header.signatureId)?.actorDeptNameSnapshot?.trim() ||
          '未记录'
        }}</p
      >
      <p
        >审计身份：义务部门 #{{ stage.snapshot.header.departmentId ?? '未记录' }} · 签名人 #{{
          stage.snapshot.header.actorId
        }}
        · 服务器签名时间 {{ stage.snapshot.header.signedAt }}</p
      >
      <p
        >DCC 签名投影 #{{ stage.snapshot.header.signatureId }} ·
        {{ signatureLabel(stage.signatureEvidenceStatus) }} ·
        {{ stage.signatureIdentityMatches ? '签名身份对应本节点' : '签名身份不匹配' }}</p
      >
      <p>{{ stage.snapshot.header.reason }}</p>
      <el-table
        :data="stage.snapshot.members"
        :aria-label="`义务 ${stage.snapshot.header.obligationId} 当时完整关联集合`"
      >
        <el-table-column prop="fileNameSnapshot" label="当时关联文件" />
        <el-table-column prop="fileNumberSnapshot" label="冻结编号" />
        <el-table-column prop="versionNoSnapshot" label="固定所选版本"
          ><template #default="{ row }">{{
            getDccVersionDisplayText(row.versionNoSnapshot)
          }}</template></el-table-column
        >
        <el-table-column label="本节点来源"
          ><template #default="{ row }">{{
            row.supplemental ? '本次补充' : row.origin === 'UPLOAD' ? '原上传选择' : '此前已签补充'
          }}</template></el-table-column
        >
        <el-table-column label="整改安排"
          ><template #default="{ row }">{{
            row.assigneeUserId == null
              ? '本节点未安排'
              : `负责人 ${row.assigneeUserId} · 期限 ${row.dueAt}`
          }}</template></el-table-column
        >
        <el-table-column label="正文"
          ><template #default="{ row }"
            ><el-button
              link
              :loading="readingId === row.selectedControlledFileId"
              :disabled="Boolean(readingId)"
              @click="openPreview(row)"
              >查看固定版本</el-button
            ></template
          ></el-table-column
        >
      </el-table>
    </article>
    <el-dialog
      :model-value="Boolean(previewId)"
      title="固定关联版本正文"
      width="88vw"
      destroy-on-close
      @close="closePreview"
    >
      <ProtectedPdfViewer
        v-if="previewSource"
        :key="previewId"
        :controlled-file-id="previewId"
        :preview-source="previewSource"
        title="固定关联版本正文"
      />
    </el-dialog>
  </section>
</template>
<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { getDccVersionDisplayText } from '../shared/version-display'
import { getControlledFileRelationPermissions } from '@/api/dcc/controlledFile/applicationRead'
import { buildDccControlledFilePreviewSource } from '@/api/common/filePreview'
import type {
  RelationRoundEvidence,
  DccApplicationType
} from '@/api/dcc/controlledFile/applicationRead'
import ProtectedPdfViewer from '../view/index.vue'
export interface RelationStageSelectionView {
  relatedMasterId: string
  selectedControlledFileId: string
  projectId: string
  fileNumber: string
  fileName: string
  versionNo: string
  origin: string
  supplemental: boolean
}
export interface RelationStageHeaderView {
  id: string
  tenantId: string
  sourceFileId: string
  sourceMasterId: string
  projectId: string
  processInstanceId: string
  taskId: string
  obligationId: string
  stageCode: string
  nodeInstanceId: string
  departmentId: string | null
  departmentName?: string | null
  actorId: string
  signatureId: string
  signedAt: string
  commandKey: string
  payloadHash: string
  expectedDigest: string
  completeDigest: string
  actionType: string
  reason: string
  applicationType: string | null
  attributeRound: number | null
  formActionInstanceId: string | null
}
export interface RelationStageMemberView {
  tenantId: string
  snapshotId: string
  relatedMasterId: string
  selectedControlledFileId: string
  projectId: string
  fileNumberSnapshot: string
  fileNameSnapshot: string
  versionNoSnapshot: string
  origin: string
  supplemental: boolean
  assigneeUserId: string | null
  dueAt: string | null
}
export interface RelationStageView {
  snapshot: { header: RelationStageHeaderView; members: RelationStageMemberView[] }
  signatureEvidenceStatus: string
  signatureIdentityMatches: boolean
}
export type RelationRoundEvidenceView = RelationRoundEvidence
export interface RelationSignatureFact {
  id?: string | number
  actorNicknameSnapshot?: string | null
  actorDeptNameSnapshot?: string | null
}
const props = defineProps<{
  evidence: RelationRoundEvidence | null
  tenantId: string
  fileId: string
  applicationType: DccApplicationType
  bpmRound: string
  attributeRound: number
  contextKey: string
  signatureFacts?: RelationSignatureFact[]
}>()
const projection = ref<RelationRoundEvidenceView | null>(null),
  error = ref(''),
  previewId = ref('')
const readingId = ref('')
const previewSource = ref<ReturnType<typeof buildDccControlledFilePreviewSource>>()
let generation = 0,
  readSequence = 0
const closePreview = () => {
  readSequence++
  previewId.value = ''
  previewSource.value = undefined
  readingId.value = ''
}
const identity = (value: unknown): string => {
  if (
    typeof value !== 'string' ||
    !/^[1-9][0-9]*$/.test(value) ||
    BigInt(value) > 9223372036854775807n
  )
    throw new Error('关联阶段身份必须是精确Long字符串')
  return value
}
const text = (value: unknown): string => {
  if (typeof value !== 'string' || !value.trim()) throw new Error('关联阶段冻结文字缺失')
  return value
}
const factIdentity = (value: string | number | undefined): string => {
  if (typeof value === 'number' && !Number.isSafeInteger(value))
    throw new Error('签名事实身份精度损失')
  return identity(typeof value === 'number' ? String(value) : value)
}
const signatureFact = (id: string): RelationSignatureFact | undefined =>
  props.signatureFacts?.find((row) => row.id != null && factIdentity(row.id) === id)
const obligationDepartmentName = (header: RelationStageHeaderView): string =>
  header.departmentName?.trim() || '未记录'
const instant = (value: unknown): string => {
  const valueText = text(value),
    parts = /^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$/.exec(valueText)
  if (!parts) throw new Error('关联阶段服务器时间格式无效')
  const [year, month, day, hour, minute, second] = parts.slice(1).map(Number)
  const date = new Date(0)
  date.setUTCFullYear(year, month - 1, day)
  date.setUTCHours(hour, minute, second, 0)
  if (
    year < 1 ||
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() !== month - 1 ||
    date.getUTCDate() !== day ||
    date.getUTCHours() !== hour ||
    date.getUTCMinutes() !== minute ||
    date.getUTCSeconds() !== second
  )
    throw new Error('关联阶段服务器时间不是有效日历日期')
  return valueText
}
const validate = (value: RelationRoundEvidenceView): RelationRoundEvidenceView => {
  if (
    !value ||
    identity(value.tenantId) !== identity(props.tenantId) ||
    identity(value.fileId) !== identity(props.fileId) ||
    value.applicationType !== props.applicationType ||
    value.bpmRound !== props.bpmRound ||
    value.attributeRound !== props.attributeRound ||
    !Number.isSafeInteger(value.attributeRound) ||
    value.attributeRound < 1 ||
    typeof value.recorded !== 'boolean' ||
    !Array.isArray(value.stages) ||
    !Array.isArray(value.originalUploadSelections) ||
    !Array.isArray(value.completeSelections)
  )
    throw new Error('关联阶段证据与所选租户、文件或正式申请轮次不一致')
  identity(value.masterId)
  identity(value.projectId)
  text(value.bpmRound)
  const signatureIds = new Set<string>()
  for (const fact of props.signatureFacts || []) {
    if (fact.id == null) continue
    const id = factIdentity(fact.id)
    if (signatureIds.has(id)) throw new Error('签名事实记录身份重复，不能推断人员名字')
    signatureIds.add(id)
    if (
      (fact.actorNicknameSnapshot != null && typeof fact.actorNicknameSnapshot !== 'string') ||
      (fact.actorDeptNameSnapshot != null && typeof fact.actorDeptNameSnapshot !== 'string')
    )
      throw new Error('签名人员冻结名称格式无效')
  }
  if (
    !['UPLOAD', 'REVISION', 'OBSOLETE'].includes(value.applicationType) ||
    !/^[a-f0-9]{64}$/.test(value.relationDigest) ||
    value.recorded !== value.stages.length > 0
  )
    throw new Error('关联阶段记录事实缺失或不一致')
  const headers = new Set<string>(),
    obligations = new Set<string>()
  for (const stage of value.stages) {
    const header = stage?.snapshot?.header,
      members = stage?.snapshot?.members
    if (!header || !Array.isArray(members) || typeof stage.signatureIdentityMatches !== 'boolean')
      throw new Error('关联阶段签名事实缺失')
    text(stage.signatureEvidenceStatus)
    const headerId = identity(header.id),
      node = JSON.stringify([header.taskId, header.obligationId])
    if (
      headers.has(headerId) ||
      obligations.has(node) ||
      identity(header.tenantId) !== value.tenantId ||
      identity(header.sourceFileId) !== value.fileId ||
      identity(header.sourceMasterId) !== value.masterId ||
      identity(header.projectId) !== value.projectId ||
      header.processInstanceId !== value.bpmRound ||
      (header.applicationType != null &&
        (header.applicationType !== value.applicationType ||
          header.attributeRound !== value.attributeRound))
    )
      throw new Error('关联阶段头身份或义务不一致')
    headers.add(headerId)
    obligations.add(node)
    for (const id of [header.actorId, header.signatureId]) identity(id)
    if (header.departmentId != null) identity(header.departmentId)
    if (header.formActionInstanceId != null) identity(header.formActionInstanceId)
    instant(header.signedAt)
    text(header.taskId)
    text(header.obligationId)
    text(header.nodeInstanceId)
    text(header.stageCode)
    text(header.reason)
    const masters = new Set<string>()
    for (const row of members) {
      if (
        identity(row.tenantId) !== value.tenantId ||
        identity(row.snapshotId) !== headerId ||
        typeof row.supplemental !== 'boolean' ||
        !['UPLOAD', 'SUPPLEMENT'].includes(row.origin)
      )
        throw new Error('关联阶段成员归属不一致')
      const master = identity(row.relatedMasterId)
      identity(row.selectedControlledFileId)
      identity(row.projectId)
      if (masters.has(master)) throw new Error('关联阶段同义务内重复文件身份')
      masters.add(master)
      text(row.fileNameSnapshot)
      text(row.fileNumberSnapshot)
      text(row.versionNoSnapshot)
      if ((row.assigneeUserId != null) !== (row.dueAt != null))
        throw new Error('关联阶段整改事实不完整')
      if (row.assigneeUserId != null) {
        identity(row.assigneeUserId)
        instant(row.dueAt)
      }
    }
  }
  return value
}
const openPreview = async (member: RelationStageMemberView) => {
  const token = generation,
    sequence = ++readSequence
  error.value = ''
  previewId.value = ''
  previewSource.value = undefined
  try {
    if (!projection.value) throw new Error('本轮关联阶段尚未读取')
    const saved = projection.value.stages
      .flatMap((stage) => stage.snapshot.members)
      .find(
        (row) =>
          row.snapshotId === member.snapshotId &&
          row.relatedMasterId === member.relatedMasterId &&
          row.selectedControlledFileId === member.selectedControlledFileId
      )
    if (!saved) throw new Error('预览对象不是本轮正式固定成员')
    const id = identity(saved.selectedControlledFileId)
    readingId.value = id
    const metadata = await getControlledFileRelationPermissions(id)
    if (token !== generation || sequence !== readSequence) return
    if (
      metadata.controlledFileId !== id ||
      metadata.tenantId !== props.tenantId ||
      metadata.masterId !== saved.relatedMasterId ||
      metadata.projectId !== saved.projectId ||
      metadata.versionNo !== saved.versionNoSnapshot
    )
      throw new Error('正文权限投影与固定所选版本不一致')
    if (metadata.pendingEffect || metadata.status === 'CONTROLLED_PENDING_EFFECTIVE')
      throw new Error('待生效历史关联不开放日常正文')
    if (!['ACTIVE', 'OBSOLETE', 'SUPERSEDED'].includes(metadata.status))
      throw new Error('该历史关联版本尚无正式正文查阅状态')
    if (metadata.canPreview !== true) throw new Error('没有该固定版本正文查看权限')
    previewSource.value = buildDccControlledFilePreviewSource(id, {
      intent: 'VERSION_HISTORY',
      controlledFileId: id,
      expectedVersionNo: saved.versionNoSnapshot
    })
    previewId.value = id
  } catch (cause) {
    if (token === generation && sequence === readSequence)
      error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    if (token === generation && sequence === readSequence) readingId.value = ''
  }
}
const stageLabels: Record<string, string> = {
  MATRIX_REVIEW: '会签阶段',
  MATRIX_APPROVAL: '批准阶段',
  DOC_CONTROL_REVIEW: '文控审核阶段'
}
const signatureLabels: Record<string, string> = {
  VALID: '签名证据有效',
  INVALID: '签名证据异常',
  MISSING: '签名记录缺失',
  IDENTITY_MISMATCH: '签名身份不一致',
  UNRECORDED: '签名状态未记录'
}
const stageLabel = (code: string) => stageLabels[code] || '审批阶段'
const signatureLabel = (status: string) => signatureLabels[status] || `签名状态：${status}`
watch(
  () => [
    props.evidence,
    props.contextKey,
    props.tenantId,
    props.fileId,
    props.applicationType,
    props.bpmRound,
    props.attributeRound,
    props.signatureFacts
  ],
  () => {
    generation++
    readSequence++
    projection.value = null
    error.value = ''
    previewId.value = ''
    previewSource.value = undefined
    readingId.value = ''
    try {
      if (props.evidence != null) projection.value = validate(props.evidence)
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : String(cause)
    }
  },
  { immediate: true, deep: true, flush: 'sync' }
)
onBeforeUnmount(() => {
  generation++
  closePreview()
})
</script>
