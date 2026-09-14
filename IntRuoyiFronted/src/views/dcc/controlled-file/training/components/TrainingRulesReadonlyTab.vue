<template>
  <ContentWrap>
    <el-form
      class="training-rule-toolbar -mb-15px"
      :inline="true"
      :model="queryParams"
      label-width="82px"
      data-testid="dcc-training-rule-toolbar"
    >
      <el-form-item label="文件类别">
        <el-select
          v-model="queryParams.categoryId"
          class="!w-280px"
          clearable
          filterable
          placeholder="请选择文件类别"
        >
          <el-option
            v-for="item in activeCategoryOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" />
          查询映射
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-5px" />
          重置
        </el-button>
      </el-form-item>
      <el-form-item class="training-rule-context-item">
        <div class="training-rule-context" data-testid="dcc-training-rule-context">
          {{ trainingRuleToolbarContextText }}
        </div>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      v-if="trainingRuleWarningText"
      class="mb-16px"
      type="warning"
      :closable="false"
      :title="trainingRuleWarningText"
    />

    <el-alert
      class="mb-16px"
      data-testid="dcc-training-rule-permission-precheck"
      type="info"
      :closable="false"
      title="发布前权限预检"
      description="培训对象必须拥有 dcc:controlled-file:training:mine 菜单权限；若对象缺少该权限，发布后会生成任务但无法进入我的培训完成阅读确认。"
    />

    <el-alert
      v-if="errorMessage"
      class="mb-16px"
      type="error"
      :closable="false"
      :title="errorMessage"
    />

    <el-empty v-if="!queryParams.categoryId" description="请选择文件类别后查看培训对象映射" />

    <el-table v-else v-loading="loading" :data="mappingRows" empty-text="当前类别暂无分发部门映射">
      <el-table-column label="分发部门" min-width="180">
        <template #default="{ row }">
          {{ row.departmentName }}
        </template>
      </el-table-column>
      <el-table-column label="训练对象来源" min-width="240">
        <template #default="{ row }">
          {{ row.userSummary }}
        </template>
      </el-table-column>
      <el-table-column label="人数" align="center" width="100" prop="userCount" />
      <el-table-column label="规则状态" align="center" width="120">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'info'">
            {{ row.active ? '启用' : '关闭' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { getFileCategoryList, getCategoryDistributionRules, type ControlledFileCategoryVO } from '@/api/dcc/controlledFile/fileCategories'
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { buildDccSimpleUserLabelMap } from '../../shared/utils'
import { buildResolvedTrainingUsers, resolveTrainingPageErrorMessage } from '../presentation'

defineOptions({ name: 'TrainingRulesReadonlyTab' })

interface MappingRow {
  departmentId: number
  departmentName: string
  userSummary: string
  userCount: number
  active: boolean
}

const loading = ref(false)
const errorMessage = ref('')
const categories = ref<ControlledFileCategoryVO[]>([])
const departments = ref<DeptVO[]>([])
const users = ref<UserVO[]>([])
const mappingRows = ref<MappingRow[]>([])

const queryParams = reactive<{ categoryId?: number }>({
  categoryId: undefined
})

const activeCategoryOptions = computed(() =>
  categories.value.filter(
    (item): item is ControlledFileCategoryVO & { id: number } => item.active && item.id !== undefined
  )
)

const currentCategory = computed(() =>
  categories.value.find((item) => Number(item.id) === Number(queryParams.categoryId))
)

const deptNameMap = computed(() => new Map(departments.value.map((item) => [item.id, item.name])))
const userNameMap = computed(() => buildDccSimpleUserLabelMap(users.value as UserVO[]))
const trainingRuleToolbarContextText = computed(() => {
  if (!queryParams.categoryId) {
    return '请选择文件类别后查看培训对象映射'
  }
  const categoryName = currentCategory.value?.name || '当前类别'
  return `${categoryName} · 映射 ${mappingRows.value.length} 条 · 培训对象继承分发接收人`
})
const trainingRuleWarningText = computed(() => {
  if (!queryParams.categoryId || !currentCategory.value) {
    return ''
  }
  const warnings: string[] = []
  if (!currentCategory.value.trainingRequired) {
    warnings.push('未开启“要求培训”，发布后不会生成培训任务')
  }
  if (!currentCategory.value.distributionRequired) {
    warnings.push('未开启“要求分发”，培训对象来源为空')
  }
  return warnings.length > 0 ? `类别配置需补齐：${warnings.join('；')}` : ''
})

const loadBaseData = async () => {
  const [categoryList, deptList, userList] = await Promise.all([
    getFileCategoryList(),
    getSimpleDeptList(),
    getSimpleUserList()
  ])
  categories.value = categoryList
  departments.value = deptList
  users.value = userList
  if (!queryParams.categoryId) {
    queryParams.categoryId = activeCategoryOptions.value[0]?.id
  }
}

const loadMapping = async () => {
  if (!queryParams.categoryId) {
    mappingRows.value = []
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const distributionRules = await getCategoryDistributionRules(queryParams.categoryId)
    mappingRows.value = distributionRules.map((rule) => {
      const departmentName = deptNameMap.value.get(rule.departmentId) || `部门#${rule.departmentId}`
      const resolvedUsers = buildResolvedTrainingUsers(rule.departmentId, users.value, userNameMap.value)
      return {
        departmentId: rule.departmentId,
        departmentName,
        userSummary: resolvedUsers.length ? resolvedUsers.join('、') : '当前部门暂无可解析用户',
        userCount: resolvedUsers.length,
        active: Boolean(rule.active)
      }
    })
  } catch (error) {
    errorMessage.value = resolveTrainingPageErrorMessage(
      error,
      '培训对象映射加载失败，请查看错误提示后重试。'
    )
    mappingRows.value = []
  } finally {
    loading.value = false
  }
}

const handleQuery = async () => {
  await loadMapping()
}

const resetQuery = async () => {
  queryParams.categoryId = activeCategoryOptions.value[0]?.id
  errorMessage.value = ''
  await loadMapping()
}

onMounted(async () => {
  await loadBaseData()
  await loadMapping()
})
</script>

<style scoped>
.training-rule-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
}

.training-rule-context-item {
  margin-left: auto;
}

.training-rule-context {
  min-height: 32px;
  max-width: 520px;
  color: #4b5563;
  font-size: 13px;
  line-height: 20px;
  text-align: right;
}

@media (max-width: 768px) {
  .training-rule-context-item {
    margin-left: 0;
    width: 100%;
  }

  .training-rule-context {
    max-width: none;
    text-align: left;
  }
}
</style>
