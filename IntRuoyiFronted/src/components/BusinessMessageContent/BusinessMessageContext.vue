<template>
  <details v-if="fields.length" class="message-context">
    <summary>
      <span v-for="field in fields.filter(item => ['工序', '发起人'].includes(item.label))" :key="field.label" class="message-context__brief">
        {{ field.label }}：{{ field.value }}
      </span>
      <span>查看关联信息</span>
    </summary>
    <div v-for="field in fields" :key="field.label" class="message-context__field">
      {{ field.label }}：{{ field.value }}
    </div>
  </details>
</template>

<script setup lang="ts">
import type { BusinessMessagePresentation } from '@/utils/businessMessagePresentation'
defineProps<{ fields: BusinessMessagePresentation['fields'] }>()
</script>

<style scoped>
.message-context {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.65;
  text-align: left;
}
.message-context summary {
  cursor: pointer;
}
.message-context summary:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}
.message-context__brief {
  margin-right: 14px;
}
.message-context__field {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
