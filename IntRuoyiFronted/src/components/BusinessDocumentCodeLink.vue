<template>
  <BusinessCodeLink :code="code" :permission="target.permission" :to="{ path: target.path, query: { code: code?.trim() || undefined, openId: documentId == null ? undefined : String(documentId) } }" />
</template>
<script setup lang="ts">
import { computed } from 'vue'
import BusinessCodeLink from './BusinessCodeLink.vue'
import { documentTargets, type DocumentType } from '@/utils/documentCodeLink'
const props = defineProps<{ code?: string | null; documentId?: number | string; documentType: DocumentType }>()
const target = computed(() => documentTargets[props.documentType])
</script>
