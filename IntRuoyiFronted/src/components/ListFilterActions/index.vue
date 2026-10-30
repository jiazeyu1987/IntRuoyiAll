<template>
  <div class="list-filter-actions" role="group" aria-label="筛选操作">
    <el-button
      v-bind="queryButtonAttrs"
      type="primary"
      native-type="button"
      :loading="loading"
      @click="$emit('query')"
    >
      <Icon icon="ep:search" class="mr-5px" />
      查询
    </el-button>
    <el-button native-type="button" @click="$emit('reset')">
      <Icon icon="ep:refresh-left" class="mr-5px" />
      重置
    </el-button>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ListFilterActions' })

defineProps<{
  loading?: boolean
  queryButtonAttrs?: Record<string, unknown>
}>()

defineEmits<{
  query: []
  reset: []
}>()
</script>

<style scoped>
.list-filter-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.list-filter-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
