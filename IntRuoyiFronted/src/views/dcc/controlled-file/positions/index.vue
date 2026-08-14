<template>
  <ContentWrap>
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
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" />
          查询
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-5px" />
          重置
        </el-button>
        <el-button plain type="info" @click="loadData">
          <Icon icon="ep:refresh-right" class="mr-5px" />
          刷新列表
        </el-button>
        <el-button
          type="primary"
          plain
          @click="openCreateDialog"
          v-hasPermi="['dcc:controlled-file:position:manage']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新增审批角色
        </el-button>
        <el-button
          type="success"
          plain
          :loading="exportLoading"
          @click="handleExport"
          v-hasPermi="['dcc:controlled-file:position:manage']"
        >
          <Icon icon="ep:download" class="mr-5px" />
          导出配置包
        </el-button>
        <el-button
          type="warning"
          plain
          :loading="importLoading"
          @click="openImport"
          v-hasPermi="['dcc:controlled-file:position:manage']"
        >
          <Icon icon="ep:upload" class="mr-5px" />
          导入配置包
        </el-button>
      </el-form-item>
      <el-form-item class="position-toolbar-context-item">
        <div class="position-toolbar-context" data-testid="dcc-position-toolbar-summary">
          <span class="position-toolbar-context__count">{{ positionToolbarSummaryText }}</span>
          <span class="position-toolbar-context__filter">{{ positionToolbarFilterText }}</span>
        </div>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <div class="mb-12px text-12px text-[var(--el-text-color-secondary)]">
      审批角色：控制 DCC 审批语义与分配。
    </div>
    <el-table v-loading="loading" :data="filteredPositions">
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
    <el-table :data="assignmentRows" empty-text="当前审批角色暂无分配">
      <el-table-column label="分配类型" width="150">
        <template #default="{ row }">
          <el-select v-model="row.assignmentType" class="w-full" @change="handleAssignmentTypeChange(row)">
            <el-option
              v-for="item in POSITION_ASSIGNMENT_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="分配对象" min-width="260">
        <template #default="{ row }">
          <el-select
            v-if="row.assignmentType === 'USER'"
            v-model="row.userId"
            class="w-full"
            clearable
            filterable
            placeholder="请选择用户"
          >
            <el-option
              v-for="item in users"
              :key="item.id"
              :label="formatDccSimpleUserLabel(item)"
              :value="item.id"
            />
          </el-select>
          <el-select
            v-else
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
  <input ref="importInputRef" accept=".json" class="hidden" type="file" @change="handleImportFileChange" />
</template>

<script lang="ts" setup>
import { isSearchFormInputEmpty } from '@/utils/search'
import download from '@/utils/download'
import { formatDateTimeValue } from '@/utils/formatTime'
import {
  createApprovalPosition,
  exportApprovalPositionConfigPackage,
  getApprovalPositionList,
  importApprovalPositionConfigPackage,
  saveApprovalPositionAssignments,
  type ControlledFileApprovalPositionCreateReqVO,
  type ControlledFileApprovalPositionVO,
  type ControlledFilePositionAssignmentVO
} from '@/api/dcc/controlledFile/approvalPositions'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { getSimplePostList, type PostVO } from '@/api/system/post'
import { ACTIVE_STATUS_OPTIONS, POSITION_ASSIGNMENT_TYPE_OPTIONS, getOptionLabel } from '../shared/options'
import { formatBooleanLabel, formatDccSimpleUserLabel, getBooleanTagType } from '../shared/utils'

defineOptions({ name: 'DccControlledFilePositions' })

const HIDDEN_COMBINED_POSITION_SOURCE = 'INTAUTH:19'
const UPLOADER_DERIVED_POSITION_NAMES = new Set(['编制人直接主管', '部门负责人', '部门授权代表'])
const AUTHORIZED_REPRESENTATIVE_POSITION_NAMES = new Set(['部门授权代表'])

const loading = ref(false)
const createLoading = ref(false)
const saveLoading = ref(false)
const exportLoading = ref(false)
const importLoading = ref(false)
const positions = ref<ControlledFileApprovalPositionVO[]>([])
const users = ref<UserVO[]>([])
const posts = ref<PostVO[]>([])
const queryFormRef = ref()
const createFormRef = ref()
const importInputRef = ref<HTMLInputElement>()

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

const positionToolbarSummaryText = computed(
  () => `显示 ${filteredPositions.value.length} / 全部 ${visiblePositions.value.length} 个审批角色`
)

const positionToolbarFilterText = computed(() => {
  const filters: string[] = []
  if (appliedQueryParams.code) {
    filters.push(`编码：${appliedQueryParams.code}`)
  }
  if (appliedQueryParams.name) {
    filters.push(`名称：${appliedQueryParams.name}`)
  }
  if (appliedQueryParams.active !== undefined) {
    filters.push(`状态：${getOptionLabel(ACTIVE_STATUS_OPTIONS, appliedQueryParams.active)}`)
  }
  return filters.length > 0 ? filters.join(' · ') : '全部审批角色'
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
  return String(getActiveAssignments(row).length)
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
  const invalidRow = assignmentRows.value.find(
    (item) =>
      !item.assignmentType ||
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

const handleExport = async () => {
  exportLoading.value = true
  try {
    const data = await exportApprovalPositionConfigPackage()
    download.json(data, '审批角色配置包.json')
    message.success('审批角色配置包已导出')
  } finally {
    exportLoading.value = false
  }
}

const openImport = () => {
  importInputRef.value?.click()
}

const handleImportFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  importLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    await importApprovalPositionConfigPackage(formData)
    message.success('审批角色配置包导入成功')
    await loadData()
  } finally {
    importLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.position-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
}

.position-toolbar-context-item {
  margin-left: auto;
}

.position-toolbar-context {
  display: flex;
  min-height: 32px;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  color: #4b5563;
  font-size: 13px;
  line-height: 20px;
}

.position-toolbar-context__count {
  color: #172033;
  font-weight: 600;
}

.position-toolbar-context__filter {
  min-width: 0;
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

@media (max-width: 768px) {
  .position-toolbar-context-item {
    margin-left: 0;
    width: 100%;
  }

  .position-toolbar-context {
    justify-content: flex-start;
  }
}
</style>
