<template>
  <div class="governance-section">
    <div class="section-header">
      <div>
        <div class="section-title">{{ title }}</div>
        <div class="section-caption">{{ caption }}</div>
      </div>
      <div class="section-actions">
        <el-button plain @click="$emit('add')">
          <Icon icon="ep:plus" class="mr-5px" />
          {{ addButtonText }}
        </el-button>
        <el-button type="primary" :loading="saving" @click="$emit('save')">
          <Icon icon="ep:check" class="mr-5px" />
          {{ saveButtonText }}
        </el-button>
      </div>
    </div>
    <el-alert
      v-if="showRequirementWarning"
      class="mb-12px"
      :title="requirementWarningText"
      type="warning"
      :closable="false"
    />
    <el-alert
      v-if="errorMessage"
      class="mb-12px"
      :title="errorMessage"
      type="error"
      :closable="false"
    />
    <el-table v-loading="loading" :data="rules" :empty-text="emptyText">
      <el-table-column :label="departmentColumnLabel" min-width="280">
        <template #default="{ row }">
          <el-select
            v-model="row.departmentId"
            class="w-full"
            clearable
            filterable
            :placeholder="selectPlaceholder"
          >
            <el-option
              v-for="item in departmentOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column
        v-if="mediumOptions?.length"
        :label="mediumColumnLabel || '发放方式'"
        min-width="180"
      >
        <template #default="{ row }">
          <el-select
            v-model="row.distributionMedium"
            class="w-full"
            :placeholder="mediumPlaceholder || '请选择发放方式'"
          >
            <el-option
              v-for="item in mediumOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="启用" align="center" width="90">
        <template #default="{ row }">
          <el-switch v-model="row.active" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="88">
        <template #default="{ $index }">
          <el-button link type="danger" @click="$emit('remove', $index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script lang="ts" setup>
import type { CategoryDepartmentRuleDraft } from '../../categories/governance'

interface SubjectOption {
  label: string
  value: number
}

interface MediumOption {
  label: string
  value: 'PUBLIC_FOLDER' | 'PAPER'
}

defineProps<{
  title: string
  caption: string
  rules: CategoryDepartmentRuleDraft[]
  departmentOptions: SubjectOption[]
  mediumOptions?: MediumOption[]
  saving: boolean
  emptyText: string
  selectPlaceholder: string
  departmentColumnLabel: string
  mediumColumnLabel?: string
  mediumPlaceholder?: string
  addButtonText: string
  saveButtonText: string
  loading?: boolean
  showRequirementWarning?: boolean
  requirementWarningText?: string
  errorMessage?: string
}>()

defineEmits<{
  add: []
  save: []
  remove: [index: number]
}>()
</script>

<style lang="scss" scoped>
.governance-section {
  padding-top: 24px;
  border-top: 1px solid #e5ebf3;
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.section-title {
  color: #172033;
  font-size: 15px;
  font-weight: 600;
}

.section-caption {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}

.section-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

@media (max-width: 960px) {
  .section-header {
    flex-direction: column;
  }

  .section-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
