<template>
  <div class="sampling-rule-editor">
    <el-select :model-value="inspectionLevel" placeholder="检验水平" aria-label="巡检检验水平"
      :disabled="disabled" @update:model-value="$emit('update:inspectionLevel', $event)">
      <el-option v-for="level in levels" :key="level" :value="level"
        :label="level.startsWith('S') ? `S-${level.slice(1)}` : level" />
    </el-select>
    <span>AQL</span>
    <el-input-number :model-value="aql" :min="0.001" :precision="3" :step="0.1"
      controls-position="right" placeholder="AQL" aria-label="AQL" :disabled="disabled"
      @update:model-value="$emit('update:aql', $event ?? undefined)" />
  </div>
</template>
<script setup lang="ts">
defineProps<{ inspectionLevel?: string; aql?: number; disabled?: boolean }>()
defineEmits<{ 'update:inspectionLevel': [value: string]; 'update:aql': [value: number | undefined] }>()
const levels = ['I', 'II', 'III', 'S1', 'S2', 'S3', 'S4']
</script>
<style scoped>
.sampling-rule-editor { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.sampling-rule-editor .el-select { width: 110px; }
.sampling-rule-editor .el-input-number { width: 140px; }
</style>
