<template>
  <ContentWrap class="bpm-model-page">
    <div class="bpm-model-page__header">
      <div class="bpm-model-page__title">
        <h3>流程模型</h3>
      </div>
    </div>

    <UnifiedListTemplate
      table-key="bpm.model.main"
      :query-model="queryParams"
      label-width="88px"
      :filter-definitions="modelQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="modelQuickFilter.state"
      :selected-filter-definition="modelQuickFilter.selectedDefinition.value"
      :operator-options="modelQuickFilter.operatorOptions.value"
      :columns="modelColumns"
      :column-saving="modelColumnSaving"
      :show-column-settings="false"
      :show-column-reset="false"
      :total="filteredModelList.length"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="modelQuickFilter.updateState"
      @quick-filter-query="handleQuery"
      @column-change="saveModelColumnConfig"
      @column-reset="resetModelColumnConfig"
    >
      <template #actions>
        <el-button
          type="primary"
          @click="openModelForm('create')"
          v-hasPermi="['bpm:model:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新建模型
        </el-button>
      </template>

      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          class="bpm-model-unified-table"
          data-user-table-column-explicit
          data-user-table-key="bpm.model.main"
          :data="pagedModelList"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          row-key="id"
          :row-style="{ height: '58px' }"
          @header-dragend="handleModelHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isModelColumnVisible('name')"
            label="流程名"
            prop="name"
            :width="getModelColumnWidthString('name')"
            :min-width="getModelColumnMinWidthString('name', 220)"
            v-bind="sortColumnAttrs('name')"
          >
            <template #default="{ row }">
              <div class="bpm-model-name-cell">
                <el-image v-if="row.icon" :src="row.icon" class="bpm-model-name-cell__icon" />
                <div v-else class="bpm-model-name-cell__fallback">
                  {{ subString(resolveModelDisplayName(row), 0, 2) }}
                </div>
                <span class="bpm-model-name-cell__text">{{ resolveModelDisplayName(row) }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column
            v-if="isModelColumnVisible('categoryName')"
            label="流程分类"
            prop="categoryName"
            :width="getModelColumnWidthString('categoryName')"
            :min-width="getModelColumnMinWidthString('categoryName', 140)"
            v-bind="sortColumnAttrs('categoryName')"
          >
            <template #default="{ row }">
              <el-tag v-if="row.categoryName" effect="plain">{{ row.categoryName }}</el-tag>
              <span v-else class="bpm-model-muted">未分类</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="isModelColumnVisible('visibleRange')"
            label="可见范围"
            prop="startUserIds"
            :width="getModelColumnWidthString('visibleRange')"
            :min-width="getModelColumnMinWidthString('visibleRange', 160)"
            v-bind="sortColumnAttrs('startUserIds')"
          >
            <template #default="{ row }">
              <el-text v-if="!row.startUsers?.length && !row.startDepts?.length">
                全部可见
              </el-text>
              <el-text v-else-if="row.startUsers?.length === 1">
                {{ row.startUsers[0].nickname }}
              </el-text>
              <el-text v-else-if="row.startDepts?.length === 1">
                {{ row.startDepts[0].name }}
              </el-text>
              <el-text v-else-if="row.startDepts?.length > 1">
                <el-tooltip
                  effect="dark"
                  placement="top"
                  :content="row.startDepts.map((dept: any) => dept.name).join('、')"
                >
                  {{ row.startDepts[0].name }}等 {{ row.startDepts.length }} 个部门可见
                </el-tooltip>
              </el-text>
              <el-text v-else>
                <el-tooltip
                  effect="dark"
                  placement="top"
                  :content="row.startUsers.map((user: any) => user.nickname).join('、')"
                >
                  {{ row.startUsers[0].nickname }}等 {{ row.startUsers.length }} 人可见
                </el-tooltip>
              </el-text>
            </template>
          </el-table-column>

          <el-table-column
            v-if="isModelColumnVisible('type')"
            label="流程类型"
            prop="type"
            :width="getModelColumnWidthString('type')"
            :min-width="getModelColumnMinWidthString('type', 130)"
            v-bind="sortColumnAttrs('type')"
          >
            <template #default="{ row }">
              <dict-tag :value="row.type" :type="DICT_TYPE.BPM_MODEL_TYPE" />
            </template>
          </el-table-column>

          <el-table-column
            v-if="isModelColumnVisible('formInfo')"
            label="表单信息"
            prop="formType"
            :width="getModelColumnWidthString('formInfo')"
            :min-width="getModelColumnMinWidthString('formInfo', 240)"
            v-bind="sortColumnAttrs('formType')"
          >
            <template #default="{ row }">
              <el-button
                v-if="row.formType === BpmModelFormType.NORMAL"
                type="primary"
                link
                @click="handleFormDetail(row)"
              >
                {{ row.formName }}
              </el-button>
              <el-button
                v-else-if="row.formType === BpmModelFormType.CUSTOM"
                type="primary"
                link
                @click="handleFormDetail(row)"
              >
                {{ row.formCustomCreatePath }}
              </el-button>
              <span v-else class="bpm-model-muted">暂无表单</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="isModelColumnVisible('deployment')"
            label="最后发布"
            prop="deployment"
            :width="getModelColumnWidthString('deployment')"
            :min-width="getModelColumnMinWidthString('deployment', 230)"
            v-bind="sortColumnAttrs('deployment')"
          >
            <template #default="{ row }">
              <div class="bpm-model-deployment-cell">
                <span v-if="row.processDefinition" class="bpm-model-deployment-cell__time">
                  {{ formatDate(row.processDefinition.deploymentTime) }}
                </span>
                <el-tag v-if="row.processDefinition" size="small">
                  v{{ row.processDefinition.version }}
                </el-tag>
                <el-tag v-else type="warning" size="small">未部署</el-tag>
                <el-tag
                  v-if="row.processDefinition?.suspensionState === 2"
                  type="warning"
                  size="small"
                >
                  已停用
                </el-tag>
              </div>
            </template>
          </el-table-column>

          <el-table-column
            v-if="isModelColumnVisible('actions')"
            label="操作"
            prop="actions"
            fixed="right"
            :width="getModelColumnWidthString('actions', 260)"
          >
            <template #default="{ row }">
              <el-button link type="primary" @click="openModelView(row)"> 查看 </el-button>
              <el-button
                link
                type="primary"
                @click="openModelForm('update', row.id)"
                :disabled="!isManagerUser(row) && !hasPermiUpdate"
              >
                修改
              </el-button>
              <el-button
                link
                type="primary"
                @click="openModelForm('copy', row.id)"
                :disabled="!isManagerUser(row) && !hasPermiUpdate"
              >
                复制
              </el-button>
              <el-button
                link
                class="!ml-5px"
                type="primary"
                @click="handleDeploy(row)"
                :disabled="!isManagerUser(row) && !hasPermiDeploy"
              >
                发布
              </el-button>
              <el-dropdown
                v-if="hasPermiMore"
                class="!align-middle ml-5px"
                @command="(command) => handleModelCommand(command, row)"
              >
                <el-button type="primary" link>更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="handleDefinitionList" v-if="hasPermiPdQuery">
                      历史
                    </el-dropdown-item>
                    <el-dropdown-item
                      command="handleReport"
                      v-if="
                        checkPermi(['bpm:process-instance:manager-query']) && row.processDefinition
                      "
                      :disabled="!isManagerUser(row)"
                    >
                      报表
                    </el-dropdown-item>
                    <el-dropdown-item
                      command="handleChangeState"
                      v-if="hasPermiUpdate && row.processDefinition"
                      :disabled="!isManagerUser(row)"
                    >
                      {{ row.processDefinition.suspensionState === 1 ? '停用' : '启用' }}
                    </el-dropdown-item>
                    <el-dropdown-item
                      type="danger"
                      command="handleClean"
                      v-if="checkPermi(['bpm:model:clean'])"
                      :disabled="!isManagerUser(row)"
                    >
                      清理
                    </el-dropdown-item>
                    <el-dropdown-item
                      type="danger"
                      command="handleDelete"
                      v-if="hasPermiDelete"
                      :disabled="!isManagerUser(row)"
                    >
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <Dialog :title="modelApprovalRouteDialogTitle" v-model="viewDetailVisible" width="520">
    <div
      v-if="selectedModel"
      v-loading="modelDetailLoading"
      class="bpm-model-approval-route"
      data-bpm-model-view="approval-route"
    >
      <div
        v-for="(step, index) in modelApprovalRouteSteps"
        :key="step.key"
        class="bpm-model-approval-route__step"
        :data-approval-role="step.key"
      >
        <div class="bpm-model-approval-route__marker">
          {{ index + 1 }}
        </div>
        <div class="bpm-model-approval-route__content">
          <div class="bpm-model-approval-route__label">{{ step.label }}</div>
          <div class="bpm-model-approval-route__value">{{ step.value }}</div>
        </div>
      </div>
    </div>
  </Dialog>
  <Dialog title="表单详情" v-model="formDetailVisible" width="800">
    <form-create :rule="formDetailPreview.rule" :option="formDetailPreview.option" />
  </Dialog>
</template>

<script lang="ts" setup>
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { CategoryApi, CategoryVO } from '@/api/bpm/category'
import * as ModelApi from '@/api/bpm/model'
import * as FormApi from '@/api/bpm/form'
import * as RoleApi from '@/api/system/role'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useUserTableColumns,
  type UserTableColumnDefinition
} from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterOption,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'
import { formatDate } from '@/utils/formatTime'
import { setConfAndFields2 } from '@/utils/formCreate'
import { BpmModelFormType } from '@/utils/constants'
import { checkPermi } from '@/utils/permission'
import { useUserStoreWithOut } from '@/store/modules/user'
import { subString } from '@/utils/index'

defineOptions({ name: 'BpmModel' })

interface UserInfo {
  nickname: string
  [key: string]: any
}

interface DeptInfo {
  name: string
  [key: string]: any
}

interface ProcessDefinition {
  id: string
  deploymentTime: string
  version: number
  suspensionState: number
}

interface ModelInfo {
  id: number
  key: string
  name: string
  icon?: string
  type?: number | string
  categoryName?: string
  startUsers?: UserInfo[]
  startDepts?: DeptInfo[]
  processDefinition?: ProcessDefinition
  formType?: number
  formId?: number
  formName?: string
  formCustomCreatePath?: string
  managerUserIds?: number[]
  bpmnXml?: string
  simpleModel?: SimpleModelNode
  [key: string]: any
}

interface SimpleModelNode {
  id?: string
  type?: number
  name?: string
  showText?: string
  candidateStrategy?: number | string
  candidateParam?: string
  childNode?: SimpleModelNode
  conditionNodes?: SimpleModelNode[]
}

interface ParticipantGroups {
  reviewers: string[]
  approvers: string[]
}

type ModelQuickFilterFieldKey = 'key' | 'categoryName' | 'type' | 'deploymentState'

const message = useMessage()
const { t } = useI18n()
const router = useRouter()
const { push } = router
const userStore = useUserStoreWithOut()

const modelDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'name', label: '流程名', minWidth: 220 },
  { key: 'categoryName', label: '流程分类', minWidth: 140 },
  { key: 'visibleRange', label: '可见范围', minWidth: 160 },
  { key: 'type', label: '流程类型', minWidth: 130 },
  { key: 'formInfo', label: '表单信息', minWidth: 240 },
  { key: 'deployment', label: '最后发布', minWidth: 230 },
  { key: 'actions', label: '操作', width: 260, hideable: false, business: false }
]

const {
  columns: modelColumns,
  saving: modelColumnSaving,
  isColumnVisible: isModelColumnVisible,
  getColumnWidthString: getModelColumnWidthString,
  getColumnMinWidthString: getModelColumnMinWidthString,
  handleHeaderDragend: handleModelHeaderDragend,
  saveConfig: saveModelColumnConfig,
  resetConfig: resetModelColumnConfig
} = useUserTableColumns('bpm.model.main', modelDefaultColumns)

const loading = ref(true)
const modelList = ref<ModelInfo[]>([])
const categoryList = ref<CategoryVO[]>([])

const MODEL_DISPLAY_NAME_MAP: Record<string, string> = {
  'DCC Controlled File Approval': 'DCC 受控文件审批',
  'Expense Dept Leader Approval': '费用部门负责人审批',
  'eDHR Approval V1': 'eDHR 审批 V1'
}

const REGISTRATION_CERTIFICATE_APPROVAL_PROCESS_KEY = 'dcc-registration-certificate-access'
const REGISTRATION_CERTIFICATE_APPROVER_ROLE_CODE = 'dcc_registration_certificate_approver'

const resolveModelDisplayName = (row?: Pick<ModelInfo, 'name'> | null) => {
  const modelName = row?.name?.trim()
  if (!modelName) return ''
  return MODEL_DISPLAY_NAME_MAP[modelName] || modelName
}

const queryParams = reactive<{
  pageNo: number
  pageSize: number
  name?: string
  quickFilter?: TableQuickFilterValue
}>({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  quickFilter: undefined
})

const categoryOptions = computed<TableQuickFilterOption[]>(() =>
  categoryList.value.map((category) => ({
    label: category.name,
    value: category.name
  }))
)

const modelTypeOptions = computed<TableQuickFilterOption[]>(() =>
  getIntDictOptions(DICT_TYPE.BPM_MODEL_TYPE).map((item) => ({
    label: item.label,
    value: item.value
  }))
)

const modelQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'name',
    label: '流程名',
    type: 'text',
    queryParamKey: 'name',
    placeholder: '搜索流程'
  },
  { key: 'key', label: '流程标识', type: 'text', placeholder: '请输入流程标识' },
  { key: 'categoryName', label: '流程分类', type: 'select', options: categoryOptions.value },
  { key: 'type', label: '流程类型', type: 'select', options: modelTypeOptions.value },
  {
    key: 'deploymentState',
    label: '发布状态',
    type: 'select',
    options: [
      { label: '已发布', value: 'deployed' },
      { label: '未部署', value: 'undeployed' },
      { label: '已停用', value: 'suspended' }
    ]
  }
])

const resolveDeploymentState = (row: ModelInfo) => {
  if (!row.processDefinition) return 'undeployed'
  return row.processDefinition.suspensionState === 2 ? 'suspended' : 'deployed'
}

const resolveQuickFilterTarget = (row: ModelInfo, fieldKey: ModelQuickFilterFieldKey) => {
  if (fieldKey === 'deploymentState') return resolveDeploymentState(row)
  return row[fieldKey]
}

const matchesQuickFilter = (row: ModelInfo) => {
  const quickFilter = queryParams.quickFilter
  if (!quickFilter) return true
  const fieldKey = quickFilter.fieldKey as ModelQuickFilterFieldKey
  const target = resolveQuickFilterTarget(row, fieldKey)
  const value = quickFilter.value
  if (value === undefined || value === null) return true
  if (quickFilter.operator === 'eq') {
    return String(target ?? '') === String(value)
  }
  return String(target ?? '')
    .toLowerCase()
    .includes(String(value).trim().toLowerCase())
}

const filteredModelList = computed(() => modelList.value.filter((row) => matchesQuickFilter(row)))

const pagedModelList = computed(() => {
  const start = (queryParams.pageNo - 1) * queryParams.pageSize
  return filteredModelList.value.slice(start, start + queryParams.pageSize)
})

const hasPermiUpdate = computed(() => checkPermi(['bpm:model:update']))
const hasPermiDelete = computed(() => checkPermi(['bpm:model:delete']))
const hasPermiDeploy = computed(() => checkPermi(['bpm:model:deploy']))
const hasPermiMore = computed(() =>
  checkPermi([
    'bpm:process-definition:query',
    'bpm:process-instance:manager-query',
    'bpm:model:update',
    'bpm:model:delete',
    'bpm:model:clean'
  ])
)
const hasPermiPdQuery = computed(() => checkPermi(['bpm:process-definition:query']))

const handleQuery = async () => {
  await modelQuickFilter.applyQuickFilter()
}

const getList = async () => {
  loading.value = true
  try {
    const data = await ModelApi.getModelList(queryParams.name)
    const categories = await CategoryApi.getCategorySimpleList()
    modelList.value = data
    categoryList.value = categories
    const maxPage = Math.max(1, Math.ceil(filteredModelList.value.length / queryParams.pageSize))
    if (queryParams.pageNo > maxPage) {
      queryParams.pageNo = maxPage
    }
  } finally {
    loading.value = false
  }
}

const viewDetailVisible = ref(false)
const modelDetailLoading = ref(false)
const selectedModel = ref<ModelInfo | null>(null)
const approvalRoleList = ref<RoleApi.RoleVO[]>([])

const SIMPLE_NODE_TYPE = {
  START_USER: 10,
  USER_TASK: 11,
  TRANSACTOR: 13
} as const

const CANDIDATE_STRATEGY_LABELS: Record<string, string> = {
  '1': '审批人为空',
  '10': '审批角色',
  '20': '部门成员',
  '21': '部门负责人',
  '22': '岗位',
  '23': '连续多级部门负责人',
  '30': '用户',
  '34': '由当前审批人指定',
  '35': '由发起人指定',
  '36': '发起人本人',
  '37': '发起人所在部门负责人',
  '38': '发起人连续多级部门负责人',
  '40': '用户组',
  '50': '表单内用户字段',
  '51': '表单内部门负责人',
  '60': '流程表达式'
}

const APPROVAL_ROUTE_TEMPLATE_FIELD_LABELS: Record<string, string> = {
  batchRecordName: '批记录名称',
  versionNo: '版本号',
  batchExecutionCode: '批次执行编码',
  workOrderCode: '工单编码',
  batchCode: '批次号',
  productName: '产品名称',
  productCode: '产品编码',
  routeName: '工艺路线名称',
  routeCode: '工艺路线编码',
  processName: '工序名称',
  processCode: '工序编码'
}

const APPROVAL_ROUTE_TEXT_LABELS: Record<string, string> = {
  'Dept Leader Approval': '部门负责人审批',
  'Start user dept leader': '发起人所在部门负责人',
  Requester: '发起人本人',
  发起人自己: '发起人本人'
}

const APPROVAL_ROUTE_PARAMETERLESS_STRATEGIES = new Set(['34', '35', '36', '37'])

const escapeApprovalRoutePattern = (value: string) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const buildParticipantGroups = (): ParticipantGroups => ({
  reviewers: [],
  approvers: []
})

const dedupeText = (items: string[]) => Array.from(new Set(items.filter(Boolean)))

const formatParticipantList = (items: string[], emptyText = '未配置') => {
  const values = dedupeText(items)
  return values.length > 0 ? values.join('；') : emptyText
}

const formatStartParticipants = (row?: ModelInfo | null) => {
  const startUsers = row?.startUsers || []
  const startDepts = row?.startDepts || []
  if (startUsers.length === 0 && startDepts.length === 0) return '全部人员可发起'
  const users = startUsers.map((item) => item.nickname).filter(Boolean)
  const depts = startDepts.map((item) => item.name).filter(Boolean)
  return formatParticipantList([...users, ...depts])
}

const formatApprovalRouteTemplateText = (value?: string) => {
  const rawText = value?.trim()
  if (!rawText) return ''
  let readableText = rawText
    .replace(/\$\{([^}]+)\}/g, (_matched, fieldKey: string) => {
      return APPROVAL_ROUTE_TEMPLATE_FIELD_LABELS[fieldKey] || fieldKey
    })
    .replace(/\s+/g, ' ')
  Object.entries(APPROVAL_ROUTE_TEMPLATE_FIELD_LABELS).forEach(([fieldKey, readableLabel]) => {
    const fieldPattern = new RegExp(`\\b${escapeApprovalRoutePattern(fieldKey)}\\b`, 'g')
    readableText = readableText.replace(fieldPattern, readableLabel)
  })
  readableText = readableText
    .replace(/批记录名称\s+版本号/g, '批记录名称、版本号')
    .replace(/^批记录升版\s+(.+)$/, '批记录升版：$1')
  Object.entries(APPROVAL_ROUTE_TEXT_LABELS).forEach(([sourceText, readableLabel]) => {
    readableText = readableText.split(sourceText).join(readableLabel)
  })
  readableText = readableText.replace(/[；;]\s*/g, '；')
  return readableText
}

const resolveApprovalRoleName = (
  roleId: string | number,
  roles: ReadonlyArray<RoleApi.RoleVO>
) => {
  const roleIdText = String(roleId).trim()
  const matchedRole = roles.find((item) => String(item.id) === roleIdText)
  return matchedRole?.name || `未识别角色（ID：${roleIdText}）`
}

const resolveApprovalRoleNameByCode = (
  roleCode: string,
  roles: ReadonlyArray<RoleApi.RoleVO>
) => {
  const roleCodeText = roleCode.trim()
  const matchedRole = roles.find((item) => item.code === roleCodeText)
  return matchedRole?.name || `未识别角色（编码：${roleCodeText}）`
}

const formatApprovalRoleCandidateParam = (
  candidateParam: string | undefined,
  roles: ReadonlyArray<RoleApi.RoleVO>
) => {
  const rawText = candidateParam?.trim()
  if (!rawText) return '未配置'
  const roleIds = rawText.match(/\d+/g) || []
  if (roleIds.length === 0) {
    return `未识别角色（ID：${formatApprovalRouteTemplateText(rawText)}）`
  }
  return roleIds.map((roleId) => resolveApprovalRoleName(roleId, roles)).join('、')
}

const formatCandidateRule = (
  strategy?: number | string,
  candidateParam?: string,
  roles: ReadonlyArray<RoleApi.RoleVO> = approvalRoleList.value
) => {
  const strategyKey = strategy === undefined || strategy === null ? '' : String(strategy)
  const strategyLabel = CANDIDATE_STRATEGY_LABELS[strategyKey]
  const paramText =
    strategyKey === '10'
      ? formatApprovalRoleCandidateParam(candidateParam, roles)
      : APPROVAL_ROUTE_PARAMETERLESS_STRATEGIES.has(strategyKey)
        ? ''
        : formatApprovalRouteTemplateText(candidateParam)
  if (!strategyLabel && !paramText) return ''
  if (!paramText) return strategyLabel || ''
  if (!strategyLabel) return paramText
  return `${strategyLabel}：${paramText}`
}

const formatParticipantNode = (nodeName?: string, ruleText?: string) => {
  const name = formatApprovalRouteTemplateText(nodeName) || '未命名节点'
  const text = formatApprovalRouteTemplateText(ruleText)
  if (!text || text === name) return `节点：${name}`
  return `节点：${name}\n审批对象：${text}`
}

const formatApprovalRouteParticipant = (participantText: string, routeName?: string) => {
  const approvalRouteName = routeName?.trim() || '未配置审批路线名称'
  const approvalObjectLine = participantText
    .split('\n')
    .map((item) => item.trim())
    .find((item) => item.startsWith('审批对象：'))
  return approvalObjectLine
    ? `审批路线：${approvalRouteName}\n${approvalObjectLine}`
    : `审批路线：${approvalRouteName}`
}

const formatBusinessApprovalRouteParticipant = (routeName: string, roleNames: string) => {
  const approvalRouteName = routeName.trim() || '未配置审批路线名称'
  return `审批路线：${approvalRouteName}\n审批对象：${roleNames}`
}

const selectParticipantSource = (primaryItems: string[], fallbackItems: string[]) => {
  return primaryItems.length > 0 ? primaryItems : fallbackItems
}

const selectBusinessParticipantSource = (
  businessItems: string[] | undefined,
  primaryItems: string[],
  fallbackItems: string[]
) => {
  return businessItems !== undefined ? businessItems : selectParticipantSource(primaryItems, fallbackItems)
}

const isRegistrationCertificateApprovalModel = (model?: Pick<ModelInfo, 'key'> | null) => {
  return model?.key === REGISTRATION_CERTIFICATE_APPROVAL_PROCESS_KEY
}

const resolveBusinessApprovalRouteParticipants = (model?: ModelInfo | null) => {
  if (!isRegistrationCertificateApprovalModel(model)) return {}
  const roleNames = resolveApprovalRoleNameByCode(
    REGISTRATION_CERTIFICATE_APPROVER_ROLE_CODE,
    approvalRoleList.value
  )
  return {
    reviewers: [],
    approvers: [formatBusinessApprovalRouteParticipant(resolveModelDisplayName(model), roleNames)]
  }
}

const classifyParticipantRole = (nodeName?: string, nodeType?: number) => {
  const name = nodeName || ''
  if (/审核|复核|review/i.test(name)) return 'reviewer'
  if (/审批|批准|approval|approve/i.test(name)) return 'approver'
  if (nodeType === SIMPLE_NODE_TYPE.TRANSACTOR) return 'reviewer'
  if (nodeType === SIMPLE_NODE_TYPE.USER_TASK) return 'approver'
  return undefined
}

const collectSimpleModelParticipants = (
  node: SimpleModelNode | undefined,
  groups = buildParticipantGroups()
) => {
  if (!node) return groups
  const role = classifyParticipantRole(node.name, node.type)
  if (role === 'reviewer') {
    groups.reviewers.push(
      formatParticipantNode(
        node.name,
        node.showText ||
          formatCandidateRule(node.candidateStrategy, node.candidateParam, approvalRoleList.value)
      )
    )
  }
  if (role === 'approver') {
    groups.approvers.push(
      formatParticipantNode(
        node.name,
        node.showText ||
          formatCandidateRule(node.candidateStrategy, node.candidateParam, approvalRoleList.value)
      )
    )
  }
  node.conditionNodes?.forEach((item) => collectSimpleModelParticipants(item, groups))
  collectSimpleModelParticipants(node.childNode, groups)
  return groups
}

const getBpmnElementText = (element: Element, localName: string) => {
  return Array.from(element.getElementsByTagName('*'))
    .find((item) => item.localName === localName)
    ?.textContent?.trim()
}

const parseBpmnUserTaskParticipants = (bpmnXml?: string) => {
  const groups = buildParticipantGroups()
  if (!bpmnXml?.trim()) return groups
  const xmlDoc = new DOMParser().parseFromString(bpmnXml, 'application/xml')
  const userTasks = Array.from(xmlDoc.getElementsByTagName('*')).filter(
    (item) => item.localName === 'userTask'
  )
  userTasks.forEach((task) => {
    const nodeName = task.getAttribute('name') || task.getAttribute('id') || ''
    const role = classifyParticipantRole(nodeName, SIMPLE_NODE_TYPE.USER_TASK)
    const ruleText = formatCandidateRule(
      getBpmnElementText(task, 'candidateStrategy'),
      getBpmnElementText(task, 'candidateParam'),
      approvalRoleList.value
    )
    if (role === 'reviewer') {
      groups.reviewers.push(formatParticipantNode(nodeName, ruleText))
    }
    if (role === 'approver') {
      groups.approvers.push(formatParticipantNode(nodeName, ruleText))
    }
  })
  return groups
}

const modelViewParticipants = computed(() => {
  const model = selectedModel.value
  const simpleParticipants = collectSimpleModelParticipants(model?.simpleModel)
  const bpmnParticipants = parseBpmnUserTaskParticipants(model?.bpmnXml)
  const approvalRouteName = resolveModelDisplayName(model)
  const businessParticipants = resolveBusinessApprovalRouteParticipants(model)
  return {
    starter: formatStartParticipants(model),
    reviewer: formatParticipantList(
      selectBusinessParticipantSource(
        businessParticipants.reviewers,
        simpleParticipants.reviewers,
        bpmnParticipants.reviewers
      ),
      '未配置审核环节'
    ),
    approver: formatParticipantList(
      selectBusinessParticipantSource(
        businessParticipants.approvers,
        simpleParticipants.approvers,
        bpmnParticipants.approvers
      ).map((item) => formatApprovalRouteParticipant(item, approvalRouteName)),
      '未配置批准环节'
    )
  }
})

const isUnconfiguredApprovalRouteStep = (step: { value: string }) => /^未配置.+环节$/.test(step.value)

const modelApprovalRouteSteps = computed(() => {
  const steps = [
    {
      key: 'starter',
      label: '发起权限',
      value: modelViewParticipants.value.starter
    },
    {
      key: 'reviewer',
      label: '审核环节',
      value: modelViewParticipants.value.reviewer
    },
    {
      key: 'approver',
      label: '批准环节',
      value: modelViewParticipants.value.approver
    }
  ]
  if (isRegistrationCertificateApprovalModel(selectedModel.value)) {
    return steps.filter((step) => step.key === 'starter' || !isUnconfiguredApprovalRouteStep(step))
  }
  return steps
})

const modelApprovalRouteDialogTitle = computed(() => {
  const approvalRouteName = resolveModelDisplayName(selectedModel.value)
  return approvalRouteName ? `审批路线：${approvalRouteName}` : '流程审批路线'
})

const openModelView = async (row: ModelInfo) => {
  modelDetailLoading.value = true
  try {
    const [modelDetail, roles] = await Promise.all([
      ModelApi.getModel(String(row.id)),
      RoleApi.getSimpleRoleList()
    ])
    approvalRoleList.value = roles
    selectedModel.value = {
      ...row,
      ...modelDetail,
      categoryName: modelDetail?.categoryName || row.categoryName,
      processDefinition: modelDetail?.processDefinition || row.processDefinition,
      startUsers: modelDetail?.startUsers?.length ? modelDetail.startUsers : row.startUsers,
      startDepts: modelDetail?.startDepts?.length ? modelDetail.startDepts : row.startDepts
    }
    viewDetailVisible.value = true
  } catch (error) {
    message.error('流程模型详情或审批角色名称加载失败，请查看接口响应')
    throw error
  } finally {
    modelDetailLoading.value = false
  }
}

const formDetailVisible = ref(false)
const formDetailPreview = ref({
  rule: [],
  option: {}
})

const handleFormDetail = async (row: ModelInfo) => {
  if (row.formType === BpmModelFormType.NORMAL) {
    if (!row.formId) {
      const errorMessage = `流程模型 ${resolveModelDisplayName(row)} 缺少普通表单编号`
      message.error(errorMessage)
      throw new Error(errorMessage)
    }
    const data = await FormApi.getForm(row.formId)
    setConfAndFields2(formDetailPreview, data.conf, data.fields)
    formDetailVisible.value = true
    return
  }
  if (row.formType === BpmModelFormType.CUSTOM) {
    if (!row.formCustomCreatePath) {
      const errorMessage = `流程模型 ${resolveModelDisplayName(row)} 缺少自定义表单地址`
      message.error(errorMessage)
      throw new Error(errorMessage)
    }
    await push({ path: row.formCustomCreatePath })
  }
}

const isManagerUser = (row: ModelInfo) => {
  const userId = userStore.getUser.id
  return row.managerUserIds && row.managerUserIds.includes(userId)
}

const isCancelError = (error: unknown) => {
  if (error === 'cancel' || error === 'close') return true
  if (!error || typeof error !== 'object') return false
  const messageValue = (error as { message?: string }).message
  return messageValue === 'cancel' || messageValue === 'close'
}

const handleActionError = (error: unknown) => {
  if (isCancelError(error)) return
  message.error('操作失败，请查看接口响应')
  throw error
}

const handleDelete = async (row: ModelInfo) => {
  try {
    await message.delConfirm()
    await ModelApi.deleteModel(row.id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch (error) {
    handleActionError(error)
  }
}

const handleClean = async (row: ModelInfo) => {
  try {
    await message.confirm('是否确认清理流程名字为"' + resolveModelDisplayName(row) + '"的数据项?')
    await ModelApi.cleanModel(row.id)
    message.success('清理成功')
    await getList()
  } catch (error) {
    handleActionError(error)
  }
}

const handleChangeState = async (row: ModelInfo) => {
  if (!row.processDefinition) {
    const errorMessage = `流程模型 ${resolveModelDisplayName(row)} 未发布，不能切换状态`
    message.error(errorMessage)
    throw new Error(errorMessage)
  }
  const state = row.processDefinition.suspensionState
  const newState = state === 1 ? 2 : 1
  try {
    const statusState = state === 1 ? '停用' : '启用'
    await message.confirm(
      '是否确认' + statusState + '流程名字为"' + resolveModelDisplayName(row) + '"的数据项?'
    )
    await ModelApi.updateModelState(row.id, newState)
    message.success(statusState + '成功')
    await getList()
  } catch (error) {
    handleActionError(error)
  }
}

const handleDeploy = async (row: ModelInfo) => {
  try {
    await message.confirm('是否确认发布该流程？')
    await ModelApi.deployModel(row.id)
    message.success('发布成功')
    await getList()
  } catch (error) {
    handleActionError(error)
  }
}

const handleDefinitionList = (row: ModelInfo) => {
  push({
    name: 'BpmProcessDefinition',
    query: {
      key: row.key
    }
  })
}

const handleModelCommand = (command: string, row: ModelInfo) => {
  switch (command) {
    case 'handleDefinitionList':
      handleDefinitionList(row)
      break
    case 'handleDelete':
      handleDelete(row)
      break
    case 'handleChangeState':
      handleChangeState(row)
      break
    case 'handleClean':
      handleClean(row)
      break
    case 'handleReport':
      if (!row.processDefinition) return
      router.push({
        name: 'BpmProcessInstanceReport',
        query: {
          processDefinitionId: row.processDefinition.id,
          processDefinitionKey: row.key
        }
      })
      break
    default:
      break
  }
}

const openModelForm = async (type: string, id?: number) => {
  if (type === 'create') {
    await push({ name: 'BpmModelCreate' })
    return
  }
  await push({
    name: 'BpmModelUpdate',
    params: { id, type }
  })
}

const modelQuickFilter = useTableQuickFilter(
  'bpm.model.main',
  modelQuickFilterDefinitions,
  queryParams,
  getList
)

onMounted(() => {
  getList()
})

</script>

<style lang="scss" scoped>
.bpm-model-page {
  :deep(.el-card__body) {
    padding: 20px 20px 16px;
  }
}

.bpm-model-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.bpm-model-page__title {
  display: flex;
  align-items: center;
  gap: 10px;

  h3 {
    margin: 0;
    color: var(--el-text-color-primary);
    font-size: 18px;
    font-weight: 700;
    line-height: 28px;
  }
}

.bpm-model-page__sort-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bpm-model-page__icon-button {
  width: 32px;
  padding: 8px;
}

.bpm-model-unified-table {
  :deep(.el-table__header th.el-table__cell) {
    background: #f3f6f9;
    color: #596579;
    font-weight: 600;
  }

  :deep(.el-table__cell) {
    padding: 9px 0;
  }
}

.bpm-model-approval-route {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 4px 2px;
}

.bpm-model-approval-route__step {
  position: relative;
  display: flex;
  gap: 14px;
  min-height: 78px;
  padding-bottom: 18px;
}

.bpm-model-approval-route__step::after {
  position: absolute;
  top: 34px;
  bottom: 0;
  left: 16px;
  width: 1px;
  background: #dbe3ef;
  content: '';
}

.bpm-model-approval-route__step:last-child {
  min-height: 52px;
  padding-bottom: 0;
}

.bpm-model-approval-route__step:last-child::after {
  display: none;
}

.bpm-model-approval-route__marker {
  z-index: 1;
  display: inline-flex;
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  align-items: center;
  justify-content: center;
  border: 1px solid #1677ff;
  border-radius: 50%;
  background: #ffffff;
  color: #1677ff;
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
}

.bpm-model-approval-route__content {
  min-width: 0;
  flex: 1;
  padding: 7px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
}

.bpm-model-approval-route__label {
  color: #596579;
  font-size: 13px;
  font-weight: 600;
  line-height: 18px;
}

.bpm-model-approval-route__value {
  margin-top: 4px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  overflow-wrap: anywhere;
  white-space: pre-line;
}

.bpm-model-name-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.bpm-model-name-cell__drag {
  flex: 0 0 auto;
  color: #8a909c;
  cursor: move;
}

.bpm-model-name-cell__icon,
.bpm-model-name-cell__fallback {
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  border-radius: 6px;
}

.bpm-model-name-cell__fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--el-color-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.bpm-model-name-cell__text {
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bpm-model-deployment-cell {
  display: inline-flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.bpm-model-deployment-cell__time {
  min-width: 150px;
}

.bpm-model-muted {
  color: var(--el-text-color-secondary);
}
</style>
