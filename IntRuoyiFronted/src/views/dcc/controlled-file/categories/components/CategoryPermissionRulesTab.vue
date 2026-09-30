<template>
  <div data-testid="dcc-category-permission-rules-tab">
    <el-alert
      v-if="errorMessage"
      class="mb-12px"
      type="error"
      :closable="false"
      :title="errorMessage"
    />

    <div class="mb-12px flex items-center gap-8px">
      <el-input
        v-model="keyword"
        clearable
        class="!w-280px"
        placeholder="筛选类别编码或名称"
      />
      <el-button type="primary" plain :loading="loading" @click="loadCategories">
        <Icon icon="ep:refresh" class="mr-5px" />
        刷新
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="filteredCategories"
      border
      :stripe="true"
      :show-overflow-tooltip="true"
      row-key="id"
      empty-text="暂无文件类别"
    >
      <el-table-column label="类别编码" prop="code" min-width="180" />
      <el-table-column label="类别名称" prop="name" min-width="220" />
      <el-table-column label="类别状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'info'" size="small">
            {{ row.active ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right" align="center">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :disabled="!row.id"
            @click="openRules(row)"
            v-hasPermi="['dcc:controlled-file:category:manage']"
          >
            维护权限
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer
      v-model="drawerVisible"
      :title="drawerTitle"
      size="880px"
      data-testid="dcc-category-permission-rules-drawer"
    >
      <div v-loading="rulesLoading" class="flex flex-col gap-12px">
        <el-alert
          v-if="matrixRules.length"
          type="info"
          :closable="false"
          :title="`审核矩阵管理 ${matrixRules.length} 条审核/批准规则；这些规则在此只读展示，不由本页替换。`"
        />

        <div class="flex items-center justify-between gap-12px">
          <el-tag type="info" effect="plain">
            当前可配置规则 {{ editingRules.length }} 条
          </el-tag>
          <div class="flex gap-8px">
            <el-button
              type="primary"
              plain
              :disabled="rulesLoading || saving"
              @click="addRule"
            >
              <Icon icon="ep:plus" class="mr-5px" />
              新增规则
            </el-button>
            <el-button
              type="primary"
              :loading="saving"
              :disabled="rulesLoading"
              @click="saveRules"
              v-hasPermi="['dcc:controlled-file:category:manage']"
            >
              <Icon icon="ep:check" class="mr-5px" />
              保存权限规则
            </el-button>
          </div>
        </div>

        <el-table
          :data="editingRules"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          row-key="localId"
          empty-text="当前类别暂无可配置权限规则"
        >
          <el-table-column label="权限动作" min-width="150">
            <template #default="{ row }">
              <el-select v-model="row.actionType" class="w-full">
                <el-option
                  v-for="option in configurableActionOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="主体类型" width="145">
            <template #default="{ row }">
              <el-select
                v-model="row.subjectType"
                class="w-full"
                @change="row.subjectId = undefined"
              >
                <el-option
                  v-for="option in subjectTypeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="授权对象" min-width="220">
            <template #default="{ row }">
              <el-select
                v-model="row.subjectId"
                class="w-full"
                clearable
                filterable
                placeholder="请选择授权对象"
              >
                <el-option
                  v-for="option in subjectOptions(row.subjectType)"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="数据范围" width="145">
            <template #default="{ row }">
              <el-select v-model="row.scopeType" class="w-full">
                <el-option label="全局" value="GLOBAL" />
                <el-option label="产品组" value="PRODUCT_GROUP" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="启用" width="82" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.active" />
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.remark" maxlength="200" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="78" fixed="right" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeRule($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import { getSimplePostList, type PostVO } from '@/api/system/post'
import { getSimpleRoleList, type RoleVO } from '@/api/system/role'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import {
  getCategoryPermissionRules,
  getFileCategoryList,
  replaceCategoryPermissionRules,
  type ControlledFileCategoryPermissionAction,
  type ControlledFileCategoryPermissionRuleVO,
  type ControlledFileCategoryPermissionSubjectType,
  type ControlledFileCategoryVO
} from '@/api/dcc/controlledFile/fileCategories'
import { DCC_CATEGORY_PERMISSION_OPTIONS } from '../../shared/lifecycle'

defineOptions({ name: 'CategoryPermissionRulesTab' })

const CATEGORY_PERMISSION_RULE_MATRIX_ACTIONS = new Set<string>(['REVIEW', 'APPROVE'])
type ConfigurablePermissionAction = Exclude<ControlledFileCategoryPermissionAction, 'REVIEW' | 'APPROVE'>
type PermissionRuleDraft = {
  localId: number
  actionType: ConfigurablePermissionAction
  subjectType: ControlledFileCategoryPermissionSubjectType
  subjectId?: number
  scopeType: 'GLOBAL' | 'PRODUCT_GROUP'
  active: boolean
  remark: string
}
type SubjectOption = { label: string; value: number }
type CategoryRow = ControlledFileCategoryVO & { id: number }

const subjectTypeOptions: Array<{ label: string; value: ControlledFileCategoryPermissionSubjectType }> = [
  { label: '用户', value: 'USER' },
  { label: '部门', value: 'DEPT' },
  { label: '权限角色', value: 'ROLE' },
  { label: '系统岗位', value: 'POSITION' }
]
const configurableActionOptions = DCC_CATEGORY_PERMISSION_OPTIONS.filter(
  (option): option is typeof option & { value: ConfigurablePermissionAction } =>
    !CATEGORY_PERMISSION_RULE_MATRIX_ACTIONS.has(option.value)
)

const props = withDefaults(
  defineProps<{ active?: boolean; categoryRevision?: number }>(),
  { active: true, categoryRevision: 0 }
)
const message = useMessage()
const loading = ref(false)
const rulesLoading = ref(false)
const saving = ref(false)
const loaded = ref(false)
const loadedRevision = ref(props.categoryRevision)
const errorMessage = ref('')
const keyword = ref('')
const categories = ref<CategoryRow[]>([])
const drawerVisible = ref(false)
const selectedCategory = ref<CategoryRow>()
const editingRules = ref<PermissionRuleDraft[]>([])
const matrixRules = ref<ControlledFileCategoryPermissionRuleVO[]>([])
const users = ref<UserVO[]>([])
const departments = ref<DeptVO[]>([])
const roles = ref<RoleVO[]>([])
const posts = ref<PostVO[]>([])
let nextLocalId = 0

const drawerTitle = computed(() =>
  selectedCategory.value ? `类别权限：${selectedCategory.value.name}` : '类别权限'
)
const filteredCategories = computed(() => {
  const value = keyword.value.trim().toLocaleLowerCase()
  if (!value) return categories.value
  return categories.value.filter(
    (category) =>
      category.code.toLocaleLowerCase().includes(value) ||
      category.name.toLocaleLowerCase().includes(value)
  )
})

const subjectOptions = (type: ControlledFileCategoryPermissionSubjectType): SubjectOption[] => {
  if (type === 'USER') {
    return users.value.map((user) => ({
      label: `${user.nickname || user.username} (${user.username})`,
      value: user.id
    }))
  }
  if (type === 'DEPT') {
    return departments.value.map((department) => ({ label: department.name, value: department.id }))
  }
  if (type === 'ROLE') {
    return roles.value.map((role) => ({ label: `${role.name} (${role.code})`, value: role.id }))
  }
  return posts.value
    .filter((post): post is PostVO & { id: number } => post.id !== undefined)
    .map((post) => ({ label: `${post.name} (${post.code})`, value: post.id }))
}

const toDraft = (rule: ControlledFileCategoryPermissionRuleVO): PermissionRuleDraft => ({
  localId: ++nextLocalId,
  actionType: rule.actionType as ConfigurablePermissionAction,
  subjectType: rule.subjectType,
  subjectId: rule.subjectId,
  scopeType: rule.scopeType === 'PRODUCT_GROUP' ? 'PRODUCT_GROUP' : 'GLOBAL',
  active: rule.active,
  remark: rule.remark || ''
})

const loadCategories = async () => {
  if (!props.active) return
  loading.value = true
  errorMessage.value = ''
  try {
    categories.value = (await getFileCategoryList()).filter(
      (category): category is CategoryRow => category.id !== undefined
    )
    loaded.value = true
    loadedRevision.value = props.categoryRevision
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '类别列表加载失败'
  } finally {
    loading.value = false
  }
}

const openRules = async (category: CategoryRow) => {
  selectedCategory.value = category
  drawerVisible.value = true
  rulesLoading.value = true
  errorMessage.value = ''
  editingRules.value = []
  matrixRules.value = []
  try {
    const [rules, userList, departmentList, roleList, postList] = await Promise.all([
      getCategoryPermissionRules(category.id),
      getSimpleUserList(),
      getSimpleDeptList(),
      getSimpleRoleList(),
      getSimplePostList()
    ])
    matrixRules.value = rules.filter((rule) =>
      CATEGORY_PERMISSION_RULE_MATRIX_ACTIONS.has(rule.actionType.toUpperCase())
    )
    editingRules.value = rules
      .filter((rule) => !CATEGORY_PERMISSION_RULE_MATRIX_ACTIONS.has(rule.actionType.toUpperCase()))
      .map(toDraft)
    users.value = userList
    departments.value = departmentList
    roles.value = roleList
    posts.value = postList
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '类别权限规则加载失败'
  } finally {
    rulesLoading.value = false
  }
}

const addRule = () => {
  editingRules.value.push({
    localId: ++nextLocalId,
    actionType: 'DISTRIBUTE',
    subjectType: 'USER',
    subjectId: undefined,
    scopeType: 'GLOBAL',
    active: true,
    remark: ''
  })
}

const removeRule = (index: number) => {
  editingRules.value.splice(index, 1)
}

const saveRules = async () => {
  const category = selectedCategory.value
  if (!category) return
  if (editingRules.value.some((rule) => !Number.isInteger(rule.subjectId) || !rule.subjectId)) {
    message.error('保存前请为每条规则选择授权对象')
    return
  }
  saving.value = true
  try {
    await replaceCategoryPermissionRules(
      category.id,
      editingRules.value.map((rule) => ({
        actionType: rule.actionType,
        subjectType: rule.subjectType,
        subjectId: rule.subjectId as number,
        scopeType: rule.scopeType,
        active: rule.active,
        remark: rule.remark.trim() || undefined
      }))
    )
    message.success('类别权限规则已保存')
    await openRules(category)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '类别权限规则保存失败')
  } finally {
    saving.value = false
  }
}

watch(
  () => [props.active, props.categoryRevision] as const,
  ([active, revision]) => {
    if (active && (!loaded.value || revision !== loadedRevision.value)) {
      void loadCategories()
    }
  },
  { immediate: true }
)
</script>
