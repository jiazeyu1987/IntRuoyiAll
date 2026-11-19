<template>
  <a v-if="normalizedCode && permitted" :href="router.resolve(to).href" class="business-code-link" @click.stop="navigate">{{ code }}</a>
  <span v-else>{{ code || emptyText }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, type RouteLocationRaw } from 'vue-router'
import { checkPermi } from '@/utils/permission'

const props = withDefaults(defineProps<{
  code?: string | null
  to: RouteLocationRaw
  permission?: string | string[]
  emptyText?: string
}>(), { emptyText: '-' })
const router = useRouter()
let visit = 0
const normalizedCode = computed(() => {
  const value = props.code?.trim()
  return value && !['-', '--', '—'].includes(value) ? value : undefined
})
const permitted = computed(() => !props.permission || checkPermi(
  Array.isArray(props.permission) ? props.permission : [props.permission]
))
const navigate = (event: MouseEvent) => {
  if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return
  event.preventDefault()
  const destination = router.resolve(props.to)
  return router.push({
    path: destination.path,
    query: { ...destination.query, codeLinkVisit: `${Date.now()}-${++visit}` },
    hash: destination.hash
  })
}
</script>

<style scoped>
.business-code-link { color: var(--el-color-primary); cursor: pointer; overflow-wrap: anywhere; }
.business-code-link:hover { text-decoration: underline; }
.business-code-link:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
</style>
