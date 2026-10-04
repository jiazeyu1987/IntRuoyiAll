<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="1100px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="110px"
    >
      <el-form-item label="文件类别" prop="categoryId">
        <el-select
          v-if="categorySelectable"
          v-model="formData.categoryId"
          class="!w-320px"
          filterable
          placeholder="请选择文件类别"
        >
          <el-option
            v-for="item in categories"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <span v-else>{{ currentCategory?.name || '-' }}</span>
      </el-form-item>
      <el-form-item label="动作类型" prop="actionType">
        <el-select
          v-model="formData.actionType"
          class="!w-320px"
          :disabled="editingRoute"
          placeholder="请选择动作类型"
          @change="handleActionTypeChange"
        >
          <el-option
            v-for="item in ROUTE_ACTION_TYPE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="生效时间" prop="effectiveTime">
        <el-date-picker
          v-model="formData.effectiveTime"
          class="!w-320px"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择路线生效时间"
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入路线说明" />
      </el-form-item>
      <div class="mb-12px flex items-center justify-between">
        <div>
          <div class="text-13px font-600">审批节点</div>
          <div class="mt-2px text-12px text-gray-500">
            {{ routePolicyDescription }}
          </div>
        </div>
        <el-button type="primary" plain @click="addNode">
          <Icon icon="ep:plus" class="mr-5px" />
          新增节点
        </el-button>
      </div>
      <el-table :data="formData.nodes" empty-text="请至少新增一个审批节点">
        <el-table-column label="阶段号" width="90">
          <template #default="{ row }">
            <el-input-number v-model="row.stageNo" :min="1" class="w-full" />
          </template>
        </el-table-column>
        <el-table-column label="阶段名称" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.stageName" placeholder="例如：文控审核" />
          </template>
        </el-table-column>
        <el-table-column label="候选来源" width="140">
          <template #default="{ row }">
            <el-select v-model="row.candidateSourceType" class="w-full" @change="handleSourceTypeChange(row)">
              <el-option
                v-for="item in ROUTE_CANDIDATE_SOURCE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="候选对象" min-width="220">
          <template #default="{ row }">
            <el-select
              v-if="row.candidateSourceType === 'USER'"
              v-model="row.candidateSourceId"
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
              v-else-if="row.candidateSourceType === 'POSITION'"
              v-model="row.candidateSourceId"
              class="w-full"
              clearable
              filterable
              placeholder="请选择 DCC 审批岗位"
            >
              <el-option
                v-for="item in positions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
            <el-select
              v-else
              v-model="row.candidateSourceIds"
              class="w-full"
              clearable
              filterable
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择会签部门"
            >
              <el-option
                v-for="item in departments"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="审批方式" width="150">
          <template #default="{ row }">
            <el-tag effect="plain">{{ getFixedRouteApprovalPolicy(row.stageNo)?.label || '不支持的固定阶段' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="通过比例" width="110">
          <template #default="{ row }">
            {{ getFixedRouteApprovalPolicy(row.stageNo)?.ratioLabel || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="必经" align="center" width="80">
          <template #default="{ row }">
            <el-tag type="success" effect="plain">
              {{ getFixedRouteApprovalPolicy(row.stageNo)?.requiredLabel || '固定必经' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="90">
          <template #default="{ row }">
            <el-input-number v-model="row.sort" :min="0" class="w-full" />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="88">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeNode($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="formLoading" @click="submitForm">保存路线</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormRules } from 'element-plus'
import {
  saveApprovalRoute,
  type ControlledFileApprovalRouteActionType,
  type ControlledFileApprovalRouteNodeVO,
  type ControlledFileApprovalRouteSaveReqVO,
  type ControlledFileApprovalRouteVO
} from '@/api/dcc/controlledFile/approvalRoutes'
import type { ControlledFileApprovalPositionVO } from '@/api/dcc/controlledFile/approvalPositions'
import type { ControlledFileCategoryVO } from '@/api/dcc/controlledFile/fileCategories'
import type { DeptVO } from '@/api/system/dept'
import type { UserVO } from '@/api/system/user'
import {
  ROUTE_ACTION_TYPE_OPTIONS,
  ROUTE_CANDIDATE_SOURCE_OPTIONS
} from '../../shared/options'
import { formatDccSimpleUserLabel } from '../../shared/utils'
import { formatDateTimeValue } from '@/utils/formatTime'

defineOptions({ name: 'DccControlledFileRouteForm' })

type ControlledFileApprovalRouteFormVO = Omit<ControlledFileApprovalRouteVO, 'effectiveTime'> & {
  effectiveTime: string
}

type FixedRouteApprovalPolicy = {
  approveMethod: 'ANY' | 'ALL'
  approveRatio?: number | null
  required: boolean
  label: string
  ratioLabel: string
  requiredLabel: string
}

const FIXED_ROUTE_APPROVAL_POLICY: Record<number, FixedRouteApprovalPolicy> = {
  1: {
    approveMethod: 'ANY',
    approveRatio: null,
    required: true,
    label: '任意通过',
    ratioLabel: '-',
    requiredLabel: '必经'
  },
  2: {
    approveMethod: 'ALL',
    approveRatio: 100,
    required: true,
    label: '全部通过',
    ratioLabel: '100%',
    requiredLabel: '必经'
  },
  3: {
    approveMethod: 'ANY',
    approveRatio: null,
    required: true,
    label: '任意通过',
    ratioLabel: '-',
    requiredLabel: '必经'
  },
  4: {
    approveMethod: 'ANY',
    approveRatio: null,
    required: true,
    label: '任意通过',
    ratioLabel: '-',
    requiredLabel: '必经'
  }
}

const EXPECTED_FIXED_ROUTE_STAGE_NOS = [1, 2, 3, 4]
const ACTION_ROUTE_APPROVAL_POLICY: Record<number, FixedRouteApprovalPolicy> = {
  1: {
    approveMethod: 'ALL',
    approveRatio: 100,
    required: true,
    label: '全部通过',
    ratioLabel: '100%',
    requiredLabel: '必经'
  },
  2: {
    approveMethod: 'ANY',
    approveRatio: null,
    required: true,
    label: '任意通过',
    ratioLabel: '-',
    requiredLabel: '必经'
  },
  3: {
    approveMethod: 'ANY',
    approveRatio: null,
    required: true,
    label: '任意通过',
    ratioLabel: '-',
    requiredLabel: '必经'
  }
}
const EXPECTED_ACTION_ROUTE_STAGE_NOS = [1, 2, 3]
const ACTION_ROUTE_DEFAULT_STAGE_NAMES: Record<number, string> = {
  1: '会签',
  2: '批准',
  3: '文控审核'
}
const LEGACY_ROUTE_DEFAULT_STAGE_NAMES: Record<number, string> = {
  1: '文控审核',
  2: '会签审核',
  3: '会签批准',
  4: '文控批准'
}

const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('审批路线')
const formLoading = ref(false)
const formRef = ref()
const categories = ref<Array<ControlledFileCategoryVO & { id: number }>>([])
const editingRoute = ref(false)
const users = ref<UserVO[]>([])
const positions = ref<ControlledFileApprovalPositionVO[]>([])
const departments = ref<DeptVO[]>([])
const formData = ref<ControlledFileApprovalRouteFormVO>({
  categoryId: undefined,
  actionType: 'NEW',
  effectiveTime: '',
  remark: '',
  nodes: []
})

const formRules = reactive<FormRules>({
  categoryId: [{ required: true, message: '文件类别不能为空', trigger: 'change' }],
  actionType: [{ required: true, message: '动作类型不能为空', trigger: 'change' }],
  effectiveTime: [{ required: true, message: '生效时间不能为空', trigger: 'change' }]
})

const emit = defineEmits<{
  success: []
}>()

const isLegacyRoute = computed(() => formData.value.actionType === 'LEGACY')
const currentRouteApprovalPolicy = computed(() =>
  isLegacyRoute.value ? FIXED_ROUTE_APPROVAL_POLICY : ACTION_ROUTE_APPROVAL_POLICY
)
const currentExpectedStageNos = computed(() =>
  isLegacyRoute.value ? EXPECTED_FIXED_ROUTE_STAGE_NOS
    : formData.value.actionType === 'OBSOLETE' ? [1, 2] : EXPECTED_ACTION_ROUTE_STAGE_NOS
)
const routePolicyDescription = computed(() =>
  isLegacyRoute.value
    ? '历史通用流程固定四阶段：文控审核、会签审核、会签批准、文控批准。'
    : formData.value.actionType === 'OBSOLETE'
      ? '作废：各部门指派并签名会签，批准通过即作废结束。'
      : '上传/升版：会签→批准→培训（如需）→文控审核→受控→下发。'
)
const getFixedRouteApprovalPolicy = (stageNo?: number) =>
  stageNo == null ? undefined : currentRouteApprovalPolicy.value[stageNo]

const resolveDefaultStageName = (stageNo: number) =>
  isLegacyRoute.value
    ? LEGACY_ROUTE_DEFAULT_STAGE_NAMES[stageNo] || ''
    : ACTION_ROUTE_DEFAULT_STAGE_NAMES[stageNo] || ''

const createDefaultNode = (index: number): ControlledFileApprovalRouteNodeVO => ({
  stageNo: currentExpectedStageNos.value[index] ?? index + 1,
  stageName: resolveDefaultStageName(currentExpectedStageNos.value[index] ?? index + 1),
  candidateSourceType: !isLegacyRoute.value && index === 0 ? 'DEPT' : 'POSITION',
  candidateSourceId: 0,
  candidateSourceIds: [],
  approveMethod: getFixedRouteApprovalPolicy(currentExpectedStageNos.value[index] ?? index + 1)?.approveMethod || 'ANY',
  approveRatio: getFixedRouteApprovalPolicy(currentExpectedStageNos.value[index] ?? index + 1)?.approveRatio ?? undefined,
  required: getFixedRouteApprovalPolicy(currentExpectedStageNos.value[index] ?? index + 1)?.required ?? true,
  sort: index + 1
})

const categorySelectable = computed(() => !editingRoute.value)
const currentCategory = computed(() =>
  categories.value.find((item) => item.id === formData.value.categoryId)
)

const open = (payload: {
  category?: ControlledFileCategoryVO
  categories?: Array<ControlledFileCategoryVO & { id: number }>
  route?: ControlledFileApprovalRouteVO
  users: UserVO[]
  positions: ControlledFileApprovalPositionVO[]
  departments: DeptVO[]
}) => {
  dialogVisible.value = true
  editingRoute.value = Boolean(payload.route)
  const routeCategory = payload.category?.id
    ? (payload.category as ControlledFileCategoryVO & { id: number })
    : undefined
  categories.value = payload.categories ? [...payload.categories] : []
  if (routeCategory && !categories.value.some((item) => item.id === routeCategory.id)) {
    categories.value = [routeCategory, ...categories.value]
  }
  users.value = payload.users
  positions.value = payload.positions
  departments.value = payload.departments
  resetForm()
  if (payload.route) {
    formData.value = {
      ...JSON.parse(JSON.stringify(payload.route)),
      categoryId: routeCategory?.id ?? payload.route.categoryId,
      actionType: payload.route.actionType || 'LEGACY',
      effectiveTime: formatDateTimeValue(payload.route.effectiveTime, ''),
      nodes: payload.route.nodes.map((item) => ({
        ...JSON.parse(JSON.stringify(item)),
        candidateSourceIds: item.candidateSourceIds ?? (item.candidateSourceId ? [item.candidateSourceId] : [])
      }))
    }
  } else {
    formData.value.categoryId = routeCategory?.id
    formData.value.actionType = 'NEW'
  }
  dialogTitle.value = payload.route
    ? `编辑路线 - ${currentCategory.value?.name || payload.route.categoryName || '-'}`
    : '新增路线'
  if (formData.value.nodes.length === 0) {
    resetNodesForActionType()
  }
}

defineExpose({ open })

const resetForm = () => {
  formData.value = {
    categoryId: undefined,
    actionType: 'NEW',
    effectiveTime: '',
    remark: '',
    nodes: []
  }
  formRef.value?.resetFields()
}

const addNode = () => {
  formData.value.nodes.push(createDefaultNode(formData.value.nodes.length))
}

const resetNodesForActionType = () => {
  formData.value.nodes = currentExpectedStageNos.value.map((_, index) => createDefaultNode(index))
}

const handleActionTypeChange = () => {
  resetNodesForActionType()
}

const removeNode = (index: number) => {
  formData.value.nodes.splice(index, 1)
}

const handleSourceTypeChange = (row: ControlledFileApprovalRouteNodeVO) => {
  row.candidateSourceId = 0
  row.candidateSourceIds = []
}

const resolveCandidateSourceIds = (item: ControlledFileApprovalRouteNodeVO) => {
  const ids = item.candidateSourceType === 'DEPT'
    ? item.candidateSourceIds || []
    : item.candidateSourceId
      ? [item.candidateSourceId]
      : []
  return ids.filter((id): id is number => Number.isFinite(Number(id)) && Number(id) > 0)
}

const normalizeRouteNodeFixedApprovalPolicy = (item: ControlledFileApprovalRouteNodeVO) => {
  const fixedPolicy = getFixedRouteApprovalPolicy(item.stageNo)
  const candidateSourceIds = resolveCandidateSourceIds(item)
  if (!fixedPolicy || candidateSourceIds.length === 0) {
    return undefined
  }
  return {
    stageNo: item.stageNo,
    stageName: item.stageName,
    candidateSourceType: item.candidateSourceType,
    candidateSourceId: candidateSourceIds[0],
    candidateSourceIds,
    approveMethod: fixedPolicy.approveMethod,
    approveRatio: fixedPolicy.approveRatio ?? undefined,
    required: fixedPolicy.required,
    sort: item.sort
  }
}

const validateFixedRouteStageUniqueness = (nodes: ControlledFileApprovalRouteSaveReqVO['nodes']) => {
  const expectedStageNos = new Set<number>(currentExpectedStageNos.value)
  const seenStageNos = new Set<number>()
  if (nodes.length !== expectedStageNos.size) {
    message.warning(isLegacyRoute.value ? '固定四阶段审批路线要求每个阶段恰好一条' : '三动作审批路线要求每个阶段恰好一条')
    return false
  }
  for (const node of nodes) {
    if (!expectedStageNos.has(node.stageNo) || seenStageNos.has(node.stageNo)) {
      message.warning(isLegacyRoute.value ? '固定四阶段审批路线要求每个阶段恰好一条' : '三动作审批路线要求每个阶段恰好一条')
      return false
    }
    if (!isLegacyRoute.value && node.stageNo === 1 && node.candidateSourceType !== 'DEPT') {
      message.warning('三动作流程的会签节点只能选择部门负责人')
      return false
    }
    seenStageNos.add(node.stageNo)
  }
  return true
}

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) {
    return
  }
  if (!formData.value.categoryId) {
    message.warning('请选择文件类别')
    return
  }
  if (formData.value.nodes.length === 0) {
    message.warning('请至少新增一个审批节点')
    return
  }
  const invalidNode = formData.value.nodes.find(
    (item) =>
      !item.stageNo ||
      !item.stageName ||
      !item.candidateSourceType ||
      resolveCandidateSourceIds(item).length === 0 ||
      !item.approveMethod
  )
  if (invalidNode) {
    message.warning('请完善审批节点后再保存')
    return
  }
  const fixedNodes: ControlledFileApprovalRouteSaveReqVO['nodes'] = []
  for (const item of formData.value.nodes) {
    const fixedNode = normalizeRouteNodeFixedApprovalPolicy(item)
    if (!fixedNode) {
      message.warning('审批路线仅支持固定四阶段审批策略')
      return
    }
    fixedNodes.push(fixedNode)
  }
  if (fixedNodes.length !== formData.value.nodes.length) {
    message.warning('审批路线仅支持固定四阶段审批策略')
    return
  }
  if (!validateFixedRouteStageUniqueness(fixedNodes)) {
    return
  }
  formLoading.value = true
  try {
    await saveApprovalRoute(formData.value.categoryId, {
      actionType: formData.value.actionType as ControlledFileApprovalRouteActionType,
      effectiveTime: formData.value.effectiveTime,
      remark: formData.value.remark,
      nodes: fixedNodes
    })
    message.success(t('common.updateSuccess'))
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
