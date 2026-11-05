<template>
  <header class="parser-header">
    <h2>{{ title }}</h2>
    <div class="parser-actions">
      <el-select
        v-if="requiresProject"
        :model-value="projectId"
        aria-label="所属项目"
        placeholder="选择所属项目"
        filterable
        remote
        :remote-method="(keyword: string) => emit('search', keyword)"
        :loading="projectLoading"
        :disabled="busy || (hasContent && !published)"
        class="project-select"
        @update:model-value="emit('update:projectId', $event)"
      >
        <el-option
          v-for="project in projects"
          :key="project.id"
          :value="project.id"
          :disabled="project.occupied"
          :label="`${project.projectCode} / ${project.projectName}${project.occupied ? '（已有工艺流程）' : ''}`"
        />
      </el-select>
      <el-upload
        v-if="canImport"
        :auto-upload="false"
        :show-file-list="false"
        :accept="importAccept"
        :disabled="busy || (requiresProject && !projectId)"
        :on-change="(file: UploadFile) => emit('import', file)"
      >
        <el-button :disabled="busy || (requiresProject && !projectId)">导入 Word</el-button>
      </el-upload>
      <el-button v-if="canImport" :disabled="busy || !hasContent" @click="emit('export')"
        >导出 Word</el-button
      >
      <el-button :disabled="busy || !hasContent || published" @click="emit('save')"
        >保存草稿</el-button
      >
      <el-button :disabled="busy" @click="emit('restore')">恢复草稿</el-button>
      <el-button
        v-if="canPublish"
        type="primary"
        :disabled="busy || !hasContent || published || (requiresProject && (!projectId || projects.some((p) => p.id === projectId && p.occupied)))"
        @click="emit('preview')"
        >预览发布</el-button
      >
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { UploadFile } from 'element-plus'
import { hasPermission } from '@/directives/permission/hasPermi'

const props = withDefaults(
  defineProps<{
    title: string
    projectId?: number
    requiresProject?: boolean
    projects: Array<{ id: number; projectCode: string; projectName: string; occupied?: boolean }>
    projectLoading?: boolean
    busy: boolean
    hasContent: boolean
    published: boolean
    importPermission?: string
    importAccept?: string
    publishPermission: string
  }>(),
  { importAccept: '.docx', requiresProject: true }
)
const emit = defineEmits<{
  'update:projectId': [value: number]
  search: [keyword: string]
  import: [file: UploadFile]
  export: []
  save: []
  restore: []
  preview: []
}>()
const canImport = computed(() => !props.importPermission || hasPermission([props.importPermission]))
const canPublish = computed(() => hasPermission([props.publishPermission]))
</script>

<style scoped>
.parser-header,
.parser-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.parser-header {
  justify-content: space-between;
  margin-bottom: 16px;
}
.parser-header h2 {
  margin: 0;
  font-size: 22px;
}
.parser-actions {
  margin-left: auto;
}
.project-select {
  width: 320px;
}
.parser-actions .el-button + .el-button {
  margin-left: 0;
}
@media (max-width: 600px) {
  .parser-actions {
    width: 100%;
  }
  .project-select {
    width: 100%;
  }
}
</style>
