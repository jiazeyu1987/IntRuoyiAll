<template>
  <BusinessCodeLink :code="code" :to="target" permission="mes:wm-warehouse:query" />
</template>
<script setup lang="ts">
import { computed } from 'vue'
import BusinessCodeLink from './BusinessCodeLink.vue'
const props = defineProps<{ code?: string | null; storageType: 'warehouse' | 'location' | 'area'; storageId?: number | string }>()
// 本系统 location 为库区，area 为库位。
const target = computed(() => ({
  path: `/mes/wm/warehouse${props.storageType === 'warehouse' ? '' : '/' + props.storageType}`,
  query: props.storageId ? { id: String(props.storageId) } : { code: props.code?.trim() }
}))
</script>
