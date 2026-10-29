<template>
  <section class="business-evidence" :aria-label="title">
    <strong>{{ title }}</strong>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <template v-else>
      <div class="business-evidence__table">
        <table v-if="rows.length">
          <thead
            ><tr
              ><th scope="col">业务信息</th
              ><th scope="col">{{ compare ? '修改前' : '记录内容' }}</th
              ><th v-if="compare" scope="col">修改后</th></tr
            ></thead
          >
          <tbody
            ><tr v-for="row in rows" :key="row.key">
              <th scope="row">{{ row.label }}</th
              ><td>{{ row.value }}</td
              ><td v-if="compare">{{ row.after }}</td>
            </tr></tbody
          >
        </table>
        <span v-else>未记录</span>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { readBusinessEvidence } from './businessEvidenceDisplay'

const props = defineProps<{
  title: string
  field?: string
  source?: string
  afterSource?: string
  compare?: boolean
}>()
const before = computed(() => readBusinessEvidence(props.source, props.field))
const after = computed(() =>
  props.compare ? readBusinessEvidence(props.afterSource, props.field) : { rows: [], error: '' }
)
const error = computed(() => [before.value.error, after.value.error].filter(Boolean).join(' '))
const rows = computed(() => {
  const left = new Map(before.value.rows.map((row) => [row.key, row]))
  const right = new Map(after.value.rows.map((row) => [row.key, row]))
  return [...new Set([...left.keys(), ...right.keys()])].map((key) => ({
    key,
    label: (left.get(key) || right.get(key))!.label,
    value: left.get(key)?.value ?? '未记录该字段',
    after: right.get(key)?.value ?? '未记录该字段'
  }))
})
</script>

<style scoped>
.business-evidence {
  width: 100%;
  min-width: 0;
  text-align: left;
}
.business-evidence__table {
  overflow-x: auto;
  margin-top: 8px;
}
.business-evidence table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}
.business-evidence th,
.business-evidence td {
  padding: 8px 12px;
  border: 1px solid var(--el-border-color);
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  text-align: left;
}
.business-evidence th {
  background: var(--el-fill-color-light);
  font-weight: 500;
}
</style>
