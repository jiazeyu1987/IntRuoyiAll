<template>
  <div class="readable-data">
    <BusinessEvidenceDetails :title="title" :source="source" />
    <details v-if="source || checksum" class="readable-data__technical">
      <summary>技术详情（原始数据与校验码）</summary>
      <p v-if="checksum">内容校验码：{{ checksum }}</p>
      <pre v-if="source">{{ source }}</pre>
    </details>
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
.readable-data__technical {
  margin-top: 12px;
  overflow-wrap: anywhere;
}
.readable-data__technical summary {
  cursor: pointer;
  color: var(--el-text-color-secondary);
}
.readable-data__technical pre {
  margin: 8px 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 12px;
}
</style>
