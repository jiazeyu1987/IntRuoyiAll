<template>
  <ContentWrap title="文件夹模板">
    <div class="mb-16px flex gap-8px">
      <el-button v-hasPermi="['dcc:project-code:update']" type="primary" :disabled="loading || !!error" @click="editor?.open()">
        <Icon icon="ep:plus" class="mr-5px" />新增模板
      </el-button>
      <el-button :loading="loading" @click="loadTemplates">刷新</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mb-16px" />
    <el-table v-loading="loading" :data="templates" border stripe>
      <el-table-column prop="name" label="模板名称" min-width="240" show-overflow-tooltip />
      <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? '启用' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="历史使用" width="110">
        <template #default="{ row }">{{ row.everUsed ? '已使用' : '未使用' }}</template>
      </el-table-column>
      <el-table-column prop="editedByUserId" label="修改人账号ID" width="150" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="['dcc:project-code:update']" link type="primary" @click="editor?.open(row.id)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-alert title="模板修改用于后续新申请，已有项目目录独立保留；已使用的模板可停用，不能删除。" type="info" :closable="false" class="mt-16px" />
    <FolderTemplateLibraryEditor ref="editor" @saved="loadTemplates" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getFolderTemplates, type FolderTemplate } from '@/api/dcc/controlledFile/projectAttributes'
import FolderTemplateLibraryEditor from '../components/FolderTemplateLibraryEditor.vue'

defineOptions({ name: 'DccFolderTemplateBasicDataPage' })
const editor = ref<InstanceType<typeof FolderTemplateLibraryEditor>>()
const templates = ref<FolderTemplate[]>([])
const loading = ref(false)
const error = ref('')
const loadTemplates = async () => {
  if (loading.value) return
  loading.value = true
  error.value = ''
  try {
    templates.value = await getFolderTemplates()
  } catch (cause) {
    templates.value = []
    error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    loading.value = false
  }
}
onMounted(loadTemplates)
</script>
