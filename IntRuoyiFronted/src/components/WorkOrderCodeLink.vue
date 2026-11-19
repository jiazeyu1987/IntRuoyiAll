<template>
  <a v-if="normalizedCode" :href="router.resolve(destination).href" class="work-order-code-link" @click.stop="navigate">{{ code }}</a>
  <span v-else>{{ code || emptyText }}</span>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()
let visit = 0
const props = defineProps<{ code?: string | null; workOrderId?: number | string | null; emptyText?: string }>()
const normalizedCode = computed(() => {
  const value = props.code?.trim()
  return value && !['-', '--', '—'].includes(value) ? value : undefined
})
const destination = computed(() => ({
  path: '/mes/pro/work-order',
  query: { code: normalizedCode.value, ...(props.workOrderId ? { openId: String(props.workOrderId) } : {}) }
}))
const navigate = (event: MouseEvent) => {
  if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button !== 0) return
  event.preventDefault()
  return router.push({ ...destination.value, query: { ...destination.value.query, detailVisit: `${Date.now()}-${++visit}` } })
}
</script>
<style scoped>
.work-order-code-link { color: var(--el-color-primary); cursor: pointer; overflow-wrap: anywhere; }
.work-order-code-link:hover { text-decoration: underline; }
.work-order-code-link:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
</style>
