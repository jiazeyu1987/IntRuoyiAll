<template>
  <BusinessCodeLink v-if="batchExecutionId" :code="code" :to="{ path: '/mes/pro/feedback/edhr-batch-execution/detail', query: { id: String(batchExecutionId) } }" permission="mes:pro-edhr-batch-execution:query" />
  <span v-else>{{ code || '--' }}</span>
</template>
<script setup lang="ts">
import BusinessCodeLink from './BusinessCodeLink.vue'
defineProps<{ code?: string | null; batchExecutionId?: number | string | null }>()
</script>
