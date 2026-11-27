<template>
  <div class="readable-data">
    <BusinessEvidenceDetails :title="title" :source="source" />
    <p v-if="checksum" class="readable-data__checksum">内容校验码：{{ checksum }}</p>
  </div>
</template>

<script setup lang="ts">
import BusinessEvidenceDetails from '@/views/mes/pro/processpool/components/BusinessEvidenceDetails.vue'

withDefaults(defineProps<{ title?: string; source?: string; checksum?: string }>(), {
  title: '业务明细'
})
</script>

<style scoped>
.readable-data {
  width: 100%;
  min-width: 0;
  max-height: 420px;
  overflow: auto;
}
.readable-data__checksum {
  margin-top: 12px;
  overflow-wrap: anywhere;
}
</style>
