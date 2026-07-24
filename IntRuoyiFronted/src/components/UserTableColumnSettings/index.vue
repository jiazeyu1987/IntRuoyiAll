<template>
  <div class="user-table-column-settings">
    <el-popover trigger="click" placement="bottom-end" width="260">
      <template #reference>
        <el-button plain>
          <Icon icon="ep:setting" class="mr-5px" />
          {{ buttonLabel }}
        </el-button>
      </template>
      <div class="user-table-column-settings__panel">
        <div class="user-table-column-settings__title">显示字段</div>
        <div class="user-table-column-settings__hint">至少保留 1 个业务字段</div>
        <el-checkbox
          v-for="column in columns"
          :key="column.key"
          :model-value="column.visible"
          :disabled="saving || column.hideable === false"
          @change="(value) => handleToggle(column.key, Boolean(value))"
        >
          {{ column.label }}
        </el-checkbox>
      </div>
    </el-popover>
    <el-button v-if="showReset" plain :loading="saving" @click="$emit('reset')">重置列</el-button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { UserTableColumnState } from '@/hooks/web/useUserTableColumns'

const props = withDefaults(defineProps<{
  columns: UserTableColumnState[]
  saving?: boolean
  showReset?: boolean
  buttonLabel?: string
}>(), {
  showReset: true,
  buttonLabel: '显示字段'
})

const showReset = computed(() => props.showReset !== false)
const buttonLabel = computed(() => props.buttonLabel || '显示字段')

const emit = defineEmits<{
  reset: []
  change: [columns: UserTableColumnState[]]
}>()

const handleToggle = (key: string, visible: boolean) => {
  const target = props.columns.find((column) => column.key === key)
  if (!target || target.hideable === false) return
  const businessVisibleCount = props.columns.filter(
    (column) => column.business !== false && column.visible && column.key !== key
  ).length
  if (!visible && target.business !== false && businessVisibleCount < 1) {
    ElMessage.warning('至少保留 1 个业务字段')
    return
  }
  target.visible = visible
  emit('change', props.columns)
}
</script>

<style scoped>
.user-table-column-settings {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.user-table-column-settings__panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-table-column-settings__title {
  font-weight: 600;
  color: #172033;
}

.user-table-column-settings__hint {
  color: #6b7280;
  font-size: 12px;
}
</style>
