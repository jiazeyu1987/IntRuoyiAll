<template>
  <BusinessCodeLink :code="code" :empty-text="emptyText" permission="mes:md-item:query" :to="{ path: '/mes/md/item', query: { code: code?.trim() } }" />
</template>
<script setup lang="ts">
import BusinessCodeLink from './BusinessCodeLink.vue'
defineProps<{ code?: string | null; emptyText?: string }>()
</script>
