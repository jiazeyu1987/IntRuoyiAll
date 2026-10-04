<template>
  <section v-loading="loading">
    <el-alert v-if="state.error" :title="state.error" type="error" :closable="false" />
    <div v-if="state.projectId" class="mb-8px">
      本次申请属性
      <el-button v-if="!readonly" link type="primary" @click="restore">恢复项目默认值</el-button>
    </div>
    <ProjectAttributesFields v-if="state.actual" :model-value="state.actual" :readonly="readonly" @update:model-value="edit" />
    <el-alert v-else title="请先选择项目并读取默认属性" type="info" :closable="false" />
  </section>
</template>
<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { getProjectDefaults } from '@/api/dcc/controlledFile/projectAttributes'
import ProjectAttributesFields from './ProjectAttributesFields.vue'
import { createAttributeState, loadProjectDefaults, restoreDefaults, editActual, buildSnapshot, type ApplicationType, type AttributeSnapshot, type ProjectAttributes } from './state'
const props = defineProps<{ action: ApplicationType; savedSnapshot?: AttributeSnapshot; readonly?: boolean }>()
const emit = defineEmits<{ change: [snapshot: AttributeSnapshot]; 'restore-defaults': [snapshot: AttributeSnapshot]; 'project-change-cancelled': [projectId?: string] }>()
const state = reactive(createAttributeState(props.action))
const loading = ref(false)
let commandRevision = 0
const applySavedSnapshot = (value?: AttributeSnapshot) => {
  commandRevision++; loading.value = false
  const nextSequence = state.requestSequence + 1
  Object.assign(state, createAttributeState(props.action), { requestSequence: nextSequence })
  try { Object.assign(state, createAttributeState(props.action, value), { requestSequence: nextSequence }) }
  catch (cause) { state.error = cause instanceof Error ? cause.message : String(cause) }
}
applySavedSnapshot(props.savedSnapshot)
watch(() => [props.action, props.savedSnapshot] as const, ([, value]) => applySavedSnapshot(value))
watch(() => props.readonly, (readonly) => {
  if (readonly) {
    commandRevision++
    state.requestSequence++
    loading.value = false
  }
}, { flush: 'sync' })
const confirm = async (): Promise<boolean> => {
  try { await ElMessageBox.confirm('将替换本次申请已填写的属性，是否继续？', '确认替换', { type: 'warning' }); return true }
  catch (error) { if (error === 'cancel' || error === 'close') return false; throw error }
}
const load = (projectId: string) => getProjectDefaults(projectId, props.action)
const selectProject = async (projectId: string | number): Promise<boolean> => {
  if (props.readonly) throw new Error('历史属性不可修改')
  const revision = ++commandRevision
  loading.value = true
  try {
    const accepted = await loadProjectDefaults(state, String(projectId), load, confirm)
    if (revision !== commandRevision) return false
    if (accepted) emit('change', buildSnapshot(state))
    else emit('project-change-cancelled', state.projectId)
    return accepted
  } catch (error) {
    if (revision === commandRevision) {
      state.error = error instanceof Error ? error.message : String(error)
      emit('project-change-cancelled', state.projectId)
    }
    return false
  } finally { if (revision === commandRevision) loading.value = false }
}
const restore = async () => {
  if (props.readonly) return
  const revision = ++commandRevision
  loading.value = true
  try {
    if (await restoreDefaults(state, load, confirm) && revision === commandRevision) emit('restore-defaults', buildSnapshot(state))
  } catch (error) { if (revision === commandRevision) state.error = error instanceof Error ? error.message : String(error) }
  finally { if (revision === commandRevision) loading.value = false }
}
const edit = (value: ProjectAttributes) => {
  if (props.readonly) return
  editActual(state, value)
  // 条件输入尚不完整时保留编辑态，调用 getSnapshot 才严格阻止提交
  emit('change', JSON.parse(JSON.stringify({ projectId: state.projectId!, applicationType: props.action, defaultSource: state.defaultSource!, actual: value })))
}
defineExpose({ selectProject, getSnapshot: () => buildSnapshot(state) })
</script>
