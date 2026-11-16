<template>
  <div class="import-result-content">
    <p class="import-result-content__summary">{{ result.summary }}</p>
    <dl v-if="visibleStats.length" class="import-result-content__stats">
      <div v-for="stat in visibleStats" :key="stat.label">
        <dt>{{ stat.label }}</dt>
        <dd>{{ stat.value }}</dd>
      </div>
    </dl>
    <p v-if="result.guidance" class="import-result-content__guidance">{{ result.guidance }}</p>
    <details
      v-for="section in visibleSections"
      :key="section.title"
      :open="section.expanded"
      class="import-result-content__section"
    >
      <summary>{{ section.title }}（{{ section.rows.length }} 条）</summary>
      <div class="import-result-content__table">
        <table>
          <thead>
            <tr
              ><th v-for="column in section.columns" :key="column" scope="col">{{ column }}</th></tr
            >
          </thead>
          <tbody>
            <tr v-for="(row, index) in section.rows" :key="index">
              <td v-for="(cell, cellIndex) in row" :key="cellIndex">{{ cell }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </details>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ImportResultOptions } from '@/utils/importResult'

const props = defineProps<{ result: ImportResultOptions }>()
const visibleStats = computed(() => props.result.stats.filter((stat) => stat.value !== 0))
const visibleSections = computed(
  () => props.result.sections?.filter((section) => section.rows.length > 0) ?? []
)
</script>

<style>
.import-result-message-box {
  width: 680px;
  max-width: calc(100vw - 32px);
}
.import-result-message-box .el-message-box__message {
  min-width: 0;
  width: 100%;
}
.import-result-message-box .el-message-box__container {
  align-items: flex-start;
}
.import-result-message-box .el-message-box__status {
  flex-shrink: 0;
  margin-top: 3px;
}
</style>

<style scoped>
.import-result-content {
  max-height: 65vh;
  overflow: auto;
  overflow-wrap: anywhere;
  line-height: 1.65;
  text-align: left;
}
.import-result-content__summary {
  margin: 0 0 12px;
}
.import-result-content__stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
  gap: 12px;
  margin: 0 0 12px;
}
.import-result-content__stats dt {
  color: var(--el-text-color-secondary);
}
.import-result-content__stats dd {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
.import-result-content__guidance {
  margin: 12px 0;
}
.import-result-content__section {
  margin-top: 12px;
}
.import-result-content__section summary {
  cursor: pointer;
  font-weight: 600;
}
.import-result-content__section summary:focus-visible {
  outline: 2px solid var(--el-color-primary);
}
.import-result-content__table {
  max-height: 260px;
  overflow: auto;
  margin-top: 8px;
}
table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}
th,
td {
  padding: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  vertical-align: top;
  white-space: pre-wrap;
}
th {
  position: sticky;
  top: 0;
  background: var(--el-bg-color);
  text-align: left;
}
</style>
