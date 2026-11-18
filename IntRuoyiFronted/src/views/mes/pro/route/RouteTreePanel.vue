<template>
  <ContentWrap v-loading="loading" class="route-tree">
    <div class="route-tree__toolbar">
      <div><strong>工艺流程树</strong><p>分组统一维护共性，具体流程保留个性化配置。</p></div>
      <el-button @click="load">刷新</el-button>
      <el-button v-hasPermi="['mes:pro-route:create']" type="primary" @click="openGroup()">新增顶级分组</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div class="route-tree__layout">
      <aside>
        <el-input v-model="filter" placeholder="搜索分组或流程" clearable aria-label="搜索工艺流程树" />
        <el-tree ref="treeRef" :data="tree" node-key="id" :props="{ label: 'name' }"
          :filter-node-method="filterNode" highlight-current default-expand-all :expand-on-click-node="false"
          @node-click="selected = $event" empty-text="暂无流程，请先在列表中新增流程">
          <template #default="{ data }">
            <span class="route-tree__node" :data-tree-node="data.name">
              <Icon :icon="data.nodeType === 'GROUP' ? 'ep:folder' : 'ep:document'" />
              <span>{{ data.name }}</span>
              <el-tag v-if="data.different" size="small" type="warning">工序有差异</el-tag>
            </span>
          </template>
        </el-tree>
      </aside>
      <section v-if="selected">
        <div class="route-tree__heading">
          <h2>{{ selected.name }}</h2>
          <el-tag>{{ selected.nodeType === 'GROUP' ? '分组模板' : '工艺流程' }}</el-tag>
          <el-tag type="success">{{ selected.activeRouteVersionNo }}</el-tag>
          <el-tag v-if="selected.pendingStatus" type="warning">{{ statusLabel(selected.pendingStatus) }}</el-tag>
        </div>
        <p v-if="selected.nodeType === 'GROUP'">修改后预览全部下属流程的变更，一次审核，统一发布新版本。</p>
        <p v-else>单独修改只影响本流程；父级后续修改对应字段时，仍会覆盖该字段。</p>
        <el-alert v-if="selected.different" title="该节点与直接父节点的工序组成或顺序不同，保留已确认的个性化配置。"
          type="warning" :closable="false" />
        <div class="route-tree__actions">
          <el-button type="primary" v-hasPermi="['mes:pro-route:version-create']" @click="emit('edit', selected)">修改配置</el-button>
          <el-button @click="emit('view', selected.id)">查看配置</el-button>
          <el-button v-hasPermi="['mes:pro-route:version-query']" @click="emit('versions', selected)">版本与审核</el-button>
          <template v-if="selected.nodeType === 'GROUP'">
            <el-button v-hasPermi="['mes:pro-route:create']" @click="openGroup(selected)">新增下级分组</el-button>
            <el-button v-hasPermi="['mes:pro-route:create']" @click="openInherited">新增继承流程</el-button>
            <el-button v-hasPermi="['mes:pro-route:update']" @click="openMove(true)">添加已有流程</el-button>
          </template>
          <el-button v-hasPermi="['mes:pro-route:update']" @click="openMove(false)">移动归属</el-button>
        </div>
        <h3>直接下级</h3>
        <el-table :data="selected.children || []" @row-click="selected = $event" empty-text="暂无直接下级">
          <el-table-column prop="name" label="名称" />
          <el-table-column label="类型" width="110"><template #default="{ row }">{{ row.nodeType === 'GROUP' ? '分组' : '流程' }}</template></el-table-column>
          <el-table-column prop="activeRouteVersionNo" label="已发布版本" width="120" />
          <el-table-column label="工序结构" width="130"><template #default="{ row }">{{ row.different ? '有差异' : '与父级一致' }}</template></el-table-column>
        </el-table>
        <el-alert class="mt-16px" title="移动只改变归属，不改配置或历史批记录；新流程继承父节点已发布配置。"
          type="info" :closable="false" />
      </section>
      <el-empty v-else description="选择分组或具体工艺流程" />
    </div>
  </ContentWrap>
  <Dialog v-model="groupVisible" title="新增工艺分组" width="540px">
    <el-form label-width="110px">
      <el-form-item label="分组名称" required><el-input v-model="group.name" aria-label="分组名称" /></el-form-item>
      <el-form-item label="直接父节点">{{ group.parentId ? groupParentName : '顶级分组' }}</el-form-item>
      <el-form-item v-if="!group.parentId" label="初始工艺模板" required>
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
import { ElMessageBox } from 'element-plus'
import { RouteTreeApi, type RouteTreeNode } from '@/api/mes/pro/route/tree'
import type { MesRouteId, ProRouteVO } from '@/api/mes/pro/route'
import RouteProjectSelect from './RouteProjectSelect.vue'
import MdWorkshopSelect from '@/views/mes/md/workstation/components/MdWorkshopSelect.vue'
import { isRouteConfirmCancel, resolveRouteOperationErrorMessage } from './routeError'
const emit = defineEmits<{ edit: [row: ProRouteVO]; view: [id?: number]; versions: [row: ProRouteVO]; changed: [] }>()
const message = useMessage()
const rows = ref<RouteTreeNode[]>([]), selected = ref<RouteTreeNode>(), loading = ref(false), saving = ref(false), error = ref('')
const filter = ref(''), treeRef = ref()
const tree = computed(() => {
  const nodes = new Map(rows.value.map(row => [String(row.id), { ...row, children: [] as RouteTreeNode[] }]))
  const roots: RouteTreeNode[] = []
  for (const node of nodes.values()) {
    if (node.parentId != null && nodes.has(String(node.parentId))) nodes.get(String(node.parentId))!.children.push(node)
    else roots.push(node)
  }
  return roots
})
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
const filterNode = (value: string, data: RouteTreeNode) => !value || data.name.includes(value) || data.code.includes(value)
watch(filter, value => treeRef.value?.filter(value))
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
  if (!group.name.trim() || (!group.parentId && !group.sourceRouteId)) throw new Error('请填写名称并选择初始模板')
  const id = await RouteTreeApi.group(group); groupVisible.value = false
  await load(); selected.value = rows.value.find(row => String(row.id) === String(id)); message.success('分组已创建')
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
.route-tree__toolbar > div { flex: 1; }
.route-tree p { color: var(--el-text-color-secondary); }
.route-tree__layout { display: grid; grid-template-columns: minmax(260px, 32%) 1fr; min-height: 480px; margin-top: 20px; }
.route-tree aside { border-right: 1px solid var(--el-border-color-lighter); padding-right: 20px; overflow: auto; max-height: 70vh; }
.route-tree section { padding-left: 24px; min-width: 0; }
.route-tree__node { display: flex; align-items: center; gap: 8px; }
.route-tree__heading h2 { font-size: 20px; margin: 0; }
.route-tree__actions { margin: 24px 0; }
:deep(.el-tree) { margin-top: 12px; }
:deep(.el-tree-node__content) { min-height: 36px; }
@media (max-width: 800px) { .route-tree__layout { grid-template-columns: 1fr; } .route-tree section { padding: 20px 0 0; } }
</style>
