<template>
  <ContentWrap>
    <UnifiedListTemplate
      @query="handleQuery"
      @reset="resetQuery"
      table-key="dcc.controlledFile.positions.main"
      :query-model="queryParams"
      :rows="filteredPositions"
      :columns="listFilterColumns0"
      :show-column-settings="false"
      :column-filter-definitions="[...listColumnFilterDefinitions0, ...allColumnDisplayFilters0]"
    >
      <template #filters>
        <el-form
          ref="queryFormRef"
          class="position-toolbar -mb-15px"
          :inline="true"
          :model="queryParams"
          label-width="82px"
        >
          <el-form-item label="审批角色编码" prop="code">
            <el-input
              v-model="queryParams.code"
              class="!w-220px"
              clearable
              placeholder="请输入审批角色编码"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="审批角色名称" prop="name">
            <el-input
              v-model="queryParams.name"
              class="!w-220px"
              clearable
              placeholder="请输入审批角色名称"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="启用状态" prop="active">
            <el-select
              v-model="queryParams.active"
              class="!w-180px"
              clearable
              placeholder="请选择启用状态"
            >
              <el-option
                v-for="item in ACTIVE_STATUS_OPTIONS"
                :key="String(item.value)"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </template>
      <template #actions>

        <el-button
          type="primary"
          plain
          @click="openCreateDialog"
          v-hasPermi="['dcc:controlled-file:position:manage']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新增审批角色
        </el-button>
      </template>
      <template #table="{ resolveRows, rows: standardRows }">

        <el-table stripe border v-loading="loading" :data="resolveRows(standardRows)">
          <el-table-column label="审批角色编码" min-width="150" prop="code" show-overflow-tooltip />
          <el-table-column label="审批角色名称" min-width="180" prop="name" show-overflow-tooltip />
          <el-table-column label="审批角色摘要" min-width="240">
            <template #default="{ row }">
              <div class="position-summary" data-testid="dcc-position-summary">
                <div class="position-summary__line">
                  <el-tag :type="getBooleanTagType(row.active)" size="small">
                    启用：{{ formatBooleanLabel(row.active) }}
                  </el-tag>
                  <span class="position-summary__meta">
                    人数：{{ getPositionAssignmentCountLabel(row) }}
                  </span>
                </div>
                <div class="position-summary__meta">
                  创建：{{ formatDateTimeValue(row.createTime, '-') }}
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="分配摘要" min-width="260">
            <template #default="{ row }">
              {{ getAssignmentSummary(row) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" fixed="right" width="120">
            <template #default="{ row }">
              <el-button
                v-if="!isUploaderDerivedPosition(row)"
                link
                type="primary"
                @click="openAssignmentDialog(row)"
                v-hasPermi="['dcc:controlled-file:position:manage']"
              >
                维护分配
              </el-button>
              <el-tag v-else :type="isAuthorizedRepresentativePosition(row) ? 'warning' : 'info'">
                {{ isAuthorizedRepresentativePosition(row) ? '来源待定' : '按上传人计算' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
</ContentWrap>

  <Dialog v-model="createDialogVisible" title="新增审批角色" width="520px">
    <el-form
      ref="createFormRef"
      :model="createFormData"
      :rules="createFormRules"
      label-width="88px"
    >
      <el-form-item label="审批角色名称" prop="name">
        <el-input
          v-model="createFormData.name"
          maxlength="64"
          placeholder="请输入审批角色名称"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="变更原因" prop="changeReason">
        <el-input
          v-model="createFormData.changeReason"
          :rows="3"
          maxlength="200"
          placeholder="请填写新增审批角色原因"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="createLoading" @click="submitCreatePosition">
        保存审批角色
      </el-button>
      <el-button @click="createDialogVisible = false">取消</el-button>
    </template>
  </Dialog>

  <Dialog v-model="dialogVisible" :title="dialogTitle" width="920px">
    <div class="mb-12px flex items-center justify-between">
      <div class="text-13px text-[var(--el-text-color-secondary)]">
        当前审批角色：{{ currentPosition?.name || '-' }}
      </div>
      <el-button type="primary" plain @click="addAssignment">
        <Icon icon="ep:plus" class="mr-5px" />
        新增审批角色分配
      </el-button>
    </div>
    <el-form-item label="选择人员或关系">
      <UserSelectV2
        :model-value="assignmentUserIds" :relationships="assignmentRelationships"
        multiple :allow-relationships="supportsRelationshipAssignments" :user-options="users" :disabled="saveLoading"
        placeholder="选择固定人员或申请人关系" @selection-change="applyAssignmentSelection" />
      <span v-if="!supportsRelationshipAssignments" class="ml-8px text-12px">后端尚未启用关系规则，暂不能配置申请人关系。</span>
    </el-form-item>
    <el-table :data="assignmentRows" empty-text="当前审批角色暂无分配">
      <el-table-column label="分配类型" width="150">
        <template #default="{ row }">
          <el-select v-model="row.assignmentType" class="w-full" @change="handleAssignmentTypeChange(row)">
            <el-option
              v-for="item in availableAssignmentTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="分配对象" min-width="260">
        <template #default="{ row }">
          <UserSelectV2
            v-if="row.assignmentType === 'USER'"
            v-model="row.userId"
            class="w-full"
            clearable
            placeholder="请选择用户"
            :user-options="users"
          />
          <el-select
            v-else-if="row.assignmentType === 'POST'"
            v-model="row.systemPostId"
            class="w-full"
            clearable
            filterable
            placeholder="请选择组织角色"
          >
            <el-option
              v-for="item in availablePosts"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-tag v-else effect="plain">{{ relationshipLabel(row.assignmentType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" align="center" width="90">
        <template #default="{ row }">
          <el-switch v-model="row.active" />
        </template>
      </el-table-column>
      <el-table-column label="变更原因" min-width="220">
        <template #default="{ row }">
          <el-input v-model="row.changeReason" placeholder="例如调整审批角色分配人员" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="88">
        <template #default="{ $index }">
          <el-button link type="danger" @click="removeAssignment($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button type="primary" :loading="saveLoading" @click="saveAssignments">保存分配</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { type ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'

import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import { isSearchFormInputEmpty } from '@/utils/search'
import { formatDateTimeValue } from '@/utils/formatTime'
import {
  createApprovalPosition,
  getApprovalPositionList,
  saveApprovalPositionAssignments,
  type ControlledFileApprovalPositionCreateReqVO,
  type ControlledFileApprovalPositionVO,
  type ControlledFilePositionAssignmentVO
} from '@/api/dcc/controlledFile/approvalPositions'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { getSimplePostList, type PostVO } from '@/api/system/post'
import { ACTIVE_STATUS_OPTIONS, POSITION_ASSIGNMENT_TYPE_OPTIONS } from '../shared/options'
import { USER_RELATIONSHIPS, type UserId, type UserRelationship, type UserPickerSelection } from '@/views/system/user/components/userPicker'
import { formatBooleanLabel, formatDccSimpleUserLabel, getBooleanTagType } from '../shared/utils'

defineOptions({ name: 'DccControlledFilePositions' })

const HIDDEN_COMBINED_POSITION_SOURCE = 'INTAUTH:19'
const UPLOADER_DERIVED_POSITION_NAMES = new Set(['编制人直接主管', '部门负责人', '部门授权代表'])
const AUTHORIZED_REPRESENTATIVE_POSITION_NAMES = new Set(['部门授权代表'])

const loading = ref(false)
const createLoading = ref(false)
const saveLoading = ref(false)
const positions = ref<ControlledFileApprovalPositionVO[]>([])
const users = ref<UserVO[]>([])
const posts = ref<PostVO[]>([])
const queryFormRef = ref()
const createFormRef = ref()

const queryParams = reactive<{
  code: string
  name: string
  active?: boolean
}>({
  code: '',
  name: '',
  active: undefined
})
const appliedQueryParams = reactive<{
  code: string
  name: string
  active?: boolean
}>({
  code: '',
  name: '',
  active: undefined
})

const dialogVisible = ref(false)
const dialogTitle = ref('审批角色分配')
const currentPosition = ref<ControlledFileApprovalPositionVO>()
const assignmentRows = ref<ControlledFilePositionAssignmentVO[]>([])
const createDialogVisible = ref(false)
const createFormData = reactive<ControlledFileApprovalPositionCreateReqVO>({
  name: '',
  changeReason: ''
})
const createFormRules = reactive({
  name: [{ required: true, message: '请输入审批角色名称', trigger: 'blur' }],
  changeReason: [{ required: true, message: '请输入变更原因', trigger: 'blur' }]
})
const message = useMessage()
const availablePosts = computed(() =>
  posts.value.filter((item): item is PostVO & { id: number } => item.id !== undefined)
)

const visiblePositions = computed(() =>
  positions.value.filter((item) => item.source !== HIDDEN_COMBINED_POSITION_SOURCE)
)

const filteredPositions = computed(() => {
  const codeKeyword = appliedQueryParams.code.toLowerCase()
  const nameKeyword = appliedQueryParams.name.toLowerCase()
  return visiblePositions.value.filter((item) => {
    const codeMatch = !codeKeyword || item.code.toLowerCase().includes(codeKeyword)
    const nameMatch = !nameKeyword || item.name.toLowerCase().includes(nameKeyword)
    const activeMatch =
      appliedQueryParams.active === undefined || item.active === appliedQueryParams.active
    return codeMatch && nameMatch && activeMatch
  })
})

const loadData = async () => {
  loading.value = true
  try {
    const [positionList, userList, postList] = await Promise.all([
      getApprovalPositionList(),
      getSimpleUserList(),
      getSimplePostList()
    ])
    positions.value = positionList
    users.value = userList
    posts.value = postList
  } finally {
    loading.value = false
  }
}

const handleQuery = (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    resetQuery()
    return
  }
  appliedQueryParams.code = queryParams.code.trim()
  appliedQueryParams.name = queryParams.name.trim()
  appliedQueryParams.active = queryParams.active
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery(true)
}

const resetCreateForm = () => {
  createFormData.name = ''
  createFormData.changeReason = ''
}

const openCreateDialog = () => {
  resetCreateForm()
  createDialogVisible.value = true
  nextTick(() => createFormRef.value?.clearValidate?.())
}

const getActiveAssignments = (row: ControlledFileApprovalPositionVO) =>
  row.assignments.filter((item) => item.active)

const isUploaderDerivedPosition = (row?: ControlledFileApprovalPositionVO) =>
  UPLOADER_DERIVED_POSITION_NAMES.has(row?.name?.trim() || '')

const isAuthorizedRepresentativePosition = (row?: ControlledFileApprovalPositionVO) =>
  AUTHORIZED_REPRESENTATIVE_POSITION_NAMES.has(row?.name?.trim() || '')

const getPositionAssignmentCountLabel = (row: ControlledFileApprovalPositionVO) => {
  if (isAuthorizedRepresentativePosition(row)) {
    return '待定'
  }
  if (isUploaderDerivedPosition(row)) {
    return '动态'
  }
  return getActiveAssignments(row).some(item => isRelationship(item.assignmentType)) ? '动态' : String(getActiveAssignments(row).length)
}

const getAssignmentSummary = (row: ControlledFileApprovalPositionVO) => {
  if (isAuthorizedRepresentativePosition(row)) {
    return '授权代表真实来源待确认，运行时将阻塞'
  }
  if (isUploaderDerivedPosition(row)) {
    return '按上传人动态解析'
  }
  const summary = getActiveAssignments(row)
    .map((item) => {
      if (isRelationship(item.assignmentType)) return relationshipLabel(item.assignmentType)
      if (item.assignmentType === 'USER') {
        const user = users.value.find((candidate) => candidate.id === item.userId)
        return user ? formatDccSimpleUserLabel(user) : `用户#${item.userId}`
      }
      return (
        posts.value.find((post) => Number(post.id) === item.systemPostId)?.name ||
        `组织角色#${item.systemPostId}`
      )
    })
    .filter(Boolean)
  return summary.length > 0 ? summary.join('、') : '-'
}

const openAssignmentDialog = (row: ControlledFileApprovalPositionVO) => {
  if (isUploaderDerivedPosition(row)) {
    message.warning(
      isAuthorizedRepresentativePosition(row)
        ? '该审批角色缺少确认的授权代表来源，当前不允许指定固定人员'
        : '该审批角色按上传人动态解析，不允许指定固定人员'
    )
    return
  }
  currentPosition.value = row
  dialogTitle.value = `审批角色分配 - ${row.name}`
  assignmentRows.value = row.assignments.map((item) => ({ ...item }))
  dialogVisible.value = true
}

const addAssignment = () => {
  assignmentRows.value.push({
    assignmentType: 'USER',
    userId: undefined,
    systemPostId: undefined,
    active: true,
    changeReason: ''
  })
}

const removeAssignment = (index: number) => {
  assignmentRows.value.splice(index, 1)
}

const handleAssignmentTypeChange = (row: ControlledFilePositionAssignmentVO) => {
  row.userId = undefined
  row.systemPostId = undefined
}

const isRelationship = (type: string): type is UserRelationship => USER_RELATIONSHIPS.some(option => option.value === type)
const supportsRelationshipAssignments = computed(() => USER_RELATIONSHIPS.every(option =>
  currentPosition.value?.supportedAssignmentTypes?.includes(option.value)))
const availableAssignmentTypes = computed(() => POSITION_ASSIGNMENT_TYPE_OPTIONS.filter(option =>
  !isRelationship(option.value) || supportsRelationshipAssignments.value))
const relationshipLabel = (type: string) => USER_RELATIONSHIPS.find(option => option.value === type)?.label || '未识别关系'
const assignmentUserIds = computed<UserId[]>(() => assignmentRows.value.filter(row => row.assignmentType === 'USER' && row.userId != null).map(row => row.userId!))
const assignmentRelationships = computed(() => [...new Set(assignmentRows.value.map(row => row.assignmentType).filter(isRelationship))])
const applyAssignmentSelection = (selection: UserPickerSelection) => {
  const previous = assignmentRows.value
  assignmentRows.value = [
    ...previous.filter(row => row.assignmentType === 'POST'),
    ...selection.users.map(user => previous.find(row => row.assignmentType === 'USER' && String(row.userId) === String(user.id))
      || { assignmentType: 'USER', userId: user.id as number, active: true, changeReason: '' }),
    ...selection.relationships.map(type => previous.find(row => row.assignmentType === type)
      || { assignmentType: type, active: true, changeReason: '' })
  ]
}

const submitCreatePosition = async () => {
  await createFormRef.value?.validate()
  createLoading.value = true
  try {
    await createApprovalPosition({ ...createFormData })
    message.success('审批角色已新增')
    createDialogVisible.value = false
    resetCreateForm()
    await loadData()
  } finally {
    createLoading.value = false
  }
}

const saveAssignments = async () => {
  if (!currentPosition.value?.id) {
    return
  }
  if (!supportsRelationshipAssignments.value && assignmentRows.value.some(row => isRelationship(row.assignmentType))) {
    message.warning('后端尚未启用关系规则，不能保存动态关系配置')
    return
  }
  const invalidRow = assignmentRows.value.find(
    (item) =>
      !POSITION_ASSIGNMENT_TYPE_OPTIONS.some(option => option.value === item.assignmentType) ||
      (item.assignmentType === 'USER' && !item.userId) ||
      (item.assignmentType === 'POST' && !item.systemPostId)
  )
  if (invalidRow) {
    message.warning('请完善分配对象后再保存')
    return
  }
  saveLoading.value = true
  try {
    await saveApprovalPositionAssignments(currentPosition.value.id, assignmentRows.value)
    message.success('审批角色分配已保存')
    dialogVisible.value = false
    await loadData()
  } finally {
    saveLoading.value = false
  }
}

onMounted(() => {
  loadData()
})

const listColumnFilterDefinitions0 = computed<ListColumnFilterDefinition[]>(() => [
  {
    key: 'active',
    label: '审批角色摘要',
    type: 'text',
    getValue: (row) =>
      [
        formatBooleanLabel(row.active),
        getPositionAssignmentCountLabel(row),
        formatDateTimeValue(row.createTime, '-')
      ].join(' ')
  }
])

const listFilterColumns0 = [
  {
    key: 'code',
    label: '审批角色编码',
    visible: true
  },
  {
    key: 'name',
    label: '审批角色名称',
    visible: true
  },
  {
    key: 'active',
    label: '审批角色摘要',
    visible: true
  },
  {
    key: 'displayColumn3',
    label: '分配摘要',
    visible: true
  }
]

const allColumnDisplayFilters0: ListColumnFilterDefinition[] = [
  {
    key: 'active',
    label: '审批角色摘要',
    type: 'text',
    getValue: (row) =>
      [
        formatBooleanLabel(row.active),
        getPositionAssignmentCountLabel(row),
        formatDateTimeValue(row.createTime, '-')
      ].join(' ')
  },
  {
    key: 'displayColumn3',
    label: '分配摘要',
    type: 'text',
    getValue: (row) => [getAssignmentSummary(row)].join(' ')
  }
]
</script>

<style scoped>
.position-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
}

.position-summary {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.position-summary__line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.position-summary__meta {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

</style>
