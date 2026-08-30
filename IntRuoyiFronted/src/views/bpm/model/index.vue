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
          @click="openCreateApprovalParticipantConfig"
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
                @click="openApprovalParticipantConfig(row)"
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
  <Dialog :title="participantConfigDialogTitle" v-model="participantConfigVisible" width="860">
    <el-form
      ref="participantConfigFormRef"
      v-loading="participantConfigLoading"
      :model="participantConfigForm"
      :rules="participantConfigRules"
      label-width="88px"
      data-bpm-model-view="participant-config"
    >
      <el-form-item
        v-if="participantConfigMode === 'create'"
        prop="name"
        label="流程名字"
      >
        <el-input
          v-model="participantConfigForm.name"
          maxlength="80"
          show-word-limit
          clearable
          placeholder="请输入流程名字"
        />
      </el-form-item>
      <el-form-item prop="reviewers" label="审核人">
        <div class="bpm-participant-config">
          <div class="bpm-participant-config__toolbar">
            <el-radio-group v-model="participantConfigForm.reviewers.relation">
              <el-radio-button value="or">或关系</el-radio-button>
              <el-radio-button value="and">和关系</el-radio-button>
            </el-radio-group>
            <el-button link type="primary" @click="addParticipantObject('reviewers')">
              添加审核对象
            </el-button>
          </div>
          <div
            v-for="(item, index) in participantConfigForm.reviewers.objects"
            :key="item.id"
            class="bpm-participant-config__row"
          >
            <el-select
              v-model="item.objectType"
              class="bpm-participant-config__type"
              @change="resetParticipantObject(item)"
            >
              <el-option
                v-for="option in PARTICIPANT_OBJECT_TYPE_OPTIONS"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-select
              v-if="item.objectType === 'user'"
              v-model="item.objectIds"
              class="bpm-participant-config__target"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择用户"
            >
              <el-option
                v-for="user in participantUserList"
                :key="user.id"
                :label="user.nickname || user.username"
                :value="user.id"
              />
            </el-select>
            <el-select
              v-else-if="item.objectType === 'role'"
              v-model="item.objectIds"
              class="bpm-participant-config__target"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择权限角色"
            >
              <el-option
                v-for="role in participantRoleList"
                :key="role.id"
                :label="role.name"
                :value="role.id"
              />
            </el-select>
            <el-tree-select
              v-else-if="item.objectType === 'dept'"
              v-model="item.objectIds"
              class="bpm-participant-config__target"
              :data="participantDeptTree"
              :props="defaultProps"
              multiple
              check-strictly
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择部门"
            />
            <el-tag v-else class="bpm-participant-config__target" effect="plain">
              发起对象直属主管
            </el-tag>
            <el-button
              link
              type="danger"
              :disabled="participantConfigForm.reviewers.objects.length === 1"
              @click="removeParticipantObject('reviewers', index)"
            >
              删除
            </el-button>
          </div>
        </div>
      </el-form-item>
      <el-form-item prop="approvers" label="批准人">
        <div class="bpm-participant-config">
          <div class="bpm-participant-config__toolbar">
            <el-radio-group v-model="participantConfigForm.approvers.relation">
              <el-radio-button value="or">或关系</el-radio-button>
              <el-radio-button value="and">和关系</el-radio-button>
            </el-radio-group>
            <el-button link type="primary" @click="addParticipantObject('approvers')">
              添加批准对象
            </el-button>
          </div>
          <div
            v-for="(item, index) in participantConfigForm.approvers.objects"
            :key="item.id"
            class="bpm-participant-config__row"
          >
            <el-select
              v-model="item.objectType"
              class="bpm-participant-config__type"
              @change="resetParticipantObject(item)"
            >
              <el-option
                v-for="option in PARTICIPANT_OBJECT_TYPE_OPTIONS"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-select
              v-if="item.objectType === 'user'"
              v-model="item.objectIds"
              class="bpm-participant-config__target"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择用户"
            >
              <el-option
                v-for="user in participantUserList"
                :key="user.id"
                :label="user.nickname || user.username"
                :value="user.id"
              />
            </el-select>
            <el-select
              v-else-if="item.objectType === 'role'"
              v-model="item.objectIds"
              class="bpm-participant-config__target"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择权限角色"
            >
              <el-option
                v-for="role in participantRoleList"
                :key="role.id"
                :label="role.name"
                :value="role.id"
              />
            </el-select>
            <el-tree-select
              v-else-if="item.objectType === 'dept'"
              v-model="item.objectIds"
              class="bpm-participant-config__target"
              :data="participantDeptTree"
              :props="defaultProps"
              multiple
              check-strictly
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择部门"
            />
            <el-tag v-else class="bpm-participant-config__target" effect="plain">
              发起对象直属主管
            </el-tag>
            <el-button
              link
              type="danger"
              @click="removeParticipantObject('approvers', index)"
            >
              删除
            </el-button>
          </div>
          <div
            v-if="participantConfigForm.approvers.objects.length === 0"
            class="bpm-participant-config__empty"
          >
            未配置批准人
          </div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="participantConfigVisible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="participantConfigSaving"
        @click="handleSaveParticipantConfig"
      >
        保存
      </el-button>
    </template>
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
import * as UserApi from '@/api/system/user'
import * as DeptApi from '@/api/system/dept'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  CandidateStrategy,
  NodeType,
  ApproveMethodType,
  ApproveType,
  RejectHandlerType,
  AssignEmptyHandlerType,
  AssignStartUserHandlerType,
  DEFAULT_BUTTON_SETTING,
  START_USER_BUTTON_SETTING
} from '@/components/SimpleProcessDesignerV2/src/consts'
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
import { BpmAutoApproveType, BpmModelFormType, BpmModelType } from '@/utils/constants'
import { checkPermi } from '@/utils/permission'
import { useUserStoreWithOut } from '@/store/modules/user'
import { subString } from '@/utils/index'
import { defaultProps, handleTree } from '@/utils/tree'

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
  approveType?: number
  approveMethod?: number
  approveRatio?: number
  buttonsSetting?: any[]
  fieldsPermission?: Array<Record<string, string>>
  rejectHandler?: { type: number; returnNodeId?: string }
  timeoutHandler?: { enable: boolean }
  assignEmptyHandler?: { type: number; userIds?: number[] }
  assignStartUserHandlerType?: number
  signEnable?: boolean
  reasonRequire?: boolean
  childNode?: SimpleModelNode
  conditionNodes?: SimpleModelNode[]
}

interface ParticipantGroups {
  reviewers: string[]
  approvers: string[]
}

type ParticipantSectionKey = 'reviewers' | 'approvers'
type ParticipantRelation = 'or' | 'and'
type ParticipantObjectType = 'user' | 'role' | 'dept' | 'startUserLeader'

interface ApprovalParticipantObject {
  id: string
  objectType: ParticipantObjectType
  objectIds: number[]
}

interface ApprovalParticipantSection {
  relation: ParticipantRelation
  objects: ApprovalParticipantObject[]
}

interface ApprovalParticipantForm {
  name?: string
  reviewers: ApprovalParticipantSection
  approvers: ApprovalParticipantSection
}

interface CandidateEntry {
  strategy: number
  param: string
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
const participantConfigVisible = ref(false)
const participantConfigLoading = ref(false)
const participantConfigSaving = ref(false)
const participantConfigFormRef = ref()
const participantConfigModel = ref<ModelInfo | null>(null)
const participantConfigMode = ref<'create' | 'update'>('update')
const participantUserList = ref<UserApi.UserVO[]>([])
const participantRoleList = ref<RoleApi.RoleVO[]>([])
const participantDeptList = ref<DeptApi.DeptVO[]>([])
const participantDeptTree = ref<DeptApi.DeptVO[]>([])

const PARTICIPANT_OBJECT_TYPE_OPTIONS: Array<{ label: string; value: ParticipantObjectType }> = [
  { label: '用户', value: 'user' },
  { label: '权限角色', value: 'role' },
  { label: '部门', value: 'dept' },
  { label: '发起对象直属主管', value: 'startUserLeader' }
]

const PARTICIPANT_SECTION_LABELS: Record<ParticipantSectionKey, string> = {
  reviewers: '审核人',
  approvers: '批准人'
}

const createParticipantObject = (objectType: ParticipantObjectType = 'user'): ApprovalParticipantObject => ({
  id: `participant_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
  objectType,
  objectIds: []
})

const createEmptyParticipantSection = (): ApprovalParticipantSection => ({
  relation: 'or',
  objects: []
})

const createParticipantSection = (): ApprovalParticipantSection => ({
  relation: 'or',
  objects: [createParticipantObject()]
})

const createParticipantConfigForm = (): ApprovalParticipantForm => ({
  name: '',
  reviewers: createParticipantSection(),
  approvers: createEmptyParticipantSection()
})

const participantConfigForm = reactive<ApprovalParticipantForm>(createParticipantConfigForm())

const resetParticipantConfigForm = () => {
  const initialValue = createParticipantConfigForm()
  participantConfigForm.name = initialValue.name
  participantConfigForm.reviewers = initialValue.reviewers
  participantConfigForm.approvers = initialValue.approvers
}

const participantConfigRules = {
  name: [
    {
      validator: (_rule: unknown, value: string | undefined, callback: (error?: Error) => void) => {
        if (participantConfigMode.value === 'create' && !value?.trim()) {
          callback(new Error('请输入流程名字'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  reviewers: [
    {
      required: true,
      validator: (_rule: unknown, value: ApprovalParticipantSection, callback: (error?: Error) => void) => {
        if (flattenParticipantObjects(value).length === 0) {
          callback(new Error('请至少配置一个审核人'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

const addParticipantObject = (sectionKey: ParticipantSectionKey) => {
  participantConfigForm[sectionKey].objects.push(createParticipantObject())
}

const removeParticipantObject = (sectionKey: ParticipantSectionKey, index: number) => {
  const objects = participantConfigForm[sectionKey].objects
  if (sectionKey === 'reviewers' && objects.length <= 1) return
  objects.splice(index, 1)
}

const resetParticipantObject = (item: ApprovalParticipantObject) => {
  item.objectIds = []
}

const loadParticipantConfigOptions = async () => {
  const [users, roles, depts] = await Promise.all([
    UserApi.getSimpleUserList(),
    RoleApi.getSimpleRoleList(),
    DeptApi.getSimpleDeptList()
  ])
  participantUserList.value = users
  participantRoleList.value = roles
  participantDeptList.value = depts
  participantDeptTree.value = handleTree(depts)
  approvalRoleList.value = roles
}

const parseModelSimpleModel = (simpleModel: unknown): SimpleModelNode | undefined => {
  if (!simpleModel) return undefined
  if (typeof simpleModel === 'string') {
    return simpleModel.trim() ? JSON.parse(simpleModel) : undefined
  }
  return simpleModel as SimpleModelNode
}

const getParticipantTargetIds = (item: ApprovalParticipantObject) => {
  if (item.objectType === 'startUserLeader') return [1]
  return item.objectIds.filter((id) => id !== undefined && id !== null)
}

const toCandidateEntries = (item: ApprovalParticipantObject): CandidateEntry[] => {
  const targetIds = getParticipantTargetIds(item)
  if (targetIds.length === 0) return []
  if (item.objectType === 'user') {
    return targetIds.map((id) => ({ strategy: CandidateStrategy.USER, param: String(id) }))
  }
  if (item.objectType === 'role') {
    return targetIds.map((id) => ({ strategy: CandidateStrategy.ROLE, param: String(id) }))
  }
  if (item.objectType === 'dept') {
    return targetIds.map((id) => ({ strategy: CandidateStrategy.DEPT_MEMBER, param: String(id) }))
  }
  return [{ strategy: CandidateStrategy.START_USER_DEPT_LEADER, param: '1' }]
}

const flattenParticipantObjects = (section: ApprovalParticipantSection | undefined): CandidateEntry[] => {
  if (!section) return []
  return section.objects.flatMap((item) => toCandidateEntries(item))
}

const hasIncompleteParticipantObject = (section: ApprovalParticipantSection, allowEmptySection: boolean) => {
  const sectionIsEmpty = flattenParticipantObjects(section).length === 0
  return section.objects.some((item) => {
    const targetIds = getParticipantTargetIds(item)
    if (allowEmptySection && sectionIsEmpty && item.objectType !== 'startUserLeader' && targetIds.length === 0) {
      return false
    }
    return targetIds.length === 0
  })
}

const validateParticipantConfig = () => {
  if (flattenParticipantObjects(participantConfigForm.reviewers).length === 0) {
    message.warning('请至少配置一个审核人')
    return false
  }
  if (hasIncompleteParticipantObject(participantConfigForm.reviewers, false)) {
    message.warning('请完善审核对象')
    return false
  }
  if (hasIncompleteParticipantObject(participantConfigForm.approvers, true)) {
    message.warning('请完善批准对象')
    return false
  }
  return true
}

const resolveParticipantEntryName = (entry: CandidateEntry) => {
  if (entry.strategy === CandidateStrategy.USER) {
    const user = participantUserList.value.find((item) => String(item.id) === entry.param)
    return `用户：${user?.nickname || user?.username || `ID ${entry.param}`}`
  }
  if (entry.strategy === CandidateStrategy.ROLE) {
    const role = participantRoleList.value.find((item) => String(item.id) === entry.param)
    return `审批角色：${role?.name || `ID ${entry.param}`}`
  }
  if (entry.strategy === CandidateStrategy.DEPT_MEMBER) {
    const dept = participantDeptList.value.find((item) => String(item.id) === entry.param)
    return `部门：${dept?.name || `ID ${entry.param}`}`
  }
  return '发起对象直属主管'
}

const formatParticipantEntryText = (entries: CandidateEntry[]) => {
  return entries.map((entry) => resolveParticipantEntryName(entry)).join('；')
}

const createSimpleModelNodeId = (prefix: string) =>
  `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`

const buildApprovalTaskNode = (
  sectionKey: ParticipantSectionKey,
  entries: CandidateEntry[],
  nodeName = PARTICIPANT_SECTION_LABELS[sectionKey]
): SimpleModelNode => {
  const candidateStrategy = entries.length === 1 ? entries[0].strategy : CandidateStrategy.MIXED
  const candidateParam = entries.length === 1 ? entries[0].param : JSON.stringify(entries)
  return {
    id: createSimpleModelNodeId('Activity'),
    type: NodeType.USER_TASK_NODE,
    name: nodeName,
    showText: formatParticipantEntryText(entries),
    approveType: ApproveType.USER,
    candidateStrategy,
    candidateParam,
    approveMethod: ApproveMethodType.ANY_APPROVE,
    approveRatio: 100,
    rejectHandler: {
      type: RejectHandlerType.FINISH_PROCESS
    },
    timeoutHandler: {
      enable: false
    },
    assignEmptyHandler: {
      type: AssignEmptyHandlerType.REJECT
    },
    assignStartUserHandlerType: AssignStartUserHandlerType.START_USER_AUDIT,
    fieldsPermission: [],
    buttonsSetting: DEFAULT_BUTTON_SETTING,
    signEnable: false,
    reasonRequire: false
  }
}

const buildParticipantSectionNode = (
  sectionKey: ParticipantSectionKey,
  section: ApprovalParticipantSection
): SimpleModelNode | undefined => {
  const entries = flattenParticipantObjects(section)
  if (entries.length === 0) return undefined
  if (section.relation === 'or' || entries.length === 1) {
    return buildApprovalTaskNode(sectionKey, entries)
  }
  return {
    id: createSimpleModelNodeId('GateWay'),
    type: NodeType.PARALLEL_BRANCH_NODE,
    name: `${PARTICIPANT_SECTION_LABELS[sectionKey]}和关系`,
    showText: `${PARTICIPANT_SECTION_LABELS[sectionKey]}：全部对象通过`,
    conditionNodes: entries.map((entry, index) => ({
      id: createSimpleModelNodeId('Flow'),
      type: NodeType.CONDITION_NODE,
      name: `${PARTICIPANT_SECTION_LABELS[sectionKey]}对象${index + 1}`,
      showText: '无需配置条件同时执行',
      childNode: buildApprovalTaskNode(sectionKey, [entry], PARTICIPANT_SECTION_LABELS[sectionKey])
    }))
  }
}

const buildApprovalParticipantSimpleModel = (): SimpleModelNode => {
  const reviewerNode = buildParticipantSectionNode('reviewers', participantConfigForm.reviewers)
  if (!reviewerNode) {
    throw new Error('请至少配置一个审核人')
  }
  const approverNode = buildParticipantSectionNode('approvers', participantConfigForm.approvers)
  reviewerNode.childNode = approverNode
  return {
    id: 'StartUserNode',
    type: NodeType.START_USER_NODE,
    name: '发起人',
    showText: '全部人员可发起',
    fieldsPermission: [],
    buttonsSetting: START_USER_BUTTON_SETTING,
    childNode: reviewerNode
  }
}

const resolveParticipantConfigName = () => participantConfigForm.name?.trim() || ''

const buildApprovalModelKey = (name: string) => {
  const nameSeed = name
    .trim()
    .replace(/[^A-Za-z0-9_]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .slice(0, 24)
    .toLowerCase()
  const suffix = `${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`
  return `approval_model_${nameSeed ? `${nameSeed}_` : ''}${suffix}`
}

const resolveObjectTypeByCandidateStrategy = (strategy?: number | string): ParticipantObjectType => {
  const strategyText = String(strategy)
  if (strategyText === String(CandidateStrategy.ROLE)) return 'role'
  if (strategyText === String(CandidateStrategy.DEPT_MEMBER)) return 'dept'
  if (strategyText === String(CandidateStrategy.START_USER_DEPT_LEADER)) return 'startUserLeader'
  return 'user'
}

const parseMixedCandidateEntries = (candidateParam?: string): CandidateEntry[] => {
  if (!candidateParam?.trim()) return []
  const entries = JSON.parse(candidateParam)
  if (!Array.isArray(entries)) {
    throw new Error('混合审批对象参数必须是数组')
  }
  return entries.map((entry) => {
    if (!entry?.strategy || !entry?.param) {
      throw new Error('混合审批对象参数缺少类型或目标')
    }
    return { strategy: Number(entry.strategy), param: String(entry.param) }
  })
}

const parseCandidateEntriesFromNode = (node: SimpleModelNode): CandidateEntry[] => {
  if (String(node.candidateStrategy) === String(CandidateStrategy.MIXED)) {
    return parseMixedCandidateEntries(node.candidateParam)
  }
  if (!node.candidateStrategy || !node.candidateParam) return []
  return String(node.candidateParam)
    .split(',')
    .map((param) => param.trim())
    .filter(Boolean)
    .map((param) => ({ strategy: Number(node.candidateStrategy), param }))
}

const toParticipantObjects = (entries: CandidateEntry[]): ApprovalParticipantObject[] => {
  const objects = entries.map((entry) => ({
    id: `participant_${entry.strategy}_${entry.param}_${Math.random().toString(36).slice(2, 8)}`,
    objectType: resolveObjectTypeByCandidateStrategy(entry.strategy),
    objectIds:
      String(entry.strategy) === String(CandidateStrategy.START_USER_DEPT_LEADER)
        ? []
        : [Number(entry.param)]
  }))
  return objects
}

const parseParticipantSectionFromNode = (
  sectionKey: ParticipantSectionKey,
  node: SimpleModelNode | undefined
): ApprovalParticipantSection => {
  if (!node) return createEmptyParticipantSection()
  if (node.type === NodeType.PARALLEL_BRANCH_NODE) {
    const entries = (node.conditionNodes || []).flatMap((conditionNode) =>
      parseCandidateEntriesFromNode(conditionNode.childNode || {})
    )
    return {
      relation: 'and',
      objects: toParticipantObjects(entries)
    }
  }
  return {
    relation: 'or',
    objects: toParticipantObjects(parseCandidateEntriesFromNode(node))
  }
}

const normalizeParticipantSectionForEdit = (
  sectionKey: ParticipantSectionKey,
  section: ApprovalParticipantSection | undefined
): ApprovalParticipantSection => {
  if (!section || (sectionKey === 'reviewers' && section.objects.length === 0)) {
    return createParticipantSection()
  }
  return section
}

const parseSimpleParticipantConfig = (simpleModel?: SimpleModelNode): ApprovalParticipantForm | undefined => {
  const firstNode = simpleModel?.type === NodeType.START_USER_NODE ? simpleModel.childNode : simpleModel
  if (!firstNode) return undefined
  return {
    reviewers: parseParticipantSectionFromNode('reviewers', firstNode),
    approvers: parseParticipantSectionFromNode('approvers', firstNode.childNode)
  }
}

const getBpmnCandidateValue = (element: Element, localName: string) => {
  return (
    getBpmnElementText(element, localName) ||
    element.getAttribute(localName) ||
    element.getAttribute(`flowable:${localName}`) ||
    element.getAttribute(`activiti:${localName}`) ||
    undefined
  )
}

const parseBpmnApprovalTaskNodes = (bpmnXml?: string): SimpleModelNode[] => {
  if (!bpmnXml?.trim()) return []
  const xmlDoc = new DOMParser().parseFromString(bpmnXml, 'application/xml')
  return Array.from(xmlDoc.getElementsByTagName('*'))
    .filter((item) => item.localName === 'userTask')
    .map((task) => ({
      id: task.getAttribute('id') || undefined,
      type: NodeType.USER_TASK_NODE,
      name: task.getAttribute('name') || task.getAttribute('id') || '',
      candidateStrategy: getBpmnCandidateValue(task, 'candidateStrategy'),
      candidateParam: getBpmnCandidateValue(task, 'candidateParam')
    }))
    .filter((node) => parseCandidateEntriesFromNode(node).length > 0)
}

const parseBpmnParticipantConfig = (bpmnXml?: string): ApprovalParticipantForm | undefined => {
  const taskNodes = parseBpmnApprovalTaskNodes(bpmnXml)
  if (taskNodes.length === 0) return undefined
  return {
    reviewers: parseParticipantSectionFromNode('reviewers', taskNodes[0]),
    approvers: parseParticipantSectionFromNode('approvers', taskNodes[1])
  }
}

const participantConfigHasEntries = (config: ApprovalParticipantForm | undefined) => {
  if (!config) return false
  return (
    flattenParticipantObjects(config.reviewers).length > 0 ||
    flattenParticipantObjects(config.approvers).length > 0
  )
}

const resolveBusinessParticipantConfig = (model?: ModelInfo | null): ApprovalParticipantForm | undefined => {
  if (!isRegistrationCertificateApprovalModel(model)) return undefined
  const role = participantRoleList.value.find(
    (item) => item.code === REGISTRATION_CERTIFICATE_APPROVER_ROLE_CODE
  )
  if (!role?.id) {
    throw new Error(`注册证审批角色未配置：${REGISTRATION_CERTIFICATE_APPROVER_ROLE_CODE}`)
  }
  return {
    reviewers: {
      relation: 'or',
      objects: [
        {
          id: `participant_role_${role.id}`,
          objectType: 'role',
          objectIds: [Number(role.id)]
        }
      ]
    },
    approvers: createEmptyParticipantSection()
  }
}

const resolveCurrentParticipantConfig = (model?: ModelInfo | null): ApprovalParticipantForm | undefined => {
  if (!model) return undefined
  const simpleConfig = parseSimpleParticipantConfig(parseModelSimpleModel(model.simpleModel))
  if (participantConfigHasEntries(simpleConfig)) return simpleConfig
  const businessConfig = resolveBusinessParticipantConfig(model)
  if (businessConfig) return businessConfig
  return parseBpmnParticipantConfig(model.bpmnXml)
}

const hydrateParticipantConfig = (config?: ApprovalParticipantForm) => {
  resetParticipantConfigForm()
  participantConfigForm.reviewers = normalizeParticipantSectionForEdit('reviewers', config?.reviewers)
  participantConfigForm.approvers = normalizeParticipantSectionForEdit('approvers', config?.approvers)
}

const participantConfigDialogTitle = computed(() => {
  if (participantConfigMode.value === 'create') {
    return '新建审批模型'
  }
  const modelName = resolveModelDisplayName(participantConfigModel.value)
  return modelName ? `修改审批模型：${modelName}` : '修改审批模型'
})

const openCreateApprovalParticipantConfig = async () => {
  participantConfigMode.value = 'create'
  participantConfigModel.value = null
  resetParticipantConfigForm()
  participantConfigLoading.value = true
  try {
    await loadParticipantConfigOptions()
    participantConfigVisible.value = true
    await nextTick()
    participantConfigFormRef.value?.clearValidate()
  } catch (error) {
    message.error('审批对象配置加载失败，请查看接口响应')
    throw error
  } finally {
    participantConfigLoading.value = false
  }
}

const openApprovalParticipantConfig = async (row: ModelInfo) => {
  participantConfigMode.value = 'update'
  participantConfigLoading.value = true
  try {
    const [modelDetail] = await Promise.all([
      ModelApi.getModel(String(row.id)),
      loadParticipantConfigOptions()
    ])
    const simpleModel = parseModelSimpleModel(modelDetail?.simpleModel)
    participantConfigModel.value = {
      ...row,
      ...modelDetail,
      simpleModel
    }
    hydrateParticipantConfig(resolveCurrentParticipantConfig(participantConfigModel.value))
    participantConfigVisible.value = true
    await nextTick()
    participantConfigFormRef.value?.clearValidate()
  } catch (error) {
    message.error('审批对象配置加载失败，请查看接口响应')
    throw error
  } finally {
    participantConfigLoading.value = false
  }
}

const handleSaveParticipantConfig = async () => {
  let formValid = false
  try {
    formValid = await participantConfigFormRef.value?.validate()
  } catch {
    formValid = false
  }
  if (!formValid || !validateParticipantConfig()) return
  if (participantConfigMode.value === 'create') {
    if (!userStore.getUser.id) {
      const errorMessage = '当前用户信息未加载，不能新建审批模型'
      message.error(errorMessage)
      throw new Error(errorMessage)
    }
    participantConfigSaving.value = true
    try {
      await ModelApi.createModel({
        name: resolveParticipantConfigName(),
        key: buildApprovalModelKey(resolveParticipantConfigName()),
        type: BpmModelType.SIMPLE,
        formType: BpmModelFormType.NORMAL,
        visible: true,
        startUserIds: [],
        startDeptIds: [],
        managerUserIds: [userStore.getUser.id],
        allowCancelRunningProcess: true,
        allowWithdrawTask: false,
        processIdRule: {
          enable: false,
          prefix: '',
          infix: '',
          postfix: '',
          length: 5
        },
        autoApprovalType: BpmAutoApproveType.NONE,
        titleSetting: {
          enable: false,
          title: ''
        },
        summarySetting: {
          enable: false,
          summary: []
        },
        printTemplateSetting: {
          enable: false
        },
        simpleModel: buildApprovalParticipantSimpleModel()
      })
      message.success('审批模型已新建')
      participantConfigVisible.value = false
      await getList()
    } catch (error) {
      message.error('审批模型新建失败，请查看接口响应')
      throw error
    } finally {
      participantConfigSaving.value = false
    }
    return
  }
  if (!participantConfigModel.value) {
    const errorMessage = '审批模型未加载，不能保存'
    message.error(errorMessage)
    throw new Error(errorMessage)
  }
  participantConfigSaving.value = true
  try {
    await ModelApi.updateModel({
      ...participantConfigModel.value,
      type: BpmModelType.SIMPLE,
      bpmnXml: undefined,
      simpleModel: buildApprovalParticipantSimpleModel()
    })
    message.success('审批模型已保存')
    participantConfigVisible.value = false
    await getList()
  } catch (error) {
    message.error('审批模型保存失败，请查看接口响应')
    throw error
  } finally {
    participantConfigSaving.value = false
  }
}

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
  '60': '流程表达式',
  '70': '混合审批对象'
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
  if (strategyKey === String(CandidateStrategy.MIXED)) {
    const mixedText = parseMixedCandidateEntries(candidateParam)
      .map((entry) => formatCandidateRule(entry.strategy, entry.param, roles))
      .filter(Boolean)
      .join('；')
    return mixedText || '混合审批对象：未配置'
  }
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

const isApprovalRoleRuleText = (text: string) => text.startsWith('审批角色：')

const formatParticipantNode = (nodeName?: string, ruleText?: string) => {
  const name = formatApprovalRouteTemplateText(nodeName) || '未命名节点'
  const text = formatApprovalRouteTemplateText(ruleText)
  if (!text || text === name) return `节点：${name}`
  if (isApprovalRoleRuleText(text)) return `节点：${name}\n${text}`
  return `节点：${name}\n审批对象：${text}`
}

const formatApprovalRouteParticipant = (participantText: string, routeName?: string) => {
  const approvalRouteName = routeName?.trim() || '未配置审批路线名称'
  const approvalParticipantLine = participantText
    .split('\n')
    .map((item) => item.trim())
    .find((item) => item.startsWith('审批角色：') || item.startsWith('审批对象：'))
  return approvalParticipantLine
    ? `审批路线：${approvalRouteName}\n${approvalParticipantLine}`
    : `审批路线：${approvalRouteName}`
}

const formatBusinessApprovalRouteParticipant = (routeName: string, roleNames: string) => {
  const approvalRouteName = routeName.trim() || '未配置审批路线名称'
  return `审批路线：${approvalRouteName}\n审批角色：${roleNames}`
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
    reviewers: [formatBusinessApprovalRouteParticipant(resolveModelDisplayName(model), roleNames)],
    approvers: []
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

.bpm-participant-config {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 12px;
}

.bpm-participant-config__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.bpm-participant-config__row {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr) 40px;
  gap: 10px;
  align-items: center;
}

.bpm-participant-config__type,
.bpm-participant-config__target {
  width: 100%;
  min-width: 0;
}

.bpm-participant-config__target.el-tag {
  min-height: 32px;
  justify-content: flex-start;
}

.bpm-participant-config__empty {
  min-height: 32px;
  display: flex;
  align-items: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

@media (max-width: 768px) {
  .bpm-participant-config__toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .bpm-participant-config__row {
    grid-template-columns: minmax(0, 1fr) 40px;
  }

  .bpm-participant-config__type {
    grid-column: 1 / -1;
  }
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
