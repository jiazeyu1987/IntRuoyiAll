<template>
  <RouterLink
    v-if="barcode"
    :to="{ path: '/mes/md/erp-product', query: { barCode: barcode } }"
    class="product-code-link"
    @click.stop
  >{{ code }}</RouterLink>
  <span v-else>{{ code || emptyText }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  code?: string | null
  emptyText?: string
}>()
const barcode = computed(() => {
  const value = props.code?.trim()
  return value && !['-', '--', '—'].includes(value) ? value : undefined
})
</script>

<style scoped>
.product-code-link {
  color: var(--el-color-primary);
  cursor: pointer;
  overflow-wrap: anywhere;
}

.product-code-link:hover {
  text-decoration: underline;
}

.product-code-link:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}
</style>
