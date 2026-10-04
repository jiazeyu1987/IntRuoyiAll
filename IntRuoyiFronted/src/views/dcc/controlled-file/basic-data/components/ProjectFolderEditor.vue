<template>
  <Dialog v-model="visible" :title="deleteMode ? '删除项目文件夹' : form.id ? '编辑项目文件夹' : '新增项目文件夹'" width="620px" @close="close">
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-form v-loading="loading" label-width="110px" :disabled="saving">
      <el-form-item label="文件夹名称" required><el-input v-model="form.name" :disabled="deleteMode" maxlength="128" /></el-form-item>
      <el-form-item label="上级文件夹" required>
        <el-select v-model="form.parentId" :disabled="deleteMode" filterable>
          <el-option value="0" label="项目根目录" />
          <el-option v-for="row in parentOptions" :key="String(row.id)" :value="String(row.id)" :label="row.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="排序" required><el-input-number v-model="form.sortOrder" :disabled="deleteMode" :min="0" :precision="0" /></el-form-item>
      <el-form-item label="修改原因" required><el-input v-model="form.changeReason" maxlength="500" /></el-form-item>
      <el-alert :title="deleteMode ? '存在子目录、文件位置或引用时不能删除。删除保留历史身份，原文件和存储位置保持不变。' : '仅修改本项目的目录结构，模板和文件的存储位置保持独立。'" type="info" :closable="false" />
    </el-form>
    <template #footer>
      <el-button v-hasPermi="['dcc:project-code:update']" :type="deleteMode ? 'danger' : 'primary'" :disabled="!ready || loading" :loading="saving" @click="deleteMode ? remove() : save()">{{ deleteMode ? '删除' : '保存' }}</el-button>
      <el-button @click="close">关闭</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { getProjectFolders, saveProjectFolder, deleteProjectFolder, type ProjectFolder, type ProjectFolderSave } from '@/api/dcc/controlledFile/projectAttributes'
import { ElMessageBox } from 'element-plus'
import { buildProjectFolderTree } from './project-folder-tree'
const emit = defineEmits<{ saved: [projectId: number | string, folder: ProjectFolder]; deleted: [projectId: number | string, folderId: number | string] }>()
const visible = ref(false), loading = ref(false), saving = ref(false), ready = ref(false), error = ref('')
const projectId = ref<number | string>(), rows = ref<ProjectFolder[]>([])
const deleteMode = ref(false)
const form = reactive<ProjectFolderSave>({ parentId: '0', name: '', sortOrder: 0, changeReason: '' })
let revision = 0
const parentOptions = computed(() => rows.value.filter(row => {
  if (!row.active) return false
  const seen = new Set<string>()
  let cursor: ProjectFolder | undefined = row
  while (cursor) {
    const id = String(cursor.id)
    if (id === String(form.id) || seen.has(id)) return false
    seen.add(id)
    if (String(cursor.parentId) === '0') return true
    cursor = rows.value.find(item => String(item.id) === String(cursor!.parentId))
  }
  return false
}))
const close = () => { revision++; visible.value = false; ready.value = false; loading.value = false; saving.value = false }
const open = async (id: number | string, folderId?: number | string) => {
  const current = ++revision
  visible.value = true; loading.value = true; saving.value = false; ready.value = false; error.value = ''
  deleteMode.value = false
  projectId.value = id; rows.value = []
  Object.assign(form, { id: undefined, parentId: '0', name: '', sortOrder: 0, changeReason: '' })
  try {
    const result = await getProjectFolders(id)
    if (current !== revision) return
    buildProjectFolderTree(String(id), result)
    rows.value = result
    if (folderId !== undefined) {
      const row = result.find(item => String(item.id) === String(folderId))
      if (!row || !row.active) throw new Error('项目文件夹不存在、已停用或不属于当前项目')
      Object.assign(form, { id: row.id, parentId: String(row.parentId), name: row.name, sortOrder: row.sortOrder })
    }
    ready.value = true
  } catch (cause) { if (current === revision) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (current === revision) loading.value = false }
}
const openDelete = async (id: number | string, folderId: number | string) => {
  const pending = open(id, folderId), current = revision
  await pending
  if (current === revision && ready.value) deleteMode.value = true
}
const remove = async () => {
  if (!deleteMode.value || !ready.value || saving.value || projectId.value === undefined || form.id === undefined) return
  const current = revision, id = projectId.value, folderId = form.id, name = form.name, reason = form.changeReason.trim()
  saving.value = true; error.value = ''
  try {
    if (!reason || reason.length > 500) throw new Error('请填写有效删除原因')
    await ElMessageBox.confirm(`确认删除项目 ${id} 的文件夹“${name}”？`, '再次确认删除', { type: 'warning' })
    if (current !== revision) return
    await deleteProjectFolder(id, folderId, { confirmed: true, changeReason: reason })
    emit('deleted', id, folderId)
    if (current === revision) { visible.value = false; ready.value = false }
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close' && current === revision) error.value = cause instanceof Error ? cause.message : String(cause)
  } finally { if (current === revision) saving.value = false }
}
const save = async () => {
  if (!ready.value || saving.value || projectId.value === undefined) return
  const current = revision, id = projectId.value
  saving.value = true; error.value = ''
  try {
    if (!form.name.trim() || form.name.length > 128 || !Number.isInteger(form.sortOrder) || form.sortOrder < 0
      || !form.changeReason.trim() || !/^(0|[1-9][0-9]*)$/.test(String(form.parentId))) throw new Error('请填写有效名称、上级、排序及修改原因')
    const payload: ProjectFolderSave = { id: form.id, parentId: form.parentId, name: form.name.trim(), sortOrder: form.sortOrder, changeReason: form.changeReason.trim() }
    const saved = await saveProjectFolder(id, payload)
    if (!saved || String(saved.projectCodeId) !== String(id) || (payload.id !== undefined && String(saved.id) !== String(payload.id))) {
      throw new Error('项目目录保存响应身份不匹配，请重新读取核对')
    }
    emit('saved', id, saved)
    if (current === revision) { visible.value = false; ready.value = false }
  } catch (cause) { if (current === revision) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (current === revision) saving.value = false }
}
defineExpose({ open, openDelete })
</script>
