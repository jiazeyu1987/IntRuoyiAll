<template>
  <Dialog v-model="visible" title="工序差异分析" width="1000px">
    <div v-loading="loading" class="route-tree-difference">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="直接父节点">{{ parent.name }}（{{ parent.activeRouteVersionNo }}）</el-descriptions-item>
        <el-descriptions-item label="当前节点">{{ node.name }}（{{ node.activeRouteVersionNo }}）</el-descriptions-item>
      </el-descriptions>
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-16px" />
      <template v-else-if="!loading">
        <h3>工序差异</h3>
        <el-table :data="differences" max-height="320" empty-text="当前已发布版本的工序组成与顺序一致">
          <el-table-column prop="name" label="工序名称" min-width="160" />
          <el-table-column prop="code" label="工序编码" min-width="140" />
          <el-table-column prop="type" label="差异类型" min-width="130" />
          <el-table-column prop="parentPosition" label="父节点顺序" width="110" />
          <el-table-column prop="currentPosition" label="当前节点顺序" width="120" />
        </el-table>
        <div class="route-tree-difference__sequences">
          <div><h3>父节点工序顺序</h3><ol><li v-for="process in parentProcesses" :key="String(process.processId)">{{ process.processName }}（<ProcessCodeLink :code="process.processCode" />）</li></ol></div>
          <div><h3>当前节点工序顺序</h3><ol><li v-for="process in currentProcesses" :key="String(process.processId)">{{ process.processName }}（<ProcessCodeLink :code="process.processCode" />）</li></ol></div>
        </div>
      </template>
    </div>
    <template #footer>
      <el-button v-if="error" @click="load">重新加载</el-button>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import ProcessCodeLink from '@/components/ProcessCodeLink.vue'
import { ProRouteApi, type RouteFlowNodeVO } from '@/api/mes/pro/route'
import type { RouteTreeNode } from '@/api/mes/pro/route/tree'
import { resolveRouteOperationErrorMessage } from './routeError'

const props = defineProps<{ modelValue: boolean; node: RouteTreeNode; parent: RouteTreeNode }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const visible = computed({ get: () => props.modelValue, set: value => emit('update:modelValue', value) })
const loading = ref(false), error = ref('')
const parentProcesses = ref<RouteFlowNodeVO[]>([]), currentProcesses = ref<RouteFlowNodeVO[]>([])
let request = 0
const load = async () => {
  const currentRequest = ++request
  loading.value = true
  error.value = ''
  parentProcesses.value = []
  currentProcesses.value = []
  try {
    const { node, parent } = props
    if (node.id == null || parent.id == null || node.activeRouteVersionId == null || parent.activeRouteVersionId == null) {
      throw new Error('节点缺少已发布版本，无法分析工序差异')
    }
    const [parentGraph, currentGraph] = await Promise.all([
      ProRouteApi.getRouteProcessFlowGraph(parent.id, parent.activeRouteVersionId),
      ProRouteApi.getRouteProcessFlowGraph(node.id, node.activeRouteVersionId)
    ])
    if (currentRequest !== request) return
    parentProcesses.value = [...parentGraph.nodes].sort((a, b) => a.sort! - b.sort!)
    currentProcesses.value = [...currentGraph.nodes].sort((a, b) => a.sort! - b.sort!)
  } catch (failure) {
    if (currentRequest === request) error.value = resolveRouteOperationErrorMessage(failure, '加载工序差异失败')
  } finally {
    if (currentRequest === request) loading.value = false
  }
}
watch(() => props.modelValue, value => {
  if (value) load()
  else request++
}, { immediate: true })
onBeforeUnmount(() => { request++ })

const differences = computed(() => {
  const parent = new Map(parentProcesses.value.map((process, index) => [String(process.processId), { process, position: index + 1 }]))
  const current = new Map(currentProcesses.value.map((process, index) => [String(process.processId), { process, position: index + 1 }]))
  // 只比较共同工序的相对顺序，单纯增加或删除工序不会被重复标为顺序变化。
  const parentCommon = [...parent.keys()].filter(id => current.has(id))
  const currentCommon = [...current.keys()].filter(id => parent.has(id))
  return [...new Set([...parent.keys(), ...current.keys()])].flatMap(id => {
    const before = parent.get(id), after = current.get(id)
    const type = !before ? '仅当前节点有' : !after ? '仅父节点有'
      : parentCommon.indexOf(id) !== currentCommon.indexOf(id) ? '顺序不同' : ''
    if (!type) return []
    const process = (after || before)!.process
    return [{ name: process.processName, code: process.processCode, type,
      parentPosition: before?.position ?? '无', currentPosition: after?.position ?? '无' }]
  })
})
</script>

<style scoped>
.route-tree-difference { min-height: 180px; max-height: 65vh; overflow: auto; }
.route-tree-difference__sequences { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
.route-tree-difference li { padding: 4px 0; overflow-wrap: anywhere; }
@media (max-width: 700px) { .route-tree-difference__sequences { grid-template-columns: 1fr; } }
</style>
