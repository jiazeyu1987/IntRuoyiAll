<template>
  <Dialog v-model="visible" title="文件夹模板库" width="980px">
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-table :data="templates" highlight-current-row @current-change="choose" max-height="200">
      <el-table-column prop="name" label="模板名称" />
      <el-table-column label="状态"><template #default="{ row }">{{ row.active ? '启用' : '停用' }}</template></el-table-column>
      <el-table-column label="历史使用"><template #default="{ row }">{{ row.everUsed ? '已使用' : '未使用' }}</template></el-table-column>
      <el-table-column prop="editedByUserId" label="修改人账号ID" />
    </el-table>
    <el-button v-hasPermi="['dcc:project-code:update']" class="my-8px" @click="reset">新增模板</el-button>
    <el-form v-if="selectionValid" label-width="100px" v-loading="loading">
      <el-form-item label="模板名称" required><el-input v-model="form.name" maxlength="128" /></el-form-item>
      <el-form-item label="说明"><el-input v-model="form.description" maxlength="2048" /></el-form-item>
      <el-form-item label="修改原因" required><el-input v-model="form.changeReason" maxlength="500" /></el-form-item>
      <el-form-item label="启用"><el-switch v-model="form.active" /></el-form-item>
      <el-table :data="form.structure.nodes" max-height="260">
        <el-table-column label="名称"><template #default="{ row }"><el-input v-model="row.name" maxlength="128" /></template></el-table-column>
        <el-table-column label="上级文件夹"><template #default="{ row }">
          <el-select v-model="row.parentKey" clearable @clear="row.parentKey = null">
            <el-option v-for="node in form.structure.nodes.filter(item => item.key !== row.key)" :key="node.key" :value="node.key" :label="node.name || '未命名'" />
          </el-select>
        </template></el-table-column>
        <el-table-column label="排序" width="150"><template #default="{ row }"><el-input-number v-model="row.sortOrder" :min="0" /></template></el-table-column>
        <el-table-column label="操作" width="80"><template #default="{ $index }"><el-button link type="danger" @click="remove($index)">移除</el-button></template></el-table-column>
      </el-table>
      <el-button class="mt-8px" @click="add">新增文件夹</el-button>
      <el-alert class="mt-8px" title="已有项目目录独立保留；模板修改用于后续新申请。" type="info" :closable="false" />
    </el-form>
    <template #footer>
      <el-button v-hasPermi="['dcc:project-code:update']" type="primary" :loading="loading" :disabled="!ready || !selectionValid" @click="save">保存</el-button>
      <el-button v-if="form.id && !selectedUsed" v-hasPermi="['dcc:project-code:update']" type="danger" :disabled="loading || !ready || !selectionValid" @click="removeTemplate">删除未使用模板</el-button>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessageBox } from 'element-plus'
import { getFolderTemplates, saveFolderTemplate, deleteFolderTemplate, type FolderTemplate, type FolderStructure } from '@/api/dcc/controlledFile/projectAttributes'
const emit = defineEmits<{ saved: [] }>()
const visible = ref(false), loading = ref(false), error = ref(''), selectedUsed = ref(false)
const templates = ref<FolderTemplate[]>([])
const ready = ref(false), selectionValid = ref(true)
let contextRevision = 0
const form = reactive<{ id?: number; name: string; description?: string; active: boolean; structure: FolderStructure; changeReason: string }>({ changeReason: '', name: '', active: true, structure: { nodes: [] } })
const reset = () => { contextRevision++; selectionValid.value = true; error.value = ''; form.changeReason = ''; form.id = undefined; form.name = ''; form.description = ''; form.active = true; form.structure = { nodes: [] }; selectedUsed.value = false }
const choose = (row?: FolderTemplate) => {
  if (!row) return
  contextRevision++; loading.value = false
  selectionValid.value = false
  try {
    const structure = JSON.parse(row.structureJson)
    if (!Array.isArray(structure.nodes)) throw new Error('模板目录结构缺失')
    Object.assign(form, { id: row.id, name: row.name, description: row.description, active: row.active, structure })
    form.changeReason = ''; selectedUsed.value = row.everUsed; error.value = ''; selectionValid.value = true
  } catch (cause) { error.value = cause instanceof Error ? cause.message : String(cause) }
}

const add = () => form.structure.nodes.push({ key: crypto.randomUUID(), parentKey: null, name: '', sortOrder: form.structure.nodes.length })
const remove = (index: number) => {
  const key = form.structure.nodes[index].key
  if (form.structure.nodes.some(node => node.parentKey === key)) { error.value = '请先移除或调整子文件夹'; return }
  form.structure.nodes.splice(index, 1)
}
const open = async () => {
  visible.value = true; loading.value = true; ready.value = false; reset(); error.value = ''; templates.value = []
  const revision = contextRevision
  try {
    const rows = await getFolderTemplates()
    if (revision !== contextRevision) return
    templates.value = rows; ready.value = true
  } catch (cause) {
    if (revision === contextRevision) error.value = cause instanceof Error ? cause.message : String(cause)
  } finally { if (revision === contextRevision) loading.value = false }
}
const save = async () => {
  if (!ready.value || !selectionValid.value) return
  const revision = contextRevision
  const payload = JSON.parse(JSON.stringify(form))
  loading.value = true; error.value = ''
  try {
    const savedId = await saveFolderTemplate(payload)
    emit('saved') // 已提交成功与后续列表查询分别处理
    if (revision !== contextRevision) return
    form.id = savedId
    try {
      const rows = await getFolderTemplates()
      if (revision !== contextRevision) return
      templates.value = rows
      selectedUsed.value = rows.find(row => row.id === savedId)?.everUsed ?? selectedUsed.value
    } catch (cause) {
      if (revision === contextRevision) error.value = '模板已保存，列表刷新失败：' + (cause instanceof Error ? cause.message : String(cause))
    }
  } catch (cause) {
    if (revision === contextRevision) error.value = cause instanceof Error ? cause.message : String(cause)
  } finally { if (revision === contextRevision) loading.value = false }
}
const removeTemplate = async () => {
  if (!ready.value || !selectionValid.value || !form.id) return
  const revision = contextRevision, id = form.id, reason = form.changeReason
  try { await ElMessageBox.confirm('删除未使用模板？', '二次确认', { type: 'warning' }) }
  catch (cause) {
    if (cause !== 'cancel' && cause !== 'close' && revision === contextRevision) error.value = cause instanceof Error ? cause.message : String(cause)
    return
  }
  if (revision !== contextRevision) return
  loading.value = true; error.value = ''
  try {
    await deleteFolderTemplate(id, reason)
    emit('saved')
    if (revision !== contextRevision) return
    reset()
    const refreshedRevision = contextRevision
    try {
      const rows = await getFolderTemplates()
      if (refreshedRevision === contextRevision) templates.value = rows
    } catch (cause) {
      if (refreshedRevision === contextRevision) error.value = '模板已删除，列表刷新失败：' + (cause instanceof Error ? cause.message : String(cause))
    } finally { if (refreshedRevision === contextRevision) loading.value = false }
  } catch (cause) {
    if (revision === contextRevision) { error.value = cause instanceof Error ? cause.message : String(cause); loading.value = false }
  }
}

defineExpose({ open })
</script>
