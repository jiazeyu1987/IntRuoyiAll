<template>
  <div class="parser-trial-navigation">
    <el-button :disabled="index <= 0" @click="emit('select', index - 1)">上一道工序</el-button>
    <el-select :model-value="index" aria-label="选择工序" @change="emit('select', $event)">
      <el-option
        v-for="(name, i) in names"
        :key="i"
        :value="i"
        :label="`${i + 1}. ${name || '未命名工序'}`"
      />
    </el-select>
    <el-button :disabled="index < 0 || index >= names.length - 1" @click="emit('select', index + 1)"
      >下一道工序</el-button
    >
    <el-button @click="emit('edit')">返回编辑</el-button>
    <span>试填内容仅用于预览，不会修改编辑内容或生成正式记录。</span>
  </div>
</template>
<script setup lang="ts">
defineProps<{ index: number; names: string[] }>()
const emit = defineEmits<{ select: [index: number]; edit: [] }>()
</script>
<style scoped>
.parser-trial-navigation {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
}
.parser-trial-navigation .el-select {
  width: 240px;
}
.parser-trial-navigation span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.parser-trial-navigation .el-button + .el-button {
  margin-left: 0;
}
</style>
