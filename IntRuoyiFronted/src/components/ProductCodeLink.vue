<template>
  <BusinessCodeLink :code="code" :empty-text="emptyText" permission="erp:product:query" :to="{ path: '/mes/md/erp-product', query: { barCode: code?.trim() } }" />
</template>
<script setup lang="ts">
import BusinessCodeLink from './BusinessCodeLink.vue'
defineProps<{ code?: string | null; emptyText?: string }>()
</script>
