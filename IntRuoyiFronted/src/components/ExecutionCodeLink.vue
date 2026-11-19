<template>
  <BusinessCodeLink v-if="executionId" :code="code" :to="{ path: '/mes/pro/feedback/edhr-execution/form', query: { id: String(executionId), viewMode: 'tracking' } }" permission="mes:pro-batch-record-execution:query" />
  <span v-else>{{ code || '--' }}</span>
</template>
<script setup lang="ts">
import BusinessCodeLink from './BusinessCodeLink.vue'
defineProps<{ code?: string | null; executionId?: number | string | null }>()
</script>
