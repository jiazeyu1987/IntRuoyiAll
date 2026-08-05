<template>
  <ContentWrap>
    <div class="edhr-frontline-fill-page">
      <header class="edhr-frontline-fill-page__header" data-edhr-frontline-production-page-title>
        <div>
          <p>一线生产</p>
          <span>按活跃订单、工序和设备填写一线生产记录。</span>
        </div>
      </header>
      <FrontlineFixedTemplatePanel mode="production" />
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import FrontlineFixedTemplatePanel from '@/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'

defineOptions({ name: 'MesProEdhrBatchProductionFill' })
</script>

<style scoped>
.edhr-frontline-fill-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-frontline-fill-page__header {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  padding: 14px 18px;
}

.edhr-frontline-fill-page__header p {
  margin: 0;
  color: #1f2d3d;
  font-size: 18px;
  font-weight: 700;
}

.edhr-frontline-fill-page__header span {
  display: block;
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}
</style>
