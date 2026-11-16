<template>
  <div class="tree-expand-actions">
    <el-tooltip content="全部展开" placement="top">
      <el-button text type="primary" size="small" aria-label="全部展开" @click="$emit('expand')">
        <Icon icon="ep:expand" />
      </el-button>
    </el-tooltip>
    <el-tooltip content="全部折叠" placement="top">
      <el-button text type="primary" size="small" aria-label="全部折叠" @click="$emit('collapse')">
        <Icon icon="ep:fold" />
      </el-button>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@/components/Icon'

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
  flex-shrink: 0;

  :deep(.el-button) {
    width: 28px;
    height: 28px;
    padding: 0;
    margin-left: 0;
  }
}
</style>
