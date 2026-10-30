<template>
  <span class="business-message">
    <strong v-if="showTitle && description.title" class="business-message__title">{{ description.title }}</strong>
    <span v-if="description.body" class="business-message__body">{{ description.body }}</span>
    <span v-if="showContext && description.fields.length" class="business-message__context">
      <span v-for="field in description.fields" :key="field.label" class="business-message__field">
        <span>{{ field.label }}：</span>{{ field.value }}
      </span>
    </span>
  </span>
</template>

<script setup lang="ts">
import type { BusinessMessagePresentation } from '@/utils/businessMessagePresentation'
withDefaults(defineProps<{ description: BusinessMessagePresentation; showContext?: boolean; showTitle?: boolean }>(), {
  showContext: true,
  showTitle: true
})
</script>

<style scoped>
.business-message {
  display: block;
  line-height: 1.65;
  text-align: left;
  white-space: normal;
  overflow-wrap: anywhere;
}
.business-message__title,
.business-message__body,
.business-message__context {
  display: block;
}
.business-message__title {
  font-weight: 600;
}
.business-message__body {
  white-space: pre-wrap;
}
.business-message__context {
  margin-top: 5px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.business-message__field {
  display: block;
}
</style>
