<template>
  <div class="tree-expand-actions">
    <el-button text type="primary" size="small" @click="$emit('expand')">全部展开</el-button>
    <el-button text type="primary" size="small" @click="$emit('collapse')">全部折叠</el-button>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'TreeExpandActions' })

defineEmits<{
  expand: []
  collapse: []
}>()
</script>

<style scoped lang="scss">
.tree-expand-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}
</style>
