<template>
  <BusinessCodeLink :code="code" :to="{ path: '/mes/pro/process', query: { code: code || undefined, openId: processId ? String(processId) : undefined } }" permission="mes:pro-process:query" :empty-text="emptyText" />
</template>
<script setup lang="ts">
import BusinessCodeLink from '@/components/BusinessCodeLink.vue'
defineProps<{ code?: string | null; processId?: number | string | null; emptyText?: string }>()
</script>
