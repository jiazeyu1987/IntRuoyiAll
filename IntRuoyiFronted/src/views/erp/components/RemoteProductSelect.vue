<template>
  <el-select
    v-bind="$attrs"
    :model-value="modelValue"
    filterable remote clearable :remote-method="search" :loading="loading"
    @update:model-value="select" @visible-change="onVisible"
  >
    <el-option v-for="item in options" :key="item.id" :label="item.name" :value="item.id" />
    <template #footer>
      <el-pagination small layout="prev, pager, next, total" :page-size="pageSize"
        :current-page="pageNo" :total="total" @current-change="loadPage" />
    </template>
  </el-select>
</template>
<script setup lang="ts">
import { ProductApi, type ProductVO } from '@/api/erp/product/product'
defineOptions({ inheritAttrs: false })
const props = defineProps<{ modelValue?: number | null }>()
const emit = defineEmits(['update:modelValue', 'change', 'selected'])
const rows = ref<ProductVO[]>([])
const selected = ref<ProductVO>()
const loading = ref(false)
const total = ref(0)
const pageNo = ref(1)
const pageSize = 20
let keyword = ''
let requestVersion = 0
let timer: ReturnType<typeof setTimeout> | undefined
const options = computed(() => selected.value && !rows.value.some(row => row.id === selected.value!.id)
  ? [selected.value, ...rows.value] : rows.value)
const loadPage = async (page = 1) => {
  const version = ++requestVersion
  loading.value = true
  try {
    const data = await ProductApi.getProductSimplePage({ pageNo: page, pageSize, keyword })
    if (version !== requestVersion) return
    rows.value = data.list
    total.value = data.total
    pageNo.value = page
  } finally {
    if (version === requestVersion) loading.value = false
  }
}
const search = (value: string) => {
  keyword = value
  ++requestVersion
  clearTimeout(timer)
  timer = setTimeout(() => { void loadPage(1) }, 250)
}
const onVisible = (visible: boolean) => {
  if (visible) { clearTimeout(timer); keyword = ''; void loadPage(1) }
}
const select = (id?: number) => {
  selected.value = options.value.find(row => row.id === id)
  if (selected.value) emit('selected', selected.value)
  emit('update:modelValue', id)
  emit('change', id)
}
watch(() => props.modelValue, async (id) => {
  if (id == null) { selected.value = undefined; return }
  const cached = options.value.find(row => row.id === id)
  if (cached) { selected.value = cached; return }
  const products = await ProductApi.getProductSimpleByIds([id])
  const product = products.find(item => item.id === id)
  if (!product) throw new Error('已选产品不存在或已删除')
  if (props.modelValue === id) selected.value = product
}, { immediate: true })
onBeforeUnmount(() => { clearTimeout(timer); ++requestVersion })
</script>
