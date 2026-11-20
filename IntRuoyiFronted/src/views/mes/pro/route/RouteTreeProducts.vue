<template>
  <div v-loading="loading" class="tree-products">
    <div class="tree-products__header">
      <h3>产品编码</h3>
      <span>{{ draft ? `草稿 ${draft.versionNo} · 发布后生效` : '点击编码查看产品详情' }}</span>
      <el-button v-if="canEdit" type="primary" plain :disabled="publishing || busy || loading || !!error || locked"
        @click="selectorVisible = true"><Icon icon="ep:plus" />添加</el-button>
      <el-input v-model="filter" clearable placeholder="搜索产品编码" aria-label="搜索产品编码">
        <template #prefix><Icon icon="ep:search" /></template>
      </el-input>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <template v-else>
      <div v-if="filteredProducts.length" class="tree-products__grid">
        <div v-for="product in filteredProducts" :key="product.itemId" class="tree-products__item">
          <ProductCodeLink :code="product.itemCode" />
          <el-button v-if="canEdit" link type="danger" :disabled="publishing || busy || loading || locked"
            :aria-label="`解绑产品 ${product.itemCode}`" @click="removeProduct(product)">解绑</el-button>
        </div>
      </div>
      <el-empty v-else :description="filter ? '未找到匹配的产品编码' : '未绑定产品'" :image-size="64" />
    </template>
    <ProductNumberSelector v-model="selectorVisible" :linked-item-ids="products.map(product => product.itemId)"
      :submitting="busy || publishing" @confirm="addProducts" />
  </div>
</template>

<script setup lang="ts">
import ProductCodeLink from '@/components/ProductCodeLink.vue'
import ProductNumberSelector from '@/components/MesProductSelector/ProductNumberSelector.vue'
import { ProRouteApi, type ProRouteVO, type ProRouteVersionVO } from '@/api/mes/pro/route'
import { ProRouteProductApi, type ProRouteProductVO } from '@/api/mes/pro/route/product'
import type { MdItemVO } from '@/api/mes/md/item'
import { hasPermission } from '@/directives/permission/hasPermi'
import { ensureSameSourceDraftCandidateForProductionConfig } from './routeCandidateEntry'
import { isRouteConfirmCancel, resolveRouteOperationErrorMessage } from './routeError'

const props = defineProps<{ route: ProRouteVO; publishing: boolean }>()
const emit = defineEmits<{ changed: []; busy: [value: boolean] }>()
const message = useMessage()
const products = ref<ProRouteProductVO[]>([]), draft = ref<ProRouteVersionVO>()
const loading = ref(false), busy = ref(false), error = ref(''), locked = ref(false)
const selectorVisible = ref(false), filter = ref('')
const canEdit = computed(() => hasPermission(['mes:pro-route:update']) &&
  hasPermission(['mes:pro-route:version-query']) && hasPermission(['mes:pro-route:version-create']))
const filteredProducts = computed(() => products.value.filter(product =>
  product.itemCode?.toLowerCase().includes(filter.value.trim().toLowerCase())))
let disposed = false
onBeforeUnmount(() => { disposed = true; emit('busy', false) })
const requireRouteId = () => {
  if (disposed || props.route.id == null) throw new Error('流程已切换，请重新操作')
  return props.route.id
}
const loadProducts = async () => {
  loading.value = true; error.value = ''; products.value = []; draft.value = undefined
  try {
    const routeId = requireRouteId()
    const versions: ProRouteVersionVO[] = hasPermission(['mes:pro-route:version-query'])
      ? await ProRouteApi.getRouteVersionList(routeId) : []
    if (disposed) return
    const drafts = versions.filter(version => !version.active && version.lifecycleStatus === 'DRAFT')
    if (drafts.length > 1) throw new Error('存在多个草稿，请在“版本与审核”中选择后维护产品绑定')
    locked.value = versions.some(version => !version.active && ['PENDING_APPROVAL', 'READY_TO_PUBLISH'].includes(version.lifecycleStatus))
    draft.value = drafts[0]
    const versionId = draft.value?.id ?? props.route.activeRouteVersionId
    if (versionId == null) throw new Error('当前流程没有可读取的版本，无法加载产品绑定')
    const rows = await ProRouteProductApi.getRouteProductListByRoute(routeId, versionId)
    if (!disposed) products.value = rows
  } catch (failure) {
    if (!disposed) error.value = resolveRouteOperationErrorMessage(failure, '加载产品绑定失败，请重试')
    throw failure
  } finally { loading.value = false }
}
const prepareDraft = async (requireProject = false) => {
  if (!canEdit.value || props.publishing) throw new Error('当前没有产品绑定编辑权限，或流程正在发布')
  const routeId = requireRouteId()
  if (requireProject) {
    const binding = await ProRouteApi.getRouteDccProjectBinding(routeId)
    if (!binding.bound || !binding.dccProjectCodeId) throw new Error('请先关联项目，再添加产品绑定')
  }
  requireRouteId()
  const result = await ensureSameSourceDraftCandidateForProductionConfig({
    routeId, actionName: '快捷维护产品绑定', changeReason: '树形管理快捷维护产品绑定'
  })
  if (!result) throw new Error('未获取到可编辑草稿')
  requireRouteId()
  return result.candidate
}
const setBusy = (value: boolean) => { busy.value = value; emit('busy', value) }
const addProducts = async (items: MdItemVO[]) => {
  if (busy.value || props.publishing || !items.length) return
  setBusy(true)
  let completed = 0
  try {
    const candidate = await prepareDraft(true)
    const current: ProRouteProductVO[] = await ProRouteProductApi.getRouteProductListByRoute(requireRouteId(), candidate.id)
    const ids = new Set(current.map(product => product.itemId))
    const pending = [...new Map(items.map(item => [item.id, item])).values()].filter(item => !ids.has(item.id))
    // 每次保存更新同一份草稿快照，必须顺序写入。
    for (const item of pending) {
      await ProRouteProductApi.createRouteProduct({ routeId: requireRouteId(), routeVersionId: candidate.id, itemId: item.id })
      completed++
    }
    selectorVisible.value = false
    await loadProducts()
    message.success(completed ? `已添加 ${completed} 个产品到草稿，请点击上方“发布”生效` : '所选产品已在草稿中绑定')
  } catch (failure) {
    message.error(`已添加 ${completed} 个产品；${resolveRouteOperationErrorMessage(failure, '添加失败，请重试未完成的产品')}`)
    // 部分写入成功也必须展示实际草稿内容，不能继续使用旧列表。
    if (completed && !disposed) {
      try { await loadProducts() } catch (refreshFailure) {
        message.error(resolveRouteOperationErrorMessage(refreshFailure, '重新加载草稿失败'))
      }
    }
  } finally { setBusy(false); if (!disposed) emit('changed') }
}
const removeProduct = async (product: ProRouteProductVO) => {
  if (busy.value || props.publishing) return
  setBusy(true)
  let saved = false
  try {
    await message.delConfirm(`确认将产品“${product.itemCode}”从草稿中解绑？发布后生效。`, '解绑产品')
    const candidate = await prepareDraft()
    await ProRouteProductApi.deleteCandidateRouteProduct(requireRouteId(), product.itemId, candidate.id)
    saved = true
    await loadProducts()
    message.success('已从草稿解绑，请点击上方“发布”生效')
  } catch (failure) {
    if (!isRouteConfirmCancel(failure)) message.error(resolveRouteOperationErrorMessage(failure, '解绑失败，请重试'))
  } finally { setBusy(false); if (saved && !disposed) emit('changed') }
}
onMounted(() => { loadProducts().catch(failure => message.error(resolveRouteOperationErrorMessage(failure, '加载产品绑定失败'))) })
</script>

<style scoped>
.tree-products { display: flex; flex: 1; min-height: 0; flex-direction: column; }
.tree-products__header { display: flex; flex-shrink: 0; min-height: 56px; margin-top: 20px; align-items: center; gap: 12px; border-bottom: 1px solid var(--el-border-color-lighter); }
.tree-products__header h3 { margin: 0; font-size: 16px; }
.tree-products__header > span { color: var(--el-text-color-secondary); font-size: 13px; margin-right: auto; }
.tree-products__header .el-input { width: 240px; }
.tree-products__grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); grid-auto-rows: minmax(40px, max-content); align-content: start; overflow-y: auto; min-height: 0; }
.tree-products__item { display: flex; align-items: center; gap: 8px; min-width: 0; min-height: 40px; box-sizing: border-box; line-height: 23px; padding: 8px 12px; border-bottom: 1px solid var(--el-border-color-lighter); }
.tree-products__item :deep(a) { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tree-products__item .el-button { flex-shrink: 0; }
.tree-products__item:hover, .tree-products__item:focus-within { background: var(--el-color-primary-light-9); }
@media (max-width: 1100px) { .tree-products__grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .tree-products__header { flex-wrap: wrap; padding: 10px 0; } }
@media (max-width: 560px) { .tree-products__grid { grid-template-columns: 1fr; } .tree-products__header .el-input { width: 100%; } }
</style>
