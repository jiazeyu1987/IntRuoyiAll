<template>
  <ContentWrap>
    <div class="edhr-frontline-fill-page">
      <EdhrBatchRecordTabs active-tab="pqc" />
      <FrontlineFixedTemplatePanel mode="pqc" />
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import FrontlineFixedTemplatePanel from '@/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
import EdhrBatchRecordTabs from './EdhrBatchRecordTabs.vue'

defineOptions({ name: 'MesProEdhrBatchPqcFill' })
</script>

<style scoped>
.edhr-frontline-fill-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
