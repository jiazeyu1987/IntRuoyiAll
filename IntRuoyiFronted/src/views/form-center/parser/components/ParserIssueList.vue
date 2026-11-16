<template>
  <section class="parser-issues" aria-label="待修正问题">
    <h3>待修正问题（{{ issues.length }}）</h3>
    <p v-if="!issues.length">检查通过，可以确认发布。</p>
    <div v-for="(issue, index) in issues" :key="issueKey ? issueKey(issue) : index" class="parser-issue">
      <span>{{ issue.message }}</span>
      <slot name="action" :issue="issue" :index="index">
        <el-button
          v-if="issue.locatable !== false"
          link
          type="primary"
          :disabled="disabled"
          @click="emit('locate', index)"
          >定位修改</el-button
        >
      </slot>
    </div>
  </section>
</template>
<script setup lang="ts" generic="T extends { message: string; locatable?: boolean }">
defineProps<{ issues: T[]; disabled?: boolean; issueKey?: (issue: T) => string }>()
const emit = defineEmits<{ locate: [index: number] }>()
</script>
<style scoped>
.parser-issues {
  margin-top: 20px;
  padding: 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
}
h3 {
  margin: 0 0 8px;
}
p {
  margin: 0;
  color: var(--el-text-color-secondary);
}
.parser-issue {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 6px 0;
  color: var(--el-color-warning-dark-2);
}
</style>
