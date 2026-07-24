<!-- 基于 ruoyi-vue3 的 Pagination 重构，核心是简化无用的属性，并使用 ts 重写 -->
<template>
  <el-pagination
    v-show="total > 0"
    v-model:current-page="currentPage"
    v-model:page-size="pageSize"
    :background="true"
    :page-sizes="PAGE_SIZE_OPTIONS"
    :pager-count="pagerCount"
    :total="total"
    :small="isSmall"
    class="float-right mb-15px mt-15px"
    layout="total, sizes, prev, pager, next, jumper"
    @size-change="handleSizeChange"
    @current-change="handleCurrentChange"
  />
</template>
<script lang="ts" setup>
import { computed, onMounted, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/modules/app'

defineOptions({ name: 'Pagination' })

const DEFAULT_PAGE_SIZE = 20
const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100]
const PAGE_SIZE_STORAGE_PREFIX = 'int:list:page-size:'
let ANONYMOUS_PAGE_SIZE_STORAGE_SEED = 0

// 此处解决了当全局size为small的时候分页组件样式太大的问题
const appStore = useAppStore()
const route = useRoute()
const anonymousPageSizeStorageIndex = ANONYMOUS_PAGE_SIZE_STORAGE_SEED++
const layoutCurrentSize = computed(() => appStore.currentSize)
const isSmall = ref<boolean>(layoutCurrentSize.value === 'small')
watchEffect(() => {
  isSmall.value = layoutCurrentSize.value === 'small'
})

const props = defineProps({
  // 总条目数
  total: {
    required: true,
    type: Number
  },
  // 当前页数：pageNo
  page: {
    type: Number,
    default: 1
  },
  // 每页显示条目个数：pageSize
  limit: {
    type: Number,
    default: 20
  },
  // 稳定列表标识，用于记住当前列表的每页条数
  storageKey: {
    type: String,
    default: ''
  },
  // 设置最大页码按钮数。 页码按钮的数量，当总页数超过该值时会折叠
  // 移动端页码按钮的数量端默认值 5
  pagerCount: {
    type: Number,
    default: document.body.clientWidth < 992 ? 5 : 7
  }
})

const emit = defineEmits(['update:page', 'update:limit', 'pagination'])
const resolvedStorageKey = computed(() => {
  if (props.storageKey) return props.storageKey
  const routePath = route.path || window.location.pathname
  return routePath ? `route:${routePath}:${anonymousPageSizeStorageIndex}` : ''
})
const pageSizeStorageKey = computed(() =>
  resolvedStorageKey.value ? `${PAGE_SIZE_STORAGE_PREFIX}${resolvedStorageKey.value}` : ''
)
const isValidPageSize = (value: number) => PAGE_SIZE_OPTIONS.includes(value)
const readRememberedPageSize = () => {
  if (!pageSizeStorageKey.value) return undefined
  const rawPageSize = window.localStorage.getItem(pageSizeStorageKey.value)
  if (rawPageSize == null || rawPageSize === '') return undefined
  const rememberedPageSize = Number(rawPageSize)
  if (!Number.isInteger(rememberedPageSize) || !isValidPageSize(rememberedPageSize)) {
    throw new Error(`列表分页条数配置无效：${pageSizeStorageKey.value}=${rawPageSize}`)
  }
  return rememberedPageSize
}
const saveRememberedPageSize = (nextPageSize: number) => {
  if (!pageSizeStorageKey.value) return
  if (!isValidPageSize(nextPageSize)) {
    throw new Error(`列表分页条数不在允许范围内：${nextPageSize}`)
  }
  window.localStorage.setItem(pageSizeStorageKey.value, String(nextPageSize))
}
const applyRememberedPageSize = () => {
  const nextPageSize = readRememberedPageSize() ?? DEFAULT_PAGE_SIZE
  if (nextPageSize === props.limit) return
  const nextPage = currentPage.value * nextPageSize > props.total ? 1 : currentPage.value
  if (nextPage !== currentPage.value) {
    emit('update:page', nextPage)
  }
  emit('update:limit', nextPageSize)
}
const currentPage = computed({
  get() {
    return props.page
  },
  set(val) {
    // 触发 update:page 事件，更新 limit 属性，从而更新 pageNo
    emit('update:page', val)
  }
})
const pageSize = computed({
  get() {
    return props.limit
  },
  set(val) {
    // 触发 update:limit 事件，更新 limit 属性，从而更新 pageSize
    emit('update:limit', val)
  }
})
const handleSizeChange = (val) => {
  saveRememberedPageSize(val)
  // 如果修改后超过最大页面，强制跳转到第 1 页
  if (currentPage.value * val > props.total) {
    currentPage.value = 1
  }
  // 触发 pagination 事件，重新加载列表
  emit('pagination', { page: currentPage.value, limit: val })
}
const handleCurrentChange = (val) => {
  // 触发 pagination 事件，重新加载列表
  emit('pagination', { page: val, limit: pageSize.value })
}
onMounted(applyRememberedPageSize)
</script>
