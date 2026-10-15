<template>
  <ContentWrap v-loading="loading" data-testid="dcc-detail-relations">
    <p class="font-600">所选版本关联</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <p v-if="selectedSource"
      >关联来源：{{ selectedSource.fileName }} ·
      {{ getDccVersionDisplayText(selectedSource.versionNo) }} · 文件 #{{
        selectedSource.controlledFileId
      }}</p
    >
    <p v-if="applicationSelection"
      >所选申请：{{ applicationSelection.applicationType }} · BPM
      {{ applicationSelection.bpmRound }} · 属性第 {{ applicationSelection.attributeRound }} 轮</p
    >
    <el-alert v-if="unavailableReason" :title="unavailableReason" type="info" :closable="false" />
    <el-table
      v-if="selectedSource && !unavailableReason"
      :data="files"
      empty-text="所选版本未关联文件"
      aria-label="所选版本固定关联集合"
    >
      <el-table-column prop="fileName" label="冻结关联文件" />
      <el-table-column prop="fileNumber" label="冻结编号" />
      <el-table-column label="固定所选版本"
        ><template #default="{ row }">{{
          getDccVersionDisplayText(row.versionNo)
        }}</template></el-table-column
      >
      <el-table-column v-if="applicationSelection" label="关联来源"
        ><template #default="{ row }">{{
          row.origin === 'UPLOAD' ? '原上传选择' : '后补选择'
        }}</template></el-table-column
      >
      <el-table-column label="正文"
        ><template #default="{ row }"
          ><el-button
            link
            :disabled="!row.canPreview || Boolean(readingId)"
            :loading="readingId === row.controlledFileId"
            @click="openPreview(row)"
            >查看固定版本</el-button
          ></template
        ></el-table-column
      >
    </el-table>
    <DetailRelationStageEvidence
      v-if="applicationEvidence && relationEvidence && applicationSelection && selectedSource"
      :key="selectedSource.contextKey"
      :evidence="relationEvidence"
      :tenant-id="relationEvidence.tenantId"
      :file-id="relationEvidence.fileId"
      :application-type="applicationSelection.applicationType"
      :bpm-round="applicationSelection.bpmRound"
      :attribute-round="applicationSelection.attributeRound"
      :context-key="selectedSource.contextKey"
      :signature-facts="applicationEvidence.signatures"
    />
    <el-dialog v-model="previewVisible" title="固定关联版本正文" width="88vw" destroy-on-close>
      <ProtectedPdfViewer
        v-if="previewSource"
        :key="previewId"
        :controlled-file-id="previewId"
        :preview-source="previewSource"
        title="固定关联版本正文"
      />
    </el-dialog>
  </ContentWrap>
</template>
<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { buildDccControlledFilePreviewSource } from '@/api/common/filePreview'
import {
  getControlledFileRelationPermissions,
  getControlledFileApplicationEvidence
} from '@/api/dcc/controlledFile/applicationRead'
import type {
  ControlledFileRelationPermissions,
  ControlledFileApplicationEvidence,
  RelationRoundEvidence
} from '@/api/dcc/controlledFile/applicationRead'
import { listHistoricalRelations } from '@/api/dcc/controlledFile/relations'
import type { SelectorSource } from '../relations/DccFileSelector.vue'
import ProtectedPdfViewer from '../view/index.vue'
import DetailRelationStageEvidence from './DetailRelationStageEvidence.vue'
import { getDccVersionDisplayText } from '../shared/version-display'
import {
  detailIdentity,
  assertRelationSource,
  historicalRelationCandidate,
  applicationRelationCandidate,
  assertRelationApplicationSelection,
  assertRelationApplicationEvidence
} from './relation-contract'
import type {
  DetailRelationFile,
  HistoricalRelationSnapshot,
  DetailRelationApplicationSelection,
  DetailRelationRow
} from './relation-contract'

const props = defineProps<{
  file: DetailRelationFile
  allowEdit: boolean
  autoOpenEditor?: boolean
  applicationSelection?: DetailRelationApplicationSelection | null
}>()
const loading = ref(false),
  error = ref(''),
  unavailableReason = ref('')
const selectedSource = ref<SelectorSource>(),
  files = ref<DetailRelationRow[]>([])
const applicationEvidence = ref<ControlledFileApplicationEvidence>(),
  relationEvidence = ref<RelationRoundEvidence>()
const previewId = ref(''),
  readingId = ref('')
const previewSource = ref<ReturnType<typeof buildDccControlledFilePreviewSource>>()
let generation = 0,
  readSequence = 0
const previewVisible = computed({
  get: () => Boolean(previewId.value),
  set: (value) => {
    if (!value) {
      readSequence++
      previewId.value = ''
      previewSource.value = undefined
      readingId.value = ''
    }
  }
})
const tenant = () => detailIdentity(getVisitTenantId() ?? getTenantId())
const assertMetadata = (metadata: ControlledFileRelationPermissions, tenantId: string) => {
  if (metadata.tenantId !== tenantId) throw new Error('关联名称投影租户不一致')
  return metadata
}
const identityFile = (metadata: ControlledFileRelationPermissions): DetailRelationFile => ({
  id: metadata.controlledFileId,
  masterId: metadata.masterId,
  dccProjectCodeId: metadata.projectId,
  projectFolderId: metadata.projectFolderId,
  versionNo: metadata.versionNo,
  title: metadata.fileName,
  fileNumber: metadata.fileNumber
})
const assertMembers = (
  file: DetailRelationFile,
  rows: Array<{ id: unknown; master: unknown; project: unknown }>
) => {
  const masters = new Set<string>(),
    ids = new Set<string>()
  for (const row of rows) {
    const id = detailIdentity(row.id),
      master = detailIdentity(row.master)
    if (
      id === detailIdentity(file.id) ||
      master === detailIdentity(file.masterId) ||
      masters.has(master) ||
      ids.has(id) ||
      detailIdentity(row.project) !== detailIdentity(file.dccProjectCodeId)
    )
      throw new Error('所选冻结关联包含自身、重复或其他项目成员')
    masters.add(master)
    ids.add(id)
  }
}
const navigation = async (
  id: string,
  masterId: string,
  projectId: string,
  versionNo: string,
  tenantId: string
) => {
  const target = assertMetadata(await getControlledFileRelationPermissions(id), tenantId)
  if (
    target.controlledFileId !== id ||
    target.masterId !== masterId ||
    target.projectId !== projectId ||
    target.versionNo !== versionNo
  )
    throw new Error('关联权限投影与固定所选版本不一致')
  return {
    tenantId: target.tenantId,
    controlledFileId: id,
    masterId,
    projectId,
    projectName: target.projectName,
    folderName: '历史未记录',
    canPreview: target.canPreview
  }
}
const load = async () => {
  const token = ++generation
  readSequence++
  loading.value = true
  error.value = ''
  unavailableReason.value = ''
  files.value = []
  selectedSource.value = undefined
  applicationEvidence.value = undefined
  relationEvidence.value = undefined
  previewId.value = ''
  previewSource.value = undefined
  readingId.value = ''
  try {
    if (props.applicationSelection === null) throw new Error('请先选择并核验正式申请轮次')
    const file = { ...props.file },
      selectedId = detailIdentity(file.id),
      tenantId = tenant()
    const selection = props.applicationSelection ? { ...props.applicationSelection } : null
    if (selection) assertRelationApplicationSelection(file, selection)
    const selected = assertMetadata(
      await getControlledFileRelationPermissions(selectedId),
      tenantId
    )
    if (token !== generation) return
    assertRelationSource(file, identityFile(selected), selectedId)
    const context: SelectorSource = {
      contextKey: JSON.stringify([tenantId, file.masterId, selectedId, selection, token]),
      tenantId,
      controlledFileId: selectedId,
      masterId: selected.masterId,
      projectId: selected.projectId,
      folderId: selected.projectFolderId ?? undefined,
      projectName: selected.projectName,
      folderName: selected.projectFolderName || '未记录',
      fileName: selected.fileName,
      fileNumber: selected.fileNumber,
      versionNo: selected.versionNo
    }
    let rows: DetailRelationRow[]
    if (selection) {
      const evidence = await getControlledFileApplicationEvidence(
        selectedId,
        selection.applicationType,
        selection.bpmRound
      )
      if (token !== generation) return
      const relations = assertRelationApplicationEvidence(file, selection, tenantId, evidence)
      if (relations === null) {
        selectedSource.value = context
        applicationEvidence.value = evidence
        unavailableReason.value = '所选申请关联阶段证据未记录'
        return
      }
      assertMembers(
        file,
        relations.completeSelections.map((row) => ({
          id: row.selectedControlledFileId,
          master: row.relatedMasterId,
          project: row.projectId
        }))
      )
      rows = await Promise.all(
        relations.completeSelections.map(async (row) =>
          applicationRelationCandidate(
            row,
            await navigation(
              detailIdentity(row.selectedControlledFileId),
              detailIdentity(row.relatedMasterId),
              detailIdentity(row.projectId),
              row.versionNo,
              tenantId
            )
          )
        )
      )
      if (token !== generation) return
      applicationEvidence.value = evidence
      relationEvidence.value = relations
    } else {
      const snapshots = (await listHistoricalRelations(selectedId)) as HistoricalRelationSnapshot[]
      if (token !== generation) return
      if (!Array.isArray(snapshots)) throw new Error('所选版本关联冻结响应缺失')
      assertMembers(
        file,
        snapshots.map((row) => ({
          id: row.controlledFileId,
          master: row.masterId,
          project: row.projectCodeId
        }))
      )
      rows = await Promise.all(
        snapshots.map(async (row) =>
          historicalRelationCandidate(
            row,
            await navigation(
              detailIdentity(row.controlledFileId),
              detailIdentity(row.masterId),
              detailIdentity(row.projectCodeId),
              row.versionNo,
              tenantId
            )
          )
        )
      )
    }
    if (token !== generation) return
    selectedSource.value = context
    files.value = rows
  } catch (cause) {
    if (token === generation) error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    if (token === generation) loading.value = false
  }
}
const openPreview = async (row: DetailRelationRow) => {
  const token = generation,
    sequence = ++readSequence
  previewId.value = ''
  previewSource.value = undefined
  error.value = ''
  try {
    const saved = files.value.find(
      (file) => file.controlledFileId === row.controlledFileId && file.masterId === row.masterId
    )
    if (!saved || !selectedSource.value) throw new Error('预览对象不是所选版本的正式固定关联')
    const id = detailIdentity(saved.controlledFileId)
    readingId.value = id
    const permission = assertMetadata(
      await getControlledFileRelationPermissions(id),
      selectedSource.value.tenantId
    )
    if (token !== generation || sequence !== readSequence) return
    if (
      permission.controlledFileId !== id ||
      permission.masterId !== saved.masterId ||
      permission.projectId !== saved.projectId ||
      permission.versionNo !== saved.versionNo
    )
      throw new Error('关联正文权限与固定版本不一致')
    if (
      permission.pendingEffect ||
      !['ACTIVE', 'SUPERSEDED', 'OBSOLETE'].includes(permission.status) ||
      permission.canPreview !== true
    )
      throw new Error('该固定关联版本尚无正式正文查阅权限或状态')
    previewSource.value = buildDccControlledFilePreviewSource(id, {
      intent: 'VERSION_HISTORY',
      controlledFileId: id,
      expectedVersionNo: saved.versionNo
    })
    previewId.value = id
  } catch (cause) {
    if (token === generation && sequence === readSequence)
      error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    if (token === generation && sequence === readSequence) readingId.value = ''
  }
}
watch(() => [props.file, props.applicationSelection, tenant()], load, {
  immediate: true,
  deep: true,
  flush: 'sync'
})
onBeforeUnmount(() => {
  generation++
  readSequence++
  previewId.value = ''
  previewSource.value = undefined
  readingId.value = ''
})
</script>
