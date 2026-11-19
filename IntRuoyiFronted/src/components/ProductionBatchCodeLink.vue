<template>
  <a v-if="value && permitted" :href="router.resolve(target).href" class="production-batch-code-link" @click.stop="navigate">{{ code }}</a>
  <span v-else>{{ code || '-' }}</span>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { checkPermi } from '@/utils/permission'
const router = useRouter()
let visit = 0
const props = defineProps<{ code?: string | null; batchExecutionId?: number | string }>()
const value = computed(() => {
  const code = props.code?.trim()
  return code && !['-', '--', '—'].includes(code) ? code : undefined
})
const permitted = computed(() => checkPermi([props.batchExecutionId
  ? 'mes:pro-edhr-batch-execution:query' : 'mes:pro-work-order:query']))
const target = computed(() => props.batchExecutionId
  ? { path: '/mes/pro/feedback/edhr-batch-execution/detail', query: { id: String(props.batchExecutionId) } }
  : { path: '/mes/pro/work-order', query: { batchCode: value.value } })
const navigate = (event: MouseEvent) => {
  if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button !== 0) return
  event.preventDefault()
  return router.push({
    ...target.value, query: { ...target.value.query, ...(props.batchExecutionId ? { codeLinkVisit: `${Date.now()}-${++visit}` } : { detailVisit: `${Date.now()}-${++visit}` }) }
  })
}
</script>
<style scoped>
.production-batch-code-link { color: var(--el-color-primary); overflow-wrap: anywhere; }
.production-batch-code-link:hover { text-decoration: underline; }
.production-batch-code-link:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
</style>
