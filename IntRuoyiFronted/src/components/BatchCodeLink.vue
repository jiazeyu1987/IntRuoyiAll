<template>
  <template v-if="value && permitted">
    <el-link v-if="batchId" type="primary" @click.stop="openDetail">{{ code }}</el-link>
    <a v-else :href="router.resolve({ path: '/mes/qc/batch-trace', query: { code: value } }).href" class="batch-code-link" @click.stop="navigate">{{ code }}</a>
    <BatchForm v-if="detailMounted" ref="detailRef" />
  </template>
  <span v-else>{{ code || '-' }}</span>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()
let visit = 0
import { checkPermi } from '@/utils/permission'
import BatchForm from '@/views/mes/wm/batch/BatchForm.vue'
const props = defineProps<{ code?: string | null; batchId?: number }>()
const value = computed(() => {
  const code = props.code?.trim()
  return code && !['-', '--', '—'].includes(code) ? code : undefined
})
const permitted = computed(() => checkPermi(['mes:wm-batch:query']))
const detailRef = ref()
const detailMounted = ref(false)
const navigate = (event: MouseEvent) => {
  if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return
  event.preventDefault()
  return router.push({ path: '/mes/qc/batch-trace', query: { code: value.value, codeLinkVisit: `${Date.now()}-${++visit}` } })
}
const openDetail = async () => {
  detailMounted.value = true
  await nextTick()
  await detailRef.value.open(props.batchId)
}
</script>

<style scoped>
.batch-code-link { color: var(--el-color-primary); overflow-wrap: anywhere; }
.batch-code-link:hover { text-decoration: underline; }
.batch-code-link:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
</style>
