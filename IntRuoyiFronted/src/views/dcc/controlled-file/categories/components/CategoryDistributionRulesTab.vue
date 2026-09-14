<template>
  <div data-testid="dcc-distribution-rules-tab">
    <el-alert
      v-if="errorMessage"
      class="mb-12px"
      type="error"
      :closable="false"
      :title="errorMessage"
    />

    <UnifiedListTemplate
      class="category-rule-list-template"
      table-key="dcc.controlledFile.permission.distributionRules"
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
          data-user-table-key="dcc.controlledFile.permission.distributionRules"
          :data="paginatedRows"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          row-key="categoryId"
          empty-text="当前暂无分发规则"
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
            label="分发要求"
            prop="required"
            :width="getColumnWidthString('required', 130)"
            v-bind="sortColumnAttrs('required')"
          >
            <template #default="{ row }">
              <el-tag :type="getRequirementTagType(row.required, 'primary')" size="small">
                {{ formatRequirementLabel(row.required) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isColumnVisible('departmentSummary')"
            label="分发部门"
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
              <el-tag :type="row.rulesLoaded && row.activeRuleCount > 0 ? 'success' : 'info'" size="small">
                {{ row.rulesLoaded ? (row.activeRuleCount > 0 ? `启用 ${row.activeRuleCount} 条` : '未配置') : '加载中' }}
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
              <el-button link type="primary" :disabled="!row.rulesLoaded" @click="openRuleDrawer(row, 'edit')">编辑</el-button>
              <el-button link type="primary" :disabled="!row.rulesLoaded" @click="openRuleDrawer(row, 'preview')">预览</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="720px">
      <CategoryDepartmentRulesSection
        v-if="drawerMode === 'edit' && selectedCategory"
        title="分发部门规则"
        caption="配置成功发布后必须接收该类别文件的分发部门。"
        :rules="editingRules"
        :department-options="departmentOptions"
        :medium-options="distributionMediumOptions"
        :saving="saving"
        :loading="false"
        empty-text="当前类别暂无分发部门规则。"
        select-placeholder="请选择分发部门"
        department-column-label="分发部门"
        medium-column-label="发放方式"
        medium-placeholder="请选择发放方式"
        add-button-text="新增分发部门"
        save-button-text="保存分发规则"
        :show-requirement-warning="!selectedCategory.distributionRequired"
        requirement-warning-text="当前类别未开启“要求分发”，如果后续需要强制分发，请先编辑类别基础信息。"
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
        empty-text="当前类别暂无分发部门规则。"
      >
        <el-table-column label="分发部门" min-width="220">
          <template #default="{ row }">{{ formatDepartmentName(row.departmentId) }}</template>
        </el-table-column>
        <el-table-column label="发放方式" width="140">
          <template #default="{ row }">{{ formatDistributionMedium(row.distributionMedium) }}</template>
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
  getCategoryDistributionRules,
  getFileCategoryList,
  replaceCategoryDistributionRules,
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

defineOptions({ name: 'CategoryDistributionRulesTab' })

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
  rulesLoaded: boolean
}

interface SubjectOption {
  label: string
  value: number
}

interface MediumOption {
  label: string
  value: 'PUBLIC_FOLDER' | 'PAPER'
}

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const loaded = ref(false)
const loadedRuleCategoryIds = ref(new Set<number>())
const loadedCategoryRevision = ref(props.categoryRevision)
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

const distributionMediumOptions: MediumOption[] = [
  { label: '公盘目录', value: 'PUBLIC_FOLDER' },
  { label: '纸质发放', value: 'PAPER' }
]

const quickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'code', label: '类别编码', type: 'text', queryParamKey: 'code', placeholder: '请输入类别编码' },
  { key: 'name', label: '类别名称', type: 'text', queryParamKey: 'name', placeholder: '请输入类别名称' },
  {
    key: 'required',
    label: '分发要求',
    type: 'select',
    queryParamKey: 'required',
    options: [
      { label: '要求分发', value: true },
      { label: '不要求分发', value: false }
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
  { key: 'required', label: '分发要求', width: 130 },
  { key: 'departmentSummary', label: '分发部门', minWidth: 360 },
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
} = useUserTableColumns('dcc.controlledFile.permission.distributionRules', defaultColumns)

const departmentOptions = computed<SubjectOption[]>(() =>
  depts.value
    .filter((item): item is DeptVO & { id: number } => item.id !== undefined)
    .map((item) => ({ label: item.name, value: item.id }))
)
const deptNameMap = computed(() => new Map(depts.value.map((item) => [item.id, item.name])))

const formatDistributionMedium = (value?: 'PUBLIC_FOLDER' | 'PAPER') =>
  distributionMediumOptions.find((item) => item.value === value)?.label || '-'

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
      distributionMedium: rule.distributionMedium || 'PUBLIC_FOLDER',
      active: rule.active
    })
  )

const buildDepartmentSummary = (rules: CategoryDepartmentRuleDraft[]) => {
  const activeRules = rules.filter((rule) => rule.active)
  if (!activeRules.length) return '-'
  return activeRules
    .map((rule) => `${formatDepartmentName(rule.departmentId)}（${formatDistributionMedium(rule.distributionMedium)}）`)
    .join('、')
}

const rows = computed<RuleListRow[]>(() =>
  categories.value
    .filter((category): category is ControlledFileCategoryVO & { id: number } => category.id !== undefined)
    .map((category) => {
      const rulesLoaded = loadedRuleCategoryIds.value.has(category.id)
      const rules = ruleMap.value.get(category.id) || []
      return {
        categoryId: category.id,
        code: category.code,
        name: category.name,
        active: category.active,
        required: category.distributionRequired,
        ruleCount: rules.length,
        activeRuleCount: rules.filter((rule) => rule.active).length,
        departmentSummary: rulesLoaded ? buildDepartmentSummary(rules) : '加载中',
        rules,
        rulesLoaded
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
const visibleCategoryIds = computed(() => paginatedRows.value.map((row) => row.categoryId))

const drawerTitle = computed(() =>
  selectedCategory.value
    ? `${drawerMode.value === 'edit' ? '编辑' : '预览'}分发规则：${selectedCategory.value.name}`
    : '分发规则'
)

const handleQuery = () => {
  queryParams.pageNo = 1
}

const handlePagination = () => {
  void ensureVisibleRuleRowsLoaded()
}

const quickFilter = useTableQuickFilter(
  'dcc.controlledFile.permission.distributionRules',
  quickFilterDefinitions,
  queryParams,
  handleQuery
)

const loadData = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const [categoryList, deptList] = await Promise.all([getFileCategoryList(), getSimpleDeptList()])
    categories.value = categoryList
    depts.value = deptList
    ruleMap.value = new Map()
    loadedRuleCategoryIds.value = new Set<number>()
    loadedCategoryRevision.value = props.categoryRevision
    loaded.value = true
    await ensureVisibleRuleRowsLoaded()
  } catch (error) {
    errorMessage.value = resolveErrorMessage(error, '分发规则加载失败，请查看错误提示后重试。')
  } finally {
    loading.value = false
  }
}

const ensureVisibleRuleRowsLoaded = async () => {
  if (!props.active || !loaded.value) {
    return
  }
  const categoryIds = visibleCategoryIds.value.filter((categoryId) => {
    return !loadedRuleCategoryIds.value.has(categoryId)
  })
  if (!categoryIds.length) {
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const ruleEntries = await Promise.all(
      categoryIds.map(async (categoryId) => {
        const rules = await getCategoryDistributionRules(categoryId)
        return [categoryId, mapRulesToDrafts(rules)] as const
      })
    )
    const nextRuleMap = new Map(ruleMap.value)
    const nextLoadedRuleCategoryIds = new Set(loadedRuleCategoryIds.value)
    ruleEntries.forEach(([categoryId, rules]) => {
      nextRuleMap.set(categoryId, rules)
      nextLoadedRuleCategoryIds.add(categoryId)
    })
    ruleMap.value = nextRuleMap
    loadedRuleCategoryIds.value = nextLoadedRuleCategoryIds
  } catch (error) {
    errorMessage.value = resolveErrorMessage(error, '分发规则加载失败，请查看错误提示后重试。')
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
  editingRules.value.push(createDepartmentRuleDraft({ distributionMedium: 'PUBLIC_FOLDER' }))
}

const removeRule = (index: number) => {
  editingRules.value.splice(index, 1)
}

const saveRules = async () => {
  if (!selectedCategory.value) return
  const invalidRule = editingRules.value.find((rule) => !rule.departmentId)
  if (invalidRule) {
    message.warning('请先选择分发部门后再保存')
    return
  }
  saving.value = true
  drawerErrorMessage.value = ''
  try {
    const savedRules = await replaceCategoryDistributionRules(
      selectedCategory.value.id,
      buildDepartmentRulePayload(editingRules.value)
    )
    ruleMap.value = new Map(ruleMap.value).set(selectedCategory.value.id, mapRulesToDrafts(savedRules))
    loadedRuleCategoryIds.value = new Set(loadedRuleCategoryIds.value).add(selectedCategory.value.id)
    editingRules.value = mapRulesToDrafts(savedRules)
    message.success('分发规则已保存')
  } catch (error) {
    drawerErrorMessage.value = resolveErrorMessage(error, '分发规则保存失败，请查看错误提示后重试。')
  } finally {
    saving.value = false
  }
}

watch(
  () => [props.active, props.categoryRevision] as const,
  async ([active]) => {
    if (active) {
      if (!loaded.value || props.categoryRevision !== loadedCategoryRevision.value) {
        await loadData()
        return
      }
      await ensureVisibleRuleRowsLoaded()
    }
  }
)

watch(
  () =>
    [
      queryParams.pageNo,
      queryParams.pageSize,
      queryParams.code,
      queryParams.name,
      queryParams.required,
      queryParams.active
    ] as const,
  async () => {
    await ensureVisibleRuleRowsLoaded()
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
