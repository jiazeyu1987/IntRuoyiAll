<template>
  <ContentWrap v-loading="loading" class="route-tree">
    <div class="route-tree__toolbar">
      <el-button @click="load">刷新</el-button>
      <el-button v-hasPermi="['mes:pro-route:create']" type="primary" @click="openGroup()">新增顶级分组</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div class="route-tree__layout">
      <aside>
        <div class="route-tree__search">
          <el-select v-model="searchType" aria-label="搜索类型" class="route-tree__search-type">
            <el-option label="综合搜索" value="keyword" />
            <el-option label="生产订单" value="order" />
          </el-select>
          <el-input v-model="filter" :placeholder="searchType === 'order' ? '输入生产订单号' : '搜索分组、流程或产品编码'"
            clearable :aria-label="searchType === 'order' ? '搜索生产订单' : '搜索分组、流程或产品编码'" />
        </div>
        <el-alert v-if="orderSearchError" :title="orderSearchError" type="error" :closable="false" />
        <p v-else-if="searchType === 'order' && filter.trim()" class="route-tree__search-hint">
          {{ orderSearchLoading ? '正在查询订单对应产品…' : orderSearchResult?.orderCount
            ? `匹配 ${orderSearchResult.orderCount} 个生产订单，按对应产品编码筛选流程`
            : '未找到匹配的生产订单' }}
        </p>
        <el-tree ref="treeRef" :data="tree" node-key="id" :props="{ label: 'name' }"
          :filter-node-method="filterNode" highlight-current default-expand-all :expand-on-click-node="false"
          @node-click="selected = $event" :empty-text="searchEmptyText">
          <template #default="{ data }">
            <span class="route-tree__node" :data-tree-node="data.name">
              <Icon :icon="data.nodeType === 'GROUP' ? 'ep:folder' : 'ep:document'" />
              <span>{{ nodeLabel(data) }}</span>
              <el-tag v-if="data.different" size="small" type="warning">工序有差异</el-tag>
            </span>
          </template>
        </el-tree>
      </aside>
      <section v-if="selected">
        <div class="route-tree__heading">
          <h2>{{ nodeLabel(selected) }}</h2>
          <el-tag>{{ selected.nodeType === 'GROUP' ? (selected.activeRouteVersionId ? '分组模板' : '目录') : '工艺流程' }}</el-tag>
          <el-tag v-if="selected.activeRouteVersionNo" type="success">{{ selected.activeRouteVersionNo }}</el-tag>
          <el-tag v-if="selected.pendingStatus" type="warning">{{ statusLabel(selected.pendingStatus) }}</el-tag>
        </div>
        <div class="route-tree__actions">
          <template v-if="selected.activeRouteVersionId">
          <el-button type="primary" v-hasPermi="['mes:pro-route:version-create']" @click="emit('edit', selected)">修改配置</el-button>
          <el-button @click="emit('view', selected.id)">查看配置</el-button>
          <el-button v-hasPermi="['mes:pro-route:version-query']" @click="emit('versions', selected)">版本与审核</el-button>
          </template>
          <template v-if="selected.nodeType === 'GROUP'">
            <el-button v-hasPermi="['mes:pro-route:create']" @click="openGroup(selected)">新增下级分组</el-button>
            <el-button v-if="selected.activeRouteVersionId" v-hasPermi="['mes:pro-route:create']" @click="openInherited">新增继承流程</el-button>
            <el-button v-hasPermi="['mes:pro-route:update']" @click="openMove(true)">添加已有流程</el-button>
            <el-button v-hasPermi="['mes:pro-route:update']" @click="renameGroup">重命名目录</el-button>
            <el-button v-if="!selected.children?.length" v-hasPermi="['mes:pro-route:delete']" type="danger" @click="deleteGroup">删除空目录</el-button>
          </template>
          <el-button v-hasPermi="['mes:pro-route:update']" @click="openMove(false)">移动归属</el-button>
          <el-button v-if="selected.different" @click="openDifference">差异分析</el-button>
        </div>
        <div v-if="isFlowLeaf" v-loading="bindingsLoading" class="route-tree__bindings" aria-label="流程绑定信息">
          <h3>绑定信息</h3>
          <el-alert v-if="bindingsError" :title="bindingsError" type="error" :closable="false" />
          <el-descriptions v-else-if="bindings" :column="1" border>
            <el-descriptions-item label="项目代码">{{ bindings.dccProjectCode || '未绑定项目' }}</el-descriptions-item>
            <el-descriptions-item label="产品编码"><span class="route-tree__product-codes"><template v-if="bindings.productCodes"><ProductCodeLink v-for="code in bindings.productCodes.split('、')" :key="code" :code="code" /></template><span v-else>未绑定产品</span></span></el-descriptions-item>
          </el-descriptions>
        </div>
        <template v-else>
          <h3>直接下级</h3>
          <el-table :data="selected.children || []" @row-click="selected = $event" empty-text="暂无直接下级">
            <el-table-column label="名称"><template #default="{ row }">{{ nodeLabel(row) }}</template></el-table-column>
            <el-table-column label="类型" width="110"><template #default="{ row }">{{ row.nodeType === 'GROUP' ? '分组' : '流程' }}</template></el-table-column>
            <el-table-column prop="activeRouteVersionNo" label="已发布版本" width="120" />
            <el-table-column label="工序结构" width="130"><template #default="{ row }">{{ row.different ? '有差异' : '与父级一致' }}</template></el-table-column>
          </el-table>
        </template>
      </section>
      <el-empty v-else description="选择分组或具体工艺流程" />
    </div>
  </ContentWrap>
  <RouteTreeDifference v-if="comparison" v-model="differenceVisible" :node="comparison.node" :parent="comparison.parent" />
  <Dialog v-model="groupVisible" title="新增工艺分组" width="540px">
    <el-form label-width="110px">
      <el-form-item label="分组名称" required><el-input v-model="group.name" aria-label="分组名称" /></el-form-item>
      <el-form-item label="直接父节点">{{ group.parentId ? groupParentName : '顶级分组' }}</el-form-item>
      <el-form-item v-if="groupNeedsTemplate" label="初始工艺模板" required>
        <el-select v-model="group.sourceRouteId" filterable placeholder="选择已有流程作为初始模板" aria-label="初始工艺模板">
          <el-option v-for="row in rows.filter(item => item.nodeType === 'FLOW')" :key="row.id" :value="row.id!" :label="row.name" />
        </el-select>
      </el-form-item>
      <el-alert title="只复制初始工艺配置，不改变来源流程。下级分组自动继承父分组。" type="info" :closable="false" />
    </el-form>
    <template #footer><el-button @click="groupVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveGroup">创建分组</el-button></template>
  </Dialog>
  <Dialog v-model="inheritVisible" title="新增继承流程" width="560px">
    <el-form label-width="110px">
      <el-form-item label="流程名称" required><el-input v-model="inherit.name" aria-label="继承流程名称" /></el-form-item>
      <el-form-item label="项目代码" required><RouteProjectSelect v-model="inherit.dccProjectCodeId" /></el-form-item>
      <el-form-item label="所属车间" required><MdWorkshopSelect v-model="inherit.workshopId" /></el-form-item>
      <el-alert :title="'继承分组「' + selected?.name + '」的已发布工序、参数及表单配置。'" type="info" :closable="false" />
    </el-form>
    <template #footer><el-button @click="inheritVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveInherited">创建并继承</el-button></template>
  </Dialog>
  <Dialog v-model="moveVisible" :title="addingExisting ? '添加已有流程' : '移动工艺归属'" width="600px">
    <el-form label-width="110px">
      <el-form-item label="移动节点">
        <el-select v-if="addingExisting" v-model="move.id" filterable aria-label="待添加流程">
          <el-option v-for="row in rows.filter(item => item.nodeType === 'FLOW')" :key="row.id" :value="row.id!" :label="row.name" />
        </el-select>
        <span v-else>{{ selected?.name }}</span>
      </el-form-item>
      <el-form-item label="目标父节点">
        <el-select v-model="move.parentId" filterable clearable placeholder="留空移至根目录" aria-label="目标父节点">
          <el-option v-for="row in rows.filter(item => item.nodeType === 'GROUP' && item.id !== move.id)" :key="row.id" :value="row.id!" :label="row.name" />
        </el-select>
      </el-form-item>
      <el-alert title="配置保持原值。工序与目标分组不同时，将列明差异并再次确认。" type="info" :closable="false" />
    </el-form>
    <template #footer><el-button @click="moveVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveMove">确认归属</el-button></template>
  </Dialog>
</template>

<script setup lang="ts">
import ProductCodeLink from '@/components/ProductCodeLink.vue'
import { ElMessageBox } from 'element-plus'
import { RouteTreeApi, type RouteTreeNode } from '@/api/mes/pro/route/tree'
import { ProRouteApi, type MesRouteId, type ProRouteVO } from '@/api/mes/pro/route'
import RouteProjectSelect from './RouteProjectSelect.vue'
import RouteTreeDifference from './RouteTreeDifference.vue'
import MdWorkshopSelect from '@/views/mes/md/workstation/components/MdWorkshopSelect.vue'
import { isRouteConfirmCancel, resolveRouteOperationErrorMessage } from './routeError'
import { findOrderProductCodes } from './routeTreeOrderSearch'
const emit = defineEmits<{ edit: [row: ProRouteVO]; view: [id?: number]; versions: [row: ProRouteVO]; changed: [] }>()
const message = useMessage()
const rows = ref<RouteTreeNode[]>([]), selected = ref<RouteTreeNode>(), loading = ref(false), saving = ref(false), error = ref('')
const filter = ref(''), treeRef = ref()
const searchType = ref<'keyword' | 'order'>('keyword')
const orderSearchResult = ref<{ productCodes: string[]; orderCount: number }>()
const orderSearchLoading = ref(false), orderSearchError = ref('')
watch([filter, searchType, rows], ([value, type], _previous, onCleanup) => {
  let cancelled = false
  let timer: ReturnType<typeof setTimeout> | undefined
  onCleanup(() => { cancelled = true; clearTimeout(timer) })
  orderSearchResult.value = undefined
  orderSearchError.value = ''
  orderSearchLoading.value = type === 'order' && Boolean(value.trim())
  if (!orderSearchLoading.value) return
  timer = setTimeout(async () => {
    try {
      const result = await findOrderProductCodes(value, () => !cancelled)
      if (!cancelled) orderSearchResult.value = result
    } catch (failure) {
      if (!cancelled) orderSearchError.value = resolveRouteOperationErrorMessage(failure, '查询生产订单失败，请重试')
    } finally {
      if (!cancelled) orderSearchLoading.value = false
    }
  }, 300)
}, { flush: 'sync' })
const searchEmptyText = computed(() => {
  if (!filter.value.trim()) return '暂无流程，请先在列表中新增流程'
  if (searchType.value !== 'order') return '未找到匹配的分组、流程或产品编码'
  if (orderSearchLoading.value) return '正在查询订单对应产品…'
  if (orderSearchError.value) return '生产订单查询失败'
  return orderSearchResult.value?.orderCount ? '未找到绑定订单对应产品的工艺流程' : '未找到匹配的生产订单'
})
const differenceVisible = ref(false)
const comparison = shallowRef<{ node: RouteTreeNode; parent: RouteTreeNode }>()
watch(selected, () => { differenceVisible.value = false })
const openDifference = () => {
  const node = selected.value
  const parent = rows.value.find(row => String(row.id) === String(node?.parentId))
  if (!node?.different || !parent) {
    message.error('未找到可比较的直接父节点，请刷新后重试')
    return
  }
  comparison.value = { node, parent }
  differenceVisible.value = true
}
const isFlowLeaf = computed(() => selected.value?.nodeType === 'FLOW' && !selected.value.children?.length)
const bindings = ref<ProRouteVO>(), bindingsLoading = ref(false), bindingsError = ref('')
watch(selected, async (node, _previous, onCleanup) => {
  let cancelled = false
  onCleanup(() => { cancelled = true })
  bindings.value = undefined
  bindingsError.value = ''
  bindingsLoading.value = false
  if (!node || !isFlowLeaf.value || node.id == null) return
  bindingsLoading.value = true
  try {
    const detail: ProRouteVO = await ProRouteApi.getRoute(node.id)
    if (!cancelled) bindings.value = detail
  } catch (failure) {
    if (!cancelled) bindingsError.value = resolveRouteOperationErrorMessage(failure, '加载流程绑定信息失败，请刷新后重试')
  } finally {
    if (!cancelled) bindingsLoading.value = false
  }
})
const tree = computed(() => {
  const nodes = new Map(rows.value.map(row => [String(row.id), { ...row, children: [] as RouteTreeNode[] }]))
  const roots: RouteTreeNode[] = []
  for (const node of nodes.values()) {
    if (node.parentId != null && nodes.has(String(node.parentId))) nodes.get(String(node.parentId))!.children.push(node)
    else roots.push(node)
  }
  const countFlows = (node: RouteTreeNode): number => {
    const childCount = (node.children || []).reduce((total, child) => total + countFlows(child), 0)
    node.flowCount = childCount
    return childCount + (node.nodeType === 'FLOW' ? 1 : 0)
  }
  roots.forEach(countFlows)
  return roots
})
const nodeLabel = (node: RouteTreeNode) => node.nodeType === 'GROUP' ? `${node.name}(${node.flowCount ?? 0})` : node.name
watch(tree, roots => {
  const find = (nodes: RouteTreeNode[]): RouteTreeNode | undefined => {
    for (const node of nodes) {
      if (String(node.id) === String(selected.value?.id)) return node
      const child = find(node.children || [])
      if (child) return child
    }
  }
  selected.value = find(roots)
})
const filterNode = (value: string, data: RouteTreeNode) => {
  const keyword = value.trim().toLowerCase()
  if (keyword && searchType.value === 'order') {
    if (orderSearchLoading.value || orderSearchError.value || data.nodeType !== 'FLOW') return false
    return Boolean(data.productCodes?.split('、').some(code => orderSearchResult.value?.productCodes.includes(code.trim().toLowerCase())))
  }
  return !keyword || [data.name, data.code, data.nodeType === 'FLOW' ? data.productCodes : undefined]
    .some(text => text?.toLowerCase().includes(keyword))
}
watch([filter, tree, searchType, orderSearchResult, orderSearchLoading, orderSearchError],
  () => treeRef.value?.filter(filter.value), { flush: 'post' })
const statusLabels: Record<string, string> = { DRAFT: '有草稿', PENDING_APPROVAL: '审核中', READY_TO_PUBLISH: '待发布' }
const statusLabel = (value: string) => statusLabels[value] || value
const load = async () => {
  loading.value = true; error.value = ''
  try { rows.value = await RouteTreeApi.list() }
  catch (failure) { error.value = resolveRouteOperationErrorMessage(failure, '加载工艺树失败') }
  finally { loading.value = false }
}
const groupVisible = ref(false), inheritVisible = ref(false), moveVisible = ref(false), addingExisting = ref(false)
const groupParentName = ref('')
const group = reactive<{ name: string; parentId?: MesRouteId; sourceRouteId?: MesRouteId }>({ name: '' })
const groupNeedsTemplate = computed(() => !group.parentId || !rows.value.find(row => String(row.id) === String(group.parentId))?.activeRouteVersionId)
const inherit = reactive<{ name: string; dccProjectCodeId?: number; workshopId?: number }>({ name: '' })
const move = reactive<{ id?: MesRouteId; parentId?: MesRouteId }>({})
const openGroup = (parent?: RouteTreeNode) => {
  Object.assign(group, { name: '', parentId: parent?.id, sourceRouteId: undefined })
  groupParentName.value = parent?.name || ''; groupVisible.value = true
}
const run = async (action: () => Promise<void>) => {
  saving.value = true
  try { await action(); await load(); emit('changed') }
  catch (failure) { if (!isRouteConfirmCancel(failure)) message.error(resolveRouteOperationErrorMessage(failure, '操作失败')) }
  finally { saving.value = false }
}
const saveGroup = () => run(async () => {
  if (!group.name.trim() || (groupNeedsTemplate.value && !group.sourceRouteId)) throw new Error('请填写名称并选择初始模板')
  const id = await RouteTreeApi.group(group); groupVisible.value = false
  await load(); selected.value = rows.value.find(row => String(row.id) === String(id)); message.success('分组已创建')
})
const renameGroup = () => run(async () => {
  const node = selected.value
  if (!node?.id || node.nodeType !== 'GROUP') throw new Error('请选择目录')
  const { value } = await ElMessageBox.prompt('请输入目录名称', '重命名目录', {
    inputValue: node.name, inputValidator: value => Boolean(value.trim()) || '请填写目录名称'
  })
  await RouteTreeApi.renameGroup(node.id, value.trim())
  message.success('目录已重命名')
})
const deleteGroup = () => run(async () => {
  const node = selected.value
  if (!node?.id || node.nodeType !== 'GROUP' || node.children?.length) throw new Error('仅可删除没有下级节点的空目录')
  await ElMessageBox.confirm(`确认删除空目录「${node.name}」？`, '删除空目录', { type: 'warning' })
  await RouteTreeApi.deleteGroup(node.id)
  message.success('空目录已删除')
})
const openInherited = () => {
  Object.assign(inherit, { name: '', dccProjectCodeId: undefined, workshopId: undefined }); inheritVisible.value = true
}
const saveInherited = () => run(async () => {
  if (!selected.value?.id || !inherit.name.trim() || !inherit.dccProjectCodeId || !inherit.workshopId) throw new Error('请完整填写流程名称、项目代码及车间')
  await RouteTreeApi.inherit(selected.value.id, { ...inherit, processIds: selected.value.processIds })
  inheritVisible.value = false; message.success('流程已创建并继承父节点配置')
})
const openMove = (adding: boolean) => {
  addingExisting.value = adding
  Object.assign(move, { id: adding ? undefined : selected.value?.id, parentId: adding ? selected.value?.id : selected.value?.parentId })
  moveVisible.value = true
}
const saveMove = () => run(async () => {
  if (!move.id) throw new Error('请选择待添加流程')
  const preview = await RouteTreeApi.movePreview(move.id, move.parentId)
  if (preview.different) await ElMessageBox.confirm(
    '工序与目标分组不同。当前：' + preview.currentProcesses.join(' → ') + '；目标：' + preview.expectedProcesses.join(' → ') + '。确认保留差异并添加？',
    '工序一致性确认', { type: 'warning', confirmButtonText: '保留差异并添加', cancelButtonText: '取消' })
  await RouteTreeApi.move({ id: move.id, parentId: move.parentId, fingerprint: preview.fingerprint, confirmDifference: preview.different })
  moveVisible.value = false; message.success('归属已更新，流程配置保持原值')
})
onMounted(load)
onActivated(load)
defineExpose({ load })
</script>

<style scoped>
.route-tree__toolbar, .route-tree__heading, .route-tree__actions { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.route-tree__toolbar { justify-content: flex-end; }
.route-tree__search { display: flex; gap: 8px; }
.route-tree__search-type { width: 120px; flex-shrink: 0; }
.route-tree__search-hint { color: var(--el-text-color-secondary); font-size: 12px; margin: 8px 0; }
.route-tree__layout { display: grid; grid-template-columns: minmax(260px, 32%) 1fr; min-height: 480px; margin-top: 20px; }
.route-tree aside { border-right: 1px solid var(--el-border-color-lighter); padding-right: 20px; overflow: auto; max-height: 70vh; }
.route-tree section { padding-left: 24px; min-width: 0; }
.route-tree__node { display: flex; align-items: center; gap: 8px; }
.route-tree__heading h2 { font-size: 20px; margin: 0; }
.route-tree__actions { margin: 24px 0; }
.route-tree__bindings { min-height: 150px; }
.route-tree__product-codes { display: inline-flex; flex-wrap: wrap; gap: 4px 12px; overflow-wrap: anywhere; }
:deep(.el-tree) { margin-top: 12px; }
:deep(.el-tree-node__content) { min-height: 36px; }
@media (max-width: 800px) { .route-tree__layout { grid-template-columns: 1fr; } .route-tree section { padding: 20px 0 0; } }
</style>
