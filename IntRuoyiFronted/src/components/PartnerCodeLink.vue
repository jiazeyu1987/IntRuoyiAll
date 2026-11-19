<template>
  <BusinessCodeLink :code="code" :to="{ path: `/mes/md/${partnerType}`, query: { code: code || '' } }" :permission="`mes:md-${partnerType}:query`" />
</template>
<script setup lang="ts">
import BusinessCodeLink from './BusinessCodeLink.vue'
defineProps<{ code?: string | null; partnerType: 'vendor' | 'client' }>()
</script>
