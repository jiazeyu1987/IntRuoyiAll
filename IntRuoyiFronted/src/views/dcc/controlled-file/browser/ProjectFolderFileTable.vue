<template>
  <el-table
    border
    stripe
    class="project-folder-file-table"
    :data="resolveRows ? resolveRows(rows) : rows"
    :row-key="(row) => row.key"
    v-loading="loading"
    highlight-current-row
    @current-change="selectRow"
  >
    <el-table-column v-if="canReference" label="选择" width="60"
      ><template #default="{ row }"
        ><el-radio
          v-if="row.reference && canReference"
          :model-value="selectedReference?.reference.id"
          :label="row.reference.reference.id"
          @change="emit('select-reference', row.reference)"
          ><span class="sr-only">选择引用</span></el-radio
        ></template
      ></el-table-column
    >
    <el-table-column label="文件" min-width="260" align="left"
      ><template #default="{ row }">
        <div class="project-file-identity">
          <span
            class="project-file-name"
            :class="{ 'is-referenced': row.referenced }"
            :data-testid="
              row.reference ? 'dcc-project-reference-file-name' : 'dcc-project-browser-file-name'
            "
            >{{ row.file.fileName }}</span
          >
          <span class="project-file-number">编号：{{ row.file.fileNumber }}</span>
        </div>
      </template></el-table-column
    >
    <el-table-column label="来源位置" min-width="190" align="left"
      ><template #default="{ row }">
        <span class="project-file-location"
          >{{ row.reference?.sourceProjectName || row.file.projectName || '未记录' }} /
          {{ row.file.folderName || '未记录' }}</span
        >
      </template></el-table-column
    >
    <el-table-column prop="file.versionNo" label="版本" width="100">
      <template #default="{ row }">
        <span class="project-file-version">{{ getDccVersionDisplayText(row.file.versionNo) }}</span>
        <span v-if="row.reference" class="project-file-version-note">固定引用版本</span>
      </template>
    </el-table-column>
    <el-table-column label="状态" min-width="160"
      ><template #default="{ row }">
        <div class="project-file-status">
          <span>{{ statusLabel(row.file.status) }}</span>
          <ArchiveDownloadEntry
            :file-id="row.file.controlledFileId"
            :version-no="row.file.versionNo"
            :file-name="row.file.fileName"
            :file-number="row.file.fileNumber"
            :status="row.file.status"
            :reference-project-id="row.reference?.reference.projectId"
            :reference-folder-id="row.reference?.reference.folderId"
          />
        </div> </template
    ></el-table-column>
    <el-table-column v-if="!readOnly" label="引用情况" min-width="180"
      ><template #default="{ row }">
        <DccReferenceBadge
          v-if="row.projectCount != null"
          :is-reference="Boolean(row.reference)"
          :project-count="row.projectCount"
          :source-project-name="
            row.reference?.sourceProjectName || row.file.projectName || '未记录'
          "
          :pending-effect="row.file.pendingEffect"
          :obsolete="row.file.status === 'OBSOLETE'"
        />
        <el-button
          link
          data-testid="dcc-project-reference-usage"
          :disabled="loading"
          @click="emit('usage', row.file)"
          >使用明细</el-button
        >
      </template></el-table-column
    >
    <el-table-column label="操作" min-width="230"
      ><template #default="{ row }">
        <div class="project-file-actions">
          <template v-if="row.reference">
            <el-button
              link
              data-testid="dcc-project-reference-preview"
              :disabled="loading || Boolean(readingReferenceId) || row.file.canPreview !== true"
              @click="emit('read-reference', row.reference, 'preview')"
              >浏览</el-button
            >
            <el-button
              link
              data-testid="dcc-project-reference-trace"
              :disabled="loading || Boolean(readingReferenceId)"
              @click="emit('read-reference', row.reference, 'trace')"
              >只读追溯</el-button
            >
          </template>
          <template v-else>
            <el-button v-if="canDownload" link @click="emit('download', row.file)"
              >下载原件</el-button
            >
            <el-button
              link
              :disabled="loading || !row.file.canPreview"
              @click="emit('preview', row.file)"
              >浏览</el-button
            >
            <el-button
              v-if="!readOnly"
              link
              data-testid="dcc-project-file-relations"
              :disabled="loading"
              @click="emit('relations', row.file)"
              >关联</el-button
            >
            <el-button
              v-if="canManage ? canManage(row.file) : !readOnly"
              link
              :disabled="loading"
              @click="emit('operation', row.file)"
              >操作面板</el-button
            >
          </template>
        </div>
      </template></el-table-column
    >
  </el-table>
</template>
<script setup lang="ts">
import ArchiveDownloadEntry from '../shared/ArchiveDownloadEntry.vue'
import { computed } from 'vue'
import { getDccVersionDisplayText } from '../shared/version-display'
import DccReferenceBadge from '../relations/DccReferenceBadge.vue'
import type { FileCandidate } from '../relations/selector-state'
import type { ReferenceRow } from '../relations/project-reference-contract'
import { buildFolderFileRows, type FolderFileRow } from './folder-file-list'
import { getDccControlledFileStatusLabel, type DccControlledFileStatus } from '../shared/lifecycle'
const props = defineProps<{
  canDownload?: boolean
  readOnly?: boolean
  canManage?: (file: FileCandidate) => boolean
  files: FileCandidate[]
  resolveRows?: (rows: FolderFileRow[]) => FolderFileRow[]
  references: ReferenceRow[]
  usage: Record<string, { referenceProjectCount: number; referenced: boolean }>
  keyword: string
  versionView?: string
  loading: boolean
  canReference: boolean
  selectedReference?: ReferenceRow
  readingReferenceId?: string
}>()
const emit = defineEmits<{
  'select-reference': [row: ReferenceRow | undefined]
  'read-reference': [row: ReferenceRow, action: 'preview' | 'trace']
  preview: [row: FileCandidate]
  relations: [row: FileCandidate]
  download: [row: FileCandidate]
  operation: [row: FileCandidate]
  usage: [file: FileCandidate | ReferenceRow['selectedVersion']]
}>()
const rows = computed(() =>
  buildFolderFileRows(props.files, props.references, props.usage, props.keyword, props.versionView)
)
const statusLabel = (status: string) =>
  getDccControlledFileStatusLabel(status as DccControlledFileStatus)
const selectRow = (row?: FolderFileRow | null) => emit('select-reference', row?.reference)
</script>
<style scoped>
.project-folder-file-table {
  width: 100%;
  --el-table-header-bg-color: #f5f8f9;
  font-size: 13px;
}
.project-folder-file-table :deep(.el-table__cell) {
  padding: 6px 0;
}
.project-folder-file-table :deep(.el-button.is-link) {
  margin: 2px 8px 2px 0;
}
.project-file-name {
  overflow-wrap: anywhere;
}
.project-file-identity,
.project-file-status {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  text-align: left;
}
.project-file-number,
.project-file-version-note {
  color: var(--el-text-color-secondary);
  overflow-wrap: anywhere;
}
.project-file-version {
  white-space: nowrap;
}
.project-file-version-note {
  display: block;
  font-size: 12px;
}
.project-file-location {
  overflow-wrap: anywhere;
}
.project-file-status :deep(.el-tag) {
  height: auto;
  max-width: 100%;
  padding: 4px 6px;
  white-space: normal;
  line-height: 1.5;
}
.project-file-status :deep(.el-tag__content) {
  white-space: normal;
  overflow-wrap: anywhere;
}
.project-file-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}
.project-file-name.is-referenced {
  color: #b85a00;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
}
</style>
