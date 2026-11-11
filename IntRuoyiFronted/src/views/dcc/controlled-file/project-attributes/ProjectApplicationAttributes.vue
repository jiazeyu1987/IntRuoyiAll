<template>
  <section v-loading="loading" class="application-attributes">
    <el-alert v-if="state.error || state.requestError" :title="state.error || state.requestError" type="error" :closable="false" />
    <div v-if="state.projectId" class="application-attributes__header">
      <span>本次申请属性</span>
      <div v-if="!readonly">
        <el-button v-if="collapsible" link type="primary" :aria-expanded="expanded" @click="expanded = !expanded">{{ expanded ? '收起' : '修改' }}</el-button>
        <el-button link type="primary" @click="restore">恢复项目默认值</el-button>
      </div>
    </div>
    <p v-if="state.actual && collapsible" class="application-attributes__summary">{{ summary }}</p>
    <ProjectAttributesFields v-if="state.actual && (!collapsible || expanded)" :model-value="state.actual" :readonly="readonly" @update:model-value="edit" />
    <el-alert v-if="!state.actual" title="请先选择项目并读取默认属性" type="info" :closable="false" />
  </section>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { getProjectDefaults } from '@/api/dcc/controlledFile/projectAttributes'
import ProjectAttributesFields from './ProjectAttributesFields.vue'
import { createAttributeState, loadProjectDefaults, restoreDefaults, editActual, buildSnapshot, type ApplicationType, type AttributeSnapshot, type ProjectAttributes } from './state'
const props = defineProps<{ action: ApplicationType; savedSnapshot?: AttributeSnapshot; readonly?: boolean; collapsible?: boolean }>()
const emit = defineEmits<{ change: [snapshot: AttributeSnapshot]; 'restore-defaults': [snapshot: AttributeSnapshot]; 'project-change-cancelled': [projectId?: string] }>()
const state = reactive(createAttributeState(props.action))
const loading = ref(false)
const expanded = ref(false)
const summary = computed(() => {
  const actual = state.actual
  if (!actual) return ''
  const markets: Record<string, string> = { NMPA: 'NMPA 国内', CE: 'CE 欧盟', FDA: 'FDA', MADSAP: 'MDSAP', OTHER: '其他', NA: '不适用' }
  const choices: Record<string, string> = { Y: '是', N: '否', NA: '不适用' }
  return `目标市场：${actual.targetMarkets.map(value => markets[value]).join('、') || '未选择'}${actual.otherMarket ? `（${actual.otherMarket}）` : ''}｜注册人：${choices[actual.licenseHolder ?? ''] ?? '未选择'}｜生产方：${choices[actual.actualManufacturer ?? ''] ?? '未选择'}｜文件转移：${choices[actual.documentTransfer ?? ''] ?? '未选择'}${actual.transferTo ? `（${actual.transferTo}）` : ''}`
})
let commandRevision = 0
const applySavedSnapshot = (value?: AttributeSnapshot) => {
  commandRevision++; loading.value = false
  expanded.value = false
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
const selectProject = async (projectId: string | number, discardConfirmed = false): Promise<boolean> => {
  if (props.readonly) throw new Error('历史属性不可修改')
  const revision = ++commandRevision
  loading.value = true
  try {
    const accepted = await loadProjectDefaults(state, String(projectId), load, discardConfirmed ? async () => true : confirm)
    if (revision !== commandRevision) return false
    if (accepted) { expanded.value = false; emit('change', buildSnapshot(state)) }
    else emit('project-change-cancelled', state.projectId)
    return accepted
  } catch (error) {
    if (revision === commandRevision) {
      if (!state.error && !state.requestError) state.error = error instanceof Error ? error.message : String(error)
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
  } catch (error) {
    // The state operation retains the error for display and decides whether the original binding is still valid.
    if (revision === commandRevision && !state.error && !state.requestError)
      state.error = error instanceof Error ? error.message : String(error)
  }
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
<style scoped>
.application-attributes { width: 100%; min-width: 0; }
.application-attributes__header { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 8px; }
.application-attributes__summary { margin: 0 0 8px; line-height: 1.7; overflow-wrap: anywhere; }
</style>
