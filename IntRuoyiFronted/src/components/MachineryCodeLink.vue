<template>
  <BusinessCodeLink :code="code" :to="target" permission="mes:dv-machinery:query" :empty-text="emptyText" />
</template>
<script setup lang="ts">
import { computed } from 'vue'
import BusinessCodeLink from './BusinessCodeLink.vue'
const props = defineProps<{ code?: string | null; id?: number | string | null; emptyText?: string }>()
const target = computed(() => ({ path: "/mes/dv/machinery", query: props.id != null && String(props.id).trim() ? { openId: String(props.id) } : { code: props.code?.trim() } }))
</script>
