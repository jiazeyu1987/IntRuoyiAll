<template>
  <div data-testid="dcc-training-rules-tab">
    <el-alert
      v-if="errorMessage"
      class="mb-12px"
      type="error"
      :closable="false"
      :title="errorMessage"
    />

    <UnifiedListTemplate
      class="category-rule-list-template"
      table-key="dcc.controlledFile.permission.trainingRules"
      :query-model="queryParams"
      label-width="76px"
      :filter-definitions="quickFilterDefinitions"
      :quick-filter-state="quickFilter.state"
      :selected-filter-definition="quickFilter.selectedDefinition.value"
      :operator-options="quickFilter.operatorOptions.value"
      :columns="columns"
      :column-saving="columnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="quickFilter.updateState"
      @quick-filter-query="quickFilter.applyQuickFilter"
      @column-change="saveColumnConfig"
      @pagination="handlePagination"
    >
      <template #actions>
        <el-button type="primary" plain :loading="loading" @click="loadData">
          <Icon icon="ep:refresh" class="mr-5px" />
          刷新
        </el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="dcc.controlledFile.permission.trainingRules"
          :data="paginatedRows"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          row-key="categoryId"
          empty-text="当前暂无培训规则"
          @header-dragend="handleHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isColumnVisible('code')"
            label="类别编码"
            prop="code"
            :width="getColumnWidthString('code')"
            :min-width="getColumnMinWidthString('code', 170)"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('code')"
          />
          <el-table-column
            v-if="isColumnVisible('name')"
            label="类别名称"
            prop="name"
            :width="getColumnWidthString('name')"
            :min-width="getColumnMinWidthString('name', 220)"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('name')"
          />
          <el-table-column
            v-if="isColumnVisible('required')"
            label="培训要求"
            prop="required"
            :width="getColumnWidthString('required', 130)"
            v-bind="sortColumnAttrs('required')"
          >
            <template #default="{ row }">
              <el-tag :type="getRequirementTagType(row.required, 'warning')" size="small">
                {{ formatRequirementLabel(row.required) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isColumnVisible('departmentSummary')"
            label="培训部门"
            prop="departmentSummary"
            :width="getColumnWidthString('departmentSummary')"
            :min-width="getColumnMinWidthString('departmentSummary', 360)"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('departmentSummary')"
          />
          <el-table-column
            v-if="isColumnVisible('ruleStatus')"
            label="规则状态"
            prop="ruleStatus"
            :width="getColumnWidthString('ruleStatus', 150)"
            v-bind="sortColumnAttrs('ruleStatus')"
          >
            <template #default="{ row }">
              <el-tag :type="row.activeRuleCount > 0 ? 'success' : 'info'" size="small">
                {{ row.activeRuleCount > 0 ? `启用 ${row.activeRuleCount} 条` : '未配置' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isColumnVisible('actions')"
            label="操作"
            prop="actions"
            align="center"
            fixed="right"
            :width="getColumnWidthString('actions', 140)"
          >
            <template #default="{ row }">
              <el-button link type="primary" @click="openRuleDrawer(row, 'edit')">编辑</el-button>
              <el-button link type="primary" @click="openRuleDrawer(row, 'preview')">预览</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="680px">
      <CategoryDepartmentRulesSection
        v-if="drawerMode === 'edit' && selectedCategory"
        title="培训部门规则"
        caption="配置成功发布后必须完成该类别文件培训的部门。"
        :rules="editingRules"
        :department-options="departmentOptions"
        :saving="saving"
        :loading="false"
        empty-text="当前类别暂无培训部门规则。"
        select-placeholder="请选择培训部门"
        department-column-label="培训部门"
        add-button-text="新增培训部门"
        save-button-text="保存培训规则"
        :show-requirement-warning="!selectedCategory.trainingRequired"
        requirement-warning-text="当前类别未开启“要求培训”，如果后续需要强制培训，请先编辑类别基础信息。"
        :error-message="drawerErrorMessage"
        @add="addRule"
        @remove="removeRule"
        @save="saveRules"
      />

      <el-table
        v-else
        :data="editingRules"
        border
        :stripe="true"
        :show-overflow-tooltip="true"
        empty-text="当前类别暂无培训部门规则。"
      >
        <el-table-column label="培训部门" min-width="220">
          <template #default="{ row }">{{ formatDepartmentName(row.departmentId) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'" size="small">
              {{ row.active ? '启用' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import {
  getCategoryTrainingRules,
  getFileCategoryList,
  replaceCategoryTrainingRules,
  type ControlledFileCategoryDepartmentRuleVO,
  type ControlledFileCategoryVO
} from '@/api/dcc/controlledFile/fileCategories'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'
import CategoryDepartmentRulesSection from '../../shared/governance/CategoryDepartmentRulesSection.vue'
import { formatRequirementLabel, getRequirementTagType } from '../../shared/utils'
import {
  buildDepartmentRulePayload,
  createDepartmentRuleDraft,
  type CategoryDepartmentRuleDraft
} from '../governance'

defineOptions({ name: 'CategoryTrainingRulesTab' })

const props = withDefaults(
  defineProps<{
    active?: boolean
    categoryRevision?: number
  }>(),
  {
    active: true,
    categoryRevision: 0
  }
)

interface RuleListRow {
  categoryId: number
  code: string
  name: string
  active: boolean
  required?: boolean
  ruleCount: number
  activeRuleCount: number
  departmentSummary: string
  rules: CategoryDepartmentRuleDraft[]
}

interface SubjectOption {
  label: string
  value: number
}

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const loaded = ref(false)
const errorMessage = ref('')
const drawerErrorMessage = ref('')
const drawerVisible = ref(false)
const drawerMode = ref<'edit' | 'preview'>('edit')
const selectedCategory = ref<ControlledFileCategoryVO & { id: number }>()
const editingRules = ref<CategoryDepartmentRuleDraft[]>([])
const categories = ref<ControlledFileCategoryVO[]>([])
const depts = ref<DeptVO[]>([])
const ruleMap = ref(new Map<number, CategoryDepartmentRuleDraft[]>())

const queryParams = reactive<{
  code: string
  name: string
  required?: boolean
  active?: boolean
  pageNo: number
  pageSize: number
  quickFilter?: TableQuickFilterValue
}>({
  code: '',
  name: '',
  required: undefined,
  active: undefined,
  pageNo: 1,
  pageSize: 10,
  quickFilter: undefined
})

const quickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'code', label: '类别编码', type: 'text', queryParamKey: 'code', placeholder: '请输入类别编码' },
  { key: 'name', label: '类别名称', type: 'text', queryParamKey: 'name', placeholder: '请输入类别名称' },
  {
    key: 'required',
    label: '培训要求',
    type: 'select',
    queryParamKey: 'required',
    options: [
      { label: '要求培训', value: true },
      { label: '不要求培训', value: false }
    ]
  },
  {
    key: 'active',
    label: '启用状态',
    type: 'select',
    queryParamKey: 'active',
    options: [
      { label: '已启用', value: true },
      { label: '已停用', value: false }
    ]
  }
]

const defaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '类别编码', minWidth: 170 },
  { key: 'name', label: '类别名称', minWidth: 220 },
  { key: 'required', label: '培训要求', width: 130 },
  { key: 'departmentSummary', label: '培训部门', minWidth: 360 },
  { key: 'ruleStatus', label: '规则状态', width: 150 },
  { key: 'actions', label: '操作', width: 140, hideable: false }
]

const {
  columns,
  saving: columnSaving,
  isColumnVisible,
  getColumnWidthString,
  getColumnMinWidthString,
  handleHeaderDragend,
  saveConfig: saveColumnConfig
} = useUserTableColumns('dcc.controlledFile.permission.trainingRules', defaultColumns)

const departmentOptions = computed<SubjectOption[]>(() =>
  depts.value
    .filter((item): item is DeptVO & { id: number } => item.id !== undefined)
    .map((item) => ({ label: item.name, value: item.id }))
)
const deptNameMap = computed(() => new Map(depts.value.map((item) => [item.id, item.name])))

const formatDepartmentName = (departmentId?: number) =>
  departmentId ? deptNameMap.value.get(departmentId) || `部门#${departmentId}` : '-'

const resolveErrorMessage = (error: unknown, messageText: string) => {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return error
  }
  return messageText
}

const mapRulesToDrafts = (
  rules: ControlledFileCategoryDepartmentRuleVO[]
): CategoryDepartmentRuleDraft[] =>
  rules.map((rule) =>
    createDepartmentRuleDraft({
      departmentId: rule.departmentId,
      active: rule.active
    })
  )

const buildDepartmentSummary = (rules: CategoryDepartmentRuleDraft[]) => {
  const activeRules = rules.filter((rule) => rule.active)
  if (!activeRules.length) return '-'
  return activeRules.map((rule) => formatDepartmentName(rule.departmentId)).join('、')
}

const rows = computed<RuleListRow[]>(() =>
  categories.value
    .filter((category): category is ControlledFileCategoryVO & { id: number } => category.id !== undefined)
    .map((category) => {
      const rules = ruleMap.value.get(category.id) || []
      return {
        categoryId: category.id,
        code: category.code,
        name: category.name,
        active: category.active,
        required: category.trainingRequired,
        ruleCount: rules.length,
        activeRuleCount: rules.filter((rule) => rule.active).length,
        departmentSummary: buildDepartmentSummary(rules),
        rules
      }
    })
)

const filteredRows = computed(() => {
  const code = queryParams.code.trim().toLowerCase()
  const name = queryParams.name.trim().toLowerCase()
  return rows.value.filter((row) => {
    const codeMatch = !code || row.code.toLowerCase().includes(code)
    const nameMatch = !name || row.name.toLowerCase().includes(name)
    const requiredMatch = queryParams.required === undefined || row.required === queryParams.required
    const activeMatch = queryParams.active === undefined || row.active === queryParams.active
    return codeMatch && nameMatch && requiredMatch && activeMatch
  })
})

const total = computed(() => filteredRows.value.length)
const paginatedRows = computed(() => {
  const start = (queryParams.pageNo - 1) * queryParams.pageSize
  return filteredRows.value.slice(start, start + queryParams.pageSize)
})

const drawerTitle = computed(() =>
  selectedCategory.value
    ? `${drawerMode.value === 'edit' ? '编辑' : '预览'}培训规则：${selectedCategory.value.name}`
    : '培训规则'
)

const handleQuery = () => {
  queryParams.pageNo = 1
}

const handlePagination = () => undefined

const quickFilter = useTableQuickFilter(
  'dcc.controlledFile.permission.trainingRules',
  quickFilterDefinitions,
  queryParams,
  handleQuery
)

const loadData = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const [categoryList, deptList] = await Promise.all([getFileCategoryList(), getSimpleDeptList()])
    const activeCategories = categoryList.filter(
      (category): category is ControlledFileCategoryVO & { id: number } => category.id !== undefined
    )
    const ruleEntries = await Promise.all(
      activeCategories.map(async (category) => {
        const rules = await getCategoryTrainingRules(category.id)
        return [category.id, mapRulesToDrafts(rules)] as const
      })
    )
    categories.value = categoryList
    depts.value = deptList
    ruleMap.value = new Map(ruleEntries)
    loaded.value = true
  } catch (error) {
    errorMessage.value = resolveErrorMessage(error, '培训规则加载失败，请查看错误提示后重试。')
  } finally {
    loading.value = false
  }
}

const openRuleDrawer = (row: RuleListRow, mode: 'edit' | 'preview') => {
  const category = categories.value.find(
    (item): item is ControlledFileCategoryVO & { id: number } => item.id === row.categoryId
  )
  if (!category) return
  selectedCategory.value = category
  drawerMode.value = mode
  drawerErrorMessage.value = ''
  editingRules.value = row.rules.map((rule) => createDepartmentRuleDraft(rule))
  drawerVisible.value = true
}

const addRule = () => {
  editingRules.value.push(createDepartmentRuleDraft())
}

const removeRule = (index: number) => {
  editingRules.value.splice(index, 1)
}

const saveRules = async () => {
  if (!selectedCategory.value) return
  const invalidRule = editingRules.value.find((rule) => !rule.departmentId)
  if (invalidRule) {
    message.warning('请先选择培训部门后再保存')
    return
  }
  saving.value = true
  drawerErrorMessage.value = ''
  try {
    const savedRules = await replaceCategoryTrainingRules(
      selectedCategory.value.id,
      buildDepartmentRulePayload(editingRules.value)
    )
    ruleMap.value = new Map(ruleMap.value).set(selectedCategory.value.id, mapRulesToDrafts(savedRules))
    editingRules.value = mapRulesToDrafts(savedRules)
    message.success('培训规则已保存')
  } catch (error) {
    drawerErrorMessage.value = resolveErrorMessage(error, '培训规则保存失败，请查看错误提示后重试。')
  } finally {
    saving.value = false
  }
}

watch(
  () => [props.active, props.categoryRevision] as const,
  async ([active]) => {
    if (active) {
      await loadData()
    }
  }
)

onMounted(async () => {
  if (props.active) {
    await loadData()
  }
})
</script>

<style scoped>
.category-rule-list-template {
  display: block;
}
</style>
