<template>
  <div data-testid="dcc-category-permission-rules-tab">
    <el-alert
      class="mb-12px"
      type="info"
      :closable="false"
      title="编辑涵盖文件全部操作，须同时具备角色分配的全局编辑资格、目标项目编辑和文件类型编辑授权。浏览仅用于查看文件内容。已有细分操作规则保留原义，不自动转换为编辑。"
    />
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
        <el-table v-if="legacyRules.length" :data="legacyRules" border aria-label="原有细分操作规则（只读保留）">
          <el-table-column label="原有操作（只读保留）" min-width="180">
            <template #default="{ row }">{{ permissionLabel(row.actionType) }}</template>
          </el-table-column>
          <el-table-column label="主体类型" prop="subjectType" width="110" />
          <el-table-column label="授权对象" min-width="190">
            <template #default="{ row }">{{ subjectOptions(row.subjectType).find((item) => item.value === row.subjectId)?.label || `#${row.subjectId}` }}</template>
          </el-table-column>
          <el-table-column label="范围" prop="scopeType" width="130" />
          <el-table-column label="启用" width="80"><template #default="{ row }">{{ row.active ? '是' : '否' }}</template></el-table-column>
          <el-table-column label="备注" prop="remark" min-width="160" />
        </el-table>

        <div class="flex items-center justify-between gap-12px">
          <el-tag type="info" effect="plain">
            当前可配置规则 {{ editingRules.length }} 条
          </el-tag>
          <div class="flex gap-8px">
            <el-button
              type="primary"
              plain
              :disabled="rulesLoading || saving || !rulesLoaded"
              @click="addRule"
            >
              <Icon icon="ep:plus" class="mr-5px" />
              新增规则
            </el-button>
            <el-button
              type="primary"
              :loading="saving"
              :disabled="rulesLoading || !rulesLoaded"
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
type ConfigurablePermissionAction = Extract<ControlledFileCategoryPermissionAction, 'EDIT' | 'VIEW'>
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
  (option) => option.value === 'EDIT' || option.value === 'VIEW'
)
const permissionLabel = (action: string) =>
  DCC_CATEGORY_PERMISSION_OPTIONS.find((option) => option.value === action)?.label || action

const props = withDefaults(
  defineProps<{ active?: boolean; categoryRevision?: number }>(),
  { active: true, categoryRevision: 0 }
)
const message = useMessage()
const loading = ref(false)
const rulesLoading = ref(false)
const rulesLoaded = ref(false)
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
const legacyRules = ref<ControlledFileCategoryPermissionRuleVO[]>([])
const users = ref<UserVO[]>([])
const departments = ref<DeptVO[]>([])
const roles = ref<RoleVO[]>([])
const posts = ref<PostVO[]>([])
let nextLocalId = 0
let rulesSequence = 0

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
  const sequence = ++rulesSequence
  selectedCategory.value = category
  drawerVisible.value = true
  rulesLoading.value = true
  rulesLoaded.value = false
  errorMessage.value = ''
  editingRules.value = []
  matrixRules.value = []
  legacyRules.value = []
  try {
    const [rules, userList, departmentList, roleList, postList] = await Promise.all([
      getCategoryPermissionRules(category.id),
      getSimpleUserList(),
      getSimpleDeptList(),
      getSimpleRoleList(),
      getSimplePostList()
    ])
    if (sequence !== rulesSequence) return
    matrixRules.value = rules.filter((rule) =>
      CATEGORY_PERMISSION_RULE_MATRIX_ACTIONS.has(rule.actionType.toUpperCase())
    )
    editingRules.value = rules
      .filter((rule) => rule.actionType === 'EDIT' || rule.actionType === 'VIEW')
      .map(toDraft)
    legacyRules.value = rules.filter((rule) =>
      rule.actionType !== 'EDIT' && rule.actionType !== 'VIEW' &&
      !CATEGORY_PERMISSION_RULE_MATRIX_ACTIONS.has(rule.actionType.toUpperCase()))
    users.value = userList
    departments.value = departmentList
    roles.value = roleList
    posts.value = postList
    rulesLoaded.value = true
  } catch (error) {
    if (sequence === rulesSequence) errorMessage.value = error instanceof Error ? error.message : '类别权限规则加载失败'
  } finally {
    if (sequence === rulesSequence) rulesLoading.value = false
  }
}

const addRule = () => {
  editingRules.value.push({
    localId: ++nextLocalId,
    actionType: 'VIEW',
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
  if (!category || !rulesLoaded.value || rulesLoading.value || saving.value) return
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
