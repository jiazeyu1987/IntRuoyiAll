<template>
  <section v-loading="state.loading" class="mt-12px" data-testid="dcc-project-independent-folders">
    <div class="mb-8px flex items-center justify-between">
      <span class="font-600">项目存储文件夹</span>
      <div>
        <el-button v-hasPermi="['dcc:project-code:update']" link type="primary" :disabled="state.loading" @click="editor?.open(projectId)">新增文件夹</el-button>
        <el-button link type="primary" :disabled="state.loading" @click="load">刷新目录</el-button>
      </div>
    </div>
    <el-alert v-if="state.error" :title="state.error" type="error" :closable="false" />
    <el-tree v-else-if="state.tree.length" :data="state.tree" node-key="id" :props="{ label: 'name' }" default-expand-all>
      <template #default="{ data }">
        <span>{{ data.name }}</span>
        <el-tag v-if="!data.active" class="ml-8px" type="info" size="small">停用</el-tag>
        <el-button v-hasPermi="['dcc:project-code:update']" link type="primary" class="ml-8px" :disabled="!data.active" @click.stop="editor?.open(projectId, data.id)">编辑</el-button>
        <el-button v-hasPermi="['dcc:project-code:update']" link type="danger" class="ml-8px" :disabled="!data.active" @click.stop="editor?.openDelete(projectId, data.id)">删除</el-button>
      </template>
    </el-tree>
    <el-empty v-else-if="!state.loading" description="项目尚未生成独立目录" :image-size="70" />
    <ProjectFolderEditor ref="editor" @saved="saved" @deleted="saved" />
  </section>
</template>
<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { getProjectFolders } from '@/api/dcc/controlledFile/projectAttributes'
import { createProjectFolderState, loadProjectFolderTree } from './project-folder-tree'
import ProjectFolderEditor from './ProjectFolderEditor.vue'
const props = defineProps<{ projectId: number | string }>()
const state = reactive(createProjectFolderState())
const editor = ref<InstanceType<typeof ProjectFolderEditor>>()
const load = () => loadProjectFolderTree(state, String(props.projectId), getProjectFolders)
const saved = (id: number | string) => { if (String(id) === String(props.projectId)) void load() }
watch(() => props.projectId, () => { void load() }, { immediate: true })
</script>
