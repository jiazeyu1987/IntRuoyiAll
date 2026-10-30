<template>
  <Dialog
    v-model="visible"
    title="选择产品编号"
    width="min(1100px, 94vw)"
    top="4vh"
    :before-close="handleClose"
  >
    <div class="product-number-selector">
      <el-form class="product-number-selector__search" @submit.prevent="search">
        <el-input
          v-model="keyword"
          aria-label="产品名称或编号"
          placeholder="请输入产品名称或编号，按回车查询"
          clearable
          :disabled="loading || submitting"
        />
        <el-button native-type="submit" type="primary" :loading="loading" :disabled="submitting">
          查询
        </el-button>
      </el-form>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <div class="product-number-selector__summary">
        <span>匹配 {{ results.length }} 个编号 · 已选 {{ selectedItems.length }} 个</span>
        <div>
          <el-button :disabled="!selectableItems.length || loading || submitting" @click="selectAll">
            全选查询结果（{{ selectableItems.length }}）
          </el-button>
          <el-button :disabled="!selectedItems.length || submitting" @click="clearSelection">
            清空已选
          </el-button>
        </div>
      </div>
      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="pageItems"
        row-key="id"
        border
        height="min(440px, 48vh)"
        :empty-text="searched ? '没有匹配的产品编号' : '请输入产品名称或编号后查询'"
        @select="handleRowSelect"
        @select-all="handlePageSelect"
      >
        <el-table-column type="selection" width="50" :selectable="isSelectable" />
        <el-table-column prop="code" label="产品编号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="name" label="产品名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="specification" label="规格型号" min-width="180" show-overflow-tooltip />
        <el-table-column label="关联状态" width="100">
          <template #default="{ row }">
            <el-tag :type="linkedIds.has(row.id) ? 'success' : 'info'">
              {{ linkedIds.has(row.id) ? '已关联' : '未关联' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNo"
        :page-size="PAGE_SIZE"
        :total="results.length"
        layout="total, prev, pager, next"
        :disabled="loading || submitting"
        class="product-number-selector__pagination"
      />
    </div>
    <template #footer>
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="loading || !!error || !selectedItems.length"
        @click="emit('confirm', selectedItems)"
      >确认关联（{{ selectedItems.length }}）</el-button>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { MdItemApi, type MdItemVO } from '@/api/mes/md/item'
import { CommonStatusEnum } from '@/utils/constants'
import type { ElTable } from 'element-plus'

const props = withDefaults(defineProps<{
  modelValue: boolean
  linkedItemIds: number[]
  submitting?: boolean
}>(), { submitting: false })
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: [items: MdItemVO[]]
}>()
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})
const PAGE_SIZE = 20
const keyword = ref('')
const results = ref<MdItemVO[]>([])
const selected = ref(new Map<number, MdItemVO>())
const loading = ref(false)
const searched = ref(false)
const error = ref('')
const pageNo = ref(1)
const tableRef = ref<InstanceType<typeof ElTable>>()
const linkedIds = computed(() => new Set(props.linkedItemIds))
const isSelectable = (item: MdItemVO) => !props.submitting && !linkedIds.value.has(item.id)
const selectableItems = computed(() => results.value.filter((item) => !linkedIds.value.has(item.id)))
const selectedItems = computed(() => [...selected.value.values()].filter((item) => !linkedIds.value.has(item.id)))
const pageItems = computed(() => results.value.slice((pageNo.value - 1) * PAGE_SIZE, pageNo.value * PAGE_SIZE))
let searchId = 0

// 查询全部分页，避免“全选”只关联第一页的编号。
const queryAll = async (field: 'code' | 'name', value: string, requestId: number) => {
  const items: MdItemVO[] = []
  let page = 1
  let total: number
  do {
    const result = await MdItemApi.getItemPage({
      pageNo: page++, pageSize: 100, status: CommonStatusEnum.ENABLE, [field]: value
    })
    if (requestId !== searchId) return []
    if (!Array.isArray(result.list) || !Number.isInteger(result.total) || result.total < 0) {
      throw new Error('产品查询返回的数据格式不正确，请重试')
    }
    total = result.total
    if (!result.list.length && items.length < total) {
      throw new Error('产品编号未加载完整，请重新查询')
    }
    items.push(...result.list)
  } while (items.length < total)
  return items
}

const search = async () => {
  if (loading.value || props.submitting) return
  const value = keyword.value.trim()
  if (!value) {
    error.value = '请输入产品名称或编号'
    return
  }
  const requestId = ++searchId
  loading.value = true
  error.value = ''
  results.value = []
  pageNo.value = 1
  try {
    const [byCode, byName] = await Promise.all([
      queryAll('code', value, requestId), queryAll('name', value, requestId)
    ])
    if (requestId !== searchId) return
    results.value = [...new Map([...byCode, ...byName].map((item) => [item.id, item])).values()]
      .sort((left, right) => left.code.localeCompare(right.code, 'zh-CN', { numeric: true }))
    searched.value = true
  } catch (cause) {
    if (requestId !== searchId) return
    error.value = cause instanceof Error ? cause.message : '产品编号查询失败，请重试'
  } finally {
    if (requestId === searchId) loading.value = false
  }
}

const handleRowSelect = (items: MdItemVO[], row: MdItemVO) => {
  if (items.some((item) => item.id === row.id)) selected.value.set(row.id, row)
  else selected.value.delete(row.id)
}
const handlePageSelect = (items: MdItemVO[]) => {
  for (const item of pageItems.value) selected.value.delete(item.id)
  for (const item of items) {
    if (!linkedIds.value.has(item.id)) selected.value.set(item.id, item)
  }
}
const syncSelection = async () => {
  await nextTick()
  tableRef.value?.clearSelection()
  for (const item of pageItems.value) {
    if (selected.value.has(item.id) && !linkedIds.value.has(item.id)) {
      tableRef.value?.toggleRowSelection(item, true)
    }
  }
}
const selectAll = () => {
  for (const item of selectableItems.value) selected.value.set(item.id, item)
  syncSelection()
}
const clearSelection = () => {
  selected.value.clear()
  syncSelection()
}
const handleClose = (done: () => void) => {
  if (!props.submitting) done()
}
watch([pageItems, linkedIds], syncSelection, { flush: 'post' })
watch(visible, (open) => {
  ++searchId
  loading.value = false
  if (open) {
    keyword.value = ''
    results.value = []
    selected.value.clear()
    searched.value = false
    error.value = ''
    pageNo.value = 1
  }
})
</script>

<style scoped>
.product-number-selector__search {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.product-number-selector__summary {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin: 16px 0;
}
.product-number-selector__pagination {
  justify-content: flex-end;
  margin-top: 16px;
  overflow-x: auto;
}
</style>
