<template>
  <el-drawer
    v-model="drawerVisible"
    class="showroom-version-diff-drawer"
    destroy-on-close
    size="720px"
    title="字段差异"
  >
    <template v-if="revision">
      <div class="showroom-version-diff-drawer__summary">
        <div>
          <span class="showroom-version-diff-drawer__label">对象</span>
          <strong>{{ revision.targetLabel }}</strong>
        </div>
        <div>
          <span class="showroom-version-diff-drawer__label">版本</span>
          <strong>V{{ revision.revisionNo }}</strong>
        </div>
        <div>
          <span class="showroom-version-diff-drawer__label">状态</span>
          <el-tag :type="statusTagType">
            {{ statusText }}
          </el-tag>
        </div>
      </div>

      <el-table :data="revision.diffItems" border>
        <el-table-column label="字段" min-width="150" prop="label" />
        <el-table-column label="旧值" min-width="220">
          <template #default="{ row }">
            <span>{{ formatHistoryDiffValue(row.oldValue) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="新值" min-width="220">
          <template #default="{ row }">
            <span>{{ formatHistoryDiffValue(row.newValue) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" prop="operatorAction" />
        <el-table-column label="时间" min-width="180" prop="createdAt" :formatter="dateTimeValueFormatter" />
      </el-table>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { formatHistoryDiffValue, type VersionHistoryRecord } from './contracts'
import { dateTimeValueFormatter } from '@/utils/formatTime'

defineOptions({ name: 'VersionDiffDrawer' })

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    revision: VersionHistoryRecord | null
    statusText: string
    statusTagType: 'success' | 'warning' | 'info' | 'danger'
  }>(),
  {
    statusText: '',
    statusTagType: 'info'
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const drawerVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
</script>

<style scoped>
.showroom-version-diff-drawer__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-version-diff-drawer__label {
  display: block;
  margin-bottom: 6px;
  color: #4b5563;
  font-size: 0.85rem;
}
</style>
