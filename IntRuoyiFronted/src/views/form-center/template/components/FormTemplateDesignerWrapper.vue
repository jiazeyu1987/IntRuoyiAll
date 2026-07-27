<template>
  <ContentWrap :body-style="{ padding: '0px' }" class="!mb-0 form-template-designer-wrapper">
    <slot></slot>
  </ContentWrap>
</template>

<script setup lang="ts">
defineOptions({ name: 'FormCenterTemplateDesignerWrapper' })
</script>

<style scoped>
.form-template-designer-wrapper {
  border: none;
  background: transparent;
}

.form-template-designer-wrapper :deep(.el-card__body) {
  padding: 0 !important;
}
</style>
