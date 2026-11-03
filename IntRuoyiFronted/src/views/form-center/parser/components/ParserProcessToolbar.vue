<template>
  <div class="parser-process-toolbar">
    <el-button :disabled="disabled || !canUndo" @click="emit('undo')">撤销</el-button>
    <el-button :disabled="disabled || index <= 0" @click="emit('move', -1)">工序上移</el-button>
    <el-button :disabled="disabled || index < 0 || index >= count - 1" @click="emit('move', 1)"
      >工序下移</el-button
    >
    <el-button type="danger" plain :disabled="disabled || index < 0" @click="emit('remove')"
      >删除工序</el-button
    >
  </div>
</template>
<script setup lang="ts">
defineProps<{ index: number; count: number; canUndo: boolean; disabled: boolean }>()
const emit = defineEmits<{ undo: []; move: [offset: number]; remove: [] }>()
</script>
<style scoped>
.parser-process-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 12px;
}
.parser-process-toolbar .el-button + .el-button {
  margin-left: 0;
}
</style>
