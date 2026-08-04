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
        <div class="text-13px font-600">审批节点</div>
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
              v-else
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
          </template>
        </el-table-column>
        <el-table-column label="审批方式" width="140">
          <template #default="{ row }">
            <el-select v-model="row.approveMethod" class="w-full" @change="handleApproveMethodChange(row)">
              <el-option
                v-for="item in ROUTE_APPROVE_METHOD_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="通过比例" width="110">
          <template #default="{ row }">
            <el-input-number
              v-model="row.approveRatio"
              :min="0"
              :max="100"
              :disabled="row.approveMethod !== 'ALL'"
              class="w-full"
            />
          </template>
        </el-table-column>
        <el-table-column label="必经" align="center" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.required" />
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
  type ControlledFileApprovalRouteNodeVO,
  type ControlledFileApprovalRouteVO
} from '@/api/dcc/controlledFile/approvalRoutes'
import type { ControlledFileApprovalPositionVO } from '@/api/dcc/controlledFile/approvalPositions'
import type { ControlledFileCategoryVO } from '@/api/dcc/controlledFile/fileCategories'
import type { UserVO } from '@/api/system/user'
import {
  ROUTE_APPROVE_METHOD_OPTIONS,
  ROUTE_CANDIDATE_SOURCE_OPTIONS
} from '../../shared/options'
import { formatDccSimpleUserLabel } from '../../shared/utils'
import { formatDateTimeValue } from '@/utils/formatTime'

defineOptions({ name: 'DccControlledFileRouteForm' })

type ControlledFileApprovalRouteFormVO = Omit<ControlledFileApprovalRouteVO, 'effectiveTime'> & {
  effectiveTime: string
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
const formData = ref<ControlledFileApprovalRouteFormVO>({
  categoryId: undefined,
  effectiveTime: '',
  remark: '',
  nodes: []
})

const formRules = reactive<FormRules>({
  categoryId: [{ required: true, message: '文件类别不能为空', trigger: 'change' }],
  effectiveTime: [{ required: true, message: '生效时间不能为空', trigger: 'change' }]
})

const emit = defineEmits<{
  success: []
}>()

const createDefaultNode = (index: number): ControlledFileApprovalRouteNodeVO => ({
  stageNo: index + 1,
  stageName: '',
  candidateSourceType: 'POSITION',
  candidateSourceId: 0,
  candidateSourceIds: [],
  approveMethod: 'ANY',
  approveRatio: undefined,
  required: true,
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
  resetForm()
  if (payload.route) {
    formData.value = {
      ...JSON.parse(JSON.stringify(payload.route)),
      categoryId: routeCategory?.id ?? payload.route.categoryId,
      effectiveTime: formatDateTimeValue(payload.route.effectiveTime, ''),
      nodes: payload.route.nodes.map((item) => ({
        ...JSON.parse(JSON.stringify(item)),
        candidateSourceIds: item.candidateSourceIds ?? (item.candidateSourceId ? [item.candidateSourceId] : [])
      }))
    }
  } else {
    formData.value.categoryId = routeCategory?.id
  }
  dialogTitle.value = payload.route
    ? `编辑路线 - ${currentCategory.value?.name || payload.route.categoryName || '-'}`
    : '新增路线'
  if (formData.value.nodes.length === 0) {
    addNode()
  }
}

defineExpose({ open })

const resetForm = () => {
  formData.value = {
    categoryId: undefined,
    effectiveTime: '',
    remark: '',
    nodes: []
  }
  formRef.value?.resetFields()
}

const addNode = () => {
  formData.value.nodes.push(createDefaultNode(formData.value.nodes.length))
}

const removeNode = (index: number) => {
  formData.value.nodes.splice(index, 1)
}

const handleSourceTypeChange = (row: ControlledFileApprovalRouteNodeVO) => {
  row.candidateSourceId = 0
  row.candidateSourceIds = []
}

const handleApproveMethodChange = (row: ControlledFileApprovalRouteNodeVO) => {
  row.approveRatio = row.approveMethod === 'ALL' ? 100 : undefined
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
      !item.candidateSourceId ||
      !item.approveMethod
  )
  if (invalidNode) {
    message.warning('请完善审批节点后再保存')
    return
  }
  formLoading.value = true
  try {
    await saveApprovalRoute(formData.value.categoryId, {
      effectiveTime: formData.value.effectiveTime,
      remark: formData.value.remark,
      nodes: formData.value.nodes.map((item) => ({
        stageNo: item.stageNo,
        stageName: item.stageName,
        candidateSourceType: item.candidateSourceType,
        candidateSourceId: item.candidateSourceId as number,
        approveMethod: item.approveMethod,
        approveRatio: item.approveRatio ?? undefined,
        required: item.required,
        sort: item.sort
      }))
    })
    message.success(t('common.updateSuccess'))
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
