<template>
  <ContentWrap v-loading="loading || deleting" class="route-tree">
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div class="route-tree__layout">
      <aside class="route-tree__sidebar">
        <div class="route-tree__directory">
        <div class="route-tree__search">
          <el-select v-model="searchType" aria-label="搜索类型" class="route-tree__search-type">
            <el-option label="综合搜索" value="keyword" />
            <el-option label="生产订单" value="order" />
          </el-select>
          <el-input v-model="filter" :placeholder="searchType === 'order' ? '输入订单号' : '搜索目录或流程'"
            clearable :aria-label="searchType === 'order' ? '搜索生产订单' : '搜索目录、流程或产品编码'" />
          <el-button class="route-tree__expand-toggle" :title="collapseOnNextClick ? '全部折叠' : '全部展开'"
            :aria-label="collapseOnNextClick ? '全部折叠' : '全部展开'" :disabled="loading || !rows.length"
            @click="toggleAllExpansion">
            <Icon :icon="collapseOnNextClick ? 'ep:fold' : 'ep:expand'" />
          </el-button>
        </div>
        <el-alert v-if="orderSearchError" :title="orderSearchError" type="error" :closable="false" />
        <p v-else-if="searchType === 'order' && filter.trim()" class="route-tree__search-hint">
          {{ orderSearchLoading ? '正在查询订单对应产品…' : orderSearchResult?.orderCount
            ? `匹配 ${orderSearchResult.orderCount} 个生产订单，按对应产品编码筛选流程`
            : '未找到匹配的生产订单' }}
        </p>
        <el-scrollbar class="route-tree__sidebar-body">
          <el-tree :key="expansionCacheKey" ref="treeRef" :data="tree" node-key="id" :props="{ label: 'name' }"
            :current-node-key="selected?.id"
            :default-expanded-keys="expandedNodeKeys"
            :filter-node-method="filterNode" highlight-current :default-expand-all="false" :auto-expand-parent="false" :expand-on-click-node="false"
            @node-expand="saveNodeExpansion($event, true)" @node-collapse="saveNodeExpansion($event, false)"
            @node-click="selected = $event" :empty-text="searchEmptyText">
            <template #default="{ data }">
              <span class="route-tree__node" :data-tree-node="data.name">
                <Icon v-if="data.nodeType === 'GROUP'" icon="ep:folder-opened" />
                <Icon v-else icon="ep:document"
                  :color="data.different ? 'var(--el-color-warning)' : 'var(--el-color-success)'"
                  :title="data.different ? '工序有差异' : '工序无差异'"
                  role="img" :aria-label="data.different ? '工序有差异' : '工序无差异'" />
                <span :title="nodeLabel(data)">{{ nodeLabel(data) }}</span>
              </span>
            </template>
          </el-tree>
        </el-scrollbar>
        </div>
        <div class="route-tree__sidebar-actions" aria-label="目录操作">
          <el-button v-hasPermi="['mes:pro-route:create']" plain type="primary" @click="openGroup()">新增顶级目录</el-button>
          <template v-if="selected?.nodeType === 'GROUP'">
            <el-button v-hasPermi="['mes:pro-route:create']" plain type="primary" @click="openGroup(selected)">新增下级目录</el-button>
            <el-button v-hasPermi="['mes:pro-route:update']" plain type="primary" @click="renameGroup">重命名目录</el-button>
            <el-button v-hasPermi="['mes:pro-route:update']" plain @click="openMove()">移动</el-button>
            <el-button v-hasPermi="['mes:pro-route:delete']" plain type="danger"
              :disabled="loading || saving || publishing || productBusy || quickConfigBusy" :loading="deleting"
              :title="selected.children?.length ? '请先移出或删除下级目录及流程' : '删除当前目录'"
              @click="deleteSelectedNode">删除目录</el-button>
          </template>
          <el-button v-if="selected?.nodeType === 'FLOW'" v-hasPermi="['mes:pro-route:delete']" plain type="danger"
            :disabled="loading || saving || publishing || productBusy || quickConfigBusy" :loading="deleting"
            @click="deleteSelectedNode">删除工艺流程</el-button>
          <el-button v-if="selected && selected.nodeType !== 'GROUP'" v-hasPermi="['mes:pro-route:update']" plain @click="openMove()"><Icon icon="ep:rank" />移动</el-button>
          <el-button v-if="selected?.different" plain @click="openDifference">差异分析</el-button>
        </div>
      </aside>
      <section v-if="selected">
        <div class="route-tree__detail-header">
        <div class="route-tree__heading">
          <h2>{{ nodeLabel(selected) }}</h2>
          <el-tag>{{ selected.nodeType === 'GROUP' ? (selected.activeRouteVersionId ? '目录模板' : '目录') : '工艺流程' }}</el-tag>
          <el-tag v-if="selected.activeRouteVersionNo" type="success">{{ selected.activeRouteVersionNo }}</el-tag>
          <el-tag v-if="selected.pendingStatus" type="warning">{{ statusLabel(selected.pendingStatus) }}</el-tag>
        </div>
        <div class="route-tree__actions">
          <el-button v-if="publishableDraft" type="success" :loading="publishing" :disabled="productBusy || deleting || quickConfigBusy"
            v-hasPermi="['mes:pro-route:version-submit']" @click="publishDraft"><Icon icon="ep:upload" />发布</el-button>
          <template v-if="selected.activeRouteVersionId">
          <el-button type="primary" v-hasPermi="['mes:pro-route:version-create']" @click="emit('edit', selected)"><Icon icon="ep:edit" />修改</el-button>
          <el-button @click="emit('view', selected.id)"><Icon icon="ep:view" />查看</el-button>
          <el-button v-hasPermi="['mes:pro-route:version-query']" @click="emit('versions', selected)"><Icon icon="ep:collection" />版本</el-button>
          </template>
          <template v-if="selected.nodeType === 'GROUP'">
            <el-button v-if="selected.activeRouteVersionId" v-hasPermi="['mes:pro-route:create']" @click="openInherited">新增继承流程</el-button>
          </template>
        </div>
        </div>
        <el-alert v-if="publishError" :title="publishError" type="error" :closable="false" />
        <div v-if="isFlowLeaf" v-loading="bindingsLoading" class="route-tree__bindings" aria-label="流程绑定信息">
          <el-alert v-if="bindingsError" :title="bindingsError" type="error" :closable="false" />
          <template v-else-if="bindings">
            <h3 class="route-tree__bindings-title">绑定信息</h3>
            <div class="route-tree__project-code">
              <span class="route-tree__binding-label">项目代码</span>
              <span>{{ bindings.dccProjectCode || '未绑定项目' }}</span>
              <span class="route-tree__binding-label">生产组长</span>
              <div class="route-tree__binding-value">
                <template v-if="!bindingConfigError && bindings.activeRouteVersionId">
                  <span v-for="(leader, index) in productionLeaders" :key="`${leader.productionLineId}-${index}`">
                    {{ leader.candidateSourceNames?.join('、') }}
                  </span>
                  <span v-if="!productionLeaders.length">未配置</span>
                </template>
                <span v-else>{{ bindingConfigError ? '加载失败' : '未发布' }}</span>
                <RouteTreeQuickConfig :key="`leaders-${selected.id}`" :route="bindings" kind="leaders"
                  :disabled="publishing || deleting || saving || productBusy || coefficientBusy"
                  @busy="leaderBusy = $event" @changed="productsChanged" />
              </div>
              <span class="route-tree__binding-label">损耗系数</span>
              <div class="route-tree__binding-value">
                <span>{{ bindingConfigError ? '加载失败' : lossCoefficientDisplay ?? '未发布' }}</span>
                <RouteTreeQuickConfig :key="`coefficient-${selected.id}`" :route="bindings" kind="coefficient"
                  :disabled="publishing || deleting || saving || productBusy || leaderBusy"
                  @busy="coefficientBusy = $event" @changed="productsChanged" />
              </div>
            </div>
            <el-alert v-if="bindingConfigError" :title="bindingConfigError" type="error" :closable="false" />
            <RouteTreeProducts :key="selected.id" class="route-tree__products" :route="bindings"
              :publishing="publishing || deleting || quickConfigBusy" @busy="productBusy = $event" @changed="productsChanged" />
          </template>
        </div>
        <template v-else>
          <h3>直接下级</h3>
          <el-table :data="selected.children || []" @row-click="selected = $event" empty-text="暂无直接下级">
            <el-table-column label="名称"><template #default="{ row }">{{ nodeLabel(row) }}</template></el-table-column>
            <el-table-column label="类型" width="110"><template #default="{ row }">{{ row.nodeType === 'GROUP' ? '目录' : '流程' }}</template></el-table-column>
            <el-table-column prop="activeRouteVersionNo" label="已发布版本" width="120" />
            <el-table-column label="工序结构" width="130"><template #default="{ row }">{{ row.different ? '有差异' : '与父级一致' }}</template></el-table-column>
          </el-table>
        </template>
      </section>
      <el-empty v-else description="选择目录或具体工艺流程" />
    </div>
  </ContentWrap>
  <RouteTreeDifference v-if="comparison" v-model="differenceVisible" :node="comparison.node" :parent="comparison.parent" />
  <Dialog v-model="groupVisible" title="新增工艺目录" width="540px">
    <el-form label-width="110px">
      <el-form-item label="目录名称" required><el-input v-model="group.name" aria-label="目录名称" /></el-form-item>
      <el-form-item label="直接父节点">{{ group.parentId ? groupParentName : '顶级目录' }}</el-form-item>
      <el-form-item v-if="groupNeedsTemplate" label="初始工艺模板" required>
        <el-select v-model="group.sourceRouteId" filterable placeholder="选择已有流程作为初始模板" aria-label="初始工艺模板">
          <el-option v-for="row in rows.filter(item => item.nodeType === 'FLOW')" :key="row.id" :value="row.id!" :label="row.name" />
        </el-select>
      </el-form-item>
      <el-alert title="只复制初始工艺配置，不改变来源流程。下级目录自动继承父目录。" type="info" :closable="false" />
    </el-form>
    <template #footer><el-button @click="groupVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveGroup">创建目录</el-button></template>
  </Dialog>
  <Dialog v-model="inheritVisible" title="新增继承流程" width="560px">
    <el-form label-width="110px">
      <el-form-item label="流程名称" required><el-input v-model="inherit.name" aria-label="继承流程名称" /></el-form-item>
      <el-form-item label="项目代码" required><RouteProjectSelect v-model="inherit.dccProjectCodeId" /></el-form-item>
      <el-form-item label="所属车间" required><MdWorkshopSelect v-model="inherit.workshopId" /></el-form-item>
      <el-alert :title="'继承目录「' + selected?.name + '」的已发布工序、参数及表单配置。'" type="info" :closable="false" />
    </el-form>
    <template #footer><el-button @click="inheritVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveInherited">创建并继承</el-button></template>
  </Dialog>
  <Dialog v-model="moveVisible" title="移动" width="600px">
    <el-form label-width="110px">
      <el-form-item label="移动节点">
        <span>{{ selected?.name }}</span>
      </el-form-item>
      <el-form-item label="目标父节点">
        <el-select v-model="move.parentId" filterable clearable placeholder="留空移至根目录" aria-label="目标父节点">
          <el-option v-for="row in rows.filter(item => item.nodeType === 'GROUP' && item.id !== move.id)" :key="row.id" :value="row.id!" :label="row.name" />
        </el-select>
      </el-form-item>
      <el-alert title="配置保持原值。工序与目标目录不同时，将列明差异并再次确认。" type="info" :closable="false" />
    </el-form>
    <template #footer><el-button @click="moveVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveMove">确认移动</el-button></template>
  </Dialog>
</template>

<script setup lang="ts">
import RouteTreeProducts from './RouteTreeProducts.vue'
import RouteTreeQuickConfig from './RouteTreeQuickConfig.vue'
import { ElMessageBox, type TreeInstance } from 'element-plus'
import { RouteTreeApi, type RouteTreeNode } from '@/api/mes/pro/route/tree'
import { ProRouteApi, type MesRouteId, type ProRouteVO, type ProRouteVersionVO } from '@/api/mes/pro/route'
import { ProRouteFlowConfigApi, type ProRouteStartProductionLeaderVO } from '@/api/mes/pro/route/flowconfig'
import RouteProjectSelect from './RouteProjectSelect.vue'
import RouteTreeDifference from './RouteTreeDifference.vue'
import MdWorkshopSelect from '@/views/mes/md/workstation/components/MdWorkshopSelect.vue'
import { isRouteConfirmCancel, resolveRouteOperationErrorMessage } from './routeError'
import { findOrderProductCodes } from './routeTreeOrderSearch'
import { useUserStore } from '@/store/modules/user'
import { hasPermission } from '@/directives/permission/hasPermi'
import { submitRouteVersionWithTreeReview } from './routeTreePublish'
import { authContextRevision, getTenantId, getVisitTenantId } from '@/utils/auth'
const emit = defineEmits<{ edit: [row: ProRouteVO]; view: [id?: number]; versions: [row: ProRouteVO]; changed: [] }>()
const message = useMessage()
const rows = ref<RouteTreeNode[]>([]), selected = ref<RouteTreeNode>(), loading = ref(false), saving = ref(false), error = ref('')
const filter = ref(''), treeRef = ref<TreeInstance>()
const userStore = useUserStore()
const expansionCacheKey = computed(() => {
  // 访问租户与登录租户分别隔离，同一浏览器切换账号时不共享目录状态。
  void authContextRevision.value
  const userId = userStore.getUser.id
  return userId ? `mes:route-tree:expanded:${getTenantId()}:${getVisitTenantId() ?? getTenantId()}:${userId}` : undefined
})
const expandedNodeKeys = ref<string[]>([])
const collapseOnNextClick = ref(true)
watch(expansionCacheKey, key => {
  expandedNodeKeys.value = []
  collapseOnNextClick.value = true
  if (!key) return
  try {
    const stored = localStorage.getItem(key)
    if (stored === null) return
    const keys: unknown = JSON.parse(stored)
    if (!Array.isArray(keys) || !keys.every(item => typeof item === 'string')) {
      throw new Error('目录展开状态格式无效')
    }
    expandedNodeKeys.value = keys
  } catch (failure) {
    message.error(resolveRouteOperationErrorMessage(failure, '恢复目录展开状态失败'))
  }
}, { immediate: true })
const persistExpansion = (keys: string[]): boolean => {
  if (!expansionCacheKey.value) {
    message.error('未获取到当前用户，无法保存目录展开状态')
    return false
  }
  try {
    localStorage.setItem(expansionCacheKey.value, JSON.stringify(keys))
    expandedNodeKeys.value = keys
    return true
  } catch (failure) {
    message.error(resolveRouteOperationErrorMessage(failure, '保存目录展开状态失败'))
    return false
  }
}
const saveNodeExpansion = (node: RouteTreeNode, expanded: boolean) => {
  if (node.id == null) return
  const keys = new Set(expandedNodeKeys.value)
  if (expanded) keys.add(String(node.id))
  else keys.delete(String(node.id))
  persistExpansion([...keys])
}
const toggleAllExpansion = () => {
  if (!treeRef.value) return
  const nodes = rows.value.flatMap(row => {
    const node = row.id == null ? undefined : treeRef.value!.getNode(row.id)
    return node?.childNodes.length ? [node] : []
  })
  const expanded = !collapseOnNextClick.value
  if (!persistExpansion(expanded ? nodes.map(node => String(node.data.id)) : [])) return
  for (const node of nodes) {
    if (expanded) node.expand(undefined, false)
    else node.collapse()
  }
  collapseOnNextClick.value = !collapseOnNextClick.value
}
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
  if (searchType.value !== 'order') return '未找到匹配的目录、流程或产品编码'
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
const publishableDraft = ref<ProRouteVersionVO>()
const publishing = ref(false), publishError = ref('')
const productBusy = ref(false)
const deleting = ref(false)
const leaderBusy = ref(false), coefficientBusy = ref(false)
const quickConfigBusy = computed(() => leaderBusy.value || coefficientBusy.value)
const productsChanged = async () => { await load(); emit('changed') }
const findPublishableDraft = async (node: RouteTreeNode) => {
  if (node.id == null || node.pendingStatus !== 'DRAFT' ||
    !hasPermission(['mes:pro-route:version-submit']) || !hasPermission(['mes:pro-route:version-query'])) return
  const versions = await ProRouteApi.getRouteVersionList(node.id)
  const drafts = versions.filter(version => !version.active && version.lifecycleStatus === 'DRAFT')
  if (drafts.length > 1) throw new Error('当前流程存在多个草稿，请在“版本”中选择要发布的版本')
  const draft = drafts[0]
  if (!draft) return
  const eligibility = await ProRouteApi.getRouteVersionBlockers(draft.id)
  if (!eligibility.publishable || eligibility.blockers.length) return
  if (node.nodeType === 'GROUP') {
    const preview = await RouteTreeApi.preview(draft.id)
    if (preview.blockers.length) return
  }
  return draft
}
watch(selected, async (node, _previous, onCleanup) => {
  let cancelled = false
  onCleanup(() => { cancelled = true })
  publishableDraft.value = undefined
  publishError.value = ''
  if (!node) return
  try {
    const draft = await findPublishableDraft(node)
    if (!cancelled) publishableDraft.value = draft
  } catch (failure) {
    if (!cancelled) publishError.value = resolveRouteOperationErrorMessage(failure, '检查草稿发布条件失败，请刷新后重试')
  }
}, { flush: 'sync' })
const publishDraft = async () => {
  const node = selected.value, candidate = publishableDraft.value
  if (!node || !candidate || publishing.value || productBusy.value || deleting.value || quickConfigBusy.value) return
  publishing.value = true
  publishError.value = ''
  try {
    const latest = await findPublishableDraft(node)
    if (!latest || latest.id !== candidate.id) throw new Error('草稿状态或发布条件已变化，请刷新后重试')
    const result = await submitRouteVersionWithTreeReview(latest)
    message.success(result.lifecycleStatus === 'PENDING_APPROVAL' ? '发布申请已提交，等待审批'
      : result.lifecycleStatus === 'ACTIVE' ? '候选版本已发布生效' : '发布请求已提交')
    publishableDraft.value = undefined
    await load()
    emit('changed')
  } catch (failure) {
    if (!isRouteConfirmCancel(failure)) {
      message.error(resolveRouteOperationErrorMessage(failure, '发布失败，请重试'))
      publishableDraft.value = undefined
      await load()
    }
  } finally {
    publishing.value = false
  }
}
const bindings = ref<ProRouteVO>(), bindingsLoading = ref(false), bindingsError = ref('')
const productionLeaders = ref<ProRouteStartProductionLeaderVO[]>([])
const lossCoefficientDisplay = ref<string>()
const bindingConfigError = ref('')
watch(selected, async (node, _previous, onCleanup) => {
  let cancelled = false
  onCleanup(() => { cancelled = true })
  bindings.value = undefined
  productionLeaders.value = []
  lossCoefficientDisplay.value = undefined
  bindingConfigError.value = ''
  bindingsError.value = ''
  bindingsLoading.value = false
  if (!node || !isFlowLeaf.value || node.id == null) return
  bindingsLoading.value = true
  try {
    const detail: ProRouteVO = await ProRouteApi.getRoute(node.id)
    if (cancelled) return
    bindings.value = detail
    if (detail.activeRouteVersionId != null) {
      try {
        const [leaders, coefficient] = await Promise.all([
          ProRouteFlowConfigApi.getRouteStartProductionLeaders(node.id, detail.activeRouteVersionId),
          ProRouteApi.getRouteLossCoefficient(detail.activeRouteVersionId)
        ])
        if (cancelled) return
        if (coefficient.lossCoefficient == null || !Number.isFinite(Number(coefficient.lossCoefficient))) {
          throw new Error('损耗系数数据无效')
        }
        productionLeaders.value = leaders
        lossCoefficientDisplay.value = Number(coefficient.lossCoefficient).toFixed(2)
      } catch (failure) {
        if (!cancelled) bindingConfigError.value = resolveRouteOperationErrorMessage(failure, '加载生产组长和损耗系数失败，请刷新后重试')
      }
    }
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
const groupVisible = ref(false), inheritVisible = ref(false), moveVisible = ref(false)
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
  await load(); selected.value = rows.value.find(row => String(row.id) === String(id)); message.success('目录已创建')
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
const deleteSelectedNode = async () => {
  if (deleting.value || loading.value || saving.value || publishing.value || productBusy.value || quickConfigBusy.value) return
  const node = selected.value
  deleting.value = true
  try {
    if (!node?.id || !['GROUP', 'FLOW'].includes(node.nodeType)) throw new Error('请选择要删除的目录或工艺流程')
    if (!hasPermission(['mes:pro-route:delete'])) throw new Error('没有删除目录或工艺流程的权限')
    if (node.children?.length) throw new Error('请先移出或删除下级目录及流程，再删除当前节点')
    const label = node.nodeType === 'GROUP' ? '目录' : '工艺流程'
    await ElMessageBox.confirm(`确认删除${label}「${node.name}」？删除后无法恢复。已启用或仍被业务引用的节点不可删除。`, `删除${label}`, {
      type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消'
    })
    if (node.nodeType === 'GROUP') await RouteTreeApi.deleteGroup(node.id)
    else await ProRouteApi.deleteRoute(node.id)
    if (String(selected.value?.id) === String(node.id)) selected.value = undefined
    rows.value = rows.value.filter(row => String(row.id) !== String(node.id))
    message.success(`${label}已删除`)
    await load()
    emit('changed')
  } catch (failure) {
    if (!isRouteConfirmCancel(failure)) message.error(resolveRouteOperationErrorMessage(failure, '删除失败，请重试'))
  } finally {
    deleting.value = false
  }
}
const openInherited = () => {
  Object.assign(inherit, { name: '', dccProjectCodeId: undefined, workshopId: undefined }); inheritVisible.value = true
}
const saveInherited = () => run(async () => {
  if (!selected.value?.id || !inherit.name.trim() || !inherit.dccProjectCodeId || !inherit.workshopId) throw new Error('请完整填写流程名称、项目代码及车间')
  await RouteTreeApi.inherit(selected.value.id, { ...inherit, processIds: selected.value.processIds })
  inheritVisible.value = false; message.success('流程已创建并继承父节点配置')
})
const openMove = () => {
  Object.assign(move, { id: selected.value?.id, parentId: selected.value?.parentId })
  moveVisible.value = true
}
const saveMove = () => run(async () => {
  if (!move.id) throw new Error('请选择要移动的目录或流程')
  const preview = await RouteTreeApi.movePreview(move.id, move.parentId)
  if (preview.different) await ElMessageBox.confirm(
    '工序与目标目录不同。当前：' + preview.currentProcesses.join(' → ') + '；目标：' + preview.expectedProcesses.join(' → ') + '。确认保留差异并移动？',
    '工序一致性确认', { type: 'warning', confirmButtonText: '保留差异并移动', cancelButtonText: '取消' })
  await RouteTreeApi.move({ id: move.id, parentId: move.parentId, fingerprint: preview.fingerprint, confirmDifference: preview.different })
  moveVisible.value = false; message.success('移动成功，流程配置保持原值')
})
onMounted(load)
onActivated(load)
defineExpose({ load })
</script>

<style scoped>
.route-tree { height: calc(100dvh - var(--top-tool-height) - var(--tags-view-height) - var(--app-content-padding) - 44px - 10px); margin-bottom: 0; }
.route-tree :deep(.el-card__body) { display: flex; height: 100%; min-height: 0; box-sizing: border-box; flex-direction: column; }
.route-tree__detail-header { display: flex; flex-shrink: 0; align-items: center; justify-content: space-between; gap: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--el-border-color-lighter); }
.route-tree__heading, .route-tree__actions { display: flex; min-width: 0; align-items: center; gap: 12px; flex-wrap: wrap; }
.route-tree__search { display: flex; gap: 8px; }
.route-tree__search-type { width: 102px; flex-shrink: 0; }
.route-tree__search > .el-input { flex: 1; min-width: 0; }
.route-tree__expand-toggle { width: 32px; flex-shrink: 0; margin-left: 0; padding: 0; }
.route-tree__search-hint { color: var(--el-text-color-secondary); font-size: 12px; margin: 8px 0; }
.route-tree__layout { display: grid; flex: 1; grid-template-columns: 380px minmax(0, 1fr); gap: 16px; min-height: 0; }
.route-tree__sidebar { display: flex; min-height: 0; min-width: 0; flex-direction: column; border: 1px solid #dbe3ef; border-radius: 8px; background: #fff; overflow: hidden; }
.route-tree__directory { display: flex; flex: 1; min-height: 0; flex-direction: column; }
.route-tree__search { padding: 8px; }
.route-tree__sidebar-body { flex: 1; min-height: 0; padding: 0 8px 8px; }
.route-tree__sidebar-actions { display: grid; flex-shrink: 0; grid-template-columns: 1fr 1fr; gap: 8px; padding: 10px; border-top: 1px solid #edf1f6; }
.route-tree__sidebar-actions:has(.el-button:only-child) { grid-template-columns: 1fr; }
.route-tree__sidebar-actions .el-button { margin-left: 0; }
.route-tree section { display: flex; min-width: 0; min-height: 0; flex-direction: column; overflow: auto; padding: 4px 12px 0 0; }
.route-tree__node { display: flex; min-width: 0; align-items: center; gap: 6px; font-size: 13px; font-weight: 500; }
.route-tree__node > :first-child { flex-shrink: 0; }
.route-tree__node > span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.route-tree__heading h2 { margin: 0; font-size: 20px; }
.route-tree__actions { justify-content: flex-end; }
.route-tree__actions .el-button { margin-left: 0; }
.route-tree__actions :deep(.el-icon) { margin-right: 6px; }
.route-tree__bindings { display: flex; flex: 1; min-height: 0; flex-direction: column; padding-top: 16px; }
.route-tree__bindings > :not(.route-tree__products) { flex-shrink: 0; }
.route-tree__bindings-title { margin: 0; font-size: 16px; }
.route-tree__project-code { display: grid; grid-template-columns: 100px minmax(0, 1fr); min-height: 42px; margin-top: 12px; border-radius: 6px; align-items: center; background: var(--el-fill-color-light); color: var(--el-text-color-primary); }
.route-tree__binding-label { padding: 12px 16px; color: var(--el-text-color-regular); font-weight: 600; }
.route-tree__binding-value { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; min-width: 0; padding: 8px 16px 8px 0; overflow-wrap: anywhere; }
:deep(.el-tree) { margin-top: 4px; }
:deep(.el-tree-node__content) { min-height: 42px; margin-bottom: 6px; border: 1px solid transparent; border-radius: 6px; color: #263247; }
:deep(.el-tree-node.is-current > .el-tree-node__content), :deep(.el-tree-node__content:hover) { border-color: #dbeafe; background: #f5f9ff; color: #1677ff; }
@media (max-width: 800px) { .route-tree { height: auto; } .route-tree__layout { grid-template-columns: 1fr; } .route-tree__sidebar { height: 60vh; min-height: 320px; } .route-tree section { padding: 20px 0 0; } }
@media (max-width: 1100px) { .route-tree__detail-header { align-items: flex-start; flex-direction: column; } .route-tree__actions { justify-content: flex-start; } }
</style>
