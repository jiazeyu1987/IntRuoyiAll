<template>
  <el-table border stripe
    :data="resolveRows ? resolveRows(rows) : rows"
    :row-key="(row) => row.key"
    v-loading="loading"
    highlight-current-row
    @current-change="selectRow"
  >
    <el-table-column label="选择" width="60"
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
    <el-table-column label="文件名称" min-width="180"
      ><template #default="{ row }">
        <span
          class="project-file-name"
          :class="{ 'is-referenced': row.referenced }"
          :data-testid="
            row.reference ? 'dcc-project-reference-file-name' : 'dcc-project-browser-file-name'
          "
          >{{ row.file.fileName }}</span
        >
      </template></el-table-column
    >
    <el-table-column prop="file.fileNumber" label="编号" min-width="110" />
    <el-table-column label="所属项目" min-width="120"
      ><template #default="{ row }">{{
        row.reference?.sourceProjectName || row.file.projectName || '未记录'
      }}</template></el-table-column
    >
    <el-table-column prop="file.folderName" label="来源文件夹" min-width="100" />
    <el-table-column prop="file.versionNo" label="版本" width="100">
      <template #default="{ row }">{{ getDccVersionDisplayText(row.file.versionNo) }}</template>
    </el-table-column>
    <el-table-column label="状态" min-width="120"
      ><template #default="{ row }">{{ statusLabel(row.file.status) }}</template></el-table-column
    >
    <el-table-column label="引用情况" min-width="180"
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
    <el-table-column label="操作" min-width="205"
      ><template #default="{ row }">
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
          <el-button
            link
            :disabled="loading || !row.file.canPreview"
            @click="emit('preview', row.file)"
            >浏览</el-button
          >
          <el-button
            link
            data-testid="dcc-project-file-relations"
            :disabled="loading"
            @click="emit('relations', row.file)"
            >关联</el-button
          >
          <el-button link :disabled="loading" @click="emit('operation', row.file)"
            >操作面板</el-button
          >
        </template>
      </template></el-table-column
    >
  </el-table>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { getDccVersionDisplayText } from '../shared/version-display'
import DccReferenceBadge from '../relations/DccReferenceBadge.vue'
import type { FileCandidate } from '../relations/selector-state'
import type { ReferenceRow } from '../relations/project-reference-contract'
import { buildFolderFileRows, type FolderFileRow } from './folder-file-list'
import { getDccControlledFileStatusLabel, type DccControlledFileStatus } from '../shared/lifecycle'
const props = defineProps<{
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
