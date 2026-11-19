<template>
  <BusinessCodeLink :code="code" :to="{ path: `/mes/qc/${kind}`, query: { code: code?.trim() } }" :permission="`mes:qc-${kind}:query`" />
</template>
<script setup lang="ts">
import BusinessCodeLink from './BusinessCodeLink.vue'
defineProps<{ code?: string | null; kind: 'iqc' | 'ipqc' | 'oqc' | 'rqc' }>()
</script>
