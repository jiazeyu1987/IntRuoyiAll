<template>
  <BusinessCodeLink :code="code" :to="{ path: '/mes/pro/route', query: { code: code || undefined, openId: routeId ? String(routeId) : undefined } }" permission="mes:pro-route:query" :empty-text="emptyText" />
</template>
<script setup lang="ts">
import BusinessCodeLink from '@/components/BusinessCodeLink.vue'
defineProps<{ code?: string | null; routeId?: number | string | null; emptyText?: string }>()
</script>
